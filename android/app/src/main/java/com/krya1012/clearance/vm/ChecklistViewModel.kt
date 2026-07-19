package com.krya1012.clearance.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.ActivityModuleDao
import com.krya1012.clearance.data.ActivityType
import com.krya1012.clearance.data.ChecklistItem
import com.krya1012.clearance.data.ChecklistItemDao
import com.krya1012.clearance.data.ChecklistType
import com.krya1012.clearance.data.PeriodicTask
import com.krya1012.clearance.data.PeriodicTaskStore
import com.krya1012.clearance.data.ScheduleStore
import com.krya1012.clearance.data.SeedData
import com.krya1012.clearance.data.TemplateEntry
import com.krya1012.clearance.data.Weekday
import com.krya1012.clearance.domain.ActivityGating
import com.krya1012.clearance.domain.AutoReset
import com.krya1012.clearance.domain.ChecklistSection
import com.krya1012.clearance.util.Haptics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * All reactive state + business logic for the checklist screen. Kotlin port
 * of iOS `ChecklistViewModel.swift`. Room's [ActivityModuleDao]/[ChecklistItemDao]
 * expose reactive `Flow`s directly, but [ScheduleStore]/[PeriodicTaskStore] are
 * one-shot `suspend` DataStore accessors with no `Flow` API — so schedule/
 * override/enabled-module/reset-hour state is held in this ViewModel's own
 * `MutableStateFlow`s, loaded once in [init] and kept in sync with DataStore
 * on every mutation, mirroring exactly the in-memory-cache-plus-persist
 * pattern the iOS ViewModel already uses over `UserDefaults`.
 */
class ChecklistViewModel(
    private val moduleDao: ActivityModuleDao,
    private val itemDao: ChecklistItemDao,
    private val scheduleStore: ScheduleStore,
    private val periodicTaskStore: PeriodicTaskStore,
    private val haptics: Haptics,
) : ViewModel() {

    private companion object {
        const val TAG = "ChecklistViewModel"
    }

    /** Snapshot of the three values that must always be computed together. */
    data class ActivitySnapshot(
        val todayActivityIds: Set<String>,
        val tomorrowActivityIds: Set<String>,
        val sections: List<ChecklistSection>,
    )

    private data class ModuleItemChecklist(
        val modules: List<ActivityModule>,
        val items: List<ChecklistItem>,
        val checklist: ChecklistType,
    )

    private data class ScheduleSnapshot(
        val enabledIds: Set<String>,
        val schedule: Map<Weekday, Set<String>>,
        val overridesMap: Map<String, Set<String>>,
    )

    val selectedChecklist = MutableStateFlow(ChecklistType.MORNING)

    val allModules: StateFlow<List<ActivityModule>> =
        moduleDao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allItems: StateFlow<List<ChecklistItem>> =
        itemDao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val enabledModuleIds = MutableStateFlow<Set<String>>(emptySet())
    private val weeklySchedule = MutableStateFlow<Map<Weekday, Set<String>>>(emptyMap())
    private val overrides = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val resetHour = MutableStateFlow(3)
    val periodicTasks = MutableStateFlow<List<PeriodicTask>>(emptyList())

    /** Bumped by [refresh] to force date-dependent (today/tomorrow) recomputation on resume. */
    private val refreshTick = MutableStateFlow(0)

    // Single combine producing today/tomorrow activity IDs and sections together, so a
    // toggle to enabledModuleIds/weeklySchedule/overrides can never be observed by one of
    // these derived values a tick before another (see ANDROID_PLAN.md's design-risk note).
    private val moduleItemChecklistFlow =
        combine(allModules, allItems, selectedChecklist) { m, i, c -> ModuleItemChecklist(m, i, c) }

    private val scheduleSnapshotFlow =
        combine(enabledModuleIds, weeklySchedule, overrides, refreshTick) { e, w, o, _ ->
            ScheduleSnapshot(e, w, o)
        }

    private val activitySnapshot: StateFlow<ActivitySnapshot> =
        moduleItemChecklistFlow.combine(scheduleSnapshotFlow) { mic, sched ->
            val now = Date()
            val tomorrow = dateOffset(1, now)
            val todayIds = ActivityGating.activitiesFor(
                weekday = Weekday.of(now),
                dateKey = scheduleStore.dateKey(now),
                overrides = sched.overridesMap,
                weeklySchedule = sched.schedule,
                enabledModuleIds = sched.enabledIds,
            )
            val tomorrowIds = ActivityGating.activitiesFor(
                weekday = Weekday.of(tomorrow),
                dateKey = scheduleStore.dateKey(tomorrow),
                overrides = sched.overridesMap,
                weeklySchedule = sched.schedule,
                enabledModuleIds = sched.enabledIds,
            )
            val builtSections = ActivityGating.buildSections(
                items = mic.items,
                modules = mic.modules,
                selectedChecklist = mic.checklist,
                enabledModuleIds = sched.enabledIds,
                todayActivityIds = todayIds,
                tomorrowActivityIds = tomorrowIds,
            )
            ActivitySnapshot(todayIds, tomorrowIds, builtSections)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ActivitySnapshot(emptySet(), emptySet(), emptyList()))

    val todayActivityIds: StateFlow<Set<String>> =
        activitySnapshot.map { it.todayActivityIds }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val tomorrowActivityIds: StateFlow<Set<String>> =
        activitySnapshot.map { it.tomorrowActivityIds }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val sections: StateFlow<List<ChecklistSection>> =
        activitySnapshot.map { it.sections }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totalActiveCount: StateFlow<Int> = sections.map { activeItemsOf(it).size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val completedCount: StateFlow<Int> = sections.map { activeItemsOf(it).count { item -> item.isCompleted } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val progress: StateFlow<Double> = combine(completedCount, totalActiveCount) { completed, total ->
        if (total > 0) completed.toDouble() / total else 0.0
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    init {
        viewModelScope.launch {
            if (scheduleStore.isOverridesDataCorrupted()) {
                Log.w(TAG, "Persisted activity overrides are corrupted; falling back to empty.")
            }
            overrides.value = scheduleStore.loadOverrides()
            resetHour.value = scheduleStore.loadResetHour()

            // Gate on allModules' first non-empty (seeded) emission -- Room's observeAll()
            // Flow may not have emitted yet here, and reconciling against an empty snapshot
            // would compute optionalIds = emptySet() and disable every module.
            val modules = allModules.first { it.isNotEmpty() }

            if (scheduleStore.isScheduleDataCorrupted()) {
                Log.w(TAG, "Persisted weekly schedule is corrupted; falling back to empty.")
            }
            weeklySchedule.value = scheduleStore.loadSchedule()

            if (scheduleStore.isEnabledModuleIDsDataCorrupted()) {
                Log.w(TAG, "Persisted enabled-module IDs are corrupted; falling back to all optional modules enabled.")
            }
            val optionalIds = modules.filter { it.isOptional }.map { it.id }.toSet()
            val saved = scheduleStore.loadEnabledModuleIDs()
            val reconciled = ActivityGating.reconcileEnabledModuleIds(saved, optionalIds)
            enabledModuleIds.value = reconciled
            scheduleStore.saveEnabledModuleIDs(reconciled)

            periodicTaskStore.seedDefaultsIfNeeded()
            periodicTasks.value = periodicTaskStore.load()

            haptics.prepare()
        }
    }

    private fun activeItemsOf(sections: List<ChecklistSection>): List<ChecklistItem> =
        sections.flatMap { it.phases }.flatMap { it.items }.filterNot { it.isSkipped }

    private fun isComplete(items: List<ChecklistItem>): Boolean =
        items.isNotEmpty() && items.all { it.isCompleted }

    private fun dateOffset(days: Int, from: Date): Date {
        val cal = Calendar.getInstance()
        cal.time = from
        cal.add(Calendar.DAY_OF_YEAR, days)
        return cal.time
    }

    private fun retainedDateKeys(): Set<String> {
        val now = Date()
        return setOf(-1, 0, 1).map { scheduleStore.dateKey(dateOffset(it, now)) }.toSet()
    }

    // MARK: - Task mutations

    fun toggle(item: ChecklistItem) = viewModelScope.launch {
        val beforeActive = activeItemsOf(activitySnapshot.value.sections)
        val wasComplete = beforeActive.isNotEmpty() && isComplete(beforeActive)

        val newCompleted = !item.isCompleted
        val updated = item.copy(isCompleted = newCompleted, isSkipped = if (newCompleted) false else item.isSkipped)
        itemDao.update(updated)
        haptics.taskToggled(newCompleted)

        // Rebuild rather than map-in-place: an item that was previously skipped (and so
        // absent from beforeActive) becomes active here if completing it un-skipped it.
        val afterActive = beforeActive.filterNot { it.id == updated.id } +
            if (!updated.isSkipped) listOf(updated) else emptyList()
        val nowComplete = afterActive.isNotEmpty() && isComplete(afterActive)
        if (!wasComplete && nowComplete) haptics.checklistCompleted()
    }

    fun setSkipped(item: ChecklistItem, skipped: Boolean) = viewModelScope.launch {
        val updated = item.copy(isSkipped = skipped, isCompleted = if (skipped) false else item.isCompleted)
        itemDao.update(updated)
        haptics.skipped()
    }

    fun skip(item: ChecklistItem) = setSkipped(item, skipped = true)
    fun restore(item: ChecklistItem) = setSkipped(item, skipped = false)

    fun resetAll(silent: Boolean = false) = viewModelScope.launch { resetAllInternal(silent) }

    private suspend fun resetAllInternal(silent: Boolean) {
        val items = itemDao.getAll()
        items.filter { it.isCompleted || it.isSkipped }
            .forEach { itemDao.update(it.copy(isCompleted = false, isSkipped = false)) }
        if (!silent) haptics.reset()
    }

    // MARK: - Auto-reset

    fun refresh() = viewModelScope.launch {
        checkAutoReset()
        periodicTasks.value = periodicTaskStore.load()
        refreshTick.value += 1
    }

    private suspend fun checkAutoReset() {
        val now = System.currentTimeMillis()
        val last = scheduleStore.loadLastAutoReset()
        if (AutoReset.shouldAutoReset(last, now, resetHour.value)) {
            resetAllInternal(silent = true)
            scheduleStore.saveLastAutoReset(now)
        }
    }

    fun setResetHour(hour: Int) = viewModelScope.launch {
        resetHour.value = hour
        scheduleStore.saveResetHour(hour)
    }

    // MARK: - Activity gating mutations

    fun toggleModuleEnabled(module: ActivityModule) = viewModelScope.launch {
        if (!module.isOptional) return@launch
        val updated = enabledModuleIds.value.let { if (module.id in it) it - module.id else it + module.id }
        enabledModuleIds.value = updated
        scheduleStore.saveEnabledModuleIDs(updated)
        haptics.moduleToggled()
    }

    fun toggleScheduleActivity(module: ActivityModule, day: Weekday) = viewModelScope.launch {
        if (!module.isOptional) return@launch
        val current = weeklySchedule.value[day] ?: emptySet()
        val updated = if (module.id in current) current - module.id else current + module.id
        val newSchedule = weeklySchedule.value + (day to updated)
        weeklySchedule.value = newSchedule
        scheduleStore.saveSchedule(newSchedule)
        haptics.moduleToggled()
    }

    fun toggleTodayActivity(module: ActivityModule) = toggleActivity(module, daysFromNow = 0)
    fun toggleTomorrowActivity(module: ActivityModule) = toggleActivity(module, daysFromNow = 1)

    private fun toggleActivity(module: ActivityModule, daysFromNow: Int) = viewModelScope.launch {
        if (!module.isOptional) return@launch
        val date = dateOffset(daysFromNow, Date())
        val key = scheduleStore.dateKey(date)
        val weekday = Weekday.of(date)
        val baseline = overrides.value[key] ?: weeklySchedule.value[weekday] ?: emptySet()
        val updated = if (module.id in baseline) baseline - module.id else baseline + module.id
        val newOverrides = overrides.value + (key to updated)
        val pruned = ActivityGating.pruneOverrideKeys(newOverrides, retainedDateKeys())
        overrides.value = pruned
        scheduleStore.saveOverrides(pruned)
        haptics.moduleToggled()
    }

    // MARK: - Module management

    fun moveModule(fromIndex: Int, toIndex: Int) = moveWithinOptional(fromIndex, toIndex) { it.isOptional }
    fun moveUnlockedModule(fromIndex: Int, toIndex: Int) =
        moveWithinOptional(fromIndex, toIndex) { it.isOptional && !it.isLocked }

    private fun moveWithinOptional(
        fromIndex: Int,
        toIndex: Int,
        predicate: (ActivityModule) -> Boolean,
    ) = viewModelScope.launch {
        val subset = allModules.value.filter(predicate).sortedBy { it.sortOrder }.toMutableList()
        if (fromIndex !in subset.indices || toIndex !in subset.indices) return@launch
        val moved = subset.removeAt(fromIndex)
        subset.add(toIndex, moved)
        subset.forEachIndexed { idx, module -> moduleDao.update(module.copy(sortOrder = idx + 1)) }
    }

    fun deleteModule(module: ActivityModule) = viewModelScope.launch {
        if (!module.isOptional || module.isLocked) return@launch

        val newEnabled = enabledModuleIds.value - module.id
        enabledModuleIds.value = newEnabled
        scheduleStore.saveEnabledModuleIDs(newEnabled)

        val newSchedule = weeklySchedule.value.mapValues { it.value - module.id }
        weeklySchedule.value = newSchedule
        scheduleStore.saveSchedule(newSchedule)

        val newOverrides = ActivityGating.pruneOverrideKeys(
            overrides.value.mapValues { it.value - module.id },
            retainedDateKeys(),
        )
        overrides.value = newOverrides
        scheduleStore.saveOverrides(newOverrides)

        itemDao.deleteByModule(module.id)
        moduleDao.delete(module)
    }

    fun addModule(name: String, emoji: String, activityType: ActivityType = ActivityType.SPORT) = viewModelScope.launch {
        val sortOrder = (moduleDao.getAll().maxOfOrNull { it.sortOrder } ?: 0) + 1
        val module = ActivityModule(name = name, emoji = emoji, sortOrder = sortOrder, activityTypeRaw = activityType.name)
        moduleDao.insert(module)
        val updatedEnabled = enabledModuleIds.value + module.id
        enabledModuleIds.value = updatedEnabled
        scheduleStore.saveEnabledModuleIDs(updatedEnabled)
        haptics.moduleToggled()
    }

    fun installTemplate(entry: TemplateEntry) = viewModelScope.launch {
        if (allModules.value.any { it.name == entry.name }) return@launch
        val sortOrder = (moduleDao.getAll().maxOfOrNull { it.sortOrder } ?: 0) + 1
        val module = ActivityModule(
            name = entry.name,
            emoji = entry.emoji,
            sortOrder = sortOrder,
            activityTypeRaw = entry.activityType.name,
        )
        moduleDao.insert(module)
        val updatedEnabled = enabledModuleIds.value + module.id
        enabledModuleIds.value = updatedEnabled
        scheduleStore.saveEnabledModuleIDs(updatedEnabled)

        val resolvedModules = moduleDao.getAll()
        val defaults = SeedData.defaultItems(resolvedModules).filter { it.associatedModule == module.id }
        itemDao.insertAll(defaults)
        haptics.moduleToggled()
    }

    fun updateModule(module: ActivityModule, name: String, emoji: String) = viewModelScope.launch {
        if (!module.isOptional) return@launch
        moduleDao.update(module.copy(name = name, emoji = emoji))
    }

    fun restoreDefaultTasks(module: ActivityModule) = viewModelScope.launch {
        itemDao.deleteByModule(module.id)
        itemDao.insertAll(SeedData.defaultItems(module, allModules.value))
        haptics.reset()
    }

    fun hasDefaultTasks(module: ActivityModule): Boolean =
        SeedData.defaultItems(module, allModules.value).isNotEmpty()

    // MARK: - Item CRUD

    fun addItem(title: String, checklist: ChecklistType, module: ActivityModule, phase: String) = viewModelScope.launch {
        val group = itemDao.getAll().filter { it.associatedChecklist == checklist && it.associatedModule == module.id }
        val matching = group.firstOrNull { it.phase == phase }
        val phaseIndex = matching?.phaseIndex ?: ((group.maxOfOrNull { it.phaseIndex } ?: -1) + 1)
        val orderIndex = (group.filter { it.phase == phase }.maxOfOrNull { it.orderIndex } ?: -1) + 1
        itemDao.insert(
            ChecklistItem(
                title = title,
                orderIndex = orderIndex,
                phase = phase,
                phaseIndex = phaseIndex,
                associatedModule = module.id,
                associatedChecklist = checklist,
            ),
        )
    }

    fun updateItem(item: ChecklistItem, title: String, phase: String? = null) = viewModelScope.launch {
        var updated = item.copy(title = title)
        if (phase != null && phase != item.phase) {
            val sibling = itemDao.getAll().firstOrNull {
                it.id != item.id &&
                    it.associatedChecklist == item.associatedChecklist &&
                    it.associatedModule == item.associatedModule &&
                    it.phase == phase
            }
            updated = updated.copy(phase = phase, phaseIndex = sibling?.phaseIndex ?: item.phaseIndex)
        }
        itemDao.update(updated)
    }

    fun moveItems(module: ActivityModule, checklist: ChecklistType, phase: String, fromIndex: Int, toIndex: Int) =
        viewModelScope.launch {
            val phaseItems = itemDao.getAll()
                .filter { it.associatedModule == module.id && it.associatedChecklist == checklist && it.phase == phase }
                .sortedBy { it.orderIndex }
                .toMutableList()
            if (fromIndex !in phaseItems.indices || toIndex !in phaseItems.indices) return@launch
            val moved = phaseItems.removeAt(fromIndex)
            phaseItems.add(toIndex, moved)
            phaseItems.forEachIndexed { idx, item -> itemDao.update(item.copy(orderIndex = idx)) }
        }

    fun availablePhases(module: ActivityModule, checklist: ChecklistType): List<Pair<String, Int>> =
        allItems.value
            .filter { it.associatedModule == module.id && it.associatedChecklist == checklist }
            .sortedBy { it.phaseIndex }
            .distinctBy { it.phase }
            .map { it.phase to it.phaseIndex }

    // MARK: - Periodic tasks

    fun togglePeriodicTask(task: PeriodicTask) = viewModelScope.launch {
        val updated = periodicTasks.value.map {
            if (it.id == task.id) {
                it.copy(
                    isCompleted = !it.isCompleted,
                    completedDateMillis = if (!it.isCompleted) System.currentTimeMillis() else null,
                )
            } else {
                it
            }
        }
        periodicTasks.value = updated
        periodicTaskStore.save(updated)
    }
}

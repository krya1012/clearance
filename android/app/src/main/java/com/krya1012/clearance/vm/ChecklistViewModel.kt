package com.krya1012.clearance.vm

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.krya1012.clearance.ClearanceApplication
import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.ActivityModuleDao
import com.krya1012.clearance.data.ActivityType
import com.krya1012.clearance.data.ChecklistItem
import com.krya1012.clearance.data.ChecklistItemDao
import com.krya1012.clearance.data.ChecklistType
import com.krya1012.clearance.data.PeriodicTask
import com.krya1012.clearance.data.PeriodicTaskStore
import com.krya1012.clearance.data.Recurrence
import com.krya1012.clearance.data.ScheduleStore
import com.krya1012.clearance.data.SeedData
import com.krya1012.clearance.data.TemplateEntry
import com.krya1012.clearance.data.Weekday
import com.krya1012.clearance.domain.ActivityGating
import com.krya1012.clearance.domain.AutoReset
import java.util.Calendar
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ChecklistViewModel"

/** One named phase within a module section, e.g. "Systems Launch". */
data class ChecklistPhase(
    val name: String,
    val phaseIndex: Int,
    val items: List<ChecklistItem>,
)

/** One module's worth of content for the selected checklist, grouped into phases. */
data class ChecklistSection(
    val module: ActivityModule,
    val phases: List<ChecklistPhase>,
) {
    val allItems: List<ChecklistItem> get() = phases.flatMap { it.items }
}

/**
 * Everything derived from one atomic snapshot of (modules, items, selectedChecklist,
 * enabledModuleIDs, weeklySchedule, activityOverrides, now) — see [computeGated]. Every
 * public output StateFlow projects one field of this so all six always reflect the exact
 * same computation, never a partially-updated mix.
 */
internal data class Gated(
    val today: Set<String>,
    val tomorrow: Set<String>,
    val sections: List<ChecklistSection>,
    val completedCount: Int,
    val totalActiveCount: Int,
    val progress: Double,
) {
    companion object {
        val EMPTY = Gated(
            today = emptySet(),
            tomorrow = emptySet(),
            sections = emptyList(),
            completedCount = 0,
            totalActiveCount = 0,
            progress = 0.0,
        )
    }
}

/**
 * Pure, non-suspend gating + grouping computation — the Kotlin analog of iOS
 * `ChecklistViewModel.sections`/`todayActivityIDs`/`tomorrowActivityIDs`/`progress`. Delegates
 * the actual gating rules to [ActivityGating], which is already unit-tested; this function's
 * own job is resolving today/tomorrow, filtering items, and grouping into sections/phases.
 */
internal fun computeGated(
    modules: List<ActivityModule>,
    items: List<ChecklistItem>,
    checklist: ChecklistType,
    enabledIds: Set<String>,
    schedule: Map<Weekday, Set<String>>,
    overrides: Map<String, Set<String>>,
    todayWeekday: Weekday,
    todayKey: String,
    tomorrowWeekday: Weekday,
    tomorrowKey: String,
): Gated {
    val today = ActivityGating.activitiesFor(todayWeekday, todayKey, overrides, schedule, enabledIds)
    val tomorrow = ActivityGating.activitiesFor(tomorrowWeekday, tomorrowKey, overrides, schedule, enabledIds)

    val moduleById = modules.associateBy { it.id }

    val visibleItems = items.filter { item ->
        if (item.associatedChecklist != checklist) return@filter false
        val module = moduleById[item.associatedModule] ?: return@filter false
        if (!ActivityGating.isModuleVisible(module, enabledIds)) return@filter false
        val role = ActivityGating.roleOf(module, checklist, item.phaseIndex)
        ActivityGating.isVisible(role, module.id, today, tomorrow)
    }

    val sections = visibleItems
        .groupBy { it.associatedModule }
        .mapNotNull { (moduleId, moduleItems) ->
            val module = moduleById[moduleId] ?: return@mapNotNull null
            val phases = moduleItems
                .groupBy { it.phase }
                .map { (phaseName, phaseItems) ->
                    ChecklistPhase(
                        name = phaseName,
                        phaseIndex = phaseItems.first().phaseIndex,
                        items = phaseItems.sortedBy { it.orderIndex },
                    )
                }
                .sortedBy { it.phaseIndex }
            ChecklistSection(module = module, phases = phases)
        }
        .sortedBy { it.module.sortOrder }

    val activeItems = sections.flatMap { it.allItems }.filterNot { it.isSkipped }
    val completedCount = activeItems.count { it.isCompleted }
    val totalActiveCount = activeItems.size
    val progress = if (totalActiveCount > 0) completedCount.toDouble() / totalActiveCount else 0.0

    return Gated(today, tomorrow, sections, completedCount, totalActiveCount, progress)
}

/** True if a module with this template's name already exists (matched by name, like iOS). */
internal fun computeTemplateInstalled(entry: TemplateEntry, modules: List<ActivityModule>): Boolean =
    modules.any { it.name == entry.name }

/**
 * Existing distinct phase names (with their phaseIndex) for a module/checklist pair, ordered
 * by phaseIndex — feeds the item editor's "reuse an existing phase" chip picker.
 */
internal fun computeAvailablePhases(
    items: List<ChecklistItem>,
    moduleId: String,
    checklist: ChecklistType,
): List<Pair<String, Int>> =
    items
        .filter { it.associatedModule == moduleId && it.associatedChecklist == checklist }
        .sortedBy { it.phaseIndex }
        .distinctBy { it.phase }
        .map { it.phase to it.phaseIndex }

/**
 * Reconciles a saved `enabledModuleIDs` set against the modules that actually exist:
 * `null` (never written) enables every optional module; a saved set that's entirely stale
 * (references only deleted/renamed modules) falls back to all-enabled rather than leaving
 * every module disabled; otherwise the valid subset survives.
 */
internal fun reconcileEnabledModuleIDs(saved: Set<String>?, optionalIds: Set<String>): Set<String> {
    if (saved == null) return optionalIds
    val validIds = saved.intersect(optionalIds)
    return if (validIds.isEmpty() && optionalIds.isNotEmpty()) optionalIds else validIds
}

/**
 * The single source of truth for the dashboard — Kotlin analog of iOS
 * `ChecklistViewModel.swift`. `ScheduleStore` exposes only one-shot `suspend` reads, so this
 * class owns `MutableStateFlow`s for schedule-derived state and refreshes them after every
 * mutating call.
 */
class ChecklistViewModel(
    private val moduleDao: ActivityModuleDao,
    private val itemDao: ChecklistItemDao,
    private val scheduleStore: ScheduleStore,
    private val periodicTaskStore: PeriodicTaskStore,
) : ViewModel() {

    // MARK: - Observed state

    val selectedChecklist = MutableStateFlow(ChecklistType.MORNING)

    val allModules: StateFlow<List<ActivityModule>> =
        moduleDao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allItems: StateFlow<List<ChecklistItem>> =
        itemDao.observeAll().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _enabledModuleIDs = MutableStateFlow<Set<String>>(emptySet())
    val enabledModuleIDs: StateFlow<Set<String>> = _enabledModuleIDs.asStateFlow()

    /** Flips true once [init]'s stale-UUID reconciliation has published a real value — see [gated]. */
    private val isEnabledModuleIDsReady = MutableStateFlow(false)

    private val _weeklySchedule = MutableStateFlow<Map<Weekday, Set<String>>>(emptyMap())
    val weeklySchedule: StateFlow<Map<Weekday, Set<String>>> = _weeklySchedule.asStateFlow()

    private val _activityOverrides = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val activityOverrides: StateFlow<Map<String, Set<String>>> = _activityOverrides.asStateFlow()

    private val _resetHour = MutableStateFlow(3)
    val resetHour: StateFlow<Int> = _resetHour.asStateFlow()

    private val _periodicTasks = MutableStateFlow<List<PeriodicTask>>(emptyList())
    val periodicTasks: StateFlow<List<PeriodicTask>> = _periodicTasks.asStateFlow()

    val weeklyTasks: StateFlow<List<PeriodicTask>> =
        periodicTasks.map { list -> list.filter { it.recurrence == Recurrence.WEEKLY } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val monthlyTasks: StateFlow<List<PeriodicTask>> =
        periodicTasks.map { list -> list.filter { it.recurrence == Recurrence.MONTHLY } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Bumped by [refresh] so date-rollover (today/tomorrow) is picked up without polling. */
    private val now = MutableStateFlow(System.currentTimeMillis())

    // MARK: - Derived (single combine — see ANDROID_PLAN.md's design-risk note)

    // kotlinx.coroutines' typed `combine` tops out at 5 flows; the inner combine below only
    // pre-packs 3 independent inputs into a Triple to work around that arity limit. All gating
    // math still happens in exactly one terminal lambda, so every output below is a projection
    // of one atomic Gated value — never recombined from another already-published StateFlow.
    private val gated: StateFlow<Gated> = combine(
        allModules,
        allItems,
        selectedChecklist,
        combine(enabledModuleIDs, isEnabledModuleIDsReady, ::Pair),
        combine(weeklySchedule, activityOverrides, now, ::Triple),
    ) { modules, items, checklist, (enabledIds, enabledIdsReady), (schedule, overrides, nowMillis) ->
        // Mirrors the allModules.first { it.isNotEmpty() } gate in init{} below: until that
        // coroutine's enabledModuleIDs reconciliation has published its first real value,
        // computing sections against the emptySet() default would incorrectly hide every
        // optional module's items — hold at Gated.EMPTY instead of flashing a Core-only
        // checklist. Do not remove this gate without also proving enabledModuleIDs can no
        // longer start at a stale/default value.
        if (!enabledIdsReady) return@combine Gated.EMPTY
        val today = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        computeGated(
            modules = modules,
            items = items,
            checklist = checklist,
            enabledIds = enabledIds,
            schedule = schedule,
            overrides = overrides,
            todayWeekday = Weekday.of(today.time, today),
            todayKey = scheduleStore.dateKey(today.time),
            tomorrowWeekday = Weekday.of(tomorrow.time, tomorrow),
            tomorrowKey = scheduleStore.dateKey(tomorrow.time),
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, Gated.EMPTY)

    val todayActivityIDs: StateFlow<Set<String>> =
        gated.map { it.today }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val tomorrowActivityIDs: StateFlow<Set<String>> =
        gated.map { it.tomorrow }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val sections: StateFlow<List<ChecklistSection>> =
        gated.map { it.sections }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val completedCount: StateFlow<Int> =
        gated.map { it.completedCount }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val totalActiveCount: StateFlow<Int> =
        gated.map { it.totalActiveCount }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    val progress: StateFlow<Double> =
        gated.map { it.progress }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    init {
        viewModelScope.launch {
            if (scheduleStore.isOverridesDataCorrupted()) {
                Log.w(TAG, "activityOverrides blob is corrupted — treating as empty")
            }
            _activityOverrides.value = scheduleStore.loadOverrides()

            if (scheduleStore.isScheduleDataCorrupted()) {
                Log.w(TAG, "weeklySchedule blob is corrupted — treating as empty")
            }
            _weeklySchedule.value = scheduleStore.loadSchedule()

            _resetHour.value = scheduleStore.loadResetHour()
            _periodicTasks.value = periodicTaskStore.load()

            // Gate reconciliation on the Flow's first seeded (non-empty) emission, not
            // `allModules.value` — Room's observeAll() may not have emitted yet at this point,
            // and reading an empty snapshot would compute optionalIds = emptySet() and disable
            // every module.
            val modules = allModules.first { it.isNotEmpty() }
            val optionalIds = modules.filter { it.isOptional }.map { it.id }.toSet()

            if (scheduleStore.isEnabledModuleIDsDataCorrupted()) {
                Log.w(TAG, "enabledModules blob is corrupted — falling back to all-enabled")
            }
            val reconciled = reconcileEnabledModuleIDs(scheduleStore.loadEnabledModuleIDs(), optionalIds)
            _enabledModuleIDs.value = reconciled
            isEnabledModuleIDsReady.value = true
            scheduleStore.saveEnabledModuleIDs(reconciled)
        }
    }

    // MARK: - Activities (today / tomorrow)

    /** Call on every foreground resume (`Lifecycle.Event.ON_RESUME`). */
    fun refresh() {
        now.value = System.currentTimeMillis()
        viewModelScope.launch {
            checkAutoReset()
            _periodicTasks.value = periodicTaskStore.load()
        }
    }

    private suspend fun checkAutoReset() {
        val nowMillis = now.value
        val last = scheduleStore.loadLastAutoReset()
        if (AutoReset.shouldAutoReset(last, nowMillis, resetHour.value)) {
            resetAll(silent = true)
            scheduleStore.saveLastAutoReset(nowMillis)
        }
    }

    fun toggleTodayActivity(module: ActivityModule) {
        toggleActivity(module, Date(now.value))
    }

    fun toggleTomorrowActivity(module: ActivityModule) {
        val tomorrow = Calendar.getInstance().apply {
            timeInMillis = now.value
            add(Calendar.DAY_OF_YEAR, 1)
        }.time
        toggleActivity(module, tomorrow)
    }

    private fun toggleActivity(module: ActivityModule, date: Date) {
        if (!module.isOptional) return
        viewModelScope.launch {
            val key = scheduleStore.dateKey(date)
            val weekday = Weekday.of(date)
            val current = activityOverrides.value[key] ?: weeklySchedule.value[weekday] ?: emptySet()
            val toggled = if (module.id in current) current - module.id else current + module.id
            _activityOverrides.update { it + (key to toggled) }
            pruneOverrides()
            scheduleStore.saveOverrides(activityOverrides.value)
        }
    }

    /** Keeps only yesterday/today/tomorrow override keys — mirrors iOS `pruneOverrides()`. */
    private fun pruneOverrides() {
        val nowMillis = now.value
        val keep = listOf(-1, 0, 1)
            .map { offset ->
                Calendar.getInstance().apply { timeInMillis = nowMillis; add(Calendar.DAY_OF_YEAR, offset) }
            }
            .map { scheduleStore.dateKey(it.time) }
            .toSet()
        _activityOverrides.update { it.filterKeys { key -> key in keep } }
    }

    // MARK: - Enabled modules

    fun toggleModuleEnabled(module: ActivityModule) {
        if (!module.isOptional) return
        viewModelScope.launch {
            _enabledModuleIDs.update { if (module.id in it) it - module.id else it + module.id }
            scheduleStore.saveEnabledModuleIDs(enabledModuleIDs.value)
        }
    }

    // MARK: - Weekly schedule

    fun toggleScheduleActivity(module: ActivityModule, day: Weekday) {
        if (!module.isOptional) return
        viewModelScope.launch {
            _weeklySchedule.update { schedule ->
                val current = schedule[day] ?: emptySet()
                val toggled = if (module.id in current) current - module.id else current + module.id
                schedule + (day to toggled)
            }
            scheduleStore.saveSchedule(weeklySchedule.value)
        }
    }

    fun setResetHour(hour: Int) {
        viewModelScope.launch {
            _resetHour.value = hour
            scheduleStore.saveResetHour(hour)
        }
    }

    // MARK: - Module management

    fun addModule(name: String, emoji: String, activityType: ActivityType = ActivityType.SPORT) {
        viewModelScope.launch {
            val sortOrder = (allModules.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val module = ActivityModule(
                name = name,
                emoji = emoji,
                sortOrder = sortOrder,
                activityTypeRaw = activityType.name,
            )
            moduleDao.insert(module)
            _enabledModuleIDs.update { it + module.id }
            scheduleStore.saveEnabledModuleIDs(enabledModuleIDs.value)
        }
    }

    fun updateModule(module: ActivityModule, name: String, emoji: String) {
        viewModelScope.launch { moduleDao.update(module.copy(name = name, emoji = emoji)) }
    }

    fun hasDefaultTasks(module: ActivityModule): Boolean =
        SeedData.defaultItems(module, allModules.value).isNotEmpty()

    fun restoreDefaultTasks(module: ActivityModule) {
        viewModelScope.launch {
            itemDao.deleteByModule(module.id)
            SeedData.defaultItems(module, allModules.value).forEach { itemDao.insert(it) }
        }
    }

    fun deleteModule(module: ActivityModule) {
        if (!module.isOptional || module.isLocked) return
        viewModelScope.launch {
            _enabledModuleIDs.update { it - module.id }
            _weeklySchedule.update { schedule -> schedule.mapValues { it.value - module.id } }
            _activityOverrides.update { it.mapValues { entry -> entry.value - module.id } }
            pruneOverrides()
            scheduleStore.saveEnabledModuleIDs(enabledModuleIDs.value)
            scheduleStore.saveSchedule(weeklySchedule.value)
            scheduleStore.saveOverrides(activityOverrides.value)
            // Query items directly rather than filtering the (possibly stale) allItems cache.
            itemDao.deleteByModule(module.id)
            moduleDao.delete(module)
        }
    }

    fun moveModule(from: Int, to: Int) {
        viewModelScope.launch {
            val mutable = allModules.value.filter { it.isOptional }.toMutableList()
            val moved = mutable.removeAt(from)
            mutable.add(to, moved)
            mutable.forEachIndexed { index, module -> moduleDao.update(module.copy(sortOrder = index + 1)) }
        }
    }

    fun moveUnlockedModule(from: Int, to: Int) {
        viewModelScope.launch {
            val mutable = allModules.value.filter { it.isOptional && !it.isLocked }.toMutableList()
            val moved = mutable.removeAt(from)
            mutable.add(to, moved)
            mutable.forEachIndexed { index, module -> moduleDao.update(module.copy(sortOrder = index + 1)) }
        }
    }

    fun isTemplateInstalled(entry: TemplateEntry): Boolean =
        computeTemplateInstalled(entry, allModules.value)

    /**
     * Adds a module from the template catalog and seeds its canonical default tasks —
     * unlike a plain [addModule], which creates an empty module with no tasks. No-ops if a
     * module with this name already exists.
     */
    fun installTemplate(entry: TemplateEntry) {
        if (isTemplateInstalled(entry)) return
        viewModelScope.launch {
            val sortOrder = (allModules.value.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val module = ActivityModule(
                name = entry.name,
                emoji = entry.emoji,
                sortOrder = sortOrder,
                activityTypeRaw = entry.activityType.name,
            )
            moduleDao.insert(module)
            _enabledModuleIDs.update { it + module.id }
            scheduleStore.saveEnabledModuleIDs(enabledModuleIDs.value)

            val resolvedModules = moduleDao.getAll()
            SeedData.defaultItems(module, resolvedModules).forEach { itemDao.insert(it) }
        }
    }

    fun availablePhases(module: ActivityModule, checklist: ChecklistType): List<Pair<String, Int>> =
        computeAvailablePhases(allItems.value, module.id, checklist)

    // MARK: - Item CRUD

    fun addItem(title: String, phase: String, module: ActivityModule, checklist: ChecklistType) {
        viewModelScope.launch {
            val groupItems = allItems.value.filter {
                it.associatedChecklist == checklist && it.associatedModule == module.id
            }
            val phaseIndex = groupItems.firstOrNull { it.phase == phase }?.phaseIndex
                ?: ((groupItems.maxOfOrNull { it.phaseIndex } ?: -1) + 1)
            val orderIndex = (groupItems.filter { it.phase == phase }.maxOfOrNull { it.orderIndex } ?: -1) + 1
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
    }

    fun updateItem(item: ChecklistItem, title: String, phase: String? = null) {
        viewModelScope.launch {
            var updated = item.copy(title = title)
            if (phase != null) {
                val groupItems = allItems.value.filter {
                    it.associatedChecklist == item.associatedChecklist &&
                        it.associatedModule == item.associatedModule &&
                        it.id != item.id
                }
                val matchedPhaseIndex = groupItems.firstOrNull { it.phase == phase }?.phaseIndex
                updated = updated.copy(phase = phase, phaseIndex = matchedPhaseIndex ?: updated.phaseIndex)
            }
            itemDao.update(updated)
        }
    }

    fun deleteItem(item: ChecklistItem) {
        viewModelScope.launch { itemDao.delete(item) }
    }

    fun moveItems(phase: ChecklistPhase, from: Int, to: Int) {
        viewModelScope.launch {
            val mutable = phase.items.toMutableList()
            val moved = mutable.removeAt(from)
            mutable.add(to, moved)
            mutable.forEachIndexed { index, item -> itemDao.update(item.copy(orderIndex = index)) }
        }
    }

    // MARK: - Toggle / skip / reset

    fun toggle(item: ChecklistItem) {
        viewModelScope.launch {
            itemDao.update(
                item.copy(
                    isCompleted = !item.isCompleted,
                    isSkipped = if (!item.isCompleted) false else item.isSkipped,
                ),
            )
        }
    }

    fun setSkipped(item: ChecklistItem, skipped: Boolean) {
        viewModelScope.launch {
            itemDao.update(
                item.copy(
                    isSkipped = skipped,
                    isCompleted = if (skipped) false else item.isCompleted,
                ),
            )
        }
    }

    fun skip(item: ChecklistItem) = setSkipped(item, skipped = true)
    fun restore(item: ChecklistItem) = setSkipped(item, skipped = false)

    /** [silent] currently has no effect (no haptics utility exists yet); kept for Milestone 6. */
    fun resetAll(silent: Boolean = false) {
        viewModelScope.launch {
            allItems.value.forEach { itemDao.update(it.copy(isCompleted = false, isSkipped = false)) }
        }
    }

    // MARK: - Periodic tasks (weekly / monthly)

    fun togglePeriodicTask(task: PeriodicTask) {
        viewModelScope.launch {
            _periodicTasks.update { list ->
                list.map {
                    if (it.id == task.id) {
                        val nowCompleted = !it.isCompleted
                        it.copy(
                            isCompleted = nowCompleted,
                            completedDateMillis = if (nowCompleted) System.currentTimeMillis() else null,
                        )
                    } else it
                }
            }
            periodicTaskStore.save(periodicTasks.value)
        }
    }

    fun addPeriodicTask(title: String, emoji: String = "📋", recurrence: Recurrence) {
        viewModelScope.launch {
            _periodicTasks.update { it + PeriodicTask(title = title, emoji = emoji, recurrence = recurrence) }
            periodicTaskStore.save(periodicTasks.value)
        }
    }

    fun deletePeriodicTask(task: PeriodicTask) {
        viewModelScope.launch {
            _periodicTasks.update { list -> list.filterNot { it.id == task.id } }
            periodicTaskStore.save(periodicTasks.value)
        }
    }

    // MARK: - Construction

    class Factory(private val application: ClearanceApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChecklistViewModel(
                moduleDao = application.database.moduleDao(),
                itemDao = application.database.itemDao(),
                scheduleStore = application.scheduleStore,
                periodicTaskStore = application.periodicTaskStore,
            ) as T
        }
    }
}

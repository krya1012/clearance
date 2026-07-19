package com.krya1012.clearance.domain

import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.ChecklistItem
import com.krya1012.clearance.data.ChecklistType
import com.krya1012.clearance.data.Weekday

/** A named sub-group of items within a module section, e.g. "Systems Launch". */
data class ChecklistPhase(
    val name: String,
    val phaseIndex: Int,
    val items: List<ChecklistItem>,
)

/** One module's gated, phase-grouped items for the currently selected checklist. */
data class ChecklistSection(
    val module: ActivityModule,
    val phases: List<ChecklistPhase>,
)

/**
 * Pure gating rules extracted from iOS `ChecklistViewModel`'s `role(of:)` /
 * `sections` computation, so the trickiest logic in the port has an
 * executable specification independent of Room/DataStore/Compose.
 */
object ActivityGating {

    enum class TaskRole { ANYTIME, GEAR_CHECK, PACK, UNLOAD }

    /**
     * Morning items for optional modules are the "grab & final check" (gear-check).
     * Evening items split by phaseIndex: 0 = pack-for-tomorrow, >0 = post-session unload.
     * Core-module items are always visible ("anytime").
     */
    fun roleOf(module: ActivityModule, checklist: ChecklistType, phaseIndex: Int): TaskRole {
        if (module.isCore) return TaskRole.ANYTIME
        if (checklist == ChecklistType.MORNING) return TaskRole.GEAR_CHECK
        return if (phaseIndex == 0) TaskRole.PACK else TaskRole.UNLOAD
    }

    /**
     * Whether an item with the given role/module should currently be shown,
     * given today's and tomorrow's scheduled (gated) activity module IDs.
     *
     * `ANYTIME` returns unconditionally `true` here — it does not itself
     * check `enabledModuleIds`. That's safe today only because `roleOf`
     * assigns `ANYTIME` exclusively to Core modules, which are never
     * optional/disableable. Callers MUST gate on [isModuleVisible] before
     * calling this function (or before displaying a module's items at all),
     * so a future bug that assigns `ANYTIME` to an optional module can't
     * silently bypass its enabled/disabled state.
     */
    fun isVisible(
        role: TaskRole,
        moduleId: String,
        todayActivityIds: Set<String>,
        tomorrowActivityIds: Set<String>,
    ): Boolean = when (role) {
        TaskRole.ANYTIME -> true
        TaskRole.GEAR_CHECK -> todayActivityIds.contains(moduleId)
        TaskRole.PACK -> tomorrowActivityIds.contains(moduleId)
        TaskRole.UNLOAD -> todayActivityIds.contains(moduleId)
    }

    /**
     * Whether a module should be considered at all, independent of role:
     * Core is always visible; an optional module only if the user has
     * enabled it. Callers should check this before calling [isVisible] —
     * `isVisible`'s `ANYTIME` branch does not perform this check itself.
     */
    fun isModuleVisible(module: ActivityModule, enabledModuleIds: Set<String>): Boolean =
        module.isCore || enabledModuleIds.contains(module.id)

    /**
     * Resolves the scheduled activity module IDs for one date: the per-date
     * override if one exists, else the recurring weekly plan for that
     * weekday — intersected with enabled modules so disabled modules can
     * never bleed in via a saved override.
     */
    fun activitiesFor(
        weekday: Weekday,
        dateKey: String,
        overrides: Map<String, Set<String>>,
        weeklySchedule: Map<Weekday, Set<String>>,
        enabledModuleIds: Set<String>,
    ): Set<String> {
        val raw = overrides[dateKey] ?: weeklySchedule[weekday] ?: emptySet()
        return raw.intersect(enabledModuleIds)
    }

    /**
     * Reconciles a saved enabled-module-ID set against the modules that
     * currently exist, ported from iOS `ChecklistViewModel.init`'s stale-UUID
     * reconciliation: `saved == null` (never written) defaults to all
     * optional modules enabled; if the saved set no longer intersects any
     * real optional module (e.g. after all previously-enabled modules were
     * deleted), that's treated as equivalent to "never configured" and also
     * defaults to all optional modules; otherwise only the still-valid IDs
     * are kept.
     */
    fun reconcileEnabledModuleIds(saved: Set<String>?, optionalIds: Set<String>): Set<String> {
        if (saved == null) return optionalIds
        val valid = saved.intersect(optionalIds)
        return if (valid.isEmpty() && optionalIds.isNotEmpty()) optionalIds else valid
    }

    /**
     * Builds the gated, module→phase-grouped sections for the currently
     * selected checklist, ported from iOS `ChecklistViewModel.sections`.
     * Items whose module no longer exists, or whose module is optional and
     * not currently enabled, are dropped entirely.
     */
    fun buildSections(
        items: List<ChecklistItem>,
        modules: List<ActivityModule>,
        selectedChecklist: ChecklistType,
        enabledModuleIds: Set<String>,
        todayActivityIds: Set<String>,
        tomorrowActivityIds: Set<String>,
    ): List<ChecklistSection> {
        val modulesById = modules.associateBy { it.id }

        val visible = items.filter { item ->
            if (item.associatedChecklist != selectedChecklist) return@filter false
            val module = modulesById[item.associatedModule] ?: return@filter false
            if (!isModuleVisible(module, enabledModuleIds)) return@filter false
            val role = roleOf(module, item.associatedChecklist, item.phaseIndex)
            isVisible(role, module.id, todayActivityIds, tomorrowActivityIds)
        }

        return visible
            .groupBy { it.associatedModule }
            .mapNotNull { (moduleId, moduleItems) ->
                val module = modulesById[moduleId] ?: return@mapNotNull null
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
    }

    /**
     * Prunes a per-date overrides map down to a small retained key window
     * (e.g. yesterday/today/tomorrow), ported from iOS `pruneOverrides()` —
     * keeps the persisted overrides from growing unbounded at the cost of
     * losing far-future/past manual overrides.
     */
    fun pruneOverrideKeys(
        overrides: Map<String, Set<String>>,
        keepKeys: Set<String>,
    ): Map<String, Set<String>> = overrides.filterKeys { it in keepKeys }
}

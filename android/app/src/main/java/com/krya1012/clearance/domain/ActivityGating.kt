package com.krya1012.clearance.domain

import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.ChecklistType
import com.krya1012.clearance.data.Weekday

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
}

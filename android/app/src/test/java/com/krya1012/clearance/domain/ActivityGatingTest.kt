package com.krya1012.clearance.domain

import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.ChecklistType
import com.krya1012.clearance.data.Weekday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityGatingTest {

    private val core = ActivityModule(id = "core", name = "Core", emoji = "🎯", sortOrder = 0, isCore = true)
    private val gym = ActivityModule(id = "gym", name = "Gym", emoji = "🏋️", sortOrder = 1)

    @Test
    fun `core module items are always anytime`() {
        assertEquals(ActivityGating.TaskRole.ANYTIME, ActivityGating.roleOf(core, ChecklistType.MORNING, 0))
        assertEquals(ActivityGating.TaskRole.ANYTIME, ActivityGating.roleOf(core, ChecklistType.EVENING, 5))
    }

    @Test
    fun `optional module morning items are gear-check`() {
        assertEquals(ActivityGating.TaskRole.GEAR_CHECK, ActivityGating.roleOf(gym, ChecklistType.MORNING, 0))
    }

    @Test
    fun `optional module evening phase 0 is pack, phase greater than 0 is unload`() {
        assertEquals(ActivityGating.TaskRole.PACK, ActivityGating.roleOf(gym, ChecklistType.EVENING, 0))
        assertEquals(ActivityGating.TaskRole.UNLOAD, ActivityGating.roleOf(gym, ChecklistType.EVENING, 1))
    }

    @Test
    fun `anytime role is always visible regardless of schedule`() {
        assertTrue(ActivityGating.isVisible(ActivityGating.TaskRole.ANYTIME, "gym", emptySet(), emptySet()))
    }

    @Test
    fun `gear-check visible only when module scheduled today`() {
        assertTrue(ActivityGating.isVisible(ActivityGating.TaskRole.GEAR_CHECK, "gym", setOf("gym"), emptySet()))
        assertFalse(ActivityGating.isVisible(ActivityGating.TaskRole.GEAR_CHECK, "gym", emptySet(), setOf("gym")))
    }

    @Test
    fun `pack visible only when module scheduled tomorrow`() {
        assertTrue(ActivityGating.isVisible(ActivityGating.TaskRole.PACK, "gym", emptySet(), setOf("gym")))
        assertFalse(ActivityGating.isVisible(ActivityGating.TaskRole.PACK, "gym", setOf("gym"), emptySet()))
    }

    @Test
    fun `unload visible only when module scheduled today`() {
        assertTrue(ActivityGating.isVisible(ActivityGating.TaskRole.UNLOAD, "gym", setOf("gym"), emptySet()))
        assertFalse(ActivityGating.isVisible(ActivityGating.TaskRole.UNLOAD, "gym", emptySet(), setOf("gym")))
    }

    @Test
    fun `evening unloads today's sport while packing a different scheduled tomorrow sport`() {
        // The exact scenario CLAUDE.md calls out: unpack Swim (today), pack Gym (tomorrow).
        val today = setOf("swim")
        val tomorrow = setOf("gym")
        assertTrue(ActivityGating.isVisible(ActivityGating.TaskRole.UNLOAD, "swim", today, tomorrow))
        assertFalse(ActivityGating.isVisible(ActivityGating.TaskRole.UNLOAD, "gym", today, tomorrow))
        assertTrue(ActivityGating.isVisible(ActivityGating.TaskRole.PACK, "gym", today, tomorrow))
        assertFalse(ActivityGating.isVisible(ActivityGating.TaskRole.PACK, "swim", today, tomorrow))
    }

    @Test
    fun `per-date override takes precedence over the recurring weekly plan`() {
        val weeklySchedule = mapOf(Weekday.MONDAY to setOf("gym"))
        val overrides = mapOf("2026-07-20" to setOf("swim"))
        val result = ActivityGating.activitiesFor(
            weekday = Weekday.MONDAY,
            dateKey = "2026-07-20",
            overrides = overrides,
            weeklySchedule = weeklySchedule,
            enabledModuleIds = setOf("gym", "swim"),
        )
        assertEquals(setOf("swim"), result)
    }

    @Test
    fun `falls back to weekly plan when no override exists for the date`() {
        val weeklySchedule = mapOf(Weekday.MONDAY to setOf("gym"))
        val result = ActivityGating.activitiesFor(
            weekday = Weekday.MONDAY,
            dateKey = "2026-07-20",
            overrides = emptyMap(),
            weeklySchedule = weeklySchedule,
            enabledModuleIds = setOf("gym", "swim"),
        )
        assertEquals(setOf("gym"), result)
    }

    @Test
    fun `disabled modules never bleed in via a stale override or weekly plan`() {
        val weeklySchedule = mapOf(Weekday.MONDAY to setOf("gym", "swim"))
        val overrides = mapOf("2026-07-20" to setOf("gym", "swim"))
        val result = ActivityGating.activitiesFor(
            weekday = Weekday.MONDAY,
            dateKey = "2026-07-20",
            overrides = overrides,
            weeklySchedule = weeklySchedule,
            enabledModuleIds = setOf("gym"), // swim disabled by the user
        )
        assertEquals(setOf("gym"), result)
    }

    @Test
    fun `core module is always module-visible regardless of enabled set`() {
        assertTrue(ActivityGating.isModuleVisible(core, emptySet()))
        assertTrue(ActivityGating.isModuleVisible(core, setOf("gym")))
    }

    @Test
    fun `optional module is module-visible only when enabled`() {
        assertTrue(ActivityGating.isModuleVisible(gym, setOf("gym")))
        assertFalse(ActivityGating.isModuleVisible(gym, emptySet()))
        assertFalse(ActivityGating.isModuleVisible(gym, setOf("swim")))
    }

    @Test
    fun `reconcile defaults to all optional modules when nothing was ever saved`() {
        val result = ActivityGating.reconcileEnabledModuleIds(saved = null, optionalIds = setOf("gym", "swim"))
        assertEquals(setOf("gym", "swim"), result)
    }

    @Test
    fun `reconcile keeps only the intersection when the saved set is partially stale`() {
        val result = ActivityGating.reconcileEnabledModuleIds(
            saved = setOf("gym", "deleted-module"),
            optionalIds = setOf("gym", "swim"),
        )
        assertEquals(setOf("gym"), result)
    }

    @Test
    fun `reconcile falls back to all optional modules when the saved set is entirely stale`() {
        val result = ActivityGating.reconcileEnabledModuleIds(
            saved = setOf("deleted-a", "deleted-b"),
            optionalIds = setOf("gym", "swim"),
        )
        assertEquals(setOf("gym", "swim"), result)
    }

    @Test
    fun `reconcile of an explicitly empty saved set with no optional modules stays empty`() {
        val result = ActivityGating.reconcileEnabledModuleIds(saved = emptySet(), optionalIds = emptySet())
        assertEquals(emptySet<String>(), result)
    }

    @Test
    fun `pruneOverrideKeys keeps only entries within the retained key window`() {
        val overrides = mapOf(
            "2026-07-18" to setOf("gym"),
            "2026-07-19" to setOf("swim"),
            "2026-07-20" to setOf("core"),
            "2026-01-01" to setOf("stale"),
        )
        val result = ActivityGating.pruneOverrideKeys(
            overrides,
            keepKeys = setOf("2026-07-18", "2026-07-19", "2026-07-20"),
        )
        assertEquals(
            mapOf("2026-07-18" to setOf("gym"), "2026-07-19" to setOf("swim"), "2026-07-20" to setOf("core")),
            result,
        )
    }
}

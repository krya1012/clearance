package com.krya1012.clearance.vm

import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.ActivityType
import com.krya1012.clearance.data.ChecklistItem
import com.krya1012.clearance.data.ChecklistType
import com.krya1012.clearance.data.TemplateEntry
import com.krya1012.clearance.data.TemplateFrequency
import com.krya1012.clearance.data.Weekday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChecklistViewModelTest {

    // MARK: - reconcileEnabledModuleIDs

    @Test
    fun `never-written saved set enables every optional module`() {
        val result = reconcileEnabledModuleIDs(saved = null, optionalIds = setOf("gym", "swim"))
        assertEquals(setOf("gym", "swim"), result)
    }

    @Test
    fun `saved set that is entirely stale falls back to all optional modules`() {
        // e.g. every module the user had enabled was since deleted/renamed.
        val result = reconcileEnabledModuleIDs(saved = setOf("deleted-1", "deleted-2"), optionalIds = setOf("gym", "swim"))
        assertEquals(setOf("gym", "swim"), result)
    }

    @Test
    fun `saved set that is partially valid keeps only the surviving intersection`() {
        val result = reconcileEnabledModuleIDs(saved = setOf("gym", "deleted"), optionalIds = setOf("gym", "swim"))
        assertEquals(setOf("gym"), result)
    }

    @Test
    fun `no optional modules yields an empty result even with a stale saved set`() {
        val result = reconcileEnabledModuleIDs(saved = setOf("deleted"), optionalIds = emptySet())
        assertEquals(emptySet<String>(), result)
    }

    @Test
    fun `saved set that exactly matches optional ids is returned unchanged`() {
        val result = reconcileEnabledModuleIDs(saved = setOf("gym"), optionalIds = setOf("gym", "swim"))
        assertEquals(setOf("gym"), result)
    }

    // MARK: - computeGated

    private val core = ActivityModule(id = "core", name = "Core", emoji = "🎯", sortOrder = 0, isCore = true)
    private val gym = ActivityModule(id = "gym", name = "Gym", emoji = "🏋️", sortOrder = 1)
    private val swim = ActivityModule(id = "swim", name = "Swim", emoji = "🏊", sortOrder = 2)

    private fun item(
        id: String,
        module: ActivityModule,
        checklist: ChecklistType,
        phase: String = "Phase",
        phaseIndex: Int = 0,
        orderIndex: Int = 0,
        isCompleted: Boolean = false,
        isSkipped: Boolean = false,
    ) = ChecklistItem(
        id = id,
        title = id,
        orderIndex = orderIndex,
        isCompleted = isCompleted,
        isSkipped = isSkipped,
        phase = phase,
        phaseIndex = phaseIndex,
        associatedModule = module.id,
        associatedChecklist = checklist,
    )

    @Test
    fun `core items are visible in sections regardless of enabled modules`() {
        val coreItem = item("core-1", core, ChecklistType.MORNING)
        val gated = computeGated(
            modules = listOf(core),
            items = listOf(coreItem),
            checklist = ChecklistType.MORNING,
            enabledIds = emptySet(),
            schedule = emptyMap(),
            overrides = emptyMap(),
            todayWeekday = Weekday.MONDAY,
            todayKey = "2026-07-20",
            tomorrowWeekday = Weekday.TUESDAY,
            tomorrowKey = "2026-07-21",
        )
        assertEquals(1, gated.sections.size)
        assertEquals(listOf(coreItem), gated.sections.single().allItems)
    }

    @Test
    fun `disabled optional module's items are excluded even if scheduled`() {
        val gymItem = item("gym-1", gym, ChecklistType.MORNING)
        val gated = computeGated(
            modules = listOf(core, gym),
            items = listOf(gymItem),
            checklist = ChecklistType.MORNING,
            enabledIds = emptySet(), // gym not enabled
            schedule = mapOf(Weekday.MONDAY to setOf("gym")),
            overrides = emptyMap(),
            todayWeekday = Weekday.MONDAY,
            todayKey = "2026-07-20",
            tomorrowWeekday = Weekday.TUESDAY,
            tomorrowKey = "2026-07-21",
        )
        assertTrue(gated.sections.isEmpty())
    }

    @Test
    fun `morning gear-check item is gated on today's schedule`() {
        val gymItem = item("gym-1", gym, ChecklistType.MORNING)
        val scheduledToday = computeGated(
            modules = listOf(core, gym),
            items = listOf(gymItem),
            checklist = ChecklistType.MORNING,
            enabledIds = setOf("gym"),
            schedule = mapOf(Weekday.MONDAY to setOf("gym")),
            overrides = emptyMap(),
            todayWeekday = Weekday.MONDAY,
            todayKey = "2026-07-20",
            tomorrowWeekday = Weekday.TUESDAY,
            tomorrowKey = "2026-07-21",
        )
        assertEquals(1, scheduledToday.sections.size)

        val notScheduledToday = computeGated(
            modules = listOf(core, gym),
            items = listOf(gymItem),
            checklist = ChecklistType.MORNING,
            enabledIds = setOf("gym"),
            schedule = mapOf(Weekday.TUESDAY to setOf("gym")), // scheduled tomorrow, not today
            overrides = emptyMap(),
            todayWeekday = Weekday.MONDAY,
            todayKey = "2026-07-20",
            tomorrowWeekday = Weekday.TUESDAY,
            tomorrowKey = "2026-07-21",
        )
        assertTrue(notScheduledToday.sections.isEmpty())
    }

    @Test
    fun `evening phase 0 is gated on tomorrow, phase greater than 0 on today`() {
        val packItem = item("gym-pack", gym, ChecklistType.EVENING, phaseIndex = 0)
        val unloadItem = item("swim-unload", swim, ChecklistType.EVENING, phaseIndex = 1)
        val gated = computeGated(
            modules = listOf(core, gym, swim),
            items = listOf(packItem, unloadItem),
            checklist = ChecklistType.EVENING,
            enabledIds = setOf("gym", "swim"),
            schedule = emptyMap(),
            overrides = mapOf(
                "2026-07-20" to setOf("swim"), // today: unload swim
                "2026-07-21" to setOf("gym"),  // tomorrow: pack gym
            ),
            todayWeekday = Weekday.MONDAY,
            todayKey = "2026-07-20",
            tomorrowWeekday = Weekday.TUESDAY,
            tomorrowKey = "2026-07-21",
        )
        val visibleModuleIds = gated.sections.map { it.module.id }.toSet()
        assertEquals(setOf("gym", "swim"), visibleModuleIds)
    }

    @Test
    fun `evening phase 0 is hidden when its module is not scheduled tomorrow`() {
        val packItem = item("gym-pack", gym, ChecklistType.EVENING, phaseIndex = 0)
        val gated = computeGated(
            modules = listOf(core, gym),
            items = listOf(packItem),
            checklist = ChecklistType.EVENING,
            enabledIds = setOf("gym"),
            schedule = emptyMap(),
            overrides = mapOf("2026-07-20" to setOf("gym")), // scheduled today, not tomorrow
            todayWeekday = Weekday.MONDAY,
            todayKey = "2026-07-20",
            tomorrowWeekday = Weekday.TUESDAY,
            tomorrowKey = "2026-07-21",
        )
        assertTrue(gated.sections.isEmpty())
    }

    @Test
    fun `per-date override takes precedence over the weekly plan when computing today`() {
        val gymItem = item("gym-1", gym, ChecklistType.MORNING)
        val gated = computeGated(
            modules = listOf(core, gym, swim),
            items = listOf(gymItem),
            checklist = ChecklistType.MORNING,
            enabledIds = setOf("gym", "swim"),
            schedule = mapOf(Weekday.MONDAY to setOf("swim")), // weekly plan says swim
            overrides = mapOf("2026-07-20" to setOf("gym")),   // override says gym
            todayWeekday = Weekday.MONDAY,
            todayKey = "2026-07-20",
            tomorrowWeekday = Weekday.TUESDAY,
            tomorrowKey = "2026-07-21",
        )
        assertEquals(1, gated.sections.size)
        assertEquals("gym", gated.sections.single().module.id)
    }

    @Test
    fun `skipped items are excluded from progress and counts but not from sections`() {
        val done = item("core-done", core, ChecklistType.MORNING, orderIndex = 0, isCompleted = true)
        val skipped = item("core-skipped", core, ChecklistType.MORNING, orderIndex = 1, isSkipped = true)
        val pending = item("core-pending", core, ChecklistType.MORNING, orderIndex = 2)
        val gated = computeGated(
            modules = listOf(core),
            items = listOf(done, skipped, pending),
            checklist = ChecklistType.MORNING,
            enabledIds = emptySet(),
            schedule = emptyMap(),
            overrides = emptyMap(),
            todayWeekday = Weekday.MONDAY,
            todayKey = "2026-07-20",
            tomorrowWeekday = Weekday.TUESDAY,
            tomorrowKey = "2026-07-21",
        )
        assertEquals(3, gated.sections.single().allItems.size) // skipped item still shown, just excluded from counts
        assertEquals(2, gated.totalActiveCount)
        assertEquals(1, gated.completedCount)
        assertEquals(0.5, gated.progress, 0.0001)
    }

    @Test
    fun `sections are sorted by module sortOrder, phases by phaseIndex, items by orderIndex`() {
        val swimItem = item("swim-1", swim, ChecklistType.MORNING)
        val gymPhase1 = item("gym-p1-b", gym, ChecklistType.MORNING, phase = "B", phaseIndex = 1, orderIndex = 1)
        val gymPhase1First = item("gym-p1-a", gym, ChecklistType.MORNING, phase = "B", phaseIndex = 1, orderIndex = 0)
        val gymPhase0 = item("gym-p0", gym, ChecklistType.MORNING, phase = "A", phaseIndex = 0, orderIndex = 0)
        val gated = computeGated(
            modules = listOf(swim, gym), // note: passed out of sortOrder to prove sorting happens
            items = listOf(swimItem, gymPhase1, gymPhase1First, gymPhase0),
            checklist = ChecklistType.MORNING,
            enabledIds = setOf("gym", "swim"),
            schedule = emptyMap(),
            overrides = mapOf(
                "2026-07-20" to setOf("gym", "swim"),
                "2026-07-21" to setOf("gym", "swim"),
            ),
            todayWeekday = Weekday.MONDAY,
            todayKey = "2026-07-20",
            tomorrowWeekday = Weekday.TUESDAY,
            tomorrowKey = "2026-07-21",
        )
        assertEquals(listOf("gym", "swim"), gated.sections.map { it.module.id }) // gym.sortOrder=1 < swim.sortOrder=2

        val gymSection = gated.sections.first { it.module.id == "gym" }
        assertEquals(listOf("A", "B"), gymSection.phases.map { it.name })
        assertEquals(listOf("gym-p1-a", "gym-p1-b"), gymSection.phases.last().items.map { it.id })
    }

    // MARK: - computeTemplateInstalled

    private val gymTemplate = TemplateEntry("Gym", "🏋️", ActivityType.SPORT, TemplateFrequency.WEEKLY, "tagline")

    @Test
    fun `template is not installed when no module shares its name`() {
        assertEquals(false, computeTemplateInstalled(gymTemplate, listOf(core, swim)))
    }

    @Test
    fun `template is installed when a module already has its name`() {
        assertEquals(true, computeTemplateInstalled(gymTemplate, listOf(core, gym)))
    }

    // MARK: - computeAvailablePhases

    @Test
    fun `available phases are empty when the module has no items for that checklist`() {
        val result = computeAvailablePhases(items = emptyList(), moduleId = "gym", checklist = ChecklistType.MORNING)
        assertEquals(emptyList<Pair<String, Int>>(), result)
    }

    @Test
    fun `available phases are distinct, ordered by phaseIndex, ignoring other modules and checklists`() {
        val items = listOf(
            item("gym-morning-a", gym, ChecklistType.MORNING, phase = "Gear check", phaseIndex = 0, orderIndex = 0),
            item("gym-morning-b", gym, ChecklistType.MORNING, phase = "Gear check", phaseIndex = 0, orderIndex = 1),
            item("gym-evening", gym, ChecklistType.EVENING, phase = "Unload", phaseIndex = 1, orderIndex = 0),
            item("swim-morning", swim, ChecklistType.MORNING, phase = "Other module", phaseIndex = 0, orderIndex = 0),
        )
        val result = computeAvailablePhases(items = items, moduleId = "gym", checklist = ChecklistType.MORNING)
        assertEquals(listOf("Gear check" to 0), result)
    }
}

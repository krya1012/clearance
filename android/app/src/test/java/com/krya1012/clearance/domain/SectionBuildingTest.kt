package com.krya1012.clearance.domain

import com.krya1012.clearance.data.ActivityModule
import com.krya1012.clearance.data.ChecklistItem
import com.krya1012.clearance.data.ChecklistType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionBuildingTest {

    private val core = ActivityModule(id = "core", name = "Core", emoji = "🎯", sortOrder = 0, isCore = true)
    private val gym = ActivityModule(id = "gym", name = "Gym", emoji = "🏋️", sortOrder = 1)
    private val swim = ActivityModule(id = "swim", name = "Swim", emoji = "🏊", sortOrder = 2)

    private fun item(
        title: String,
        module: ActivityModule,
        checklist: ChecklistType,
        phase: String = "Phase",
        phaseIndex: Int = 0,
        orderIndex: Int = 0,
    ) = ChecklistItem(
        title = title,
        orderIndex = orderIndex,
        phase = phase,
        phaseIndex = phaseIndex,
        associatedModule = module.id,
        associatedChecklist = checklist,
    )

    @Test
    fun `core items are always visible regardless of today or tomorrow gating`() {
        val items = listOf(item("Launch", core, ChecklistType.MORNING))
        val sections = ActivityGating.buildSections(
            items = items,
            modules = listOf(core),
            selectedChecklist = ChecklistType.MORNING,
            enabledModuleIds = emptySet(),
            todayActivityIds = emptySet(),
            tomorrowActivityIds = emptySet(),
        )
        assertEquals(1, sections.size)
        assertEquals(core, sections.single().module)
    }

    @Test
    fun `disabled optional module is excluded entirely even if scheduled today`() {
        val items = listOf(item("Gear check", gym, ChecklistType.MORNING))
        val sections = ActivityGating.buildSections(
            items = items,
            modules = listOf(core, gym),
            selectedChecklist = ChecklistType.MORNING,
            enabledModuleIds = emptySet(), // gym disabled
            todayActivityIds = setOf("gym"),
            tomorrowActivityIds = emptySet(),
        )
        assertTrue(sections.isEmpty())
    }

    @Test
    fun `enabled optional module morning items require today gating`() {
        val items = listOf(item("Gear check", gym, ChecklistType.MORNING))
        val notScheduled = ActivityGating.buildSections(
            items = items,
            modules = listOf(core, gym),
            selectedChecklist = ChecklistType.MORNING,
            enabledModuleIds = setOf("gym"),
            todayActivityIds = emptySet(),
            tomorrowActivityIds = emptySet(),
        )
        assertTrue(notScheduled.isEmpty())

        val scheduled = ActivityGating.buildSections(
            items = items,
            modules = listOf(core, gym),
            selectedChecklist = ChecklistType.MORNING,
            enabledModuleIds = setOf("gym"),
            todayActivityIds = setOf("gym"),
            tomorrowActivityIds = emptySet(),
        )
        assertEquals(1, scheduled.size)
    }

    @Test
    fun `evening unloads today's sport while packing a different scheduled tomorrow sport`() {
        val items = listOf(
            item("Unload swim gear", swim, ChecklistType.EVENING, phase = "Unload", phaseIndex = 1),
            item("Pack gym bag", gym, ChecklistType.EVENING, phase = "Pack", phaseIndex = 0),
        )
        val sections = ActivityGating.buildSections(
            items = items,
            modules = listOf(core, gym, swim),
            selectedChecklist = ChecklistType.EVENING,
            enabledModuleIds = setOf("gym", "swim"),
            todayActivityIds = setOf("swim"),
            tomorrowActivityIds = setOf("gym"),
        )
        val moduleIds = sections.map { it.module.id }.toSet()
        assertEquals(setOf("gym", "swim"), moduleIds)
    }

    @Test
    fun `items are grouped by phase within a module and sorted by phaseIndex then orderIndex`() {
        val items = listOf(
            item("B", core, ChecklistType.MORNING, phase = "Second", phaseIndex = 1, orderIndex = 0),
            item("A", core, ChecklistType.MORNING, phase = "First", phaseIndex = 0, orderIndex = 1),
            item("C", core, ChecklistType.MORNING, phase = "First", phaseIndex = 0, orderIndex = 0),
        )
        val sections = ActivityGating.buildSections(
            items = items,
            modules = listOf(core),
            selectedChecklist = ChecklistType.MORNING,
            enabledModuleIds = emptySet(),
            todayActivityIds = emptySet(),
            tomorrowActivityIds = emptySet(),
        )
        val phases = sections.single().phases
        assertEquals(listOf("First", "Second"), phases.map { it.name })
        assertEquals(listOf("C", "A"), phases.first().items.map { it.title })
    }

    @Test
    fun `sections are sorted by module sortOrder`() {
        val items = listOf(
            item("Swim item", swim, ChecklistType.MORNING),
            item("Core item", core, ChecklistType.MORNING),
            item("Gym item", gym, ChecklistType.MORNING),
        )
        val sections = ActivityGating.buildSections(
            items = items,
            modules = listOf(core, gym, swim),
            selectedChecklist = ChecklistType.MORNING,
            enabledModuleIds = setOf("gym", "swim"),
            todayActivityIds = setOf("gym", "swim"),
            tomorrowActivityIds = emptySet(),
        )
        assertEquals(listOf("core", "gym", "swim"), sections.map { it.module.id })
    }

    @Test
    fun `items referencing a module id that no longer exists are dropped`() {
        val items = listOf(item("Orphaned", core, ChecklistType.MORNING).copy(associatedModule = "vanished"))
        val sections = ActivityGating.buildSections(
            items = items,
            modules = listOf(core),
            selectedChecklist = ChecklistType.MORNING,
            enabledModuleIds = emptySet(),
            todayActivityIds = emptySet(),
            tomorrowActivityIds = emptySet(),
        )
        assertTrue(sections.isEmpty())
    }
}

package com.krya1012.clearance.domain

import com.krya1012.clearance.data.ActivityModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedMigrationPlanTest {

    @Test
    fun `deprecated modules are flagged for cleanup on pre-v10 installs`() {
        val existing = listOf(
            ActivityModule(id = "1", name = "Work", emoji = "💼", sortOrder = 0),
            ActivityModule(id = "2", name = "Gym", emoji = "🏋️", sortOrder = 1),
        )
        val result = SeedMigrationPlan.modulesToDeprecate(existing, storedVersion = 5)
        assertEquals(listOf(existing[0]), result)
    }

    @Test
    fun `deprecated-name cleanup never runs again once already migrated past v10`() {
        // The exact regression this fix closes: a user creates their own
        // module named "Study" (a former deprecated name) after already
        // being on v10+ — it must never be swept up by this check again.
        val existing = listOf(
            ActivityModule(id = "1", name = "Study", emoji = "📚", sortOrder = 0),
        )
        val result = SeedMigrationPlan.modulesToDeprecate(existing, storedVersion = 10)
        assertEquals(emptyList<ActivityModule>(), result)
    }

    @Test
    fun `rest module is flagged for rename only on pre-v11 installs`() {
        val existing = listOf(ActivityModule(id = "1", name = "Rest", emoji = "🚶", sortOrder = 7))
        assertEquals(existing[0], SeedMigrationPlan.moduleToRename(existing, storedVersion = 10))
        assertNull(SeedMigrationPlan.moduleToRename(existing, storedVersion = 11))
    }

    @Test
    fun `moduleToRename is a no-op when no Rest module exists`() {
        val existing = listOf(ActivityModule(id = "1", name = "Walking", emoji = "🚶", sortOrder = 7))
        assertNull(SeedMigrationPlan.moduleToRename(existing, storedVersion = 0))
    }

    @Test
    fun `resolveModules keeps the existing id and syncs sortOrder, emoji, isLocked`() {
        val existingGym = ActivityModule(id = "existing-gym-id", name = "Gym", emoji = "old-emoji", sortOrder = 99, isLocked = true)
        val seedGym = ActivityModule(id = "seed-gym-id", name = "Gym", emoji = "🏋️", sortOrder = 1, isLocked = false)

        val resolution = SeedMigrationPlan.resolveModules(
            seedModules = listOf(seedGym),
            existingByName = mapOf("Gym" to existingGym),
        )

        assertEquals(1, resolution.resolvedModules.size)
        val resolved = resolution.resolvedModules.first()
        assertEquals("existing-gym-id", resolved.id) // id preserved
        assertEquals("🏋️", resolved.emoji)           // synced from seed
        assertEquals(1, resolved.sortOrder)           // synced from seed
        assertEquals(false, resolved.isLocked)        // synced from seed
        assertTrue(resolution.modulesToUpdate.contains(resolved))
        assertTrue(resolution.modulesToInsert.isEmpty())
        assertTrue(resolution.newOptionalIds.isEmpty()) // not new, so not added
    }

    @Test
    fun `resolveModules inserts a brand-new optional module and tracks its id`() {
        val seedWalking = ActivityModule(id = "walking-id", name = "Walking", emoji = "🚶", sortOrder = 7)

        val resolution = SeedMigrationPlan.resolveModules(
            seedModules = listOf(seedWalking),
            existingByName = emptyMap(),
        )

        assertEquals(listOf(seedWalking), resolution.modulesToInsert)
        assertEquals(setOf("walking-id"), resolution.newOptionalIds)
        assertTrue(resolution.modulesToUpdate.isEmpty())
    }

    @Test
    fun `resolveModules never adds a newly-inserted Core module to newOptionalIds`() {
        val seedCore = ActivityModule(id = "core-id", name = "Core", emoji = "🎯", sortOrder = 0, isCore = true)

        val resolution = SeedMigrationPlan.resolveModules(
            seedModules = listOf(seedCore),
            existingByName = emptyMap(),
        )

        assertEquals(listOf(seedCore), resolution.modulesToInsert)
        assertTrue(resolution.newOptionalIds.isEmpty())
    }

    @Test
    fun `itemsToInsert only targets modules with zero existing items`() {
        val core = ActivityModule(id = "core", name = "Core", emoji = "🎯", sortOrder = 0, isCore = true)
        val gym = ActivityModule(id = "gym", name = "Gym", emoji = "🏋️", sortOrder = 1)
        val modules = listOf(core, gym)

        // Pretend "gym" already has at least one item — its seed items must not be duplicated.
        val itemsToInsert = SeedMigrationPlan.itemsToInsert(modules, modulesWithItems = setOf("gym"))

        assertTrue(itemsToInsert.isNotEmpty())
        assertTrue(itemsToInsert.all { it.associatedModule == "core" })
    }

    @Test
    fun `defaultModules returns all 8 canonical modules including Walking`() {
        val names = SeedMigrationPlan.defaultModules().map { it.name }
        assertEquals(
            listOf("Core", "Gym", "Swim", "Judo", "Cycling", "Running", "Yoga", "Walking"),
            names,
        )
    }

    @Test
    fun `defaultItems returns nothing when there is no Core module`() {
        val gymOnly = listOf(ActivityModule(id = "gym", name = "Gym", emoji = "🏋️", sortOrder = 1))
        assertEquals(emptyList<Any>(), SeedMigrationPlan.defaultItems(gymOnly))
    }

    // MARK: - shouldSeed

    @Test
    fun `pre-migration store seeds regardless of module count`() {
        assertTrue(SeedMigrationPlan.shouldSeed(storedVersion = 5, hasNoModules = false))
        assertTrue(SeedMigrationPlan.shouldSeed(storedVersion = 5, hasNoModules = true))
    }

    @Test
    fun `already-migrated store that is corrupted or emptied still seeds`() {
        // The exact gap that shipped broken: a device already at CURRENT_VERSION whose Room
        // database turns out to have zero modules (e.g. recovered from corruption) must still
        // re-seed, not be skipped just because storedVersion claims migration already finished.
        assertTrue(
            SeedMigrationPlan.shouldSeed(
                storedVersion = SeedMigrationPlan.CURRENT_VERSION,
                hasNoModules = true,
            ),
        )
    }

    @Test
    fun `already-migrated, healthy store does not reseed`() {
        assertFalse(
            SeedMigrationPlan.shouldSeed(
                storedVersion = SeedMigrationPlan.CURRENT_VERSION,
                hasNoModules = false,
            ),
        )
    }
}

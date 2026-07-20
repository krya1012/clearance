package com.krya1012.clearance.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.room.withTransaction
import com.krya1012.clearance.domain.SeedMigrationPlan
import kotlinx.coroutines.flow.first

/**
 * Executes the migration plan computed by `domain/SeedMigrationPlan.kt`
 * against Room and DataStore. Bump `SeedMigrationPlan.CURRENT_VERSION`
 * whenever the canonical checklist content changes. Mirrors iOS
 * `SeedData.swift`.
 */
object SeedData {

    private val VERSION_KEY = intPreferencesKey("Clearance.seedVersion.v1")

    suspend fun seedIfNeeded(
        context: Context,
        database: ClearanceDatabase,
        scheduleStore: ScheduleStore,
    ) {
        val storedVersion = context.clearanceDataStore.data.first()[VERSION_KEY] ?: 0
        // Touching Room here (even when already fully migrated) is what lets a corrupted
        // store's exception surface for ClearanceApplication.seedWithRecovery() to catch and
        // rebuild from — without this query, an already-migrated device would return below
        // having never queried Room at all, silently skipping the designed corruption-recovery
        // path.
        val hasNoModules = database.moduleDao().getAll().isEmpty()
        if (!SeedMigrationPlan.shouldSeed(storedVersion, hasNoModules)) return

        var resolvedModules: List<ActivityModule> = emptyList()
        var newOptionalIds: Set<String> = emptySet()

        // One atomic transaction for the whole migration (deprecated-module
        // cleanup + rename + module resolution + item insertion) instead of
        // each DAO call auto-committing independently — a kill mid-migration
        // can no longer leave e.g. deprecated modules deleted but Walking
        // not yet renamed/inserted.
        database.withTransaction {
            val moduleDao = database.moduleDao()
            val itemDao = database.itemDao()

            val existingModules = moduleDao.getAll()
            for (module in SeedMigrationPlan.modulesToDeprecate(existingModules, storedVersion)) {
                itemDao.deleteByModule(module.id)
                moduleDao.delete(module)
            }

            SeedMigrationPlan.moduleToRename(existingModules, storedVersion)?.let { rest ->
                moduleDao.update(rest.copy(name = "Walking", isLocked = false))
            }

            // Index existing modules by name so user-created modules survive.
            val existingByName = moduleDao.getAll().associateBy { it.name }
            // Track which module ids already have at least one task.
            val modulesWithItems = itemDao.getModulesWithItems().toSet()

            val resolution = SeedMigrationPlan.resolveModules(SeedMigrationPlan.defaultModules(), existingByName)
            resolution.modulesToUpdate.forEach { moduleDao.update(it) }
            resolution.modulesToInsert.forEach { moduleDao.insert(it) }

            itemDao.insertAll(SeedMigrationPlan.itemsToInsert(resolution.resolvedModules, modulesWithItems))

            resolvedModules = resolution.resolvedModules
            newOptionalIds = resolution.newOptionalIds
        }

        // Enable any newly-inserted optional modules without touching existing prefs.
        // (DataStore, not part of the Room transaction above.)
        if (newOptionalIds.isNotEmpty()) {
            val current = scheduleStore.loadEnabledModuleIDs()
                ?: resolvedModules.filter { !it.isCore }.map { it.id }.toSet()
            scheduleStore.saveEnabledModuleIDs(current + newOptionalIds)

            // Schedule Walking on Sunday by default.
            val walking = resolvedModules.firstOrNull { it.name == "Walking" }
            if (walking != null && newOptionalIds.contains(walking.id)) {
                val schedule = scheduleStore.loadSchedule().toMutableMap()
                schedule[Weekday.SUNDAY] = (schedule[Weekday.SUNDAY] ?: emptySet()) + walking.id
                scheduleStore.saveSchedule(schedule)
            }
        }

        context.clearanceDataStore.edit { it[VERSION_KEY] = SeedMigrationPlan.CURRENT_VERSION }
    }

    fun defaultModules(): List<ActivityModule> = SeedMigrationPlan.defaultModules()

    fun defaultItems(module: ActivityModule, allModules: List<ActivityModule>): List<ChecklistItem> =
        SeedMigrationPlan.defaultItems(module, allModules)

    fun defaultItems(modules: List<ActivityModule>): List<ChecklistItem> =
        SeedMigrationPlan.defaultItems(modules)
}

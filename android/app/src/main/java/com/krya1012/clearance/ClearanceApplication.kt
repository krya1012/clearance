package com.krya1012.clearance

import android.app.Application
import com.krya1012.clearance.data.ClearanceDatabase
import com.krya1012.clearance.data.PeriodicTaskStore
import com.krya1012.clearance.data.ScheduleStore
import com.krya1012.clearance.data.SeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** App-scoped singletons: Room database and DataStore-backed stores. */
class ClearanceApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Rebuilt in place by [seedWithRecovery] if the store turns out to be
     * corrupted/unopenable — mirrors iOS's ModelContainer in-memory fallback
     * (degrade to a fresh, empty-but-running store instead of crash-looping
     * on every future launch too).
     */
    @Volatile
    lateinit var database: ClearanceDatabase
        private set

    val scheduleStore: ScheduleStore by lazy { ScheduleStore(this) }
    val periodicTaskStore: PeriodicTaskStore by lazy { PeriodicTaskStore(this) }

    override fun onCreate() {
        super.onCreate()
        // Built synchronously (cheap — Room.databaseBuilder(...).build() does
        // no I/O by itself) so MainActivity never races an unset `database`;
        // Android guarantees Application.onCreate() completes before any
        // Activity's onCreate() runs.
        database = ClearanceDatabase.build(this)
        applicationScope.launch {
            seedWithRecovery()
            periodicTaskStore.seedDefaultsIfNeeded()
        }
    }

    /**
     * Seeds the database, recovering once if the existing store is
     * corrupted/unopenable (the exception surfaces here, on first real
     * query) by wiping it and rebuilding fresh — the Android equivalent of
     * iOS's ModelContainer in-memory fallback. A second failure after that
     * is treated as truly unrecoverable and propagates.
     */
    private suspend fun seedWithRecovery() {
        try {
            SeedData.seedIfNeeded(this, database, scheduleStore)
        } catch (e: Exception) {
            database.close()
            deleteDatabase(ClearanceDatabase.DB_NAME)
            database = ClearanceDatabase.build(this)
            // Any saved schedule/override/enabled-module UUIDs necessarily
            // reference modules that no longer exist in this fresh database —
            // clear them rather than leaving them to silently resolve to
            // nothing, same reasoning as iOS's equivalent recovery path.
            scheduleStore.clearModuleKeys()
            SeedData.seedIfNeeded(this, database, scheduleStore)
        }
    }
}

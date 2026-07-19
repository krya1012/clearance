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

    val database: ClearanceDatabase by lazy { ClearanceDatabase.getInstance(this) }
    val scheduleStore: ScheduleStore by lazy { ScheduleStore(this) }
    val periodicTaskStore: PeriodicTaskStore by lazy { PeriodicTaskStore(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            SeedData.seedIfNeeded(
                context = this@ClearanceApplication,
                moduleDao = database.moduleDao(),
                itemDao = database.itemDao(),
                scheduleStore = scheduleStore,
            )
            periodicTaskStore.seedDefaultsIfNeeded()
        }
    }
}

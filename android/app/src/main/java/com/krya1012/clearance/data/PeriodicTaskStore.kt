package com.krya1012.clearance.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

/**
 * DataStore persistence for PeriodicTask, following the ScheduleStore pattern.
 * Completions auto-reset when `load()` is called after a week or month boundary.
 */
class PeriodicTaskStore(private val context: Context) {

    private object Keys {
        val TASKS = stringPreferencesKey("Clearance.periodicTasks.v1")
        val SEED_VERSION = intPreferencesKey("Clearance.periodicSeedVersion.v1")
    }

    companion object {
        private const val CURRENT_SEED_VERSION = 2
    }

    suspend fun seedDefaultsIfNeeded() {
        val stored = context.clearanceDataStore.data.first()[Keys.SEED_VERSION] ?: 0
        // Also reseed if the stored blob is corrupted (present but undecodable) —
        // otherwise a decode failure would silently look like "already seeded"
        // forever and the next save() would permanently overwrite it with
        // whatever the corrupted read decoded to (nothing).
        if (stored >= CURRENT_SEED_VERSION && !isStoredDataCorrupted()) return

        val existing = load()
        val existingTitles = existing.map { it.title }.toSet()
        val seeds = listOf(
            PeriodicTask(title = "Laundry", emoji = "🧺", recurrence = Recurrence.WEEKLY),
            PeriodicTask(title = "Shopping", emoji = "🛒", recurrence = Recurrence.WEEKLY),
            PeriodicTask(title = "Budget", emoji = "💰", recurrence = Recurrence.MONTHLY),
            PeriodicTask(title = "Post", emoji = "📬", recurrence = Recurrence.MONTHLY),
        )
        val toAdd = seeds.filterNot { existingTitles.contains(it.title) }
        if (toAdd.isNotEmpty()) save(existing + toAdd)
        context.clearanceDataStore.edit { it[Keys.SEED_VERSION] = CURRENT_SEED_VERSION }
    }

    suspend fun load(): List<PeriodicTask> {
        val raw = context.clearanceDataStore.data.first()[Keys.TASKS] ?: return emptyList()
        val tasks = runCatching { Json.decodeFromString<List<PeriodicTask>>(raw) }.getOrNull()
            ?: return emptyList()

        val now = Calendar.getInstance()
        val currentWeek = now.get(Calendar.WEEK_OF_YEAR)
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)

        return tasks.map { task ->
            if (!task.isCompleted || task.completedDateMillis == null) return@map task
            val completed = Calendar.getInstance().apply { timeInMillis = task.completedDateMillis }
            val stillCurrent = when (task.recurrence) {
                Recurrence.WEEKLY ->
                    completed.get(Calendar.WEEK_OF_YEAR) == currentWeek && completed.get(Calendar.YEAR) == currentYear
                Recurrence.MONTHLY ->
                    completed.get(Calendar.MONTH) == currentMonth && completed.get(Calendar.YEAR) == currentYear
                Recurrence.DAILY -> true
            }
            if (stillCurrent) task else task.copy(isCompleted = false, completedDateMillis = null)
        }
    }

    suspend fun save(tasks: List<PeriodicTask>) {
        val encoded = Json.encodeToString(tasks)
        context.clearanceDataStore.edit { it[Keys.TASKS] = encoded }
    }

    /**
     * True when a periodic-tasks blob exists but fails to decode — distinct
     * from "no data yet" (key absent) or "user cleared everything" (decodes
     * fine to an empty list), neither of which should trigger a reseed.
     */
    private suspend fun isStoredDataCorrupted(): Boolean {
        val raw = context.clearanceDataStore.data.first()[Keys.TASKS] ?: return false
        return runCatching { Json.decodeFromString<List<PeriodicTask>>(raw) }.getOrNull() == null
    }
}

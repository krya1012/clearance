package com.krya1012.clearance.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Persists the recurring weekly activity plan, per-day overrides, enabled
 * module IDs, and auto-reset settings in DataStore. All module references
 * are stored as ActivityModule.id strings — Kotlin analog of iOS
 * `ScheduleStore.swift`.
 */
class ScheduleStore(private val context: Context) {

    private object Keys {
        val SCHEDULE = stringPreferencesKey("Clearance.weeklySchedule.v2")
        val OVERRIDES = stringPreferencesKey("Clearance.activityOverrides.v2")
        val ENABLED_MODULES = stringPreferencesKey("Clearance.enabledModules.v2")
        val AUTO_RESET_HOUR = intPreferencesKey("Clearance.autoResetHour.v1")
        val LAST_AUTO_RESET = longPreferencesKey("Clearance.lastAutoReset.v1")
    }

    private val dateKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    /** Stable per-day key, e.g. "2026-06-19", in the device's local timezone. */
    fun dateKey(date: Date): String = dateKeyFormat.format(date)

    // MARK: - Weekly schedule (Map<Weekday.rawValue, Set<moduleId>>)

    suspend fun loadSchedule(): Map<Weekday, Set<String>> {
        val raw = context.clearanceDataStore.data.first()[Keys.SCHEDULE] ?: return emptyMap()
        val decoded = runCatching { Json.decodeFromString<Map<String, List<String>>>(raw) }.getOrNull()
            ?: return emptyMap()
        return decoded.mapNotNull { (key, values) ->
            val day = key.toIntOrNull()?.let { Weekday.fromRawValue(it) } ?: return@mapNotNull null
            day to values.toSet()
        }.toMap()
    }

    suspend fun saveSchedule(schedule: Map<Weekday, Set<String>>) {
        val raw = schedule.entries.associate { (day, ids) -> day.rawValue.toString() to ids.toList() }
        val encoded = Json.encodeToString(raw)
        context.clearanceDataStore.edit { it[Keys.SCHEDULE] = encoded }
    }

    // MARK: - Per-day overrides (Map<dateKey, Set<moduleId>>)

    suspend fun loadOverrides(): Map<String, Set<String>> {
        val raw = context.clearanceDataStore.data.first()[Keys.OVERRIDES] ?: return emptyMap()
        val decoded = runCatching { Json.decodeFromString<Map<String, List<String>>>(raw) }.getOrNull()
            ?: return emptyMap()
        return decoded.mapValues { it.value.toSet() }
    }

    suspend fun saveOverrides(overrides: Map<String, Set<String>>) {
        val raw = overrides.mapValues { it.value.toList() }
        val encoded = Json.encodeToString(raw)
        context.clearanceDataStore.edit { it[Keys.OVERRIDES] = encoded }
    }

    // MARK: - Enabled module IDs

    /** Returns `null` when never written — caller should default to enabling all optional modules. */
    suspend fun loadEnabledModuleIDs(): Set<String>? {
        val raw = context.clearanceDataStore.data.first()[Keys.ENABLED_MODULES] ?: return null
        return runCatching { Json.decodeFromString<List<String>>(raw) }.getOrNull()?.toSet()
    }

    suspend fun saveEnabledModuleIDs(ids: Set<String>) {
        val encoded = Json.encodeToString(ids.toList())
        context.clearanceDataStore.edit { it[Keys.ENABLED_MODULES] = encoded }
    }

    // MARK: - Auto-reset

    suspend fun loadResetHour(): Int =
        context.clearanceDataStore.data.first()[Keys.AUTO_RESET_HOUR] ?: 3

    suspend fun saveResetHour(hour: Int) {
        context.clearanceDataStore.edit { it[Keys.AUTO_RESET_HOUR] = hour }
    }

    /** Epoch millis of the last auto-reset; `Long.MIN_VALUE` stands in for iOS's `.distantPast`. */
    suspend fun loadLastAutoReset(): Long =
        context.clearanceDataStore.data.first()[Keys.LAST_AUTO_RESET] ?: Long.MIN_VALUE

    suspend fun saveLastAutoReset(epochMillis: Long) {
        context.clearanceDataStore.edit { it[Keys.LAST_AUTO_RESET] = epochMillis }
    }

    /** Removes all module-related keys so stale UUIDs from a previous seed never survive. */
    suspend fun clearModuleKeys() {
        context.clearanceDataStore.edit {
            it.remove(Keys.SCHEDULE)
            it.remove(Keys.OVERRIDES)
            it.remove(Keys.ENABLED_MODULES)
        }
    }
}

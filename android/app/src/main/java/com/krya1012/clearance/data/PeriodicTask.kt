package com.krya1012.clearance.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A recurring task that resets at the start of each week or month.
 * Persisted via PeriodicTaskStore (DataStore), not Room.
 */
@Serializable
data class PeriodicTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String = "📋",
    val recurrence: Recurrence,
    val isCompleted: Boolean = false,
    /** Epoch millis of the completion, or null if not completed. */
    val completedDateMillis: Long? = null,
)

package com.krya1012.clearance.data

/** The two daily sequences. Persisted on `ChecklistItem` as a plain string column. */
enum class ChecklistType {
    MORNING,
    EVENING;

    val title: String
        get() = when (this) {
            MORNING -> "Takeoff"
            EVENING -> "Landing"
        }

    val subtitle: String
        get() = when (this) {
            MORNING -> "Morning sequence"
            EVENING -> "Evening sequence"
        }

    val emoji: String
        get() = when (this) {
            MORNING -> "🌅"
            EVENING -> "🌌"
        }

    val label: String get() = "$emoji $title"
}

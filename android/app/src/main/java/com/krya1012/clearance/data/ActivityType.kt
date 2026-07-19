package com.krya1012.clearance.data

/** User-visible energy / domain category for a module. */
enum class ActivityType {
    SPORT,
    WORK,
    STUDY,
    LEISURE;

    val label: String
        get() = when (this) {
            SPORT -> "Sport"
            WORK -> "Work"
            STUDY -> "Study"
            LEISURE -> "Leisure"
        }

    val emoji: String
        get() = when (this) {
            SPORT -> "🏅"
            WORK -> "💼"
            STUDY -> "📚"
            LEISURE -> "🎮"
        }

    companion object {
        fun fromRawValue(rawValue: String): ActivityType =
            entries.firstOrNull { it.name.equals(rawValue, ignoreCase = true) } ?: SPORT
    }
}

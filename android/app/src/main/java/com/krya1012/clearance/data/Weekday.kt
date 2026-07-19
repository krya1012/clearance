package com.krya1012.clearance.data

import java.util.Calendar
import java.util.Date

/**
 * Days of the week, aligned with `Calendar.DAY_OF_WEEK` (Sunday = 1) so a
 * `Date` maps straight onto a case — same numbering as iOS `Weekday`.
 */
enum class Weekday(val rawValue: Int) {
    SUNDAY(1),
    MONDAY(2),
    TUESDAY(3),
    WEDNESDAY(4),
    THURSDAY(5),
    FRIDAY(6),
    SATURDAY(7);

    val label: String
        get() = when (this) {
            SUNDAY -> "Sunday"
            MONDAY -> "Monday"
            TUESDAY -> "Tuesday"
            WEDNESDAY -> "Wednesday"
            THURSDAY -> "Thursday"
            FRIDAY -> "Friday"
            SATURDAY -> "Saturday"
        }

    val short: String get() = label.take(3)

    companion object {
        fun of(date: Date, calendar: Calendar = Calendar.getInstance()): Weekday {
            val cal = calendar.clone() as Calendar
            cal.time = date
            return fromRawValue(cal.get(Calendar.DAY_OF_WEEK)) ?: MONDAY
        }

        fun fromRawValue(rawValue: Int): Weekday? = entries.firstOrNull { it.rawValue == rawValue }

        /** Ordered Monday → Sunday for display (most people's mental week). */
        val displayOrder: List<Weekday> =
            listOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY)
    }
}

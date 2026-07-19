package com.krya1012.clearance.domain

import java.util.Calendar
import java.util.TimeZone

/**
 * Pure threshold computation extracted from iOS `checkAutoReset()`. The
 * nightly reset fires once the "today, resetHour:00" threshold has passed
 * since the last reset — using yesterday's threshold before that time of
 * day arrives, so e.g. a 3 AM reset hour covers the whole night.
 */
object AutoReset {

    fun resetThreshold(nowMillis: Long, resetHour: Int, zone: TimeZone = TimeZone.getDefault()): Long {
        val now = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
        val todayThreshold = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, resetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return if (nowMillis >= todayThreshold.timeInMillis) {
            todayThreshold.timeInMillis
        } else {
            (todayThreshold.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        }
    }

    fun shouldAutoReset(
        lastAutoResetMillis: Long,
        nowMillis: Long,
        resetHour: Int,
        zone: TimeZone = TimeZone.getDefault(),
    ): Boolean = lastAutoResetMillis < resetThreshold(nowMillis, resetHour, zone)
}

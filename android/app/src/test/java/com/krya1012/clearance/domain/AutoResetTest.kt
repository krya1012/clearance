package com.krya1012.clearance.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AutoResetTest {

    private val utc = TimeZone.getTimeZone("UTC")

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
        }.timeInMillis

    @Test
    fun `threshold is today's reset hour when now is already past it`() {
        val now = millisAt(2026, 7, 19, 10, 0) // 10:00, resetHour 3
        val expected = millisAt(2026, 7, 19, 3, 0)
        assertEquals(expected, AutoReset.resetThreshold(now, resetHour = 3, zone = utc))
    }

    @Test
    fun `threshold falls back to yesterday's reset hour when now is before it`() {
        val now = millisAt(2026, 7, 19, 1, 0) // 01:00, before the 3 AM threshold
        val expected = millisAt(2026, 7, 18, 3, 0)
        assertEquals(expected, AutoReset.resetThreshold(now, resetHour = 3, zone = utc))
    }

    @Test
    fun `resets when last reset predates the threshold`() {
        val now = millisAt(2026, 7, 19, 10, 0)
        val lastReset = millisAt(2026, 7, 18, 10, 0) // yesterday, before today's 3 AM threshold
        assertTrue(AutoReset.shouldAutoReset(lastReset, now, resetHour = 3, zone = utc))
    }

    @Test
    fun `does not reset twice within the same day after the threshold`() {
        val now = millisAt(2026, 7, 19, 10, 0)
        val lastReset = millisAt(2026, 7, 19, 3, 0) // already reset exactly at today's threshold
        assertFalse(AutoReset.shouldAutoReset(lastReset, now, resetHour = 3, zone = utc))
    }

    @Test
    fun `overnight before the threshold still uses yesterday's reset`() {
        val now = millisAt(2026, 7, 19, 1, 0) // 01:00, hasn't crossed today's 3 AM yet
        val lastReset = millisAt(2026, 7, 18, 3, 0) // reset yesterday at its threshold
        assertFalse(AutoReset.shouldAutoReset(lastReset, now, resetHour = 3, zone = utc))
    }
}

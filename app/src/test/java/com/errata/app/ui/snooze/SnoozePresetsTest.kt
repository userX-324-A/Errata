package com.errata.app.ui.snooze

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoozePresetsTest {

    private val zone = ZoneOffset.UTC

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDate.of(year, month, day)
            .atTime(LocalTime.of(hour, minute))
            .toInstant(zone)
            .toEpochMilli()

    @Test
    fun clock_futureToday() {
        val now = at(2026, 3, 10, 10, 0)
        val until = SnoozePresets.untilEpochMsForClock(
            hour = 14,
            minute = 30,
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(at(2026, 3, 10, 14, 30), until)
    }

    @Test
    fun clock_pastToday_rollsToTomorrow() {
        val now = at(2026, 3, 10, 15, 0)
        val until = SnoozePresets.untilEpochMsForClock(
            hour = 9,
            minute = 0,
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(at(2026, 3, 11, 9, 0), until)
    }

    @Test
    fun clock_equalNow_rollsAndFloorsOneMinute() {
        val now = at(2026, 3, 10, 12, 0)
        val until = SnoozePresets.untilEpochMsForClock(
            hour = 12,
            minute = 0,
            nowEpochMs = now,
            zone = zone,
        )
        // Equal → tomorrow 12:00, which is > now+1m
        assertEquals(at(2026, 3, 11, 12, 0), until)
        assertTrue(until >= now + 60_000L)
    }

    @Test
    fun clock_oneMinuteAhead_keepsToday() {
        val now = at(2026, 3, 10, 12, 0)
        val until = SnoozePresets.untilEpochMsForClock(
            hour = 12,
            minute = 1,
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(at(2026, 3, 10, 12, 1), until)
    }

    @Test
    fun laterToday_newYorkSpringForward_sixPmSameCalendarDay() {
        val ny = java.time.ZoneId.of("America/New_York")
        val now = LocalDate.of(2026, 3, 8).atTime(10, 0).atZone(ny).toInstant().toEpochMilli()
        val until = SnoozePresets.untilEpochMs(
            SnoozePreset.LATER_TODAY,
            nowEpochMs = now,
            zone = ny,
        )
        val expected = LocalDate.of(2026, 3, 8).atTime(18, 0).atZone(ny).toInstant().toEpochMilli()
        assertEquals(expected, until)
    }

    @Test
    fun tomorrow_newYorkFallBack_isNextLocalMidnight() {
        val ny = java.time.ZoneId.of("America/New_York")
        val now = LocalDate.of(2026, 11, 1).atTime(23, 0).atZone(ny).toInstant().toEpochMilli()
        val until = SnoozePresets.untilEpochMs(
            SnoozePreset.TOMORROW,
            nowEpochMs = now,
            zone = ny,
        )
        val expected = LocalDate.of(2026, 11, 2).atStartOfDay(ny).toInstant().toEpochMilli()
        assertEquals(expected, until)
    }
}

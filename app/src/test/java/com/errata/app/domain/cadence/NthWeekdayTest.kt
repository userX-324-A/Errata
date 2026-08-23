package com.errata.app.domain.cadence

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NthWeekdayTest {

    private val saturday = Weekdays.bit(DayOfWeek.SATURDAY)

    @Test
    fun may2026_fourthSaturdayIsNotLast() {
        // May 2026 Saturdays: 2, 9, 16, 23, 30
        val fourth = LocalDate.of(2026, 5, 23)
        val last = LocalDate.of(2026, 5, 30)
        assertTrue(NthWeekday.matches(fourth, 4, saturday))
        assertFalse(NthWeekday.matches(fourth, NthWeekday.LAST, saturday))
        assertTrue(NthWeekday.matches(last, NthWeekday.LAST, saturday))
        assertFalse(NthWeekday.matches(last, 4, saturday))
    }

    @Test
    fun february2026_fourthSaturdayIsLast() {
        // Feb 2026 Saturdays: 7, 14, 21, 28
        val day = LocalDate.of(2026, 2, 28)
        assertTrue(NthWeekday.matches(day, 4, saturday))
        assertTrue(NthWeekday.matches(day, NthWeekday.LAST, saturday))
    }

    @Test
    fun firstSaturday_isDay2InMay2026() {
        assertTrue(NthWeekday.matches(LocalDate.of(2026, 5, 2), 1, saturday))
        assertFalse(NthWeekday.matches(LocalDate.of(2026, 5, 9), 1, saturday))
    }

    @Test
    fun summary_firstAndLast() {
        val locale = Locale.US
        assertEquals("1st Sat", NthWeekday.summary(1, saturday, locale))
        assertEquals(
            "Last Fri",
            NthWeekday.summary(NthWeekday.LAST, Weekdays.bit(DayOfWeek.FRIDAY), locale),
        )
    }
}

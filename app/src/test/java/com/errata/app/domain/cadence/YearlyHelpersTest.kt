package com.errata.app.domain.cadence

import java.time.LocalDate
import java.time.Month
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YearlyHelpersTest {

    @Test
    fun yearMonths_marchAndSeptember() {
        val mask = YearMonths.bit(Month.MARCH) or YearMonths.bit(Month.SEPTEMBER)
        assertTrue(YearMonths.matches(LocalDate.of(2026, 3, 1), mask, 1))
        assertTrue(YearMonths.matches(LocalDate.of(2026, 9, 1), mask, 1))
        assertFalse(YearMonths.matches(LocalDate.of(2026, 3, 2), mask, 1))
        assertFalse(YearMonths.matches(LocalDate.of(2026, 4, 1), mask, 1))
    }

    @Test
    fun yearMonths_feb29_clampsInNonLeap() {
        val mask = YearMonths.bit(Month.FEBRUARY)
        assertTrue(YearMonths.matches(LocalDate.of(2026, 2, 28), mask, 29))
        assertFalse(YearMonths.matches(LocalDate.of(2026, 2, 27), mask, 29))
        assertTrue(YearMonths.matches(LocalDate.of(2024, 2, 29), mask, 29))
    }

    @Test
    fun seasons_civilDates() {
        assertTrue(Seasons.matches(LocalDate.of(2026, 3, 20), Seasons.SPRING))
        assertFalse(Seasons.matches(LocalDate.of(2026, 3, 21), Seasons.SPRING))
        assertTrue(
            Seasons.matches(
                LocalDate.of(2026, 9, 22),
                Seasons.SPRING or Seasons.AUTUMN,
            ),
        )
    }

    @Test
    fun yearly_summary_seasonsAndMonths() {
        val locale = Locale.US
        val months = YearMonths.bit(Month.SEPTEMBER)
        assertEquals(
            "Yearly · Spring, Sep 1",
            Yearly.summary(Seasons.SPRING, months, 1, locale),
        )
        assertEquals("Yearly · Spring, Autumn", Yearly.summary(Seasons.SPRING or Seasons.AUTUMN, 0, 0, locale))
    }

    @Test
    fun yearly_isValid() {
        assertFalse(Yearly.isValid(0, 0, 0))
        assertTrue(Yearly.isValid(0, Seasons.SPRING, 0))
        assertFalse(Yearly.isValid(YearMonths.bit(Month.MARCH), 0, 0))
        assertTrue(Yearly.isValid(YearMonths.bit(Month.MARCH), 0, 15))
    }
}

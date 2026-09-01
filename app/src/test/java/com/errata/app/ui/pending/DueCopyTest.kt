package com.errata.app.ui.pending

import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.due.DueBucket
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertTrue
import org.junit.Test

class DueCopyTest {

    private val zone = ZoneOffset.UTC

    @Test
    fun dueToday_includesTimeAndEstimate() {
        val due = LocalDate.of(2026, 3, 10).atTime(15, 30).toInstant(zone).toEpochMilli()
        val now = LocalDate.of(2026, 3, 10).atTime(12, 0).toInstant(zone).toEpochMilli()
        val text = DueCopy.subtitle(
            bucket = DueBucket.DUE_TODAY,
            nextDueAtEpochMs = due,
            snoozedUntilEpochMs = null,
            estimateMinutes = 15,
            nowEpochMs = now,
            zone = zone,
        )
        assertTrue(text.contains("Due today"))
        assertTrue(text.contains("~15 min"))
        assertTrue(text.contains("3:30") || text.contains("15:30"))
    }

    @Test
    fun dueToday_usesInjectedTimeFormat() {
        val due = LocalDate.of(2026, 3, 10).atTime(15, 30).toInstant(zone).toEpochMilli()
        val now = LocalDate.of(2026, 3, 10).atTime(12, 0).toInstant(zone).toEpochMilli()
        val text = DueCopy.subtitle(
            bucket = DueBucket.DUE_TODAY,
            nextDueAtEpochMs = due,
            snoozedUntilEpochMs = null,
            estimateMinutes = 15,
            nowEpochMs = now,
            zone = zone,
            formatTime = { "24h-label" },
        )
        assertTrue(text.contains("24h-label"))
    }

    @Test
    fun overdue_countsDays() {
        val due = CadenceCalculator.startOfDayEpochMs(LocalDate.of(2026, 3, 7).toEpochDay(), zone)
        val now = LocalDate.of(2026, 3, 10).atTime(12, 0).toInstant(zone).toEpochMilli()
        val text = DueCopy.subtitle(
            bucket = DueBucket.OVERDUE,
            nextDueAtEpochMs = due,
            snoozedUntilEpochMs = null,
            estimateMinutes = 20,
            nowEpochMs = now,
            zone = zone,
        )
        assertTrue(text.contains("3 days overdue"))
        assertTrue(text.contains("~20 min"))
    }

    @Test
    fun soon_tomorrow_isPlainLanguage() {
        val due = LocalDate.of(2026, 3, 11).atTime(9, 0).toInstant(zone).toEpochMilli()
        val now = LocalDate.of(2026, 3, 10).atTime(18, 0).toInstant(zone).toEpochMilli()
        val text = DueCopy.subtitle(
            bucket = DueBucket.SOON,
            nextDueAtEpochMs = due,
            snoozedUntilEpochMs = null,
            estimateMinutes = 20,
            nowEpochMs = now,
            zone = zone,
            formatTime = { "9:00 AM" },
        )
        assertTrue(text.contains("Due tomorrow"))
        assertTrue(!text.contains("2026"))
        assertTrue(text.contains("~20 min"))
    }

    @Test
    fun soon_weekday_usesFullDayName() {
        val due = LocalDate.of(2026, 3, 13).atTime(9, 0).toInstant(zone).toEpochMilli()
        val now = LocalDate.of(2026, 3, 10).atTime(12, 0).toInstant(zone).toEpochMilli()
        val text = DueCopy.subtitle(
            bucket = DueBucket.SOON,
            nextDueAtEpochMs = due,
            snoozedUntilEpochMs = null,
            estimateMinutes = 40,
            nowEpochMs = now,
            zone = zone,
            formatTime = { "9:00 AM" },
            locale = java.util.Locale.US,
        )
        assertTrue(text.contains("Due Friday"))
        assertTrue(!text.contains("Mar"))
    }

    @Test
    fun soon_weekOut_keepsMediumDate() {
        val due = LocalDate.of(2026, 3, 17).atTime(9, 0).toInstant(zone).toEpochMilli()
        val now = LocalDate.of(2026, 3, 10).atTime(12, 0).toInstant(zone).toEpochMilli()
        val text = DueCopy.subtitle(
            bucket = DueBucket.SOON,
            nextDueAtEpochMs = due,
            snoozedUntilEpochMs = null,
            estimateMinutes = 15,
            nowEpochMs = now,
            zone = zone,
            formatTime = { "9:00 AM" },
            locale = java.util.Locale.US,
        )
        assertTrue(text.contains("2026") || text.contains("Mar"))
        assertTrue(!text.contains("tomorrow"))
    }
}

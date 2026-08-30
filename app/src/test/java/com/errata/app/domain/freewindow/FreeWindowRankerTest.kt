package com.errata.app.domain.freewindow

import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.freewindow.FreeWindowSelection
import com.errata.app.domain.freewindow.remainingMinutes
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeWindowRankerTest {

    private val zone = ZoneOffset.UTC

    private fun noon(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atTime(12, 0).toInstant(zone).toEpochMilli()

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDate.of(year, month, day)
            .atTime(LocalTime.of(hour, minute))
            .toInstant(zone)
            .toEpochMilli()

    private fun candidate(
        id: Long,
        title: String,
        estimate: Int,
        bucket: DueBucket,
        dueMs: Long = noon(2026, 3, 10),
    ) = FreeWindowRanker.Candidate(
        id = id,
        title = title,
        estimateMinutes = estimate,
        bucket = bucket,
        nextDueAtEpochMs = dueMs,
        snoozedUntilEpochMs = null,
    )

    @Test
    fun excludesOverrun() {
        val result = FreeWindowRanker.rank(
            listOf(
                candidate(1, "Short", 10, DueBucket.DUE_TODAY),
                candidate(2, "Long", 45, DueBucket.OVERDUE),
            ),
            availableMinutes = 20,
        )
        assertEquals(listOf(1L), result.fits.map { it.id })
        assertEquals(10, result.leftoverAfterBestMinutes)
    }

    @Test
    fun urgencyBeforeLargerEstimate() {
        val result = FreeWindowRanker.rank(
            listOf(
                candidate(1, "Soon big", 20, DueBucket.SOON),
                candidate(2, "Overdue small", 10, DueBucket.OVERDUE),
                candidate(3, "Today mid", 15, DueBucket.DUE_TODAY),
            ),
            availableMinutes = 30,
        )
        assertEquals(listOf(2L, 3L, 1L), result.fits.map { it.id })
        assertEquals(20, result.leftoverAfterBestMinutes)
    }

    @Test
    fun withinBandPrefersLargerEstimate() {
        val result = FreeWindowRanker.rank(
            listOf(
                candidate(1, "A", 10, DueBucket.DUE_TODAY),
                candidate(2, "B", 25, DueBucket.DUE_TODAY),
                candidate(3, "C", 20, DueBucket.DUE_TODAY),
            ),
            availableMinutes = 30,
        )
        assertEquals(listOf(2L, 3L, 1L), result.fits.map { it.id })
        // 30 − 25 (best), not 30 − 25 − 20 − 10.
        assertEquals(5, result.leftoverAfterBestMinutes)
    }

    @Test
    fun emptyWhenNothingFits() {
        val result = FreeWindowRanker.rank(
            listOf(candidate(1, "Big", 60, DueBucket.OVERDUE)),
            availableMinutes = 15,
        )
        assertTrue(result.fits.isEmpty())
        assertNull(result.leftoverAfterBestMinutes)
    }

    @Test
    fun ignoresLaterAndHiddenBuckets() {
        val result = FreeWindowRanker.rank(
            listOf(
                candidate(1, "Later", 5, DueBucket.LATER),
                candidate(2, "Hidden", 5, DueBucket.HIDDEN),
                candidate(3, "Ok", 5, DueBucket.SOON),
            ),
            availableMinutes = 15,
        )
        assertEquals(listOf(3L), result.fits.map { it.id })
    }

    @Test
    fun zeroWindowYieldsEmpty() {
        val result = FreeWindowRanker.rank(
            listOf(candidate(1, "A", 5, DueBucket.DUE_TODAY)),
            availableMinutes = 0,
        )
        assertTrue(result.fits.isEmpty())
    }

    @Test
    fun minutesUntilWorkStart_ahead() {
        val now = at(2026, 3, 10, 8, 0)
        val minutes = FreeWindowRanker.minutesUntilWorkStart(
            workStartMinutesOfDay = 9 * 60,
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(60, minutes)
    }

    @Test
    fun minutesUntilWorkStart_pastOrUnset() {
        val now = at(2026, 3, 10, 10, 0)
        assertNull(
            FreeWindowRanker.minutesUntilWorkStart(
                workStartMinutesOfDay = 9 * 60,
                nowEpochMs = now,
                zone = zone,
            ),
        )
        assertNull(
            FreeWindowRanker.minutesUntilWorkStart(
                workStartMinutesOfDay = null,
                nowEpochMs = now,
                zone = zone,
            ),
        )
    }

    @Test
    fun minutesUntilStopBy_sameHelper() {
        val now = at(2026, 3, 10, 14, 0)
        assertEquals(
            90,
            FreeWindowRanker.minutesUntilStopBy(
                stopByMinutesOfDay = 15 * 60 + 30,
                nowEpochMs = now,
                zone = zone,
            ),
        )
    }

    @Test
    fun untilClockRemaining_shrinksWithNow() {
        val clock = FreeWindowSelection.UntilClock(9 * 60)
        val day = LocalDate.of(2026, 3, 10)
        assertEquals(
            60,
            clock.remainingMinutes(
                day.atTime(LocalTime.of(8, 0)).toInstant(zone).toEpochMilli(),
                zone,
            ),
        )
        assertEquals(
            20,
            clock.remainingMinutes(
                day.atTime(LocalTime.of(8, 40)).toInstant(zone).toEpochMilli(),
                zone,
            ),
        )
        assertEquals(
            0,
            clock.remainingMinutes(
                day.atTime(LocalTime.of(10, 0)).toInstant(zone).toEpochMilli(),
                zone,
            ),
        )
    }
}

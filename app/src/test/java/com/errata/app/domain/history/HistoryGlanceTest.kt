package com.errata.app.domain.history

import com.errata.app.domain.cadence.CadenceCalculator
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HistoryGlanceTest {

    private val zone = ZoneOffset.UTC

    private fun at(day: Int): Long =
        CadenceCalculator.startOfDayEpochMs(LocalDate.of(2026, 3, day).toEpochDay(), zone)

    private fun atMonth(month: Int, day: Int): Long =
        CadenceCalculator.startOfDayEpochMs(LocalDate.of(2026, month, day).toEpochDay(), zone)

    private fun sample(completedDay: Int, scheduledDay: Int) = HistoryGlance.Sample(
        completedAtEpochMs = at(completedDay),
        scheduledDueAtEpochMs = at(scheduledDay),
    )

    @Test
    fun empty_returnsNull() {
        assertNull(HistoryGlance.from(emptyList(), zone))
    }

    @Test
    fun oneOrTwo_lastOnly_noTypical() {
        val one = HistoryGlance.from(listOf(sample(completedDay = 12, scheduledDay = 10)), zone)!!
        assertEquals(at(12), one.lastCompletedEpochMs)
        assertNull(one.typical)

        val two = HistoryGlance.from(
            listOf(
                sample(completedDay = 12, scheduledDay = 10),
                sample(completedDay = 5, scheduledDay = 5),
            ),
            zone,
        )!!
        assertEquals(at(12), two.lastCompletedEpochMs)
        assertNull(two.typical)
    }

    @Test
    fun three_onDueDay() {
        val glance = HistoryGlance.from(
            listOf(
                sample(10, 10),
                sample(11, 11),
                sample(12, 12),
            ),
            zone,
        )!!
        assertEquals(at(12), glance.lastCompletedEpochMs)
        assertEquals(TypicalLateness.OnDueDay, glance.typical)
    }

    @Test
    fun three_usuallyAfter() {
        val glance = HistoryGlance.from(
            listOf(
                sample(12, 10),
                sample(13, 11),
                sample(14, 12),
            ),
            zone,
        )!!
        assertEquals(TypicalLateness.DaysAfter(2), glance.typical)
    }

    @Test
    fun three_usuallyBefore() {
        val glance = HistoryGlance.from(
            listOf(
                sample(8, 10),
                sample(9, 11),
                sample(10, 12),
            ),
            zone,
        )!!
        assertEquals(TypicalLateness.DaysBefore(2), glance.typical)
    }

    @Test
    fun usesLastEight_notOlder() {
        val lateOlder = (1..8).map { day ->
            HistoryGlance.Sample(
                completedAtEpochMs = atMonth(2, day),
                scheduledDueAtEpochMs = atMonth(2, day) - 10L * 86_400_000L,
            )
        }
        val recentOnTime = (20..27).map { day -> sample(day, day) }
        val glance = HistoryGlance.from(lateOlder + recentOnTime, zone)!!
        assertEquals(at(27), glance.lastCompletedEpochMs)
        assertEquals(TypicalLateness.OnDueDay, glance.typical)
    }

    @Test
    fun evenWindow_medianHalfUp() {
        val glance = HistoryGlance.from(
            listOf(
                sample(10, 10),
                sample(11, 10),
                sample(12, 10),
                sample(13, 10),
            ),
            zone,
        )!!
        // deltas 0,1,2,3 → median (1+2)/2 = 1.5 → 2
        assertEquals(TypicalLateness.DaysAfter(2), glance.typical)
    }

    @Test
    fun roundHalfUp_awayFromZero() {
        assertEquals(2, HistoryGlance.roundHalfUp(1.5))
        assertEquals(-2, HistoryGlance.roundHalfUp(-1.5))
        assertEquals(0, HistoryGlance.roundHalfUp(0.4))
    }
}

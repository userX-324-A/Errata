package com.errata.app.domain.cadence

import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CadenceCalculatorTest {

    private val zone = ZoneOffset.UTC

    private fun day(year: Int, month: Int, day: Int): Long =
        CadenceCalculator.startOfDayEpochMs(
            LocalDate.of(year, month, day).toEpochDay(),
            zone,
        )

    private fun noon(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atTime(12, 0).toInstant(zone).toEpochMilli()

    @Test
    fun fromCompletion_addsIntervalFromCompletionDay() {
        val completed = noon(2026, 1, 10)
        val scheduled = day(2026, 1, 10)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = 14,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 10).toEpochDay(),
            zone = zone,
        )
        assertEquals(day(2026, 1, 24), next)
    }

    @Test
    fun catchUp_mildOverdue_behavesLikeFromCompletion() {
        // Due Jan 1, completed Jan 5 (4 days late). Interval 14 → half = 7 → mild.
        val scheduled = day(2026, 1, 1)
        val completed = noon(2026, 1, 5)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION_CATCH_UP,
            intervalDays = 14,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            zone = zone,
        )
        assertEquals(day(2026, 1, 19), next)
    }

    @Test
    fun catchUp_severeOverdue_compressesButRespectsFloor() {
        // Due Jan 1, completed Feb 15 (45 days late). Interval 14.
        // overdue > 7 days → catch-up. floor = 7 days. max catchUp = 3.5 days.
        val scheduled = day(2026, 1, 1)
        val completed = noon(2026, 2, 15)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION_CATCH_UP,
            intervalDays = 14,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            zone = zone,
        )
        val fullInterval = day(2026, 3, 1) // Feb 15 + 14 = Mar 1
        val floorSoonest = day(2026, 2, 22) // Feb 15 + 7
        assertTrue("next should be before full from-completion", next < fullInterval)
        assertTrue("next should be on or after floor day", next >= floorSoonest)
    }

    @Test
    fun fixedAnchor_advancesPastCompletion() {
        val anchor = LocalDate.of(2026, 1, 1).toEpochDay()
        val completed = noon(2026, 1, 20) // after Jan 15 grid slot
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FIXED_ANCHOR,
            intervalDays = 14,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = day(2026, 1, 15),
            anchorEpochDay = anchor,
            zone = zone,
        )
        // Grid: Jan 1, 15, 29, ...
        assertEquals(day(2026, 1, 29), next)
    }

    @Test
    fun fixedAnchor_sameDayCompletion_movesToNextSlot() {
        val anchor = LocalDate.of(2026, 1, 1).toEpochDay()
        val completed = noon(2026, 1, 15)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FIXED_ANCHOR,
            intervalDays = 14,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = day(2026, 1, 15),
            anchorEpochDay = anchor,
            zone = zone,
        )
        assertEquals(day(2026, 1, 29), next)
    }

    @Test
    fun fromCompletion_preservesTimeOfDayFromScheduledDue() {
        val scheduled = LocalDate.of(2026, 1, 10).atTime(15, 30).toInstant(zone).toEpochMilli()
        val completed = noon(2026, 1, 12)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = 14,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 10).toEpochDay(),
            zone = zone,
        )
        val expected = LocalDate.of(2026, 1, 26).atTime(15, 30).toInstant(zone).toEpochMilli()
        assertEquals(expected, next)
    }

    @Test
    fun skip_fromCompletion_advancesOneInterval() {
        val scheduled = noon(2026, 3, 10)
        val now = noon(2026, 3, 10)
        val next = CadenceCalculator.nextDueAfterSkip(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = 7,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 3, 10).toEpochDay(),
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(noon(2026, 3, 17), next)
    }

    @Test
    fun skip_fromCompletion_overdueMultiInterval_landsAfterNow() {
        val scheduled = noon(2026, 1, 1)
        val now = noon(2026, 2, 1)
        val next = CadenceCalculator.nextDueAfterSkip(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = 7,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            nowEpochMs = now,
            zone = zone,
        )
        assertTrue(next > now)
        // Jan 1 + 7*k until after Feb 1 → Feb 5
        assertEquals(noon(2026, 2, 5), next)
    }

    @Test
    fun skip_catchUpMode_matchesFromCompletion() {
        val scheduled = noon(2026, 1, 1)
        val now = noon(2026, 2, 1)
        val fromCompletion = CadenceCalculator.nextDueAfterSkip(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = 7,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            nowEpochMs = now,
            zone = zone,
        )
        val catchUp = CadenceCalculator.nextDueAfterSkip(
            mode = CadenceMode.FROM_COMPLETION_CATCH_UP,
            intervalDays = 7,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 1).toEpochDay(),
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(fromCompletion, catchUp)
    }

    @Test
    fun skip_fixedAnchor_nextSlotAfterNow() {
        val anchor = LocalDate.of(2026, 1, 1).toEpochDay()
        val scheduled = day(2026, 1, 15)
        val now = noon(2026, 1, 20)
        val next = CadenceCalculator.nextDueAfterSkip(
            mode = CadenceMode.FIXED_ANCHOR,
            intervalDays = 14,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = anchor,
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(day(2026, 1, 29), next)
    }
}

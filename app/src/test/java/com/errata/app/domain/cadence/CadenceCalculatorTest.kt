package com.errata.app.domain.cadence

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
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

    @Test
    fun weekly_nextTuesdayAfterWednesdayDone() {
        val scheduled = noon(2026, 1, 7) // Wednesday
        val completed = noon(2026, 1, 7)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION_CATCH_UP,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 7).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.WEEKLY,
            weekdaysMask = Weekdays.bit(DayOfWeek.TUESDAY),
        )
        assertEquals(noon(2026, 1, 13), next)
    }

    @Test
    fun weekly_twoWeekdaysWrapToNextWeek() {
        val mask = Weekdays.bit(DayOfWeek.MONDAY) or
            Weekdays.bit(DayOfWeek.WEDNESDAY)
        val scheduled = noon(2026, 1, 7) // Wednesday
        val completed = noon(2026, 1, 8) // Thursday
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 7).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.WEEKLY,
            weekdaysMask = mask,
        )
        assertEquals(noon(2026, 1, 12), next)
    }

    @Test
    fun weekly_sameDayAfterDueTime_movesToNextSelected() {
        val mask = Weekdays.bit(DayOfWeek.TUESDAY) or
            Weekdays.bit(DayOfWeek.FRIDAY)
        val scheduled = LocalDate.of(2026, 1, 9).atTime(12, 0).toInstant(zone).toEpochMilli()
        val completed = LocalDate.of(2026, 1, 9).atTime(16, 0).toInstant(zone).toEpochMilli()
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FIXED_ANCHOR,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 9).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.WEEKLY,
            weekdaysMask = mask,
        )
        assertEquals(noon(2026, 1, 13), next)
    }

    @Test
    fun monthly_day31_clampsToFebruary() {
        val scheduled = noon(2026, 1, 31)
        val completed = noon(2026, 1, 31)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 31).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.MONTHLY,
            monthDay = 31,
        )
        assertEquals(noon(2026, 2, 28), next)
    }

    @Test
    fun monthly_day31_leapFebruary() {
        val scheduled = noon(2024, 1, 31)
        val completed = noon(2024, 1, 31)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2024, 1, 31).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.MONTHLY,
            monthDay = 31,
        )
        assertEquals(noon(2024, 2, 29), next)
    }

    @Test
    fun monthly_skip_matchesDone() {
        val scheduled = noon(2026, 3, 15)
        val now = noon(2026, 3, 15)
        val afterDone = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FIXED_ANCHOR,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = now,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 3, 15).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.MONTHLY,
            monthDay = 15,
        )
        val afterSkip = CadenceCalculator.nextDueAfterSkip(
            mode = CadenceMode.FIXED_ANCHOR,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 3, 15).toEpochDay(),
            nowEpochMs = now,
            zone = zone,
            scheduleKind = ScheduleKind.MONTHLY,
            monthDay = 15,
        )
        assertEquals(noon(2026, 4, 15), afterDone)
        assertEquals(afterDone, afterSkip)
    }

    @Test
    fun weekly_skip_matchesDone() {
        val mask = Weekdays.bit(DayOfWeek.TUESDAY)
        val scheduled = noon(2026, 1, 7)
        val now = noon(2026, 1, 7)
        val afterDone = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION_CATCH_UP,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = now,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 7).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.WEEKLY,
            weekdaysMask = mask,
        )
        val afterSkip = CadenceCalculator.nextDueAfterSkip(
            mode = CadenceMode.FROM_COMPLETION_CATCH_UP,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 1, 7).toEpochDay(),
            nowEpochMs = now,
            zone = zone,
            scheduleKind = ScheduleKind.WEEKLY,
            weekdaysMask = mask,
        )
        assertEquals(noon(2026, 1, 13), afterDone)
        assertEquals(afterDone, afterSkip)
    }

    @Test
    fun nthWeekday_firstSaturday_advancesToNextMonth() {
        val mask = Weekdays.bit(DayOfWeek.SATURDAY)
        // 2 May 2026 is the first Saturday
        val scheduled = noon(2026, 5, 2)
        val completed = noon(2026, 5, 2)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION_CATCH_UP,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = completed,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 5, 2).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.NTH_WEEKDAY,
            weekdaysMask = mask,
            weekdayOrdinal = 1,
        )
        assertEquals(noon(2026, 6, 6), next)
    }

    @Test
    fun nthWeekday_may2026_fourthIsNotLast() {
        val mask = Weekdays.bit(DayOfWeek.SATURDAY)
        val fourthDue = noon(2026, 5, 23)
        val lastDue = noon(2026, 5, 30)
        val afterFourth = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = fourthDue,
            scheduledDueAtEpochMs = fourthDue,
            anchorEpochDay = LocalDate.of(2026, 5, 23).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.NTH_WEEKDAY,
            weekdaysMask = mask,
            weekdayOrdinal = 4,
        )
        val afterLast = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = lastDue,
            scheduledDueAtEpochMs = lastDue,
            anchorEpochDay = LocalDate.of(2026, 5, 30).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.NTH_WEEKDAY,
            weekdaysMask = mask,
            weekdayOrdinal = NthWeekday.LAST,
        )
        assertEquals(noon(2026, 6, 27), afterFourth)
        assertEquals(noon(2026, 6, 27), afterLast)
        assertTrue(fourthDue != lastDue)
    }

    @Test
    fun nthWeekday_february2026_fourthEqualsLast_stillAdvancesAMonth() {
        val mask = Weekdays.bit(DayOfWeek.SATURDAY)
        val scheduled = noon(2026, 2, 28)
        val afterFourth = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FIXED_ANCHOR,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = scheduled,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 2, 28).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.NTH_WEEKDAY,
            weekdaysMask = mask,
            weekdayOrdinal = 4,
        )
        val afterLast = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FIXED_ANCHOR,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = scheduled,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 2, 28).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.NTH_WEEKDAY,
            weekdaysMask = mask,
            weekdayOrdinal = NthWeekday.LAST,
        )
        assertEquals(noon(2026, 3, 28), afterFourth)
        assertEquals(afterFourth, afterLast)
    }

    @Test
    fun nthWeekday_skip_matchesDone() {
        val mask = Weekdays.bit(DayOfWeek.SATURDAY)
        val scheduled = noon(2026, 5, 2)
        val now = noon(2026, 5, 2)
        val afterDone = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = now,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 5, 2).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.NTH_WEEKDAY,
            weekdaysMask = mask,
            weekdayOrdinal = 1,
        )
        val afterSkip = CadenceCalculator.nextDueAfterSkip(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 5, 2).toEpochDay(),
            nowEpochMs = now,
            zone = zone,
            scheduleKind = ScheduleKind.NTH_WEEKDAY,
            weekdaysMask = mask,
            weekdayOrdinal = 1,
        )
        assertEquals(noon(2026, 6, 6), afterDone)
        assertEquals(afterDone, afterSkip)
    }

    @Test
    fun yearly_march15_advancesToNextYear() {
        val mask = YearMonths.bit(Month.MARCH)
        val scheduled = noon(2026, 3, 15)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = scheduled,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 3, 15).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.YEARLY,
            monthDay = 15,
            yearMonthsMask = mask,
        )
        assertEquals(noon(2027, 3, 15), next)
    }

    @Test
    fun yearly_marchAndSeptember_day1() {
        val mask = YearMonths.bit(Month.MARCH) or YearMonths.bit(Month.SEPTEMBER)
        val afterMarch = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = noon(2026, 3, 1),
            scheduledDueAtEpochMs = noon(2026, 3, 1),
            anchorEpochDay = LocalDate.of(2026, 3, 1).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.YEARLY,
            monthDay = 1,
            yearMonthsMask = mask,
        )
        val afterSeptember = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = noon(2026, 9, 1),
            scheduledDueAtEpochMs = noon(2026, 9, 1),
            anchorEpochDay = LocalDate.of(2026, 9, 1).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.YEARLY,
            monthDay = 1,
            yearMonthsMask = mask,
        )
        assertEquals(noon(2026, 9, 1), afterMarch)
        assertEquals(noon(2027, 3, 1), afterSeptember)
    }

    @Test
    fun yearly_feb29_clampsInNonLeapYear() {
        val mask = YearMonths.bit(Month.FEBRUARY)
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = noon(2026, 1, 31),
            scheduledDueAtEpochMs = noon(2026, 1, 31),
            anchorEpochDay = LocalDate.of(2026, 1, 31).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.YEARLY,
            monthDay = 29,
            yearMonthsMask = mask,
        )
        assertEquals(noon(2026, 2, 28), next)
    }

    @Test
    fun yearly_springOnly_nextYear() {
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FIXED_ANCHOR,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = noon(2026, 3, 20),
            scheduledDueAtEpochMs = noon(2026, 3, 20),
            anchorEpochDay = LocalDate.of(2026, 3, 20).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.YEARLY,
            seasonMask = Seasons.SPRING,
        )
        assertEquals(noon(2027, 3, 20), next)
    }

    @Test
    fun yearly_springAndAutumn_sameYear() {
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = noon(2026, 3, 20),
            scheduledDueAtEpochMs = noon(2026, 3, 20),
            anchorEpochDay = LocalDate.of(2026, 3, 20).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.YEARLY,
            seasonMask = Seasons.SPRING or Seasons.AUTUMN,
        )
        assertEquals(noon(2026, 9, 22), next)
    }

    @Test
    fun yearly_marchDay1_andSpring_sameYear() {
        val next = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = noon(2026, 3, 1),
            scheduledDueAtEpochMs = noon(2026, 3, 1),
            anchorEpochDay = LocalDate.of(2026, 3, 1).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.YEARLY,
            monthDay = 1,
            yearMonthsMask = YearMonths.bit(Month.MARCH),
            seasonMask = Seasons.SPRING,
        )
        assertEquals(noon(2026, 3, 20), next)
    }

    @Test
    fun yearly_skip_matchesDone() {
        val mask = YearMonths.bit(Month.MARCH)
        val scheduled = noon(2026, 3, 15)
        val afterDone = CadenceCalculator.nextDueAfterCompletion(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            completedAtEpochMs = scheduled,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 3, 15).toEpochDay(),
            zone = zone,
            scheduleKind = ScheduleKind.YEARLY,
            monthDay = 15,
            yearMonthsMask = mask,
        )
        val afterSkip = CadenceCalculator.nextDueAfterSkip(
            mode = CadenceMode.FROM_COMPLETION,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            scheduledDueAtEpochMs = scheduled,
            anchorEpochDay = LocalDate.of(2026, 3, 15).toEpochDay(),
            nowEpochMs = scheduled,
            zone = zone,
            scheduleKind = ScheduleKind.YEARLY,
            monthDay = 15,
            yearMonthsMask = mask,
        )
        assertEquals(noon(2027, 3, 15), afterDone)
        assertEquals(afterDone, afterSkip)
    }
}

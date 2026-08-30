package com.errata.app.domain.cadence

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Cadence math for Errata.
 *
 * **Due** is a local datetime (`nextDueAtEpochMs`). Pending buckets use the local **calendar day**.
 * After Done, the next due keeps the **time-of-day** from the due that was open (`scheduledDueAt`).
 *
 * Schedule kind is orthogonal to after-Done mode: weekly, monthly, nth-weekday,
 * and yearly pick the next matching local calendar day strictly after Done or Skip
 * and ignore [CadenceMode].
 *
 * Catch-up constants match docs/03-product-map.md.
 */
object CadenceCalculator {

    /** Dummy interval stored on calendar-grid rows so [intervalDays] stays ≥ 1. */
    const val GRID_INTERVAL_DAYS = 7

    fun startOfDayEpochMs(epochDay: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()

    fun epochDayOf(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate().toEpochDay()

    fun minutesOfDay(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
        val t = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalTime()
        return t.hour * 60 + t.minute
    }

    fun atLocalDateMinutes(
        epochDay: Long,
        minutesOfDay: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val clamped = minutesOfDay.coerceIn(0, 24 * 60 - 1)
        val time = LocalTime.of(clamped / 60, clamped % 60)
        return LocalDate.ofEpochDay(epochDay).atTime(time).atZone(zone).toInstant().toEpochMilli()
    }

    /** Same local clock time as [previousDueEpochMs], on [epochDay]. */
    fun atLocalDateKeepingTime(
        epochDay: Long,
        previousDueEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long = atLocalDateMinutes(epochDay, minutesOfDay(previousDueEpochMs, zone), zone)

    /**
     * Next due after a Done.
     *
     * @param scheduledDueAtEpochMs the due that was open (catch-up overdue + time-of-day to preserve)
     * @param anchorEpochDay grid start for [CadenceMode.FIXED_ANCHOR]
     */
    fun nextDueAfterCompletion(
        mode: CadenceMode,
        intervalDays: Int,
        completedAtEpochMs: Long,
        scheduledDueAtEpochMs: Long,
        anchorEpochDay: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        scheduleKind: ScheduleKind = ScheduleKind.INTERVAL,
        weekdaysMask: Int = 0,
        monthDay: Int = 0,
        weekdayOrdinal: Int = 0,
        yearMonthsMask: Int = 0,
        seasonMask: Int = 0,
    ): Long {
        require(intervalDays >= 1) { "intervalDays must be >= 1" }
        nextGridDueAfter(
            afterEpochMs = completedAtEpochMs,
            scheduledDueAtEpochMs = scheduledDueAtEpochMs,
            scheduleKind = scheduleKind,
            weekdaysMask = weekdaysMask,
            monthDay = monthDay,
            weekdayOrdinal = weekdayOrdinal,
            yearMonthsMask = yearMonthsMask,
            seasonMask = seasonMask,
            zone = zone,
        )?.let { return it }

        return when (mode) {
            CadenceMode.FROM_COMPLETION ->
                fromCompletion(completedAtEpochMs, intervalDays, scheduledDueAtEpochMs, zone)

            CadenceMode.FIXED_ANCHOR ->
                fixedAnchor(
                    completedAtEpochMs,
                    intervalDays,
                    anchorEpochDay,
                    scheduledDueAtEpochMs,
                    zone,
                )

            CadenceMode.FROM_COMPLETION_CATCH_UP ->
                fromCompletionCatchUp(
                    completedAtEpochMs = completedAtEpochMs,
                    scheduledDueAtEpochMs = scheduledDueAtEpochMs,
                    intervalDays = intervalDays,
                    zone = zone,
                )
        }
    }

    /**
     * Next due after Skip (abandon this cycle — no completion record).
     *
     * From-completion modes: scheduled due day + interval (keep time), then keep adding
     * intervals while the candidate is ≤ [nowEpochMs].
     * Catch-up mode uses the same path (catch-up is Done-only).
     * Fixed anchor: next grid calendar day strictly after both now and the
     * open scheduled due (early Skip consumes that slot).
     * Weekly/monthly/nth-weekday/yearly: next matching local day strictly after
     * [nowEpochMs] (same as Done).
     */
    fun nextDueAfterSkip(
        mode: CadenceMode,
        intervalDays: Int,
        scheduledDueAtEpochMs: Long,
        anchorEpochDay: Long,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
        scheduleKind: ScheduleKind = ScheduleKind.INTERVAL,
        weekdaysMask: Int = 0,
        monthDay: Int = 0,
        weekdayOrdinal: Int = 0,
        yearMonthsMask: Int = 0,
        seasonMask: Int = 0,
    ): Long {
        require(intervalDays >= 1) { "intervalDays must be >= 1" }
        nextGridDueAfter(
            afterEpochMs = nowEpochMs,
            scheduledDueAtEpochMs = scheduledDueAtEpochMs,
            scheduleKind = scheduleKind,
            weekdaysMask = weekdaysMask,
            monthDay = monthDay,
            weekdayOrdinal = weekdayOrdinal,
            yearMonthsMask = yearMonthsMask,
            seasonMask = seasonMask,
            zone = zone,
        )?.let { return it }

        return when (mode) {
            CadenceMode.FROM_COMPLETION,
            CadenceMode.FROM_COMPLETION_CATCH_UP,
            -> skipFromScheduled(
                scheduledDueAtEpochMs = scheduledDueAtEpochMs,
                intervalDays = intervalDays,
                nowEpochMs = nowEpochMs,
                zone = zone,
            )

            CadenceMode.FIXED_ANCHOR ->
                fixedAnchor(
                    completedAtEpochMs = nowEpochMs,
                    intervalDays = intervalDays,
                    anchorEpochDay = anchorEpochDay,
                    scheduledDueAtEpochMs = scheduledDueAtEpochMs,
                    zone = zone,
                )
        }
    }

    /**
     * Next matching calendar-grid slot on a local day strictly after
     * [afterEpochMs]'s calendar day, or null for interval tasks.
     */
    private fun nextGridDueAfter(
        afterEpochMs: Long,
        scheduledDueAtEpochMs: Long,
        scheduleKind: ScheduleKind,
        weekdaysMask: Int,
        monthDay: Int,
        weekdayOrdinal: Int,
        yearMonthsMask: Int,
        seasonMask: Int,
        zone: ZoneId,
    ): Long? {
        val matches: (LocalDate) -> Boolean = when (scheduleKind) {
            ScheduleKind.INTERVAL -> return null
            ScheduleKind.WEEKLY -> {
                require(Weekdays.hasAny(weekdaysMask)) { "weekdaysMask must include at least one day" }
                val selected: (LocalDate) -> Boolean =
                    { date -> Weekdays.contains(weekdaysMask, date.dayOfWeek) }
                selected
            }
            ScheduleKind.MONTHLY -> {
                require(monthDay in 1..31) { "monthDay must be 1–31" }
                val onMonthDay: (LocalDate) -> Boolean =
                    { date -> date.dayOfMonth == monthDay.coerceAtMost(date.lengthOfMonth()) }
                onMonthDay
            }
            ScheduleKind.NTH_WEEKDAY -> {
                require(NthWeekday.isValid(weekdayOrdinal)) { "weekdayOrdinal must be 1–4 or last" }
                require(Weekdays.isSingle(weekdaysMask)) { "weekdaysMask must be exactly one weekday" }
                val onNth: (LocalDate) -> Boolean =
                    { date -> NthWeekday.matches(date, weekdayOrdinal, weekdaysMask) }
                onNth
            }
            ScheduleKind.YEARLY -> {
                require(Yearly.isValid(yearMonthsMask, seasonMask, monthDay)) {
                    "yearly needs at least one month or season"
                }
                val onYearly: (LocalDate) -> Boolean = { date ->
                    YearMonths.matches(date, yearMonthsMask, monthDay) ||
                        Seasons.matches(date, seasonMask)
                }
                onYearly
            }
        }
        var date = Instant.ofEpochMilli(afterEpochMs).atZone(zone).toLocalDate().plusDays(1)
        repeat(400) {
            if (matches(date)) {
                return atLocalDateKeepingTime(date.toEpochDay(), scheduledDueAtEpochMs, zone)
            }
            date = date.plusDays(1)
        }
        error("no matching cadence slot within a year")
    }

    private fun skipFromScheduled(
        scheduledDueAtEpochMs: Long,
        intervalDays: Int,
        nowEpochMs: Long,
        zone: ZoneId,
    ): Long {
        var day = epochDayOf(scheduledDueAtEpochMs, zone) + intervalDays
        while (true) {
            val candidate = atLocalDateKeepingTime(day, scheduledDueAtEpochMs, zone)
            if (candidate > nowEpochMs) return candidate
            day += intervalDays
        }
    }

    private fun fromCompletion(
        completedAtEpochMs: Long,
        intervalDays: Int,
        scheduledDueAtEpochMs: Long,
        zone: ZoneId,
    ): Long {
        val completedDay = epochDayOf(completedAtEpochMs, zone)
        return atLocalDateKeepingTime(completedDay + intervalDays, scheduledDueAtEpochMs, zone)
    }

    /**
     * First grid calendar day strictly after both completion and the open scheduled due.
     * Early Done or Skip on the scheduled day consumes that slot.
     */
    private fun fixedAnchor(
        completedAtEpochMs: Long,
        intervalDays: Int,
        anchorEpochDay: Long,
        scheduledDueAtEpochMs: Long,
        zone: ZoneId,
    ): Long {
        val afterDay = max(
            epochDayOf(completedAtEpochMs, zone),
            epochDayOf(scheduledDueAtEpochMs, zone),
        )
        var n = 0L
        if (anchorEpochDay <= afterDay) {
            n = (afterDay - anchorEpochDay) / intervalDays
        }
        while (true) {
            val candidateDay = anchorEpochDay + n * intervalDays
            if (candidateDay > afterDay) {
                return atLocalDateKeepingTime(candidateDay, scheduledDueAtEpochMs, zone)
            }
            n++
        }
    }

    private fun fromCompletionCatchUp(
        completedAtEpochMs: Long,
        scheduledDueAtEpochMs: Long,
        intervalDays: Int,
        zone: ZoneId,
    ): Long {
        val overdue = Duration.ofMillis(completedAtEpochMs - scheduledDueAtEpochMs)
        val interval = Duration.ofDays(intervalDays.toLong())
        val mildThreshold = interval.dividedBy(2)

        if (overdue <= mildThreshold) {
            return fromCompletion(completedAtEpochMs, intervalDays, scheduledDueAtEpochMs, zone)
        }

        val overdueDays = overdue.toMillis().toDouble() / Duration.ofDays(1).toMillis()
        val catchUpDays = min(overdueDays * 0.5, intervalDays * 0.25)
        val floorDays = max(1.0, intervalDays * 0.5)
        val waitDays = max(floorDays, intervalDays - catchUpDays)

        val rawNext = Instant.ofEpochMilli(completedAtEpochMs)
            .plus(Duration.ofMillis((waitDays * Duration.ofDays(1).toMillis()).toLong()))
        val floorInstant = Instant.ofEpochMilli(completedAtEpochMs)
            .plus(Duration.ofMillis((floorDays * Duration.ofDays(1).toMillis()).toLong()))
        val bounded = if (rawNext.isBefore(floorInstant)) floorInstant else rawNext

        val nextDay = epochDayOf(bounded.toEpochMilli(), zone)
        val completedDay = epochDayOf(completedAtEpochMs, zone)
        val minDay = completedDay + ceil(floorDays).toLong().coerceAtLeast(1L)
        return atLocalDateKeepingTime(max(nextDay, minDay), scheduledDueAtEpochMs, zone)
    }
}

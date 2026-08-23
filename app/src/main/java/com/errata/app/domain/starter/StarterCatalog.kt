package com.errata.app.domain.starter

import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.area.TaskAreas
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.NthWeekday
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.cadence.Weekdays
import com.errata.app.domain.cadence.Yearly
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class StarterSpec(
    val id: String,
    val title: String,
    val estimateMinutes: Int,
    val scheduleKind: ScheduleKind,
    val intervalDays: Int = CadenceCalculator.GRID_INTERVAL_DAYS,
    val weekdaysMask: Int = 0,
    val monthDay: Int = 0,
    val weekdayOrdinal: Int = 0,
    val yearMonthsMask: Int = 0,
    val seasonMask: Int = 0,
    val area: String? = null,
)

/**
 * In-app seed pack for true empty state. User-chosen; never auto-inserted.
 */
object StarterCatalog {

    val ALL: List<StarterSpec> = listOf(
        StarterSpec(
            id = "nails",
            title = "Trim nails",
            estimateMinutes = 10,
            scheduleKind = ScheduleKind.INTERVAL,
            intervalDays = 14,
            area = "Body",
        ),
        StarterSpec(
            id = "bathroom",
            title = "Clean bathroom",
            estimateMinutes = 25,
            scheduleKind = ScheduleKind.INTERVAL,
            intervalDays = 7,
            area = "Bathroom",
        ),
        StarterSpec(
            id = "hvac",
            title = "Change HVAC filter",
            estimateMinutes = 20,
            scheduleKind = ScheduleKind.INTERVAL,
            intervalDays = 90,
            area = "House",
        ),
        StarterSpec(
            id = "car",
            title = "Vacuum the car",
            estimateMinutes = 20,
            scheduleKind = ScheduleKind.INTERVAL,
            intervalDays = 30,
            area = "Car",
        ),
        StarterSpec(
            id = "paper",
            title = "Sort the paper pile",
            estimateMinutes = 15,
            scheduleKind = ScheduleKind.INTERVAL,
            intervalDays = 7,
            area = "Paper",
        ),
        StarterSpec(
            id = "bedding",
            title = "Wash bedding",
            estimateMinutes = 20,
            scheduleKind = ScheduleKind.INTERVAL,
            intervalDays = 14,
            area = "House",
        ),
        StarterSpec(
            id = "bins",
            title = "Put the bins out",
            estimateMinutes = 10,
            scheduleKind = ScheduleKind.WEEKLY,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            weekdaysMask = Weekdays.bit(DayOfWeek.TUESDAY),
            area = "House",
        ),
        StarterSpec(
            id = "bill",
            title = "Review a bill",
            estimateMinutes = 15,
            scheduleKind = ScheduleKind.MONTHLY,
            intervalDays = CadenceCalculator.GRID_INTERVAL_DAYS,
            monthDay = 1,
            area = "Paper",
        ),
    )

    fun specsByIds(ids: Collection<String>): List<StarterSpec> {
        if (ids.isEmpty()) return emptyList()
        val wanted = ids.toSet()
        return ALL.filter { it.id in wanted }
    }

    fun cadenceSummary(spec: StarterSpec): String = when (spec.scheduleKind) {
        ScheduleKind.INTERVAL -> "Every ${spec.intervalDays} days"
        ScheduleKind.WEEKLY -> "Weekly · ${Weekdays.shortLabels(spec.weekdaysMask)}"
        ScheduleKind.MONTHLY -> "Monthly · day ${spec.monthDay}"
        ScheduleKind.NTH_WEEKDAY -> NthWeekday.summary(spec.weekdayOrdinal, spec.weekdaysMask)
        ScheduleKind.YEARLY -> Yearly.summary(spec.seasonMask, spec.yearMonthsMask, spec.monthDay)
    }

    fun firstDueEpochMs(
        spec: StarterSpec,
        reminderMinutesOfDay: Int,
        nowEpochMs: Long,
        zone: ZoneId,
    ): Long {
        val reminder = reminderMinutesOfDay.coerceIn(0, 24 * 60 - 1)
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        return when (spec.scheduleKind) {
            ScheduleKind.INTERVAL -> CadenceCalculator.atLocalDateMinutes(
                today.plusDays(1).toEpochDay(),
                reminder,
                zone,
            )
            ScheduleKind.WEEKLY,
            ScheduleKind.MONTHLY,
            ScheduleKind.NTH_WEEKDAY,
            ScheduleKind.YEARLY,
            -> {
                val dummyDue = CadenceCalculator.atLocalDateMinutes(
                    today.toEpochDay(),
                    reminder,
                    zone,
                )
                CadenceCalculator.nextDueAfterSkip(
                    mode = CadenceMode.FROM_COMPLETION,
                    intervalDays = spec.intervalDays.coerceAtLeast(1),
                    scheduledDueAtEpochMs = dummyDue,
                    anchorEpochDay = today.toEpochDay(),
                    nowEpochMs = nowEpochMs,
                    zone = zone,
                    scheduleKind = spec.scheduleKind,
                    weekdaysMask = spec.weekdaysMask,
                    monthDay = spec.monthDay,
                    weekdayOrdinal = spec.weekdayOrdinal,
                    yearMonthsMask = spec.yearMonthsMask,
                    seasonMask = spec.seasonMask,
                )
            }
        }
    }

    fun materialize(
        spec: StarterSpec,
        cadenceMode: CadenceMode,
        reminderMinutesOfDay: Int,
        nowEpochMs: Long,
        zone: ZoneId,
    ): TaskEntity {
        val nextDue = firstDueEpochMs(spec, reminderMinutesOfDay, nowEpochMs, zone)
        val interval = if (spec.scheduleKind == ScheduleKind.INTERVAL) {
            spec.intervalDays.coerceAtLeast(1)
        } else {
            CadenceCalculator.GRID_INTERVAL_DAYS
        }
        return TaskEntity(
            id = 0,
            title = spec.title,
            estimateMinutes = spec.estimateMinutes.coerceAtLeast(1),
            intervalDays = interval,
            scheduleKind = spec.scheduleKind,
            weekdaysMask = if (
                spec.scheduleKind == ScheduleKind.WEEKLY ||
                spec.scheduleKind == ScheduleKind.NTH_WEEKDAY
            ) {
                spec.weekdaysMask
            } else {
                0
            },
            monthDay = if (
                spec.scheduleKind == ScheduleKind.MONTHLY ||
                spec.scheduleKind == ScheduleKind.YEARLY
            ) {
                spec.monthDay
            } else {
                0
            },
            weekdayOrdinal = if (spec.scheduleKind == ScheduleKind.NTH_WEEKDAY) {
                spec.weekdayOrdinal
            } else {
                0
            },
            yearMonthsMask = if (spec.scheduleKind == ScheduleKind.YEARLY) {
                spec.yearMonthsMask
            } else {
                0
            },
            seasonMask = if (spec.scheduleKind == ScheduleKind.YEARLY) spec.seasonMask else 0,
            cadenceMode = cadenceMode,
            anchorEpochDay = CadenceCalculator.epochDayOf(nextDue, zone),
            nextDueAtEpochMs = nextDue,
            reminderMinutesOfDay = null,
            area = TaskAreas.normalize(spec.area),
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs,
        )
    }
}

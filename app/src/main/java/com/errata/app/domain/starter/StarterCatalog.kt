package com.errata.app.domain.starter

import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.area.TaskAreas
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.NthWeekday
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.cadence.Seasons
import com.errata.app.domain.cadence.Weekdays
import com.errata.app.domain.cadence.YearMonths
import com.errata.app.domain.cadence.Yearly
import java.time.DayOfWeek
import java.time.Instant
import java.time.Month
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
 * In-app seed pack. User-chosen; never auto-inserted.
 */
object StarterCatalog {

    val ALL: List<StarterSpec> = listOf(
        StarterSpec("nails", "Trim nails", 10, ScheduleKind.INTERVAL, intervalDays = 14, area = "Body"),
        StarterSpec("beard", "Trim beard", 15, ScheduleKind.INTERVAL, intervalDays = 7, area = "Body"),
        StarterSpec("haircut", "Get a haircut", 60, ScheduleKind.INTERVAL, intervalDays = 42, area = "Body"),
        StarterSpec("glasses", "Clean glasses", 5, ScheduleKind.INTERVAL, intervalDays = 7, area = "Body"),
        StarterSpec("bathroom", "Clean bathroom", 40, ScheduleKind.INTERVAL, intervalDays = 7, area = "Bathroom"),
        StarterSpec("towels", "Wash towels", 20, ScheduleKind.INTERVAL, intervalDays = 7, area = "Bathroom"),
        StarterSpec("drain", "Clear shower drain", 15, ScheduleKind.INTERVAL, intervalDays = 30, area = "Bathroom"),
        StarterSpec("grout", "Scrub grout", 45, ScheduleKind.INTERVAL, intervalDays = 90, area = "Bathroom"),
        StarterSpec("fridge", "Wipe the fridge", 20, ScheduleKind.INTERVAL, intervalDays = 14, area = "Kitchen"),
        StarterSpec("dishwasher", "Clean dishwasher filter", 15, ScheduleKind.INTERVAL, intervalDays = 30, area = "Kitchen"),
        StarterSpec("counters", "Wipe kitchen counters", 10, ScheduleKind.INTERVAL, intervalDays = 7, area = "Kitchen"),
        StarterSpec(
            id = "compost",
            title = "Empty compost caddy",
            estimateMinutes = 5,
            scheduleKind = ScheduleKind.WEEKLY,
            weekdaysMask = Weekdays.bit(DayOfWeek.SUNDAY),
            area = "Kitchen",
        ),
        StarterSpec(
            id = "deep_kitchen",
            title = "Deep-clean kitchen",
            estimateMinutes = 90,
            scheduleKind = ScheduleKind.NTH_WEEKDAY,
            weekdaysMask = Weekdays.bit(DayOfWeek.SATURDAY),
            weekdayOrdinal = 1,
            area = "Kitchen",
        ),
        StarterSpec("hvac", "Change HVAC filter", 20, ScheduleKind.INTERVAL, intervalDays = 90, area = "House"),
        StarterSpec("bedding", "Wash bedding", 40, ScheduleKind.INTERVAL, intervalDays = 14, area = "House"),
        StarterSpec(
            id = "bins",
            title = "Put the bins out",
            estimateMinutes = 10,
            scheduleKind = ScheduleKind.WEEKLY,
            weekdaysMask = Weekdays.bit(DayOfWeek.TUESDAY),
            area = "House",
        ),
        StarterSpec("vacuum", "Vacuum living space", 25, ScheduleKind.INTERVAL, intervalDays = 7, area = "House"),
        StarterSpec("cobwebs", "Dust high corners", 15, ScheduleKind.INTERVAL, intervalDays = 30, area = "House"),
        StarterSpec("windows", "Wash windows", 40, ScheduleKind.INTERVAL, intervalDays = 180, area = "House"),
        StarterSpec("mattress", "Rotate the mattress", 25, ScheduleKind.INTERVAL, intervalDays = 180, area = "House"),
        StarterSpec(
            id = "gutters",
            title = "Clear the gutters",
            estimateMinutes = 90,
            scheduleKind = ScheduleKind.YEARLY,
            seasonMask = Seasons.AUTUMN,
            area = "House",
        ),
        StarterSpec(
            id = "detectors",
            title = "Replace detector batteries",
            estimateMinutes = 20,
            scheduleKind = ScheduleKind.YEARLY,
            yearMonthsMask = YearMonths.bit(Month.MARCH),
            monthDay = 1,
            area = "House",
        ),
        StarterSpec("laundry", "Do laundry", 30, ScheduleKind.INTERVAL, intervalDays = 7, area = "Clothes"),
        StarterSpec("lint", "Empty dryer lint", 5, ScheduleKind.INTERVAL, intervalDays = 7, area = "Clothes"),
        StarterSpec(
            id = "coats",
            title = "Wash winter coats",
            estimateMinutes = 30,
            scheduleKind = ScheduleKind.YEARLY,
            seasonMask = Seasons.SPRING,
            area = "Clothes",
        ),
        StarterSpec("car", "Vacuum the car", 35, ScheduleKind.INTERVAL, intervalDays = 30, area = "Car"),
        StarterSpec("tires", "Check tire pressure", 10, ScheduleKind.INTERVAL, intervalDays = 30, area = "Car"),
        StarterSpec("oil", "Oil change", 60, ScheduleKind.INTERVAL, intervalDays = 90, area = "Car"),
        StarterSpec("wipers", "Replace wiper blades", 20, ScheduleKind.INTERVAL, intervalDays = 180, area = "Car"),
        StarterSpec("paper", "Sort the paper pile", 15, ScheduleKind.INTERVAL, intervalDays = 7, area = "Paper"),
        StarterSpec(
            id = "bill",
            title = "Review a bill",
            estimateMinutes = 15,
            scheduleKind = ScheduleKind.MONTHLY,
            monthDay = 1,
            area = "Paper",
        ),
        StarterSpec(
            id = "insurance",
            title = "Review insurance",
            estimateMinutes = 30,
            scheduleKind = ScheduleKind.YEARLY,
            yearMonthsMask = YearMonths.bit(Month.JANUARY),
            monthDay = 15,
            area = "Paper",
        ),
        StarterSpec(
            id = "taxes",
            title = "Gather tax papers",
            estimateMinutes = 45,
            scheduleKind = ScheduleKind.YEARLY,
            yearMonthsMask = YearMonths.bit(Month.FEBRUARY),
            monthDay = 1,
            area = "Paper",
        ),
        StarterSpec(
            id = "recycling",
            title = "Recycling day",
            estimateMinutes = 10,
            scheduleKind = ScheduleKind.WEEKLY,
            weekdaysMask = Weekdays.bit(DayOfWeek.WEDNESDAY),
            area = "House",
        ),
        StarterSpec(
            id = "plants",
            title = "Water the plants",
            estimateMinutes = 10,
            scheduleKind = ScheduleKind.INTERVAL,
            intervalDays = 7,
            area = "House",
        ),
        StarterSpec(
            id = "lights",
            title = "Pack holiday lights",
            estimateMinutes = 40,
            scheduleKind = ScheduleKind.YEARLY,
            seasonMask = Seasons.WINTER,
            area = "House",
        ),
        StarterSpec(
            id = "ac_cover",
            title = "Store the AC cover",
            estimateMinutes = 15,
            scheduleKind = ScheduleKind.YEARLY,
            seasonMask = Seasons.SPRING,
            area = "House",
        ),
    )

    fun specsByIds(ids: Collection<String>): List<StarterSpec> {
        if (ids.isEmpty()) return emptyList()
        val wanted = ids.toSet()
        return ALL.filter { it.id in wanted }
    }

    fun specById(id: String?): StarterSpec? {
        if (id.isNullOrBlank()) return null
        return ALL.firstOrNull { it.id == id }
    }

    /** Preset area order, then any other labels, unnamed last. */
    fun groupedByArea(): List<Pair<String, List<StarterSpec>>> {
        val groups = ALL.groupBy { it.area?.takeIf { label -> label.isNotBlank() } ?: "" }
        val extra = groups.keys
            .filter { it.isNotEmpty() && it !in TaskAreas.PRESETS }
            .sortedBy { it.lowercase() }
        val order = TaskAreas.PRESETS + extra + listOf("")
        return order.mapNotNull { key ->
            val rows = groups[key] ?: return@mapNotNull null
            val label = key.ifEmpty { "Other" }
            label to rows
        }
    }

    fun cadenceSummary(spec: StarterSpec): String = when (spec.scheduleKind) {
        ScheduleKind.INTERVAL -> "Every ${spec.intervalDays} days"
        ScheduleKind.WEEKLY ->
            "Weekly · ${Weekdays.shortLabels(spec.weekdaysMask)} · change the day"
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
            -> CadenceCalculator.nextGridDueOnOrAfter(
                nowEpochMs = nowEpochMs,
                reminderMinutesOfDay = reminder,
                scheduleKind = spec.scheduleKind,
                weekdaysMask = spec.weekdaysMask,
                monthDay = spec.monthDay,
                weekdayOrdinal = spec.weekdayOrdinal,
                yearMonthsMask = spec.yearMonthsMask,
                seasonMask = spec.seasonMask,
                zone = zone,
            )
        }
    }

    fun materialize(
        spec: StarterSpec,
        cadenceMode: CadenceMode,
        reminderMinutesOfDay: Int,
        storedReminderMinutes: Int? = null,
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
            reminderMinutesOfDay = storedReminderMinutes,
            area = TaskAreas.normalize(spec.area),
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs,
        )
    }
}

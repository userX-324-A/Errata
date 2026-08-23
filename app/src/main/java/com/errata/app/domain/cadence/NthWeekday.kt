package com.errata.app.domain.cadence

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Ordinal weekday-of-month for [ScheduleKind.NTH_WEEKDAY].
 * 1–4 = first through fourth; [LAST] = last occurrence in that month.
 */
object NthWeekday {
    const val LAST = 5

    fun isValid(ordinal: Int): Boolean = ordinal in 1..LAST

    fun matches(date: LocalDate, ordinal: Int, weekdaysMask: Int): Boolean {
        require(isValid(ordinal)) { "weekdayOrdinal must be 1–4 or last" }
        val day = Weekdays.singleDay(weekdaysMask)
            ?: error("weekdaysMask must be exactly one weekday")
        if (date.dayOfWeek != day) return false
        return if (ordinal == LAST) {
            isLastInMonth(date)
        } else {
            occurrenceInMonth(date) == ordinal
        }
    }

    fun occurrenceInMonth(date: LocalDate): Int = ((date.dayOfMonth - 1) / 7) + 1

    fun isLastInMonth(date: LocalDate): Boolean = date.plusWeeks(1).month != date.month

    fun summary(
        ordinal: Int,
        weekdaysMask: Int,
        locale: Locale = Locale.getDefault(),
    ): String {
        val day = Weekdays.singleDay(weekdaysMask) ?: return ""
        val dayLabel = day.getDisplayName(TextStyle.SHORT, locale)
        val ord = when (ordinal) {
            LAST -> "Last"
            1 -> "1st"
            2 -> "2nd"
            3 -> "3rd"
            4 -> "4th"
            else -> ordinal.toString()
        }
        return "$ord $dayLabel"
    }
}

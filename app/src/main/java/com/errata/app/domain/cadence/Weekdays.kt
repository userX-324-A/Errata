package com.errata.app.domain.cadence

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Weekday bitmask for [ScheduleKind.WEEKLY].
 * Bit 0 = Monday … bit 6 = Sunday.
 */
object Weekdays {
    const val ALL_BITS = 0x7F

    fun bit(day: DayOfWeek): Int = 1 shl (day.value - 1)

    fun contains(mask: Int, day: DayOfWeek): Boolean = mask and bit(day) != 0

    fun toggle(mask: Int, day: DayOfWeek): Int = (mask xor bit(day)) and ALL_BITS

    fun hasAny(mask: Int): Boolean = mask and ALL_BITS != 0

    fun shortLabels(mask: Int, locale: Locale = Locale.getDefault()): String =
        DayOfWeek.entries
            .filter { contains(mask, it) }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
}

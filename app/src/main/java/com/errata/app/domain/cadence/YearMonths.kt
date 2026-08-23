package com.errata.app.domain.cadence

import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

/**
 * Calendar-month bitmask for [ScheduleKind.YEARLY].
 * Bit 0 = January … bit 11 = December.
 */
object YearMonths {
    const val ALL_BITS = 0xFFF

    fun bit(month: Month): Int = 1 shl (month.value - 1)

    fun contains(mask: Int, month: Month): Boolean = mask and bit(month) != 0

    fun toggle(mask: Int, month: Month): Int = (mask xor bit(month)) and ALL_BITS

    fun hasAny(mask: Int): Boolean = mask and ALL_BITS != 0

    fun matches(date: LocalDate, mask: Int, monthDay: Int): Boolean {
        if (!hasAny(mask) || monthDay !in 1..31) return false
        if (!contains(mask, date.month)) return false
        return date.dayOfMonth == monthDay.coerceAtMost(date.lengthOfMonth())
    }

    fun shortLabels(
        mask: Int,
        monthDay: Int,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (!hasAny(mask) || monthDay !in 1..31) return ""
        return Month.entries
            .filter { contains(mask, it) }
            .joinToString(", ") { month ->
                val name = month.getDisplayName(TextStyle.SHORT, locale)
                "$name $monthDay"
            }
    }
}

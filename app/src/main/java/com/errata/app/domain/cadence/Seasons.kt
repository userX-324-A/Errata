package com.errata.app.domain.cadence

import java.time.LocalDate
import java.time.Month
import java.time.MonthDay

/**
 * Northern civil season starts for [ScheduleKind.YEARLY].
 * Not astronomy: Spring 20 Mar, Summer 21 Jun, Autumn 22 Sep, Winter 21 Dec.
 */
object Seasons {
    const val SPRING = 1
    const val SUMMER = 2
    const val AUTUMN = 4
    const val WINTER = 8
    const val ALL_BITS = 0xF

    val SPRING_DATE: MonthDay = MonthDay.of(Month.MARCH, 20)
    val SUMMER_DATE: MonthDay = MonthDay.of(Month.JUNE, 21)
    val AUTUMN_DATE: MonthDay = MonthDay.of(Month.SEPTEMBER, 22)
    val WINTER_DATE: MonthDay = MonthDay.of(Month.DECEMBER, 21)

    data class Season(
        val bit: Int,
        val label: String,
        val date: MonthDay,
    )

    val ENTRIES: List<Season> = listOf(
        Season(SPRING, "Spring", SPRING_DATE),
        Season(SUMMER, "Summer", SUMMER_DATE),
        Season(AUTUMN, "Autumn", AUTUMN_DATE),
        Season(WINTER, "Winter", WINTER_DATE),
    )

    fun contains(mask: Int, bit: Int): Boolean = mask and bit != 0

    fun toggle(mask: Int, bit: Int): Int = (mask xor bit) and ALL_BITS

    fun hasAny(mask: Int): Boolean = mask and ALL_BITS != 0

    fun matches(date: LocalDate, mask: Int): Boolean {
        if (!hasAny(mask)) return false
        val monthDay = MonthDay.from(date)
        return ENTRIES.any { contains(mask, it.bit) && monthDay == it.date }
    }

    fun labels(mask: Int): String =
        ENTRIES.filter { contains(mask, it.bit) }.joinToString(", ") { it.label }
}

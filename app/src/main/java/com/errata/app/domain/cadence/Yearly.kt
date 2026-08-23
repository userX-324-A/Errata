package com.errata.app.domain.cadence

import java.util.Locale

/** Combined copy for [ScheduleKind.YEARLY] month-set + season starts. */
object Yearly {
    fun isValid(yearMonthsMask: Int, seasonMask: Int, monthDay: Int): Boolean {
        val months = YearMonths.hasAny(yearMonthsMask)
        val seasons = Seasons.hasAny(seasonMask)
        if (!months && !seasons) return false
        if (months && monthDay !in 1..31) return false
        return true
    }

    fun summary(
        seasonMask: Int,
        yearMonthsMask: Int,
        monthDay: Int,
        locale: Locale = Locale.getDefault(),
    ): String {
        val parts = buildList {
            val seasons = Seasons.labels(seasonMask)
            if (seasons.isNotEmpty()) add(seasons)
            val months = YearMonths.shortLabels(yearMonthsMask, monthDay, locale)
            if (months.isNotEmpty()) add(months)
        }
        return if (parts.isEmpty()) "Yearly" else "Yearly · ${parts.joinToString(", ")}"
    }
}

package com.errata.app.ui.pending

import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.due.PendingClassifier
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

object DueCopy {

    private val dayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    fun formatTimeDefault(time: LocalTime): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time)

    fun subtitle(
        bucket: DueBucket,
        nextDueAtEpochMs: Long,
        snoozedUntilEpochMs: Long?,
        estimateMinutes: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        formatTime: (LocalTime) -> String = DueCopy::formatTimeDefault,
        locale: Locale = Locale.getDefault(),
    ): String {
        val effective = PendingClassifier.effectiveDueEpochMs(nextDueAtEpochMs, snoozedUntilEpochMs)
        val zoned = Instant.ofEpochMilli(effective).atZone(zone)
        val timeLabel = formatTime(zoned.toLocalTime())
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        val duePhrase = when (bucket) {
            DueBucket.OVERDUE -> {
                val days = ChronoUnit.DAYS.between(
                    LocalDate.ofEpochDay(CadenceCalculator.epochDayOf(effective, zone)),
                    today,
                ).toInt().coerceAtLeast(1)
                if (days == 1) "1 day overdue" else "$days days overdue"
            }
            DueBucket.DUE_TODAY -> "Due today · $timeLabel"
            DueBucket.SOON -> "Due ${soonDayPhrase(zoned.toLocalDate(), today, locale)} · $timeLabel"
            else -> "Due ${dayFormatter.format(zoned.toLocalDate())} · $timeLabel"
        }
        return "$duePhrase · ~$estimateMinutes min"
    }

    internal fun soonDayPhrase(
        dueDate: LocalDate,
        today: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): String {
        val days = ChronoUnit.DAYS.between(today, dueDate)
        return when {
            days == 1L -> "tomorrow"
            days in 2L..6L -> dueDate.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
            else -> dayFormatter.format(dueDate)
        }
    }
}

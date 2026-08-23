package com.errata.app.ui.pending

import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.due.PendingClassifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

object DueCopy {

    private val dayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    fun subtitle(
        bucket: DueBucket,
        nextDueAtEpochMs: Long,
        snoozedUntilEpochMs: Long?,
        estimateMinutes: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val effective = PendingClassifier.effectiveDueEpochMs(nextDueAtEpochMs, snoozedUntilEpochMs)
        val zoned = Instant.ofEpochMilli(effective).atZone(zone)
        val timeLabel = timeFormatter.format(zoned.toLocalTime())
        val duePhrase = when (bucket) {
            DueBucket.OVERDUE -> {
                val days = ChronoUnit.DAYS.between(
                    LocalDate.ofEpochDay(CadenceCalculator.epochDayOf(effective, zone)),
                    Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate(),
                ).toInt().coerceAtLeast(1)
                if (days == 1) "1 day overdue" else "$days days overdue"
            }
            DueBucket.DUE_TODAY -> "Due today · $timeLabel"
            DueBucket.SOON -> "Due ${dayFormatter.format(zoned.toLocalDate())} · $timeLabel"
            else -> "Due ${dayFormatter.format(zoned.toLocalDate())} · $timeLabel"
        }
        return "$duePhrase · ~$estimateMinutes min"
    }
}

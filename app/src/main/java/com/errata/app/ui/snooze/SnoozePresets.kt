package com.errata.app.ui.snooze

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

enum class SnoozePreset {
    ONE_HOUR,
    LATER_TODAY,
    TOMORROW,
}

object SnoozePresets {

    private const val ONE_MINUTE_MS = 60L * 1000L

    fun untilEpochMs(
        preset: SnoozePreset,
        nowEpochMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val now = Instant.ofEpochMilli(nowEpochMs).atZone(zone)
        return when (preset) {
            SnoozePreset.ONE_HOUR -> nowEpochMs + 60L * 60L * 1000L
            SnoozePreset.LATER_TODAY -> {
                val sixPm = LocalDateTime.of(now.toLocalDate(), LocalTime.of(18, 0))
                    .atZone(zone)
                    .toInstant()
                    .toEpochMilli()
                if (sixPm > nowEpochMs) sixPm else nowEpochMs + 2L * 60L * 60L * 1000L
            }
            SnoozePreset.TOMORROW -> {
                now.toLocalDate().plusDays(1)
                    .atStartOfDay(zone)
                    .toInstant()
                    .toEpochMilli()
            }
        }
    }

    /**
     * Snooze until today's [hour]:[minute] in [zone].
     * If that instant is ≤ [nowEpochMs], use tomorrow at the same clock time.
     * Always at least one minute in the future.
     */
    fun untilEpochMsForClock(
        hour: Int,
        minute: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        require(hour in 0..23) { "hour" }
        require(minute in 0..59) { "minute" }
        val now = Instant.ofEpochMilli(nowEpochMs).atZone(zone)
        val todayTarget = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute))
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val candidate = if (todayTarget > nowEpochMs) {
            todayTarget
        } else {
            LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.of(hour, minute))
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        }
        return maxOf(candidate, nowEpochMs + ONE_MINUTE_MS)
    }
}

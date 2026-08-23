package com.errata.app.domain.freewindow

import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.due.PendingClassifier
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Ranks pending candidates that fit a free time window.
 *
 * Fit = estimateMinutes <= availableMinutes.
 * Order = urgency band (overdue → due today → soon), then largest estimate that fits,
 * then earlier effective due, then title.
 */
object FreeWindowRanker {

    data class Candidate(
        val id: Long,
        val title: String,
        val estimateMinutes: Int,
        val bucket: DueBucket,
        val nextDueAtEpochMs: Long,
        val snoozedUntilEpochMs: Long?,
    )

    data class Result(
        val fits: List<Candidate>,
        val leftoverAfterBestMinutes: Int?,
    )

    private val bucketOrder = mapOf(
        DueBucket.OVERDUE to 0,
        DueBucket.DUE_TODAY to 1,
        DueBucket.SOON to 2,
    )

    fun rank(candidates: List<Candidate>, availableMinutes: Int): Result {
        if (availableMinutes <= 0) {
            return Result(fits = emptyList(), leftoverAfterBestMinutes = null)
        }
        val fits = candidates
            .filter { it.estimateMinutes in 1..availableMinutes }
            .filter {
                it.bucket == DueBucket.OVERDUE ||
                    it.bucket == DueBucket.DUE_TODAY ||
                    it.bucket == DueBucket.SOON
            }
            .sortedWith(
                compareBy<Candidate> { bucketOrder[it.bucket] ?: Int.MAX_VALUE }
                    .thenByDescending { it.estimateMinutes }
                    .thenBy {
                        PendingClassifier.effectiveDueEpochMs(
                            it.nextDueAtEpochMs,
                            it.snoozedUntilEpochMs,
                        )
                    }
                    .thenBy { it.title.lowercase() },
            )
        val leftover = fits.firstOrNull()?.let { availableMinutes - it.estimateMinutes }
        return Result(fits = fits, leftoverAfterBestMinutes = leftover)
    }

    /**
     * Minutes from [nowEpochMs] until today's [workStartMinutesOfDay].
     * Returns null when unset or already past today.
     */
    fun minutesUntilWorkStart(
        workStartMinutesOfDay: Int?,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int? {
        if (workStartMinutesOfDay == null) return null
        val now = Instant.ofEpochMilli(nowEpochMs).atZone(zone)
        val start = LocalDate.from(now).atTime(
            LocalTime.of(workStartMinutesOfDay / 60, workStartMinutesOfDay % 60),
        ).atZone(zone)
        val minutes = java.time.Duration.between(now, start).toMinutes()
        return if (minutes > 0) minutes.toInt() else null
    }

    /**
     * Minutes from [nowEpochMs] until [stopByMinutesOfDay] today.
     * Returns null when that clock time is already past.
     */
    fun minutesUntilStopBy(
        stopByMinutesOfDay: Int,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int? = minutesUntilWorkStart(stopByMinutesOfDay, nowEpochMs, zone)
}

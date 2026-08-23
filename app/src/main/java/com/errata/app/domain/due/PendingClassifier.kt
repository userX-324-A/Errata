package com.errata.app.domain.due

import com.errata.app.domain.cadence.CadenceCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Classifies a task into pending buckets for the home queue.
 *
 * Effective due = max(nextDueAt, snoozedUntil) when snoozed; otherwise nextDueAt.
 * Paused/archived → [DueBucket.HIDDEN].
 */
object PendingClassifier {

    data class ClassifiableTask(
        val nextDueAtEpochMs: Long,
        val snoozedUntilEpochMs: Long?,
        val isPaused: Boolean,
        val isArchived: Boolean,
    )

    fun classify(
        task: ClassifiableTask,
        nowEpochMs: Long,
        soonHorizonDays: Int = 7,
        zone: ZoneId = ZoneId.systemDefault(),
    ): DueBucket {
        if (task.isArchived || task.isPaused) return DueBucket.HIDDEN

        val effectiveDueMs = effectiveDueEpochMs(task.nextDueAtEpochMs, task.snoozedUntilEpochMs)
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        val dueDay = CadenceCalculator.epochDayOf(effectiveDueMs, zone)
        val todayDay = today.toEpochDay()

        return when {
            dueDay < todayDay -> DueBucket.OVERDUE
            dueDay == todayDay -> DueBucket.DUE_TODAY
            dueDay <= todayDay + soonHorizonDays -> DueBucket.SOON
            else -> DueBucket.LATER
        }
    }

    fun effectiveDueEpochMs(nextDueAtEpochMs: Long, snoozedUntilEpochMs: Long?): Long {
        if (snoozedUntilEpochMs == null) return nextDueAtEpochMs
        return maxOf(nextDueAtEpochMs, snoozedUntilEpochMs)
    }

    fun localDateOf(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
}

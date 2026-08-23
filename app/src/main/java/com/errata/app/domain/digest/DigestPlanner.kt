package com.errata.app.domain.digest

import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.due.PendingClassifier
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Opt-in morning digest: who is coalesced, and when the standing digest alarm fires.
 */
object DigestPlanner {

    data class Candidate(
        val id: Long,
        val estimateMinutes: Int,
        val reminderMinutesOfDay: Int?,
        val nextDueAtEpochMs: Long,
        val snoozedUntilEpochMs: Long?,
        val isPaused: Boolean,
        val isArchived: Boolean,
    )

    fun usesDefaultReminder(
        reminderMinutesOfDay: Int?,
        defaultReminderMinutesOfDay: Int,
    ): Boolean = (reminderMinutesOfDay ?: defaultReminderMinutesOfDay) ==
        defaultReminderMinutesOfDay

    /** Default-time tasks with no future snooze are covered by the digest alarm, not per-task. */
    fun coveredByDigest(
        candidate: Candidate,
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long,
    ): Boolean {
        if (candidate.isArchived || candidate.isPaused) return false
        val snooze = candidate.snoozedUntilEpochMs
        if (snooze != null && snooze > nowEpochMs) return false
        return usesDefaultReminder(candidate.reminderMinutesOfDay, defaultReminderMinutesOfDay)
    }

    fun nextDigestEpochMs(
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate().toEpochDay()
        var fire = atLocalDateMinutes(today, defaultReminderMinutesOfDay, zone)
        if (fire <= nowEpochMs) {
            fire = atLocalDateMinutes(today + 1, defaultReminderMinutesOfDay, zone)
        }
        return fire
    }

    /** After a digest has fired, skip the rest of today so inexact early wakes cannot double-notify. */
    fun nextDigestAfterFire(
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate().toEpochDay()
        return atLocalDateMinutes(today + 1, defaultReminderMinutesOfDay, zone)
    }

    fun members(
        candidates: List<Candidate>,
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<Candidate> =
        candidates
            .filter { isMember(it, defaultReminderMinutesOfDay, nowEpochMs, zone) }
            .sortedWith(
                compareBy(
                    { memberBucket(it, nowEpochMs, zone).ordinal },
                    {
                        PendingClassifier.effectiveDueEpochMs(
                            it.nextDueAtEpochMs,
                            it.snoozedUntilEpochMs,
                        )
                    },
                    { it.id },
                ),
            )

    fun totalMinutes(members: List<Candidate>): Int = members.sumOf { it.estimateMinutes }

    private fun isMember(
        candidate: Candidate,
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long,
        zone: ZoneId,
    ): Boolean {
        if (!coveredByDigest(candidate, defaultReminderMinutesOfDay, nowEpochMs)) return false
        val bucket = memberBucket(candidate, nowEpochMs, zone)
        return bucket == DueBucket.OVERDUE || bucket == DueBucket.DUE_TODAY
    }

    private fun memberBucket(
        candidate: Candidate,
        nowEpochMs: Long,
        zone: ZoneId,
    ): DueBucket =
        PendingClassifier.classify(
            task = PendingClassifier.ClassifiableTask(
                nextDueAtEpochMs = candidate.nextDueAtEpochMs,
                snoozedUntilEpochMs = candidate.snoozedUntilEpochMs,
                isPaused = candidate.isPaused,
                isArchived = candidate.isArchived,
            ),
            nowEpochMs = nowEpochMs,
            zone = zone,
        )

    private fun atLocalDateMinutes(epochDay: Long, minutesOfDay: Int, zone: ZoneId): Long {
        val clamped = minutesOfDay.coerceIn(0, 24 * 60 - 1)
        val time = LocalTime.of(clamped / 60, clamped % 60)
        return LocalDate.ofEpochDay(epochDay).atTime(time).atZone(zone).toInstant().toEpochMilli()
    }
}

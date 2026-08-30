package com.errata.app.domain.digest

import com.errata.app.domain.cadence.CadenceCalculator
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

    /**
     * Effective reminder minutes: per-task override, or the due clock when null.
     * A task is a digest member only when that time equals the settings default.
     */
    fun usesDefaultReminder(
        reminderMinutesOfDay: Int?,
        defaultReminderMinutesOfDay: Int,
        nextDueAtEpochMs: Long,
        zone: ZoneId,
    ): Boolean {
        val effective = reminderMinutesOfDay
            ?: CadenceCalculator.minutesOfDay(nextDueAtEpochMs, zone)
        return effective == defaultReminderMinutesOfDay
    }

    /** Default-time tasks with no future snooze are covered by the digest alarm, not per-task. */
    fun coveredByDigest(
        candidate: Candidate,
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (candidate.isArchived || candidate.isPaused) return false
        val snooze = candidate.snoozedUntilEpochMs
        if (snooze != null && snooze > nowEpochMs) return false
        return usesDefaultReminder(
            candidate.reminderMinutesOfDay,
            defaultReminderMinutesOfDay,
            candidate.nextDueAtEpochMs,
            zone,
        )
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
        if (!coveredByDigest(candidate, defaultReminderMinutesOfDay, nowEpochMs, zone)) return false
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

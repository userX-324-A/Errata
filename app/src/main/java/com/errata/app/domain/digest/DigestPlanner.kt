package com.errata.app.domain.digest

import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.due.PendingClassifier
import com.errata.app.domain.reminders.ReminderPolicy
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
        val createdAtEpochMs: Long = 0L,
    )

    /**
     * Effective reminder minutes: per-task override, or the due clock when null.
     * None never joins the digest. Default-clock tasks are members whenever
     * they are due today or overdue. Custom clocks stay per-task on the due
     * day, then join while overdue so they do not each RTC every morning.
     */
    fun usesDefaultReminder(
        reminderMinutesOfDay: Int?,
        defaultReminderMinutesOfDay: Int,
        nextDueAtEpochMs: Long,
        zone: ZoneId,
    ): Boolean {
        if (ReminderPolicy.isNone(reminderMinutesOfDay)) return false
        val effective = reminderMinutesOfDay
            ?: CadenceCalculator.minutesOfDay(nextDueAtEpochMs, zone)
        return effective == defaultReminderMinutesOfDay
    }

    /**
     * Covered by the standing digest alarm, not a per-task wakeup:
     * default-clock tasks (any due day), and overdue custom / non-default clocks.
     */
    fun coveredByDigest(
        candidate: Candidate,
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (candidate.isArchived || candidate.isPaused) return false
        val snooze = candidate.snoozedUntilEpochMs
        if (snooze != null && snooze > nowEpochMs) return false
        if (
            usesDefaultReminder(
                candidate.reminderMinutesOfDay,
                defaultReminderMinutesOfDay,
                candidate.nextDueAtEpochMs,
                zone,
            )
        ) {
            return true
        }
        return overdueJoinsDigest(candidate, nowEpochMs, zone)
    }

    /** Custom / non-default clocks after the due calendar day. None never joins. */
    fun overdueJoinsDigest(
        candidate: Candidate,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (candidate.isArchived || candidate.isPaused) return false
        if (ReminderPolicy.isNone(candidate.reminderMinutesOfDay)) return false
        val dueDay = CadenceCalculator.epochDayOf(candidate.nextDueAtEpochMs, zone)
        return dueDay < localEpochDay(nowEpochMs, zone)
    }

    /** Today's digest alarm is still in the future (not yet fired). */
    fun todaysDigestPending(
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val today = localEpochDay(nowEpochMs, zone)
        return atLocalDateMinutes(today, defaultReminderMinutesOfDay, zone) > nowEpochMs
    }

    /**
     * Standing digest never marked this local day, and today's window has passed.
     * Replay once (boot / force-stop / import). Post-digest new pins use [sameDayFallback].
     */
    fun shouldReplayMissedDigest(
        lastNotifiedEpochDay: Long?,
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (todaysDigestPending(defaultReminderMinutesOfDay, nowEpochMs, zone)) return false
        return lastNotifiedEpochDay != localEpochDay(nowEpochMs, zone)
    }

    fun localEpochDay(nowEpochMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate().toEpochDay()

    /** True when today's digest (or miss-replay) already posted — do not schedule another fire today. */
    fun alreadyPostedToday(
        lastNotifiedEpochDay: Long?,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean = lastNotifiedEpochDay == localEpochDay(nowEpochMs, zone)

    /**
     * Pinned after this morning's digest: notify now instead of waiting until tomorrow.
     * Tasks that were already due at digest fire stay silent (they were in the digest).
     */
    fun sameDayFallback(
        candidate: Candidate,
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        if (!coveredByDigest(candidate, defaultReminderMinutesOfDay, nowEpochMs, zone)) {
            return false
        }
        if (todaysDigestPending(defaultReminderMinutesOfDay, nowEpochMs, zone)) return false
        if (!dueTodayOrOverdue(candidate, nowEpochMs, zone)) return false
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate().toEpochDay()
        val digestToday = atLocalDateMinutes(today, defaultReminderMinutesOfDay, zone)
        return candidate.createdAtEpochMs > digestToday
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
        return dueTodayOrOverdue(candidate, nowEpochMs, zone)
    }

    private fun dueTodayOrOverdue(
        candidate: Candidate,
        nowEpochMs: Long,
        zone: ZoneId,
    ): Boolean {
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

package com.errata.app.reminders

import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.reminders.ReminderPolicy
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure next-fire calculation for reminders (unit-tested).
 */
object ReminderTimeCalculator {

    /**
     * Extra local days after the due day that still get a per-task wakeup
     * when morning digest is off. Digest on: overdue custom clocks join the
     * standing digest instead (see [com.errata.app.domain.digest.DigestPlanner]).
     */
    const val OVERDUE_NUDGE_DAYS = 1

    /**
     * @param defaultReminderMinutesOfDay unused for fire time; kept so callers pass settings.
     *   Null [TaskEntity.reminderMinutesOfDay] fires at the due clock; [ReminderPolicy.NONE]
     *   skips scheduling entirely (including an in-app snooze — quiet stays quiet).
     * @param digestEnabled when true, do not schedule a per-task fire after the due day
     *   (overdue leftovers are digest members).
     * @return epoch millis to fire, or null if the task should not be scheduled
     */
    @Suppress("UNUSED_PARAMETER")
    fun nextFireEpochMs(
        task: TaskEntity,
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        digestEnabled: Boolean = false,
    ): Long? {
        if (task.isArchived || task.isPaused) return null
        if (ReminderPolicy.isNone(task.reminderMinutesOfDay)) return null

        val snooze = task.snoozedUntilEpochMs
        if (snooze != null && snooze > nowEpochMs) {
            return snooze
        }

        val minutes = task.reminderMinutesOfDay
            ?: CadenceCalculator.minutesOfDay(task.nextDueAtEpochMs, zone)
        val dueDay = CadenceCalculator.epochDayOf(task.nextDueAtEpochMs, zone)
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate().toEpochDay()
        val lastFireDay = if (digestEnabled) {
            dueDay
        } else {
            dueDay + OVERDUE_NUDGE_DAYS
        }
        if (today > lastFireDay) return null

        val candidateDay = when {
            dueDay > today -> dueDay
            else -> today
        }
        var fire = atLocalDateMinutes(candidateDay, minutes, zone)
        if (fire <= nowEpochMs) {
            val nextDay = candidateDay + 1
            if (nextDay > lastFireDay) return null
            fire = atLocalDateMinutes(nextDay, minutes, zone)
        }
        return fire
    }

    fun atLocalDateMinutes(epochDay: Long, minutesOfDay: Int, zone: ZoneId): Long {
        val clamped = minutesOfDay.coerceIn(0, 24 * 60 - 1)
        val time = LocalTime.of(clamped / 60, clamped % 60)
        return LocalDate.ofEpochDay(epochDay).atTime(time).atZone(zone).toInstant().toEpochMilli()
    }
}

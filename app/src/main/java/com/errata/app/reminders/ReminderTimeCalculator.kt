package com.errata.app.reminders

import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.cadence.CadenceCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Pure next-fire calculation for reminders (unit-tested).
 */
object ReminderTimeCalculator {

    /**
     * @return epoch millis to fire, or null if the task should not be scheduled
     */
    fun nextFireEpochMs(
        task: TaskEntity,
        defaultReminderMinutesOfDay: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        if (task.isArchived || task.isPaused) return null

        val snooze = task.snoozedUntilEpochMs
        if (snooze != null && snooze > nowEpochMs) {
            return snooze
        }

        val minutes = task.reminderMinutesOfDay ?: defaultReminderMinutesOfDay
        val dueDay = CadenceCalculator.epochDayOf(task.nextDueAtEpochMs, zone)
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate().toEpochDay()

        val candidateDay = when {
            dueDay > today -> dueDay
            else -> today // due today or overdue → try today at reminder time
        }

        var fire = atLocalDateMinutes(candidateDay, minutes, zone)
        if (fire <= nowEpochMs) {
            // Past today's reminder (or due was earlier today) → nudge tomorrow
            fire = atLocalDateMinutes(candidateDay + 1, minutes, zone)
        }
        return fire
    }

    fun atLocalDateMinutes(epochDay: Long, minutesOfDay: Int, zone: ZoneId): Long {
        val clamped = minutesOfDay.coerceIn(0, 24 * 60 - 1)
        val time = LocalTime.of(clamped / 60, clamped % 60)
        return LocalDate.ofEpochDay(epochDay).atTime(time).atZone(zone).toInstant().toEpochMilli()
    }
}

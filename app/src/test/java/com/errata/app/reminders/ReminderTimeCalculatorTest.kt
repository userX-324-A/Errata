package com.errata.app.reminders

import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.reminders.ReminderPolicy
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderTimeCalculatorTest {

    private val zone = ZoneOffset.UTC

    private fun task(
        dueDay: LocalDate,
        reminderMinutes: Int? = null,
        snoozeMs: Long? = null,
        dueMinutes: Int = 0,
    ) = TaskEntity(
        id = 1,
        title = "t",
        estimateMinutes = 10,
        intervalDays = 7,
        cadenceMode = CadenceMode.FROM_COMPLETION,
        anchorEpochDay = dueDay.toEpochDay(),
        nextDueAtEpochMs = CadenceCalculator.atLocalDateMinutes(
            dueDay.toEpochDay(),
            dueMinutes,
            zone,
        ),
        reminderMinutesOfDay = reminderMinutes,
        snoozedUntilEpochMs = snoozeMs,
        createdAtEpochMs = 0,
        updatedAtEpochMs = 0,
    )

    @Test
    fun futureDue_schedulesOnDueDayAtReminderTime() {
        val due = LocalDate.of(2026, 4, 10)
        val now = LocalDate.of(2026, 4, 1).atTime(8, 0).toInstant(zone).toEpochMilli()
        val fire = ReminderTimeCalculator.nextFireEpochMs(
            task = task(due, reminderMinutes = 9 * 60),
            defaultReminderMinutesOfDay = 9 * 60,
            nowEpochMs = now,
            zone = zone,
        )!!
        assertEquals(
            ReminderTimeCalculator.atLocalDateMinutes(due.toEpochDay(), 9 * 60, zone),
            fire,
        )
    }

    @Test
    fun dueToday_pastReminder_schedulesTomorrow() {
        val due = LocalDate.of(2026, 4, 10)
        val now = LocalDate.of(2026, 4, 10).atTime(10, 0).toInstant(zone).toEpochMilli()
        val fire = ReminderTimeCalculator.nextFireEpochMs(
            task = task(due, reminderMinutes = 9 * 60),
            defaultReminderMinutesOfDay = 9 * 60,
            nowEpochMs = now,
            zone = zone,
        )!!
        assertEquals(
            ReminderTimeCalculator.atLocalDateMinutes(due.toEpochDay() + 1, 9 * 60, zone),
            fire,
        )
    }

    @Test
    fun snoozeInFuture_usesSnoozeInstant() {
        val due = LocalDate.of(2026, 4, 1)
        val now = LocalDate.of(2026, 4, 10).atTime(8, 0).toInstant(zone).toEpochMilli()
        val snooze = now + 30L * 60L * 1000L
        val fire = ReminderTimeCalculator.nextFireEpochMs(
            task = task(due, snoozeMs = snooze),
            defaultReminderMinutesOfDay = 9 * 60,
            nowEpochMs = now,
            zone = zone,
        )!!
        assertEquals(snooze, fire)
    }

    @Test
    fun nullReminder_firesAtDueClock() {
        val due = LocalDate.of(2026, 5, 1)
        val now = LocalDate.of(2026, 4, 1).atTime(8, 0).toInstant(zone).toEpochMilli()
        val fire = ReminderTimeCalculator.nextFireEpochMs(
            task = task(due, reminderMinutes = null, dueMinutes = 18 * 60),
            defaultReminderMinutesOfDay = 9 * 60,
            nowEpochMs = now,
            zone = zone,
        )!!
        assertTrue(fire > now)
        assertEquals(
            ReminderTimeCalculator.atLocalDateMinutes(due.toEpochDay(), 18 * 60, zone),
            fire,
        )
    }

    @Test
    fun dueToday_nearFutureReminder_firesSameDay() {
        val due = LocalDate.of(2026, 4, 10)
        val now = LocalDate.of(2026, 4, 10).atTime(14, 0).toInstant(zone).toEpochMilli()
        val reminderMinutes = 14 * 60 + 5 // 14:05
        val fire = ReminderTimeCalculator.nextFireEpochMs(
            task = task(due, reminderMinutes = reminderMinutes),
            defaultReminderMinutesOfDay = 9 * 60,
            nowEpochMs = now,
            zone = zone,
        )!!
        assertEquals(
            ReminderTimeCalculator.atLocalDateMinutes(due.toEpochDay(), reminderMinutes, zone),
            fire,
        )
        assertTrue(fire > now)
    }

    @Test
    fun dueToday_newYork_firesSameLocalMorning() {
        val ny = java.time.ZoneId.of("America/New_York")
        val due = LocalDate.of(2026, 3, 8)
        val now = due.atTime(8, 0).atZone(ny).toInstant().toEpochMilli()
        val fire = ReminderTimeCalculator.nextFireEpochMs(
            task = task(due, reminderMinutes = 9 * 60).copy(
                nextDueAtEpochMs = CadenceCalculator.atLocalDateMinutes(
                    due.toEpochDay(),
                    9 * 60,
                    ny,
                ),
            ),
            defaultReminderMinutesOfDay = 9 * 60,
            nowEpochMs = now,
            zone = ny,
        )!!
        assertEquals(
            ReminderTimeCalculator.atLocalDateMinutes(due.toEpochDay(), 9 * 60, ny),
            fire,
        )
    }

    @Test
    fun none_skipsScheduleEvenWithSnooze() {
        val due = LocalDate.of(2026, 4, 10)
        val now = LocalDate.of(2026, 4, 10).atTime(8, 0).toInstant(zone).toEpochMilli()
        val snooze = now + 30L * 60L * 1000L
        assertNull(
            ReminderTimeCalculator.nextFireEpochMs(
                task = task(due, reminderMinutes = ReminderPolicy.NONE, snoozeMs = snooze),
                defaultReminderMinutesOfDay = 9 * 60,
                nowEpochMs = now,
                zone = zone,
            ),
        )
        assertNull(
            ReminderTimeCalculator.nextFireEpochMs(
                task = task(due, reminderMinutes = ReminderPolicy.NONE),
                defaultReminderMinutesOfDay = 9 * 60,
                nowEpochMs = now,
                zone = zone,
            ),
        )
    }

    @Test
    fun overdue_graceDay_clockAhead_firesToday() {
        val due = LocalDate.of(2026, 4, 10)
        val now = LocalDate.of(2026, 4, 11).atTime(8, 0).toInstant(zone).toEpochMilli()
        val fire = ReminderTimeCalculator.nextFireEpochMs(
            task = task(due, reminderMinutes = 9 * 60),
            defaultReminderMinutesOfDay = 9 * 60,
            nowEpochMs = now,
            zone = zone,
        )!!
        assertEquals(
            ReminderTimeCalculator.atLocalDateMinutes(due.toEpochDay() + 1, 9 * 60, zone),
            fire,
        )
    }

    @Test
    fun overdue_graceDay_clockPast_noFurtherNudge() {
        val due = LocalDate.of(2026, 4, 10)
        val now = LocalDate.of(2026, 4, 11).atTime(10, 0).toInstant(zone).toEpochMilli()
        assertNull(
            ReminderTimeCalculator.nextFireEpochMs(
                task = task(due, reminderMinutes = 9 * 60),
                defaultReminderMinutesOfDay = 9 * 60,
                nowEpochMs = now,
                zone = zone,
            ),
        )
    }

    @Test
    fun overdue_beyondGrace_noFireEvenBeforeClock() {
        val due = LocalDate.of(2026, 4, 10)
        val now = LocalDate.of(2026, 4, 12).atTime(8, 0).toInstant(zone).toEpochMilli()
        assertNull(
            ReminderTimeCalculator.nextFireEpochMs(
                task = task(due, reminderMinutes = 9 * 60),
                defaultReminderMinutesOfDay = 9 * 60,
                nowEpochMs = now,
                zone = zone,
            ),
        )
    }

    @Test
    fun overdue_digestEnabled_noPerTaskFire() {
        val due = LocalDate.of(2026, 4, 10)
        val now = LocalDate.of(2026, 4, 11).atTime(8, 0).toInstant(zone).toEpochMilli()
        assertNull(
            ReminderTimeCalculator.nextFireEpochMs(
                task = task(due, reminderMinutes = 18 * 60),
                defaultReminderMinutesOfDay = 9 * 60,
                nowEpochMs = now,
                zone = zone,
                digestEnabled = true,
            ),
        )
    }

    @Test
    fun dueToday_pastReminder_digestEnabled_doesNotScheduleTomorrow() {
        val due = LocalDate.of(2026, 4, 10)
        val now = LocalDate.of(2026, 4, 10).atTime(10, 0).toInstant(zone).toEpochMilli()
        assertNull(
            ReminderTimeCalculator.nextFireEpochMs(
                task = task(due, reminderMinutes = 9 * 60),
                defaultReminderMinutesOfDay = 9 * 60,
                nowEpochMs = now,
                zone = zone,
                digestEnabled = true,
            ),
        )
    }

    @Test
    fun overdue_futureSnooze_stillFires() {
        val due = LocalDate.of(2026, 4, 1)
        val now = LocalDate.of(2026, 4, 20).atTime(8, 0).toInstant(zone).toEpochMilli()
        val snooze = now + 30L * 60L * 1000L
        val fire = ReminderTimeCalculator.nextFireEpochMs(
            task = task(due, reminderMinutes = 18 * 60, snoozeMs = snooze),
            defaultReminderMinutesOfDay = 9 * 60,
            nowEpochMs = now,
            zone = zone,
        )!!
        assertEquals(snooze, fire)
    }
}

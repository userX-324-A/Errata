package com.errata.app.domain.digest

import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.reminders.ReminderPolicy
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigestPlannerTest {

    private val zone = ZoneOffset.UTC
    private val defaultMinutes = 9 * 60

    private fun candidate(
        id: Long = 1,
        estimateMinutes: Int = 10,
        reminderMinutes: Int? = null,
        dueDay: LocalDate,
        snoozeMs: Long? = null,
        isPaused: Boolean = false,
        isArchived: Boolean = false,
        dueMinutes: Int = defaultMinutes,
        createdAtEpochMs: Long = 0L,
    ) = DigestPlanner.Candidate(
        id = id,
        estimateMinutes = estimateMinutes,
        reminderMinutesOfDay = reminderMinutes,
        nextDueAtEpochMs = CadenceCalculator.atLocalDateMinutes(
            dueDay.toEpochDay(),
            dueMinutes,
            zone,
        ),
        snoozedUntilEpochMs = snoozeMs,
        isPaused = isPaused,
        isArchived = isArchived,
        createdAtEpochMs = createdAtEpochMs,
    )

    @Test
    fun nextDigest_beforeDefault_firesToday() {
        val now = LocalDate.of(2026, 4, 10).atTime(8, 0).toInstant(zone).toEpochMilli()
        val fire = DigestPlanner.nextDigestEpochMs(defaultMinutes, now, zone)
        assertEquals(
            LocalDate.of(2026, 4, 10).atTime(9, 0).toInstant(zone).toEpochMilli(),
            fire,
        )
    }

    @Test
    fun nextDigest_atOrAfterDefault_firesTomorrow() {
        val now = LocalDate.of(2026, 4, 10).atTime(9, 0).toInstant(zone).toEpochMilli()
        val fire = DigestPlanner.nextDigestEpochMs(defaultMinutes, now, zone)
        assertEquals(
            LocalDate.of(2026, 4, 11).atTime(9, 0).toInstant(zone).toEpochMilli(),
            fire,
        )
    }

    @Test
    fun nextDigestAfterFire_skipsRestOfToday() {
        val now = LocalDate.of(2026, 4, 10).atTime(8, 50).toInstant(zone).toEpochMilli()
        val fire = DigestPlanner.nextDigestAfterFire(defaultMinutes, now, zone)
        assertEquals(
            LocalDate.of(2026, 4, 11).atTime(9, 0).toInstant(zone).toEpochMilli(),
            fire,
        )
    }

    @Test
    fun members_n0_whenNothingDueToday() {
        val now = LocalDate.of(2026, 4, 10).atTime(9, 0).toInstant(zone).toEpochMilli()
        val members = DigestPlanner.members(
            candidates = listOf(candidate(dueDay = LocalDate.of(2026, 4, 20))),
            defaultReminderMinutesOfDay = defaultMinutes,
            nowEpochMs = now,
            zone = zone,
        )
        assertTrue(members.isEmpty())
        assertEquals(0, DigestPlanner.totalMinutes(members))
    }

    @Test
    fun members_n1_singleDueToday() {
        val now = LocalDate.of(2026, 4, 10).atTime(9, 0).toInstant(zone).toEpochMilli()
        val members = DigestPlanner.members(
            candidates = listOf(
                candidate(id = 2, estimateMinutes = 15, dueDay = LocalDate.of(2026, 4, 10)),
            ),
            defaultReminderMinutesOfDay = defaultMinutes,
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(listOf(2L), members.map { it.id })
        assertEquals(15, DigestPlanner.totalMinutes(members))
    }

    @Test
    fun members_n2_overdueAndDueToday_sumsMinutes() {
        val now = LocalDate.of(2026, 4, 10).atTime(9, 0).toInstant(zone).toEpochMilli()
        val members = DigestPlanner.members(
            candidates = listOf(
                candidate(id = 1, estimateMinutes = 10, dueDay = LocalDate.of(2026, 4, 8)),
                candidate(id = 2, estimateMinutes = 20, dueDay = LocalDate.of(2026, 4, 10)),
                candidate(id = 3, estimateMinutes = 5, dueDay = LocalDate.of(2026, 4, 12)),
            ),
            defaultReminderMinutesOfDay = defaultMinutes,
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(listOf(1L, 2L), members.map { it.id })
        assertEquals(30, DigestPlanner.totalMinutes(members))
    }

    @Test
    fun members_excludesCustomReminderAndFutureSnooze() {
        val now = LocalDate.of(2026, 4, 10).atTime(9, 0).toInstant(zone).toEpochMilli()
        val members = DigestPlanner.members(
            candidates = listOf(
                candidate(
                    id = 1,
                    reminderMinutes = 18 * 60,
                    dueDay = LocalDate.of(2026, 4, 10),
                ),
                candidate(
                    id = 2,
                    dueDay = LocalDate.of(2026, 4, 10),
                    snoozeMs = now + 60_000L,
                ),
                candidate(id = 3, dueDay = LocalDate.of(2026, 4, 10)),
            ),
            defaultReminderMinutesOfDay = defaultMinutes,
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(listOf(3L), members.map { it.id })
    }

    @Test
    fun members_includesOverdueCustomClock() {
        val now = LocalDate.of(2026, 4, 10).atTime(9, 0).toInstant(zone).toEpochMilli()
        val members = DigestPlanner.members(
            candidates = listOf(
                candidate(
                    id = 1,
                    reminderMinutes = 18 * 60,
                    dueDay = LocalDate.of(2026, 4, 8),
                    dueMinutes = 18 * 60,
                ),
                candidate(
                    id = 2,
                    reminderMinutes = 18 * 60,
                    dueDay = LocalDate.of(2026, 4, 10),
                    dueMinutes = 18 * 60,
                ),
            ),
            defaultReminderMinutesOfDay = defaultMinutes,
            nowEpochMs = now,
            zone = zone,
        )
        assertEquals(listOf(1L), members.map { it.id })
    }

    @Test
    fun coveredByDigest_defaultAndExplicitSameMinutes() {
        val now = LocalDate.of(2026, 4, 10).atTime(8, 0).toInstant(zone).toEpochMilli()
        val due = LocalDate.of(2026, 4, 20)
        assertTrue(
            DigestPlanner.coveredByDigest(
                candidate(reminderMinutes = null, dueDay = due),
                defaultMinutes,
                now,
                zone,
            ),
        )
        assertTrue(
            DigestPlanner.coveredByDigest(
                candidate(reminderMinutes = defaultMinutes, dueDay = due, dueMinutes = 0),
                defaultMinutes,
                now,
                zone,
            ),
        )
        assertFalse(
            DigestPlanner.coveredByDigest(
                candidate(reminderMinutes = 8 * 60, dueDay = due),
                defaultMinutes,
                now,
                zone,
            ),
        )
        assertFalse(
            DigestPlanner.coveredByDigest(
                candidate(dueDay = due, snoozeMs = now + 1_000L),
                defaultMinutes,
                now,
                zone,
            ),
        )
        assertFalse(
            DigestPlanner.coveredByDigest(
                candidate(reminderMinutes = null, dueDay = due, dueMinutes = 18 * 60),
                defaultMinutes,
                now,
                zone,
            ),
        )
        assertFalse(
            DigestPlanner.coveredByDigest(
                candidate(
                    reminderMinutes = ReminderPolicy.NONE,
                    dueDay = due,
                    dueMinutes = defaultMinutes,
                ),
                defaultMinutes,
                now,
                zone,
            ),
        )
        val overdueNow = LocalDate.of(2026, 4, 12).atTime(8, 0).toInstant(zone).toEpochMilli()
        assertTrue(
            DigestPlanner.coveredByDigest(
                candidate(
                    reminderMinutes = 18 * 60,
                    dueDay = LocalDate.of(2026, 4, 10),
                    dueMinutes = 18 * 60,
                ),
                defaultMinutes,
                overdueNow,
                zone,
            ),
        )
        assertFalse(
            DigestPlanner.coveredByDigest(
                candidate(
                    reminderMinutes = ReminderPolicy.NONE,
                    dueDay = LocalDate.of(2026, 4, 10),
                    dueMinutes = 18 * 60,
                ),
                defaultMinutes,
                overdueNow,
                zone,
            ),
        )
    }

    @Test
    fun todaysDigestPending_beforeAndAfterDefault() {
        val before = LocalDate.of(2026, 4, 10).atTime(8, 0).toInstant(zone).toEpochMilli()
        val after = LocalDate.of(2026, 4, 10).atTime(9, 0).toInstant(zone).toEpochMilli()
        assertTrue(DigestPlanner.todaysDigestPending(defaultMinutes, before, zone))
        assertFalse(DigestPlanner.todaysDigestPending(defaultMinutes, after, zone))
    }

    @Test
    fun sameDayFallback_pinAfterDigest_dueToday() {
        val digestAt = LocalDate.of(2026, 4, 10).atTime(9, 0).toInstant(zone).toEpochMilli()
        val now = LocalDate.of(2026, 4, 10).atTime(14, 0).toInstant(zone).toEpochMilli()
        val pinned = candidate(
            dueDay = LocalDate.of(2026, 4, 10),
            createdAtEpochMs = now,
        )
        assertTrue(
            DigestPlanner.sameDayFallback(pinned, defaultMinutes, now, zone),
        )
        val alreadyDue = candidate(
            dueDay = LocalDate.of(2026, 4, 10),
            createdAtEpochMs = digestAt - 1,
        )
        assertFalse(
            DigestPlanner.sameDayFallback(alreadyDue, defaultMinutes, now, zone),
        )
    }

    @Test
    fun sameDayFallback_falseBeforeDigestAndForFutureDue() {
        val nowMorning = LocalDate.of(2026, 4, 10).atTime(8, 0).toInstant(zone).toEpochMilli()
        val nowAfternoon = LocalDate.of(2026, 4, 10).atTime(14, 0).toInstant(zone).toEpochMilli()
        val dueToday = candidate(
            dueDay = LocalDate.of(2026, 4, 10),
            createdAtEpochMs = nowAfternoon,
        )
        assertFalse(
            DigestPlanner.sameDayFallback(dueToday, defaultMinutes, nowMorning, zone),
        )
        val dueNextWeek = candidate(
            dueDay = LocalDate.of(2026, 4, 20),
            createdAtEpochMs = nowAfternoon,
        )
        assertFalse(
            DigestPlanner.sameDayFallback(dueNextWeek, defaultMinutes, nowAfternoon, zone),
        )
    }

    @Test
    fun sameDayFallback_importAfterDigest_usesAppearedAt() {
        val digestAt = LocalDate.of(2026, 4, 10).atTime(9, 0).toInstant(zone).toEpochMilli()
        val now = LocalDate.of(2026, 4, 10).atTime(14, 0).toInstant(zone).toEpochMilli()
        val restored = candidate(
            dueDay = LocalDate.of(2026, 4, 10),
            createdAtEpochMs = digestAt - 86_400_000L,
        )
        assertFalse(
            DigestPlanner.sameDayFallback(restored, defaultMinutes, now, zone),
        )
        assertTrue(
            DigestPlanner.sameDayFallback(
                restored,
                defaultMinutes,
                now,
                zone,
                appearedAtEpochMs = now,
            ),
        )
    }

    @Test
    fun shouldReplayMissedDigest_afterWindowIfNotMarkedToday() {
        val today = LocalDate.of(2026, 4, 10)
        val morning = today.atTime(8, 0).toInstant(zone).toEpochMilli()
        val afternoon = today.atTime(14, 0).toInstant(zone).toEpochMilli()
        val todayDay = today.toEpochDay()
        assertFalse(
            DigestPlanner.shouldReplayMissedDigest(null, defaultMinutes, morning, zone),
        )
        assertTrue(
            DigestPlanner.shouldReplayMissedDigest(null, defaultMinutes, afternoon, zone),
        )
        assertTrue(
            DigestPlanner.shouldReplayMissedDigest(todayDay - 1, defaultMinutes, afternoon, zone),
        )
        assertFalse(
            DigestPlanner.shouldReplayMissedDigest(todayDay, defaultMinutes, afternoon, zone),
        )
    }

    @Test
    fun alreadyPostedToday_blocksSecondSameDayFire() {
        val afternoon = LocalDate.of(2026, 4, 10).atTime(15, 0).toInstant(zone).toEpochMilli()
        val today = LocalDate.of(2026, 4, 10).toEpochDay()
        assertTrue(DigestPlanner.alreadyPostedToday(today, afternoon, zone))
        assertFalse(DigestPlanner.alreadyPostedToday(today - 1, afternoon, zone))
        assertFalse(DigestPlanner.alreadyPostedToday(null, afternoon, zone))
    }

    @Test
    fun alreadyPostedToday_standingFireRefusesAfterMissReplaySameDay() {
        val afternoon = LocalDate.of(2026, 4, 10).atTime(9, 1).toInstant(zone).toEpochMilli()
        val today = LocalDate.of(2026, 4, 10).toEpochDay()
        assertTrue(
            DigestPlanner.shouldReplayMissedDigest(null, defaultMinutes, afternoon, zone),
        )
        assertFalse(
            DigestPlanner.shouldReplayMissedDigest(today, defaultMinutes, afternoon, zone),
        )
        assertTrue(DigestPlanner.alreadyPostedToday(today, afternoon, zone))
    }
}

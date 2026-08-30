package com.errata.app.domain.digest

import com.errata.app.domain.cadence.CadenceCalculator
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
    }
}

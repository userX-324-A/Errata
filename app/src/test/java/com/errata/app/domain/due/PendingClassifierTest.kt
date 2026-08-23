package com.errata.app.domain.due

import com.errata.app.domain.cadence.CadenceCalculator
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingClassifierTest {

    private val zone = ZoneOffset.UTC

    private fun startOf(year: Int, month: Int, day: Int): Long =
        CadenceCalculator.startOfDayEpochMs(LocalDate.of(year, month, day).toEpochDay(), zone)

    private fun noon(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atTime(12, 0).toInstant(zone).toEpochMilli()

    private fun task(
        due: Long,
        snooze: Long? = null,
        paused: Boolean = false,
        archived: Boolean = false,
    ) = PendingClassifier.ClassifiableTask(
        nextDueAtEpochMs = due,
        snoozedUntilEpochMs = snooze,
        isPaused = paused,
        isArchived = archived,
    )

    @Test
    fun overdue_beforeToday() {
        val now = noon(2026, 3, 10)
        val bucket = PendingClassifier.classify(
            task(startOf(2026, 3, 9)),
            nowEpochMs = now,
            soonHorizonDays = 7,
            zone = zone,
        )
        assertEquals(DueBucket.OVERDUE, bucket)
    }

    @Test
    fun dueToday() {
        val now = noon(2026, 3, 10)
        val bucket = PendingClassifier.classify(
            task(startOf(2026, 3, 10)),
            nowEpochMs = now,
            soonHorizonDays = 7,
            zone = zone,
        )
        assertEquals(DueBucket.DUE_TODAY, bucket)
    }

    @Test
    fun soon_withinHorizon() {
        val now = noon(2026, 3, 10)
        val bucket = PendingClassifier.classify(
            task(startOf(2026, 3, 17)),
            nowEpochMs = now,
            soonHorizonDays = 7,
            zone = zone,
        )
        assertEquals(DueBucket.SOON, bucket)
    }

    @Test
    fun later_beyondHorizon() {
        val now = noon(2026, 3, 10)
        val bucket = PendingClassifier.classify(
            task(startOf(2026, 3, 18)),
            nowEpochMs = now,
            soonHorizonDays = 7,
            zone = zone,
        )
        assertEquals(DueBucket.LATER, bucket)
    }

    @Test
    fun snooze_hidesOverdueUntilSnoozeEnds() {
        val now = noon(2026, 3, 10)
        // Originally overdue Mar 1, snoozed until Mar 12
        val bucket = PendingClassifier.classify(
            task(due = startOf(2026, 3, 1), snooze = startOf(2026, 3, 12)),
            nowEpochMs = now,
            soonHorizonDays = 7,
            zone = zone,
        )
        assertEquals(DueBucket.SOON, bucket)
    }

    @Test
    fun pausedOrArchived_hidden() {
        val now = noon(2026, 3, 10)
        assertEquals(
            DueBucket.HIDDEN,
            PendingClassifier.classify(
                task(startOf(2026, 3, 10), paused = true),
                now,
                zone = zone,
            ),
        )
        assertEquals(
            DueBucket.HIDDEN,
            PendingClassifier.classify(
                task(startOf(2026, 3, 10), archived = true),
                now,
                zone = zone,
            ),
        )
    }
}

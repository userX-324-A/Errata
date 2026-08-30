package com.errata.app.domain.history

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryRetentionTest {

    private val now = 1_800_000_000_000L
    private val day = Duration.ofDays(1).toMillis()

    private fun row(id: Long, taskId: Long, daysAgo: Long) = HistoryRetention.Row(
        id = id,
        taskId = taskId,
        completedAtEpochMs = now - daysAgo * day,
    )

    @Test
    fun keepAll_deletesNothing() {
        val rows = (1L..20L).map { row(it, 1, it * 400) }
        assertTrue(
            HistoryRetention.idsToDelete(rows, now, HistoryRetention.KEEP_ALL).isEmpty(),
        )
    }

    @Test
    fun yearlySparse_keepsEightBeyond90Days() {
        val rows = (1L..8L).map { id ->
            row(id, taskId = 7, daysAgo = (8 - id) * 365)
        }
        val gone = HistoryRetention.idsToDelete(rows, now, HistoryRetention.DAYS_90)
        assertTrue(gone.isEmpty())
    }

    @Test
    fun tenOldRows_keepsEightNewest() {
        val rows = (1L..10L).map { id ->
            row(id, taskId = 1, daysAgo = 100L + id)
        }
        val gone = HistoryRetention.idsToDelete(rows, now, HistoryRetention.DAYS_90).toSet()
        assertEquals(setOf(9L, 10L), gone)
    }

    @Test
    fun recentPlusOld_protectsEightIncludingSomeOld() {
        val recent = (1L..5L).map { id -> row(id, 1, daysAgo = id) }
        val old = (6L..15L).map { id -> row(id, 1, daysAgo = 200L + id) }
        val gone = HistoryRetention.idsToDelete(recent + old, now, HistoryRetention.DAYS_90).toSet()
        assertTrue(gone.none { it in 1L..8L })
        assertEquals(setOf(9L, 10L, 11L, 12L, 13L, 14L, 15L), gone)
    }

    @Test
    fun twoTasks_areIndependent() {
        val a = (1L..10L).map { id -> row(id, 1, daysAgo = 200) }
        val b = (11L..12L).map { id -> row(id, 2, daysAgo = 200) }
        val gone = HistoryRetention.idsToDelete(a + b, now, HistoryRetention.DAYS_90).toSet()
        assertEquals(2, gone.size)
        assertTrue(gone.all { it in 1L..10L })
        assertTrue(11L !in gone && 12L !in gone)
    }

    @Test
    fun keepAll_shouldNotRunEvenWhenForced() {
        assertTrue(
            !HistoryRetention.shouldRun(
                retentionDays = HistoryRetention.KEEP_ALL,
                lastPruneEpochDay = null,
                todayEpochDay = 1,
                force = true,
            ),
        )
    }

    @Test
    fun shouldRun_oncePerDayUnlessForced() {
        assertTrue(
            HistoryRetention.shouldRun(
                retentionDays = HistoryRetention.DAYS_90,
                lastPruneEpochDay = null,
                todayEpochDay = 10,
                force = false,
            ),
        )
        assertTrue(
            !HistoryRetention.shouldRun(
                retentionDays = HistoryRetention.DAYS_90,
                lastPruneEpochDay = 10,
                todayEpochDay = 10,
                force = false,
            ),
        )
        assertTrue(
            HistoryRetention.shouldRun(
                retentionDays = HistoryRetention.DAYS_90,
                lastPruneEpochDay = 10,
                todayEpochDay = 10,
                force = true,
            ),
        )
        assertTrue(
            HistoryRetention.shouldRun(
                retentionDays = HistoryRetention.DAYS_90,
                lastPruneEpochDay = 9,
                todayEpochDay = 10,
                force = false,
            ),
        )
    }
}

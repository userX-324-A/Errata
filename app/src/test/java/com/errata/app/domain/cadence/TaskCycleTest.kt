package com.errata.app.domain.cadence

import com.errata.app.data.local.TaskEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaskCycleTest {

    @Test
    fun skipped_clearsSnoozeAndDoesNotTouchLastCompleted() {
        val task = TaskEntity(
            id = 4,
            title = "Bins",
            estimateMinutes = 10,
            intervalDays = 7,
            cadenceMode = CadenceMode.FROM_COMPLETION,
            anchorEpochDay = 1,
            nextDueAtEpochMs = 1_000L,
            lastCompletedAtEpochMs = 500L,
            snoozedUntilEpochMs = 9_999L,
            createdAtEpochMs = 0,
            updatedAtEpochMs = 0,
        )
        val next = TaskCycle.skipped(task, nextDueAtEpochMs = 2_000L, nowEpochMs = 1_500L)
        assertEquals(2_000L, next.nextDueAtEpochMs)
        assertNull(next.snoozedUntilEpochMs)
        assertEquals(500L, next.lastCompletedAtEpochMs)
        assertEquals(1_500L, next.updatedAtEpochMs)
    }
}

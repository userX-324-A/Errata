package com.errata.app.domain.cadence

import com.errata.app.data.local.TaskEntity

object TaskCycle {
    /** Advance next due without a completion row. Snooze does not carry into the next cycle. */
    fun skipped(
        task: TaskEntity,
        nextDueAtEpochMs: Long,
        nowEpochMs: Long,
    ): TaskEntity = task.copy(
        nextDueAtEpochMs = nextDueAtEpochMs,
        snoozedUntilEpochMs = null,
        updatedAtEpochMs = nowEpochMs,
    )
}

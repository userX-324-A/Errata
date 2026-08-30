package com.errata.app.reminders

import java.util.concurrent.ConcurrentHashMap

/**
 * Shade and in-app Done/Snooze: one in-flight action per task; apply only if still on
 * the same due and the task is still open (not paused/archived).
 */
object ReminderActionGuard {
    private val processInFlight: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    fun tryBegin(taskId: Long): Boolean = tryBegin(processInFlight, taskId)

    fun end(taskId: Long) {
        end(processInFlight, taskId)
    }

    fun tryBegin(inFlight: MutableSet<Long>, taskId: Long): Boolean = inFlight.add(taskId)

    fun end(inFlight: MutableSet<Long>, taskId: Long) {
        inFlight.remove(taskId)
    }

    fun shouldComplete(
        currentNextDueAtEpochMs: Long,
        expectedNextDueAtEpochMs: Long?,
        isPaused: Boolean = false,
        isArchived: Boolean = false,
    ): Boolean = shouldApply(
        currentNextDueAtEpochMs,
        expectedNextDueAtEpochMs,
        isPaused,
        isArchived,
    )

    fun shouldSnooze(
        currentNextDueAtEpochMs: Long,
        expectedNextDueAtEpochMs: Long?,
        isPaused: Boolean = false,
        isArchived: Boolean = false,
    ): Boolean = shouldApply(
        currentNextDueAtEpochMs,
        expectedNextDueAtEpochMs,
        isPaused,
        isArchived,
    )

    private fun shouldApply(
        currentNextDueAtEpochMs: Long,
        expectedNextDueAtEpochMs: Long?,
        isPaused: Boolean,
        isArchived: Boolean,
    ): Boolean {
        if (isPaused || isArchived) return false
        return expectedNextDueAtEpochMs == null ||
            currentNextDueAtEpochMs == expectedNextDueAtEpochMs
    }
}

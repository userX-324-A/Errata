package com.errata.app.reminders

/**
 * Shade Done/Snooze: one in-flight action per task; complete only if still on the same due.
 */
object ReminderActionGuard {
    fun tryBegin(inFlight: MutableSet<Long>, taskId: Long): Boolean = inFlight.add(taskId)

    fun end(inFlight: MutableSet<Long>, taskId: Long) {
        inFlight.remove(taskId)
    }

    fun shouldComplete(currentNextDueAtEpochMs: Long, expectedNextDueAtEpochMs: Long?): Boolean =
        expectedNextDueAtEpochMs == null || currentNextDueAtEpochMs == expectedNextDueAtEpochMs
}

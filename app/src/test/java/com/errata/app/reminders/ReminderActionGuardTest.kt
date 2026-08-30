package com.errata.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderActionGuardTest {

    @Test
    fun tryBegin_secondTapSameTaskIsIgnored() {
        val inFlight = mutableSetOf<Long>()
        assertTrue(ReminderActionGuard.tryBegin(inFlight, 3L))
        assertFalse(ReminderActionGuard.tryBegin(inFlight, 3L))
        ReminderActionGuard.end(inFlight, 3L)
        assertTrue(ReminderActionGuard.tryBegin(inFlight, 3L))
    }

    @Test
    fun shouldComplete_nullExpectedAlwaysApplies() {
        assertTrue(ReminderActionGuard.shouldComplete(1_000L, expectedNextDueAtEpochMs = null))
    }

    @Test
    fun shouldComplete_mismatchMeansAlreadyAdvanced() {
        assertTrue(ReminderActionGuard.shouldComplete(1_000L, 1_000L))
        assertFalse(ReminderActionGuard.shouldComplete(2_000L, 1_000L))
    }

    @Test
    fun shouldComplete_refusesPausedOrArchived() {
        assertFalse(
            ReminderActionGuard.shouldComplete(1_000L, 1_000L, isPaused = true),
        )
        assertFalse(
            ReminderActionGuard.shouldComplete(
                1_000L,
                expectedNextDueAtEpochMs = null,
                isArchived = true,
            ),
        )
    }

    @Test
    fun shouldSnooze_refusesWhenDueAdvancedAfterSheetOpened() {
        val openedDue = 1_000L
        val afterShadeDone = 2_000L
        assertFalse(ReminderActionGuard.shouldSnooze(afterShadeDone, openedDue))
    }

    @Test
    fun shouldSkip_sameDueGuardAsComplete() {
        assertTrue(ReminderActionGuard.shouldSkip(1_000L, expectedNextDueAtEpochMs = null))
        assertTrue(ReminderActionGuard.shouldSkip(1_000L, 1_000L))
        assertFalse(ReminderActionGuard.shouldSkip(2_000L, 1_000L))
        assertFalse(ReminderActionGuard.shouldSkip(1_000L, 1_000L, isPaused = true))
        assertFalse(ReminderActionGuard.shouldSkip(1_000L, 1_000L, isArchived = true))
    }
}

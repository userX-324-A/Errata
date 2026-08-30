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
}

package com.errata.app.domain.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPolicyTest {

    @Test
    fun storedFor_noneWhenDueClock() {
        assertEquals(ReminderPolicy.NONE, ReminderPolicy.storedFor(DefaultReminderKind.NONE, 9 * 60))
        assertEquals(null, ReminderPolicy.storedFor(DefaultReminderKind.WHEN_DUE, 9 * 60))
        assertEquals(18 * 60, ReminderPolicy.storedFor(DefaultReminderKind.CLOCK, 18 * 60))
    }

    @Test
    fun isNone_onlySentinel() {
        assertTrue(ReminderPolicy.isNone(ReminderPolicy.NONE))
        assertFalse(ReminderPolicy.isNone(null))
        assertFalse(ReminderPolicy.isNone(0))
        assertFalse(ReminderPolicy.isNone(9 * 60))
    }

    @Test
    fun displayMinutes_clockVsDue() {
        assertEquals(8 * 60, ReminderPolicy.displayMinutes(8 * 60, 9 * 60))
        assertEquals(9 * 60, ReminderPolicy.displayMinutes(null, 9 * 60))
        assertEquals(9 * 60, ReminderPolicy.displayMinutes(ReminderPolicy.NONE, 9 * 60))
    }
}

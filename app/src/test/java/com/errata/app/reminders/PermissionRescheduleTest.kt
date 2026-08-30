package com.errata.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionRescheduleTest {

    @Test
    fun seedMatch_doesNotReschedule() {
        val flags = true to false
        assertFalse(PermissionReschedule.shouldRun(flags, flags))
    }

    @Test
    fun notifyGranted_reschedules() {
        assertTrue(PermissionReschedule.shouldRun(false to true, true to true))
    }
}

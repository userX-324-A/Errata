package com.errata.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAccessTest {

    @Test
    fun shouldSkipWakeup_whenNotificationsDisabled() {
        assertTrue(NotificationAccess.shouldSkipWakeup(notificationsEnabled = false))
        assertFalse(NotificationAccess.shouldSkipWakeup(notificationsEnabled = true))
    }
}

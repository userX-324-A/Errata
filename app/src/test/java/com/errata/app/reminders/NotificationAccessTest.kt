package com.errata.app.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAccessTest {

    @Test
    fun shouldSkipWakeup_whenNotificationsDisabled() {
        assertTrue(NotificationAccess.shouldSkipWakeup(notificationsEnabled = false))
        assertFalse(NotificationAccess.shouldSkipWakeup(notificationsEnabled = true))
    }

    @Test
    fun shouldPrompt_onlyApi33DeniedAndNotYetAsked() {
        assertFalse(
            NotificationAccess.shouldPrompt(
                sdkInt = 32,
                notificationsEnabled = false,
                prompted = false,
            ),
        )
        assertFalse(
            NotificationAccess.shouldPrompt(
                sdkInt = 33,
                notificationsEnabled = true,
                prompted = false,
            ),
        )
        assertFalse(
            NotificationAccess.shouldPrompt(
                sdkInt = 33,
                notificationsEnabled = false,
                prompted = true,
            ),
        )
        assertTrue(
            NotificationAccess.shouldPrompt(
                sdkInt = 33,
                notificationsEnabled = false,
                prompted = false,
            ),
        )
    }

    @Test
    fun afterPinPrompt_notificationsBeforeExact() {
        assertEquals(AfterPinPrompt.Notifications, afterPinPrompt(true, true))
        assertEquals(AfterPinPrompt.Exact, afterPinPrompt(false, true))
        assertEquals(AfterPinPrompt.None, afterPinPrompt(false, false))
    }
}

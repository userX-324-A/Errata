package com.errata.app.reminders

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BootReceiverTest {

    @Test
    fun isClockAction_onlyTimeAndZone() {
        assertTrue(BootReceiver.isClockAction(Intent.ACTION_TIME_CHANGED))
        assertTrue(BootReceiver.isClockAction(Intent.ACTION_TIMEZONE_CHANGED))
        assertFalse(BootReceiver.isClockAction(Intent.ACTION_BOOT_COMPLETED))
        assertFalse(BootReceiver.isClockAction(BootReceiver.ACTION_QUICKBOOT))
    }

    @Test
    fun shouldDebounceClock_withinWindow() {
        assertTrue(BootReceiver.shouldDebounceClock(lastMs = 1_000L, nowMs = 2_500L))
        assertFalse(BootReceiver.shouldDebounceClock(lastMs = 1_000L, nowMs = 3_000L))
        assertFalse(BootReceiver.shouldDebounceClock(lastMs = 1_000L, nowMs = 4_000L))
    }
}

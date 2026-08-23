package com.errata.app.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExactAlarmAccessTest {

    @Test
    fun isRelevantSdk_fromAndroid12() {
        assertFalse(ExactAlarmAccess.isRelevantSdk(30))
        assertTrue(ExactAlarmAccess.isRelevantSdk(31))
        assertTrue(ExactAlarmAccess.isRelevantSdk(35))
    }

    @Test
    fun shouldPrompt_onlyWhenApi31DeniedAndNotYetAsked() {
        assertFalse(ExactAlarmAccess.shouldPrompt(sdkInt = 30, canExact = false, prompted = false))
        assertFalse(ExactAlarmAccess.shouldPrompt(sdkInt = 31, canExact = true, prompted = false))
        assertFalse(ExactAlarmAccess.shouldPrompt(sdkInt = 31, canExact = false, prompted = true))
        assertTrue(ExactAlarmAccess.shouldPrompt(sdkInt = 31, canExact = false, prompted = false))
        assertTrue(ExactAlarmAccess.shouldPrompt(sdkInt = 33, canExact = false, prompted = false))
    }
}

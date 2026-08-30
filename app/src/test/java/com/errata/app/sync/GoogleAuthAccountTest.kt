package com.errata.app.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthAccountTest {

    @Test
    fun accountMatches_blankAuthorizedIsUnknownOk() {
        assertTrue(GoogleAuth.accountMatches("a@example.com", null))
        assertTrue(GoogleAuth.accountMatches("a@example.com", ""))
    }

    @Test
    fun accountMatches_sameEmailIgnoresCase() {
        assertTrue(GoogleAuth.accountMatches("A@Example.com", "a@example.com"))
        assertFalse(GoogleAuth.accountMatches("a@example.com", "b@example.com"))
    }

    @Test
    fun resolveConsentEmail_pendingSurvivesBlankAuthorized() {
        assertEquals(
            "a@example.com",
            GoogleAuth.resolveConsentEmail("a@example.com", null),
        )
        assertEquals(
            "a@example.com",
            GoogleAuth.resolveConsentEmail("a@example.com", ""),
        )
    }

    @Test
    fun resolveConsentEmail_recoversFromIntentWhenPendingMissing() {
        assertEquals(
            "a@example.com",
            GoogleAuth.resolveConsentEmail(null, "a@example.com"),
        )
        assertNull(GoogleAuth.resolveConsentEmail(null, null))
        assertNull(GoogleAuth.resolveConsentEmail("", "  "))
    }

    @Test
    fun resolveConsentEmail_mismatchIsRejected() {
        assertNull(GoogleAuth.resolveConsentEmail("a@example.com", "b@example.com"))
    }
}

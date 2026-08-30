package com.errata.app.sync

import org.junit.Assert.assertFalse
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
}

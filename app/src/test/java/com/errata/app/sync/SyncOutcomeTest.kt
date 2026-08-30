package com.errata.app.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncOutcomeTest {

    @Test
    fun authAndCorruptAreSticky() {
        assertEquals(SyncOutcome.Auth, syncOutcomeForFailure("auth"))
        assertEquals(SyncOutcome.Corrupt, syncOutcomeForFailure("corrupt"))
    }

    @Test
    fun networkAndConflictRetry() {
        assertEquals(SyncOutcome.Retryable, syncOutcomeForFailure("network"))
        assertEquals(SyncOutcome.Retryable, syncOutcomeForFailure("conflict"))
        assertEquals(SyncOutcome.Retryable, syncOutcomeForFailure("wipe"))
    }
}

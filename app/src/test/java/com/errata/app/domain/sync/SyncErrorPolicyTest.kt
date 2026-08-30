package com.errata.app.domain.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncErrorPolicyTest {

    @Test
    fun blocksBackground_onlyAuthAndCorrupt() {
        assertEquals(false, SyncErrorPolicy.blocksBackground(null))
        assertEquals(false, SyncErrorPolicy.blocksBackground("network"))
        assertEquals(false, SyncErrorPolicy.blocksBackground("wipe"))
        assertEquals(true, SyncErrorPolicy.blocksBackground("auth"))
        assertEquals(true, SyncErrorPolicy.blocksBackground("corrupt"))
    }
}

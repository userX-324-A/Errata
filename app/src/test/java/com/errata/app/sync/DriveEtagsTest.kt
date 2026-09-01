package com.errata.app.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriveEtagsTest {

    @Test
    fun blankIsNotAPrecondition() {
        assertNull(DriveEtags.ifMatchValue(null))
        assertNull(DriveEtags.ifMatchValue(""))
        assertNull(DriveEtags.ifMatchValue("  "))
    }

    @Test
    fun strongQuotedEtagPassesThrough() {
        assertEquals("\"abc123\"", DriveEtags.ifMatchValue("\"abc123\""))
    }

    @Test
    fun weakMediaEtagIsIgnored() {
        assertNull(DriveEtags.ifMatchValue("W/\"md5hash\""))
        assertNull(DriveEtags.ifMatchValue("w/\"md5hash\""))
        assertNull(DriveEtags.ifMatchValue("  W/\"md5hash\"  "))
    }

    @Test
    fun falsePrecondition_sameOrMissingFreshEtag() {
        assertEquals(true, DriveEtags.falsePrecondition("\"a\"", "\"a\""))
        assertEquals(true, DriveEtags.falsePrecondition("\"a\"", null))
        assertEquals(false, DriveEtags.falsePrecondition("\"a\"", "\"b\""))
        assertEquals(false, DriveEtags.falsePrecondition(null, "\"a\""))
    }
}

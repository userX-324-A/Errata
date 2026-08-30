package com.errata.app.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DriveSyncFilesTest {

    @Test
    fun pickCanonical_empty() {
        assertNull(DriveSyncFiles.pickCanonical(emptyList()))
    }

    @Test
    fun pickCanonical_newestModifiedTime() {
        val older = DriveSyncFiles.FileRef("a", "2026-01-01T00:00:00.000Z")
        val newer = DriveSyncFiles.FileRef("b", "2026-08-30T12:00:00.000Z")
        assertEquals(newer, DriveSyncFiles.pickCanonical(listOf(older, newer)))
        assertEquals(listOf("a"), DriveSyncFiles.orphanIds(listOf(older, newer), newer.id))
    }

    @Test
    fun pickCanonical_tieBreaksById() {
        val a = DriveSyncFiles.FileRef("aaa", "2026-08-30T12:00:00.000Z")
        val b = DriveSyncFiles.FileRef("bbb", "2026-08-30T12:00:00.000Z")
        assertEquals(b, DriveSyncFiles.pickCanonical(listOf(a, b)))
    }

    @Test
    fun wipeComplete_emptyList() {
        assertEquals(true, DriveSyncFiles.wipeComplete(emptyList(), emptySet()))
    }

    @Test
    fun wipeComplete_allDeleted() {
        val files = listOf(
            DriveSyncFiles.FileRef("a"),
            DriveSyncFiles.FileRef("b"),
        )
        assertEquals(true, DriveSyncFiles.wipeComplete(files, setOf("a", "b")))
        assertEquals(false, DriveSyncFiles.wipeComplete(files, setOf("a")))
    }

    @Test
    fun mediaUnreadable_blankIsCorrupt() {
        assertEquals(true, DriveSyncFiles.mediaUnreadable(""))
        assertEquals(true, DriveSyncFiles.mediaUnreadable("  \n"))
        assertEquals(false, DriveSyncFiles.mediaUnreadable("{}"))
    }
}

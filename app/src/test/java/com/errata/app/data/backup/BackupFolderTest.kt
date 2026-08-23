package com.errata.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupFolderTest {

    @Test
    fun fileName_isStable() {
        assertEquals("errata-backup.json", BackupFolder.FILE_NAME)
    }

    @Test
    fun persistableUriString_blankIsNull() {
        assertNull(BackupFolder.persistableUriString(null))
        assertNull(BackupFolder.persistableUriString(""))
        assertNull(BackupFolder.persistableUriString("   "))
    }

    @Test
    fun persistableUriString_trims() {
        assertEquals(
            "content://com.android.externalstorage.documents/tree/primary%3AErrata",
            BackupFolder.persistableUriString(
                "  content://com.android.externalstorage.documents/tree/primary%3AErrata  ",
            ),
        )
    }

    @Test
    fun memory_persistAndClear() {
        val mem = BackupFolder.Memory()
        assertNull(mem.get())
        mem.set("  content://tree/abc  ")
        assertEquals("content://tree/abc", mem.get())
        mem.set("   ")
        assertNull(mem.get())
        mem.set("content://tree/xyz")
        mem.clear()
        assertNull(mem.get())
    }
}

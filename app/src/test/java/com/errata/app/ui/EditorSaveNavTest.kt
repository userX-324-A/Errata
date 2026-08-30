package com.errata.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorSaveNavTest {

    @Test
    fun saveFromCatalog_popsStartersInclusive() {
        assertEquals(
            listOf("pending"),
            EditorSaveNav.remainingAfterSave(listOf("pending", "starters", "task/0")),
        )
        assertEquals(
            listOf("tasks"),
            EditorSaveNav.remainingAfterSave(listOf("tasks", "starters", "task/0")),
        )
    }

    @Test
    fun saveFromEdit_popsOnce() {
        assertEquals(
            listOf("pending"),
            EditorSaveNav.remainingAfterSave(listOf("pending", "task/3")),
        )
    }
}

package com.errata.app.ui.task

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorBackTest {

    @Test
    fun afterSavePrompt_backsToListNotCatalog() {
        assertEquals(
            EditorBackAction.FinishAfterSavePrompt,
            editorBackAction(
                discardOpen = false,
                promptingAfterSave = true,
                dirty = false,
            ),
        )
    }

    @Test
    fun afterSavePrompt_winsOverDirty() {
        assertEquals(
            EditorBackAction.FinishAfterSavePrompt,
            editorBackAction(
                discardOpen = false,
                promptingAfterSave = true,
                dirty = true,
            ),
        )
    }

    @Test
    fun dirty_confirmsDiscard() {
        assertEquals(
            EditorBackAction.ConfirmDiscard,
            editorBackAction(
                discardOpen = false,
                promptingAfterSave = false,
                dirty = true,
            ),
        )
    }

    @Test
    fun cleanUnsaved_popsOnce() {
        assertEquals(
            EditorBackAction.PopOnce,
            editorBackAction(
                discardOpen = false,
                promptingAfterSave = false,
                dirty = false,
            ),
        )
    }

    @Test
    fun discardDialogOpen_closesFirst() {
        assertEquals(
            EditorBackAction.DismissDiscard,
            editorBackAction(
                discardOpen = true,
                promptingAfterSave = false,
                dirty = true,
            ),
        )
    }
}

package com.errata.app.ui.task

enum class EditorBackAction {
    DismissDiscard,
    /** Saved; notify/exact prompt is showing — leave like Save (popToList). */
    FinishAfterSavePrompt,
    ConfirmDiscard,
    PopOnce,
}

/**
 * System Back and the editor top-bar back. After Save, permission prompts keep
 * the editor; Back must still drop catalog+editor, not pop once onto the catalog.
 */
fun editorBackAction(
    discardOpen: Boolean,
    promptingAfterSave: Boolean,
    dirty: Boolean,
): EditorBackAction = when {
    discardOpen -> EditorBackAction.DismissDiscard
    promptingAfterSave -> EditorBackAction.FinishAfterSavePrompt
    dirty -> EditorBackAction.ConfirmDiscard
    else -> EditorBackAction.PopOnce
}

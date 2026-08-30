package com.errata.app.ui.adaptive

/**
 * After editor Save: drop every detail destination so the origin list
 * (Pending or All tasks) is showing. Catalog under the editor is dropped
 * with the editor. Back still pops once ([remainingAfterBack]).
 */
object PaneSaveNav {
    suspend fun popToList(
        canNavigateBack: () -> Boolean,
        navigateBack: suspend () -> Unit,
    ) {
        while (canNavigateBack()) {
            navigateBack()
        }
    }

    fun remainingAfterBack(detailKeys: List<String>): List<String> =
        detailKeys.dropLast(1)
}

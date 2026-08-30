package com.errata.app.ui

import androidx.navigation.NavController

/**
 * After editor Save: if the catalog is under the editor, pop it too so the
 * origin tab (Pending or Library) is showing. Back still pops once.
 */
object EditorSaveNav {
    fun remainingAfterSave(
        routes: List<String>,
        startersRoute: String = Routes.STARTERS,
    ): List<String> {
        val idx = routes.indexOf(startersRoute)
        return if (idx >= 0) routes.take(idx) else routes.dropLast(1)
    }

    fun popAfterSave(nav: NavController) {
        if (!nav.popBackStack(Routes.STARTERS, inclusive = true)) {
            nav.popBackStack()
        }
    }
}

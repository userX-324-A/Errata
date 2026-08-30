package com.errata.app.ui.adaptive

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaneSaveNavTest {

    @Test
    fun saveFromCatalog_popsCatalogAndEditor() = runTest {
        val stack = mutableListOf(PaneDest.CATALOG, PaneDest.task(0L, "bins"))
        PaneSaveNav.popToList(
            canNavigateBack = { stack.isNotEmpty() },
            navigateBack = { stack.removeAt(stack.lastIndex) },
        )
        assertTrue(stack.isEmpty())
    }

    @Test
    fun saveFromEdit_popsEditor() = runTest {
        val stack = mutableListOf(PaneDest.task(3L))
        PaneSaveNav.popToList(
            canNavigateBack = { stack.isNotEmpty() },
            navigateBack = { stack.removeAt(stack.lastIndex) },
        )
        assertTrue(stack.isEmpty())
    }

    @Test
    fun backFromEditor_keepsCatalog() {
        assertEquals(
            listOf(PaneDest.CATALOG),
            PaneSaveNav.remainingAfterBack(
                listOf(PaneDest.CATALOG, PaneDest.task(0L, "bins")),
            ),
        )
    }
}

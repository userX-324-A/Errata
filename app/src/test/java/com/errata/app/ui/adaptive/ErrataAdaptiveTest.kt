package com.errata.app.ui.adaptive

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrataAdaptiveTest {

    @Test
    fun twoPaneFromMediumWidth() {
        assertFalse(ErrataAdaptive.listDetailTwoPane(599))
        assertTrue(ErrataAdaptive.listDetailTwoPane(600))
        assertTrue(ErrataAdaptive.listDetailTwoPane(720))
        assertTrue(ErrataAdaptive.listDetailTwoPane(840))
    }

    @Test
    fun editorTwoColumnFitsSevenInchLandscapeHalfPane() {
        assertFalse(ErrataAdaptive.editorTwoColumn(479))
        assertTrue(ErrataAdaptive.editorTwoColumn(480))
        assertTrue(ErrataAdaptive.editorTwoColumn(520))
    }
}

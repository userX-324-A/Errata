package com.errata.app.ui.adaptive

import com.errata.app.ui.theme.ERRATA_EDITOR_TWO_COLUMN_MIN_DP

/**
 * List-detail and editor layout breakpoints. Compact is a full-screen push;
 * medium+ matches the nav rail with two panes.
 */
object ErrataAdaptive {
    /** Same lower bound as Material medium width / nav rail. */
    const val LIST_DETAIL_TWO_PANE_MIN_WIDTH_DP = 600

    fun listDetailTwoPane(windowWidthDp: Int): Boolean =
        windowWidthDp >= LIST_DETAIL_TWO_PANE_MIN_WIDTH_DP

    fun editorTwoColumn(paneWidthDp: Int): Boolean =
        paneWidthDp >= ERRATA_EDITOR_TWO_COLUMN_MIN_DP
}

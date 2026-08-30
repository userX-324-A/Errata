package com.errata.app.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val ERRATA_CONTENT_MAX_DP = 720
const val ERRATA_EDITOR_TWO_COLUMN_MIN_DP = 560

/** Center a readable column on tablets instead of stretching the form full-bleed. */
fun Modifier.errataContentWidth(): Modifier =
    fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = ERRATA_CONTENT_MAX_DP.dp)
        .fillMaxWidth()

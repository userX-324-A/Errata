package com.errata.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.errata.app.R
import com.errata.app.domain.cadence.CadenceMode

private val Modes = listOf(
    CadenceMode.FROM_COMPLETION to R.string.cadence_from_completion,
    CadenceMode.FIXED_ANCHOR to R.string.cadence_fixed,
    CadenceMode.FROM_COMPLETION_CATCH_UP to R.string.cadence_catch_up,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadenceModeRow(
    selected: CadenceMode,
    onSelect: (CadenceMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        Modes.forEachIndexed { index, (mode, labelRes) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, Modes.size),
                label = {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.Center,
                    )
                },
            )
        }
    }
}

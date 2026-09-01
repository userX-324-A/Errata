package com.errata.app.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.errata.app.R

@Composable
fun NotifyPromptDialog(
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onNotNow,
        title = { Text(stringResource(R.string.notify_prompt_title)) },
        text = { Text(stringResource(R.string.notify_prompt_body)) },
        confirmButton = {
            Button(onClick = onAllow) {
                Text(stringResource(R.string.notify_prompt_allow))
            }
        },
        dismissButton = {
            TextButton(onClick = onNotNow) {
                Text(stringResource(R.string.exact_prompt_not_now))
            }
        },
    )
}

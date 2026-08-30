package com.errata.app.ui.snooze

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.errata.app.R
import com.errata.app.ui.common.isDevice24Hour
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeSheet(
    onPreset: (SnoozePreset) -> Unit,
    onCustomUntil: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            if (!showTimePicker) onDismiss()
        },
        sheetState = sheetState,
        sheetMaxWidth = 640.dp,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.snooze_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            SnoozeOption(stringResource(R.string.snooze_1h)) {
                onPreset(SnoozePreset.ONE_HOUR)
            }
            SnoozeOption(stringResource(R.string.snooze_later_today)) {
                onPreset(SnoozePreset.LATER_TODAY)
            }
            SnoozeOption(stringResource(R.string.snooze_tomorrow)) {
                onPreset(SnoozePreset.TOMORROW)
            }
            SnoozeOption(stringResource(R.string.snooze_pick_time)) {
                showTimePicker = true
            }
        }
    }

    if (showTimePicker) {
        val now = LocalTime.now()
        val timeState = rememberTimePickerState(
            initialHour = now.hour,
            initialMinute = now.minute,
            is24Hour = isDevice24Hour(),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.snooze_pick_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val until = SnoozePresets.untilEpochMsForClock(
                            hour = timeState.hour,
                            minute = timeState.minute,
                        )
                        showTimePicker = false
                        onCustomUntil(until)
                    },
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SnoozeOption(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    )
}

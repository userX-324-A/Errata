package com.errata.app.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errata.app.R
import com.errata.app.domain.cadence.CadenceMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class TimePickerTarget { DUE, REMINDER }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(
    viewModel: TaskEditorViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var timePickerTarget by remember { mutableStateOf<TimePickerTarget?>(null) }

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (state.isNew) R.string.editor_new_title else R.string.editor_edit_title,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::save) {
                        Text(stringResource(R.string.action_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (!state.loaded) {
            Spacer(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        val dueDateLabel = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .format(LocalDate.ofEpochDay(state.dueEpochDay))
        val dueTimeLabel = formatMinutes(state.dueMinuteOfDay)
        val reminderLabel = formatMinutes(
            state.reminderMinutesOfDay ?: state.defaultReminderMinutesOfDay,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::updateTitle,
                label = { Text(stringResource(R.string.field_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = state.errorMessage == "title",
            )
            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text(stringResource(R.string.field_notes)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            OutlinedTextField(
                value = state.estimateMinutes,
                onValueChange = viewModel::updateEstimate,
                label = { Text(stringResource(R.string.field_estimate)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = state.errorMessage == "estimate",
            )
            OutlinedTextField(
                value = state.intervalDays,
                onValueChange = viewModel::updateInterval,
                label = { Text(stringResource(R.string.field_interval)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = state.errorMessage == "interval",
                supportingText = {
                    Text(stringResource(R.string.cadence_applies_after_done))
                },
            )

            Text(
                text = stringResource(R.string.field_cadence_mode),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CadenceChip(
                    label = stringResource(R.string.cadence_from_completion),
                    selected = state.cadenceMode == CadenceMode.FROM_COMPLETION,
                    onClick = { viewModel.updateCadenceMode(CadenceMode.FROM_COMPLETION) },
                )
                CadenceChip(
                    label = stringResource(R.string.cadence_fixed),
                    selected = state.cadenceMode == CadenceMode.FIXED_ANCHOR,
                    onClick = { viewModel.updateCadenceMode(CadenceMode.FIXED_ANCHOR) },
                )
                CadenceChip(
                    label = stringResource(R.string.cadence_catch_up),
                    selected = state.cadenceMode == CadenceMode.FROM_COMPLETION_CATCH_UP,
                    onClick = { viewModel.updateCadenceMode(CadenceMode.FROM_COMPLETION_CATCH_UP) },
                )
            }

            Text(
                text = stringResource(R.string.field_due_heading),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.field_due_combined, dueDateLabel, dueTimeLabel),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.pick_date))
                }
                TextButton(onClick = { timePickerTarget = TimePickerTarget.DUE }) {
                    Text(stringResource(R.string.pick_time))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    viewModel.updateDueEpochDay(LocalDate.now().toEpochDay())
                }) { Text(stringResource(R.string.due_today)) }
                TextButton(onClick = {
                    viewModel.updateDueEpochDay(LocalDate.now().plusDays(1).toEpochDay())
                }) { Text(stringResource(R.string.due_tomorrow)) }
            }

            Text(
                text = stringResource(R.string.field_reminder),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (state.reminderMinutesOfDay == null) {
                    stringResource(R.string.reminder_using_default, reminderLabel)
                } else {
                    stringResource(R.string.reminder_custom, reminderLabel)
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.reminderMinutesOfDay == null,
                    onClick = viewModel::useDefaultReminder,
                    label = {
                        Text(
                            stringResource(
                                R.string.reminder_use_default,
                                formatMinutes(state.defaultReminderMinutesOfDay),
                            ),
                        )
                    },
                )
                TextButton(onClick = { timePickerTarget = TimePickerTarget.REMINDER }) {
                    Text(stringResource(R.string.pick_time))
                }
                listOf(8 * 60, 9 * 60, 12 * 60, 18 * 60).forEach { minutes ->
                    FilterChip(
                        selected = state.reminderMinutesOfDay == minutes,
                        onClick = { viewModel.updateReminderMinutes(minutes) },
                        label = { Text(formatMinutes(minutes)) },
                    )
                }
            }

            when (state.errorMessage) {
                "title" -> ErrorText(stringResource(R.string.error_title))
                "estimate" -> ErrorText(stringResource(R.string.error_estimate))
                "interval" -> ErrorText(stringResource(R.string.error_interval))
                "missing" -> ErrorText(stringResource(R.string.error_missing_task))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dueEpochDay * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val day = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toEpochDay()
                            viewModel.updateDueEpochDay(day)
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    timePickerTarget?.let { target ->
        val initialMinutes = when (target) {
            TimePickerTarget.DUE -> state.dueMinuteOfDay
            TimePickerTarget.REMINDER ->
                state.reminderMinutesOfDay ?: state.defaultReminderMinutesOfDay
        }
        val timeState = rememberTimePickerState(
            initialHour = initialMinutes / 60,
            initialMinute = initialMinutes % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { timePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = timeState.hour * 60 + timeState.minute
                        when (target) {
                            TimePickerTarget.DUE -> viewModel.updateDueMinuteOfDay(minutes)
                            TimePickerTarget.REMINDER -> viewModel.updateReminderMinutes(minutes)
                        }
                        timePickerTarget = null
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { timePickerTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            title = {
                Text(
                    stringResource(
                        when (target) {
                            TimePickerTarget.DUE -> R.string.pick_due_time
                            TimePickerTarget.REMINDER -> R.string.pick_reminder_time
                        },
                    ),
                )
            },
            text = { TimePicker(state = timeState) },
        )
    }
}

@Composable
private fun CadenceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

private fun formatMinutes(minutesOfDay: Int): String {
    val time = LocalTime.of(minutesOfDay / 60, minutesOfDay % 60)
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time)
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}

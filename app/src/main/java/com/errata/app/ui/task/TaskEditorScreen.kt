package com.errata.app.ui.task

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errata.app.R
import com.errata.app.domain.area.TaskAreas
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.NthWeekday
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.cadence.Seasons
import com.errata.app.domain.cadence.Weekdays
import com.errata.app.domain.cadence.YearMonths
import com.errata.app.domain.history.TypicalLateness
import com.errata.app.reminders.ExactAlarmAccess
import com.errata.app.ui.theme.ErrataScreenInsets
import com.errata.app.ui.theme.ErrataTopInsets
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

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
    var showCustomArea by remember { mutableStateOf(false) }
    var customAreaText by remember { mutableStateOf("") }
    var showExactPrompt by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.rescheduleReminders()
        onDone()
    }

    LaunchedEffect(state.saved) {
        if (!state.saved) return@LaunchedEffect
        if (ExactAlarmAccess.shouldPrompt(context)) {
            showExactPrompt = true
        } else {
            onDone()
        }
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
                windowInsets = ErrataTopInsets,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ErrataScreenInsets,
    ) { innerPadding ->
        if (!state.loaded) {
            Spacer(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        val dueDateLabel = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .format(LocalDate.ofEpochDay(state.dueEpochDay))
        val dueTimeLabel = formatMinutes(state.dueMinuteOfDay)
        val reminderMinutes = state.reminderMinutesOfDay ?: state.dueMinuteOfDay
        val reminderLabel = formatMinutes(reminderMinutes)

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

            Text(
                text = stringResource(R.string.field_area),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.area_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.area == null,
                    onClick = { viewModel.updateArea(null) },
                    label = { Text(stringResource(R.string.area_none)) },
                )
                TaskAreas.PRESETS.forEach { preset ->
                    FilterChip(
                        selected = state.area == preset,
                        onClick = { viewModel.updateArea(preset) },
                        label = { Text(preset) },
                    )
                }
                val currentArea = state.area
                val customSelected = currentArea != null && currentArea !in TaskAreas.PRESETS
                FilterChip(
                    selected = customSelected,
                    onClick = {
                        customAreaText = currentArea.orEmpty()
                        showCustomArea = true
                    },
                    label = {
                        Text(
                            if (customSelected) {
                                currentArea.orEmpty()
                            } else {
                                stringResource(R.string.area_custom)
                            },
                        )
                    },
                )
            }

            OutlinedTextField(
                value = state.estimateMinutes,
                onValueChange = viewModel::updateEstimate,
                label = { Text(stringResource(R.string.field_estimate)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                isError = state.errorMessage == "estimate",
            )

            Text(
                text = stringResource(R.string.field_schedule),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CadenceChip(
                    label = stringResource(R.string.schedule_interval),
                    selected = state.scheduleKind == ScheduleKind.INTERVAL,
                    onClick = { viewModel.updateScheduleKind(ScheduleKind.INTERVAL) },
                )
                CadenceChip(
                    label = stringResource(R.string.schedule_weekly),
                    selected = state.scheduleKind == ScheduleKind.WEEKLY,
                    onClick = { viewModel.updateScheduleKind(ScheduleKind.WEEKLY) },
                )
                CadenceChip(
                    label = stringResource(R.string.schedule_monthly),
                    selected = state.scheduleKind == ScheduleKind.MONTHLY,
                    onClick = { viewModel.updateScheduleKind(ScheduleKind.MONTHLY) },
                )
                CadenceChip(
                    label = stringResource(R.string.schedule_nth_weekday),
                    selected = state.scheduleKind == ScheduleKind.NTH_WEEKDAY,
                    onClick = { viewModel.updateScheduleKind(ScheduleKind.NTH_WEEKDAY) },
                )
                CadenceChip(
                    label = stringResource(R.string.schedule_yearly),
                    selected = state.scheduleKind == ScheduleKind.YEARLY,
                    onClick = { viewModel.updateScheduleKind(ScheduleKind.YEARLY) },
                )
            }

            when (state.scheduleKind) {
                ScheduleKind.INTERVAL -> {
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
                }
                ScheduleKind.WEEKLY -> {
                    Text(
                        text = stringResource(R.string.field_weekdays),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            CadenceChip(
                                label = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                selected = Weekdays.contains(state.weekdaysMask, day),
                                onClick = { viewModel.toggleWeekday(day) },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.schedule_grid_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ScheduleKind.MONTHLY -> {
                    OutlinedTextField(
                        value = state.monthDay,
                        onValueChange = viewModel::updateMonthDay,
                        label = { Text(stringResource(R.string.field_month_day)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.errorMessage == "monthDay",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(1, 15, 28).forEach { day ->
                            CadenceChip(
                                label = day.toString(),
                                selected = state.monthDay.toIntOrNull() == day,
                                onClick = { viewModel.updateMonthDay(day.toString()) },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.schedule_grid_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ScheduleKind.NTH_WEEKDAY -> {
                    Text(
                        text = stringResource(R.string.field_weekday_ordinal),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            1 to R.string.schedule_ordinal_1st,
                            2 to R.string.schedule_ordinal_2nd,
                            3 to R.string.schedule_ordinal_3rd,
                            4 to R.string.schedule_ordinal_4th,
                            NthWeekday.LAST to R.string.schedule_ordinal_last,
                        ).forEach { (ordinal, label) ->
                            CadenceChip(
                                label = stringResource(label),
                                selected = state.weekdayOrdinal == ordinal,
                                onClick = { viewModel.updateWeekdayOrdinal(ordinal) },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.field_weekdays),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DayOfWeek.entries.forEach { day ->
                            CadenceChip(
                                label = day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                selected = Weekdays.contains(state.weekdaysMask, day),
                                onClick = { viewModel.selectNthWeekday(day) },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.schedule_grid_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ScheduleKind.YEARLY -> {
                    Text(
                        text = stringResource(R.string.field_seasons),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Seasons.ENTRIES.forEach { season ->
                            CadenceChip(
                                label = season.label,
                                selected = Seasons.contains(state.seasonMask, season.bit),
                                onClick = { viewModel.toggleSeason(season.bit) },
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.schedule_seasons_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.field_year_months),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Month.entries.forEach { month ->
                            CadenceChip(
                                label = month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                selected = YearMonths.contains(state.yearMonthsMask, month),
                                onClick = { viewModel.toggleYearMonth(month) },
                            )
                        }
                    }
                    if (YearMonths.hasAny(state.yearMonthsMask)) {
                        OutlinedTextField(
                            value = state.monthDay,
                            onValueChange = viewModel::updateMonthDay,
                            label = { Text(stringResource(R.string.field_month_day)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.errorMessage == "yearly",
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(1, 15, 28).forEach { day ->
                                CadenceChip(
                                    label = day.toString(),
                                    selected = state.monthDay.toIntOrNull() == day,
                                    onClick = { viewModel.updateMonthDay(day.toString()) },
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.schedule_grid_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
                text = when {
                    state.reminderMinutesOfDay == null ->
                        stringResource(R.string.reminder_when_due_summary, reminderLabel)
                    state.reminderMinutesOfDay == state.defaultReminderMinutesOfDay ->
                        stringResource(R.string.reminder_using_default, reminderLabel)
                    else ->
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
                    onClick = viewModel::useWhenDueReminder,
                    label = { Text(stringResource(R.string.reminder_when_due)) },
                )
                FilterChip(
                    selected = state.reminderMinutesOfDay == state.defaultReminderMinutesOfDay,
                    onClick = viewModel::useAppDefaultReminder,
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

            state.history?.let { glance ->
                Text(
                    text = stringResource(R.string.history_heading),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.history_last_done,
                        formatHistoryDate(glance.lastCompletedEpochMs),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                glance.typical?.let { typical ->
                    Text(
                        text = typicalCopy(typical),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (state.errorMessage) {
                "title" -> ErrorText(stringResource(R.string.error_title))
                "estimate" -> ErrorText(stringResource(R.string.error_estimate))
                "interval" -> ErrorText(stringResource(R.string.error_interval))
                "weekdays" -> ErrorText(stringResource(R.string.error_weekdays))
                "monthDay" -> ErrorText(stringResource(R.string.error_month_day))
                "nthWeekday" -> ErrorText(stringResource(R.string.error_nth_weekday))
                "yearly" -> ErrorText(stringResource(R.string.error_yearly))
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
                state.reminderMinutesOfDay ?: state.dueMinuteOfDay
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

    if (showCustomArea) {
        AlertDialog(
            onDismissRequest = { showCustomArea = false },
            title = { Text(stringResource(R.string.area_custom_title)) },
            text = {
                OutlinedTextField(
                    value = customAreaText,
                    onValueChange = { customAreaText = it.take(TaskAreas.MAX_LENGTH) },
                    label = { Text(stringResource(R.string.area_custom_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateArea(customAreaText)
                        showCustomArea = false
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomArea = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showExactPrompt) {
        AlertDialog(
            onDismissRequest = {
                ExactAlarmAccess.markPrompted(context)
                onDone()
            },
            title = { Text(stringResource(R.string.exact_prompt_title)) },
            text = { Text(stringResource(R.string.exact_prompt_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        ExactAlarmAccess.markPrompted(context)
                        showExactPrompt = false
                        exactLauncher.launch(ExactAlarmAccess.requestIntent(context))
                    },
                ) { Text(stringResource(R.string.exact_prompt_allow)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        ExactAlarmAccess.markPrompted(context)
                        onDone()
                    },
                ) { Text(stringResource(R.string.exact_prompt_not_now)) }
            },
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

private fun formatHistoryDate(epochMs: Long): String {
    val date = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)
}

@Composable
private fun typicalCopy(typical: TypicalLateness): String = when (typical) {
    TypicalLateness.OnDueDay -> stringResource(R.string.history_on_due_day)
    is TypicalLateness.DaysAfter ->
        pluralStringResource(R.plurals.history_after, typical.days, typical.days)
    is TypicalLateness.DaysBefore ->
        pluralStringResource(R.plurals.history_before, typical.days, typical.days)
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}

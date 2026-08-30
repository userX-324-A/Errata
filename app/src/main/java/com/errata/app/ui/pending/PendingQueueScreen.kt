package com.errata.app.ui.pending

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.errata.app.R
import com.errata.app.domain.due.DueBucket
import com.errata.app.ui.common.AreaFilterChips
import com.errata.app.ui.common.TaskAreaLabel
import com.errata.app.ui.common.isDevice24Hour
import com.errata.app.domain.estimate.EstimateHonesty
import com.errata.app.domain.freewindow.FreeWindowRanker
import com.errata.app.ui.snooze.SnoozeSheet
import com.errata.app.ui.starter.StarterPackEmpty
import com.errata.app.ui.theme.ErrataTopInsets
import com.errata.app.ui.theme.ErrataWordmark
import com.errata.app.ui.theme.errataContentWidth
import java.time.LocalTime
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingQueueScreen(
    viewModel: PendingQueueViewModel,
    onAddTask: () -> Unit,
    onOpenTask: (Long) -> Unit,
    selectedTaskId: Long? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(60_000)
                viewModel.refreshNow()
            }
        }
    }
    var snoozeTaskId by remember { mutableStateOf<Long?>(null) }
    var skipConfirmTaskId by remember { mutableStateOf<Long?>(null) }
    var showCustomWindow by remember { mutableStateOf(false) }
    var showStopByPicker by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }
    var customError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { ErrataWordmark() },
                windowInsets = ErrataTopInsets,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        floatingActionButton = {
            if (!state.hasNoPinnedTasks && !state.isEmpty) {
                FloatingActionButton(
                    onClick = onAddTask,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.add_task),
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ErrataTopInsets,
    ) { innerPadding ->
        if (state.hasNoPinnedTasks) {
            StarterPackEmpty(
                title = stringResource(R.string.pending_empty_title),
                body = stringResource(R.string.starters_body),
                onAddTask = onAddTask,
                onPin = viewModel::pinStarters,
                onRescheduleReminders = viewModel::rescheduleReminders,
                modifier = Modifier.padding(innerPadding).errataContentWidth(),
            )
        } else if (state.isEmpty) {
            PendingEmptyState(
                modifier = Modifier.padding(innerPadding).errataContentWidth(),
                onAddTask = onAddTask,
                pinnedHint = state.startersPinnedHint,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .errataContentWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    FreeWindowChips(
                        state = state,
                        onMinutes = viewModel::setWindowMinutes,
                        onCustom = {
                            customMinutesText = ""
                            customError = null
                            showCustomWindow = true
                        },
                    )
                }
                if (state.availableAreas.isNotEmpty()) {
                    item {
                        AreaFilterChips(
                            usedAreas = state.availableAreas,
                            activeArea = state.activeArea,
                            onSelect = viewModel::setActiveArea,
                        )
                    }
                }
                if (state.areaFilterEmpty) {
                    item {
                        Text(
                            text = stringResource(
                                R.string.area_filter_empty,
                                state.activeArea.orEmpty(),
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    }
                }

                val windowActive = state.activeWindowMinutes != null
                if (windowActive) {
                    item {
                        FreeWindowHeader(state = state, onClear = viewModel::clearWindow)
                    }
                    if (!state.areaFilterEmpty && state.fits.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.free_window_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    } else if (!state.areaFilterEmpty) {
                        items(state.fits, key = { "fit-${it.task.id}" }) { item ->
                            PendingRow(
                                item = item,
                                selected = item.task.id == selectedTaskId,
                                actionsEnabled = item.task.id !in state.busyTaskIds,
                                onOpen = { onOpenTask(item.task.id) },
                                onDone = { viewModel.complete(item.task.id) },
                                onSnooze = { snoozeTaskId = item.task.id },
                                onSkip = { skipConfirmTaskId = item.task.id },
                            )
                        }
                    }
                } else {
                    if (state.overdue.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.section_overdue)) }
                        items(state.overdue, key = { it.task.id }) { item ->
                            PendingRow(
                                item = item,
                                selected = item.task.id == selectedTaskId,
                                actionsEnabled = item.task.id !in state.busyTaskIds,
                                onOpen = { onOpenTask(item.task.id) },
                                onDone = { viewModel.complete(item.task.id) },
                                onSnooze = { snoozeTaskId = item.task.id },
                                onSkip = { skipConfirmTaskId = item.task.id },
                            )
                        }
                    }
                    if (state.dueToday.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.section_due_today)) }
                        items(state.dueToday, key = { it.task.id }) { item ->
                            PendingRow(
                                item = item,
                                selected = item.task.id == selectedTaskId,
                                actionsEnabled = item.task.id !in state.busyTaskIds,
                                onOpen = { onOpenTask(item.task.id) },
                                onDone = { viewModel.complete(item.task.id) },
                                onSnooze = { snoozeTaskId = item.task.id },
                                onSkip = { skipConfirmTaskId = item.task.id },
                            )
                        }
                    }
                    if (state.soon.isNotEmpty()) {
                        item { SectionHeader(stringResource(R.string.section_soon)) }
                        items(state.soon, key = { it.task.id }) { item ->
                            PendingRow(
                                item = item,
                                selected = item.task.id == selectedTaskId,
                                actionsEnabled = item.task.id !in state.busyTaskIds,
                                onOpen = { onOpenTask(item.task.id) },
                                onDone = { viewModel.complete(item.task.id) },
                                onSnooze = { snoozeTaskId = item.task.id },
                                onSkip = { skipConfirmTaskId = item.task.id },
                            )
                        }
                    }
                }
            }
        }
    }

    snoozeTaskId?.let { id ->
        SnoozeSheet(
            onPreset = { preset ->
                viewModel.snooze(id, preset)
                snoozeTaskId = null
            },
            onCustomUntil = { until ->
                viewModel.snoozeUntil(id, until)
                snoozeTaskId = null
            },
            onDismiss = { snoozeTaskId = null },
        )
    }

    skipConfirmTaskId?.let { id ->
        AlertDialog(
            onDismissRequest = { skipConfirmTaskId = null },
            title = { Text(stringResource(R.string.skip_confirm_title)) },
            text = { Text(stringResource(R.string.skip_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.skip(id)
                        skipConfirmTaskId = null
                    },
                ) {
                    Text(stringResource(R.string.action_skip))
                }
            },
            dismissButton = {
                TextButton(onClick = { skipConfirmTaskId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    state.honesty?.let { honesty ->
        AlertDialog(
            onDismissRequest = viewModel::dismissHonesty,
            title = { Text(stringResource(R.string.honesty_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.honesty_body,
                        honesty.title,
                        honesty.estimateMinutes,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.applyHonesty(EstimateHonesty.SAME) }) {
                    Text(stringResource(R.string.honesty_same))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { viewModel.applyHonesty(EstimateHonesty.SHORTER) }) {
                        Text(stringResource(R.string.honesty_shorter))
                    }
                    TextButton(onClick = { viewModel.applyHonesty(EstimateHonesty.LONGER) }) {
                        Text(stringResource(R.string.honesty_longer))
                    }
                }
            },
        )
    }

    if (showCustomWindow) {
        AlertDialog(
            onDismissRequest = { showCustomWindow = false },
            title = { Text(stringResource(R.string.free_window_custom_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = {
                            customMinutesText = it.filter { ch -> ch.isDigit() }.take(4)
                            customError = null
                        },
                        label = { Text(stringResource(R.string.free_window_custom_minutes)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = {
                        showCustomWindow = false
                        showStopByPicker = true
                    }) {
                        Text(stringResource(R.string.free_window_stop_by))
                    }
                    customError?.let { key ->
                        Text(
                            text = stringResource(
                                when (key) {
                                    "past" -> R.string.free_window_error_past
                                    else -> R.string.free_window_error_minutes
                                },
                            ),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = customMinutesText.toIntOrNull()
                        if (minutes == null || minutes <= 0) {
                            customError = "invalid"
                        } else {
                            viewModel.setWindowMinutes(minutes)
                            showCustomWindow = false
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomWindow = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showStopByPicker) {
        val now = LocalTime.now()
        val timeState = rememberTimePickerState(
            initialHour = now.hour,
            initialMinute = now.minute,
            is24Hour = isDevice24Hour(),
        )
        AlertDialog(
            onDismissRequest = { showStopByPicker = false },
            title = { Text(stringResource(R.string.free_window_stop_by_title)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val stopMinutes = timeState.hour * 60 + timeState.minute
                        val available = FreeWindowRanker.minutesUntilStopBy(
                            stopByMinutesOfDay = stopMinutes,
                            nowEpochMs = System.currentTimeMillis(),
                        )
                        if (available != null && available > 0) {
                            viewModel.setWindowMinutes(available)
                            showStopByPicker = false
                        } else {
                            showStopByPicker = false
                            customMinutesText = ""
                            customError = "past"
                            showCustomWindow = true
                        }
                    },
                ) {
                    Text(stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStopByPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FreeWindowChips(
    state: PendingQueueUiState,
    onMinutes: (Int) -> Unit,
    onCustom: () -> Unit,
) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            text = stringResource(R.string.free_window_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(15, 30, 45).forEach { minutes ->
                FilterChip(
                    selected = state.activeWindowMinutes == minutes,
                    onClick = { onMinutes(minutes) },
                    label = { Text(stringResource(R.string.free_window_minutes, minutes)) },
                )
            }
            state.untilWorkMinutes?.let { until ->
                FilterChip(
                    selected = state.activeWindowMinutes == until,
                    onClick = { onMinutes(until) },
                    label = { Text(stringResource(R.string.free_window_until_work)) },
                )
            }
            FilterChip(
                selected = false,
                onClick = onCustom,
                label = { Text(stringResource(R.string.free_window_custom)) },
            )
        }
    }
}

@Composable
private fun FreeWindowHeader(
    state: PendingQueueUiState,
    onClear: () -> Unit,
) {
    val window = state.activeWindowMinutes ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = if (state.fits.isEmpty()) {
                stringResource(R.string.free_window_header_empty, window)
            } else {
                val leftover = state.leftoverAfterBestMinutes ?: 0
                stringResource(R.string.free_window_header_fits, window, leftover)
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.free_window_show_all))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PendingRow(
    item: PendingItem,
    selected: Boolean,
    actionsEnabled: Boolean,
    onOpen: () -> Unit,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
) {
    val overdue = item.bucket == DueBucket.OVERDUE
    val mark = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .then(
                if (overdue) {
                    Modifier.drawBehind {
                        drawRect(
                            color = mark,
                            topLeft = Offset.Zero,
                            size = Size(4.dp.toPx(), size.height),
                        )
                    }
                } else {
                    Modifier
                },
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            TaskAreaLabel(area = item.task.area)
            Text(
                text = item.task.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Button(onClick = onDone, enabled = actionsEnabled) {
                    Text(stringResource(R.string.action_done))
                }
                TextButton(onClick = onSnooze, enabled = actionsEnabled) {
                    Text(stringResource(R.string.action_snooze))
                }
                TextButton(onClick = onSkip, enabled = actionsEnabled) {
                    Text(stringResource(R.string.action_skip))
                }
            }
        }
    }
}

@Composable
private fun PendingEmptyState(
    modifier: Modifier = Modifier,
    onAddTask: () -> Unit,
    pinnedHint: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.pending_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.pending_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (pinnedHint) {
            Text(
                text = stringResource(R.string.starters_pinned_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        TextButton(onClick = onAddTask) {
            Text(stringResource(R.string.add_task))
        }
    }
}

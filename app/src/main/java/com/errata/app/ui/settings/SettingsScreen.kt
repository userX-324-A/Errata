package com.errata.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errata.app.R
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.settings.AppearanceMode
import com.errata.app.reminders.ExactAlarmAccess
import com.errata.app.ui.theme.ErrataTopInsets
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class TimeTarget {
    REMINDER,
    WORK_START,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenBackup: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var timeTarget by remember { mutableStateOf<TimeTarget?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var canExact by remember { mutableStateOf(ExactAlarmAccess.canExact(context)) }
    val exactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        canExact = ExactAlarmAccess.canExact(context)
        viewModel.rescheduleReminders()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canExact = ExactAlarmAccess.canExact(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                windowInsets = ErrataTopInsets,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ErrataTopInsets,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_section_appearance),
                style = MaterialTheme.typography.titleMedium,
            )
            Label(stringResource(R.string.settings_appearance))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppearanceChip(
                    label = stringResource(R.string.appearance_system),
                    selected = state.appearanceMode == AppearanceMode.SYSTEM,
                    onClick = { viewModel.setAppearanceMode(AppearanceMode.SYSTEM) },
                )
                AppearanceChip(
                    label = stringResource(R.string.appearance_light),
                    selected = state.appearanceMode == AppearanceMode.LIGHT,
                    onClick = { viewModel.setAppearanceMode(AppearanceMode.LIGHT) },
                )
                AppearanceChip(
                    label = stringResource(R.string.appearance_dark),
                    selected = state.appearanceMode == AppearanceMode.DARK,
                    onClick = { viewModel.setAppearanceMode(AppearanceMode.DARK) },
                )
            }

            Text(
                text = stringResource(R.string.settings_section_defaults),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            Label(stringResource(R.string.settings_default_reminder))
            Text(
                text = formatMinutes(state.defaultReminderMinutesOfDay),
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(
                onClick = { timeTarget = TimeTarget.REMINDER },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(stringResource(R.string.pick_time))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Label(stringResource(R.string.settings_digest))
                    Text(
                        text = stringResource(R.string.settings_digest_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.digestEnabled,
                    onCheckedChange = viewModel::setDigestEnabled,
                )
            }

            if (ExactAlarmAccess.isRelevantSdk()) {
                Label(stringResource(R.string.settings_exact_alarms))
                Text(
                    text = stringResource(
                        if (canExact) R.string.settings_exact_on else R.string.settings_exact_off,
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                )
                TextButton(
                    onClick = { exactLauncher.launch(ExactAlarmAccess.requestIntent(context)) },
                    contentPadding = PaddingValues(0.dp),
                ) {
                    Text(stringResource(R.string.settings_exact_open))
                }
            }

            Label(stringResource(R.string.settings_default_cadence))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CadenceChip(
                    label = stringResource(R.string.cadence_from_completion),
                    selected = state.defaultCadenceMode == CadenceMode.FROM_COMPLETION,
                    onClick = { viewModel.setDefaultCadenceMode(CadenceMode.FROM_COMPLETION) },
                )
                CadenceChip(
                    label = stringResource(R.string.cadence_fixed),
                    selected = state.defaultCadenceMode == CadenceMode.FIXED_ANCHOR,
                    onClick = { viewModel.setDefaultCadenceMode(CadenceMode.FIXED_ANCHOR) },
                )
                CadenceChip(
                    label = stringResource(R.string.cadence_catch_up),
                    selected = state.defaultCadenceMode == CadenceMode.FROM_COMPLETION_CATCH_UP,
                    onClick = {
                        viewModel.setDefaultCadenceMode(CadenceMode.FROM_COMPLETION_CATCH_UP)
                    },
                )
            }
            Text(
                text = stringResource(R.string.settings_cadence_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Label(stringResource(R.string.settings_work_start))
            Text(
                text = state.defaultWorkStartMinutesOfDay?.let { formatMinutes(it) }
                    ?: stringResource(R.string.settings_work_start_unset),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.settings_work_start_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { timeTarget = TimeTarget.WORK_START }) {
                    Text(stringResource(R.string.settings_set_time))
                }
                if (state.defaultWorkStartMinutesOfDay != null) {
                    TextButton(onClick = { viewModel.setWorkStartMinutes(null) }) {
                        Text(stringResource(R.string.settings_clear))
                    }
                }
            }

            Text(
                text = stringResource(R.string.settings_section_data),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenBackup)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.backup_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_backup_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPrivacy)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.privacy_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.privacy_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.settings_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    timeTarget?.let { target ->
        val initial = when (target) {
            TimeTarget.REMINDER -> state.defaultReminderMinutesOfDay
            TimeTarget.WORK_START ->
                state.defaultWorkStartMinutesOfDay ?: (9 * 60)
        }
        val timeState = rememberTimePickerState(
            initialHour = initial / 60,
            initialMinute = initial % 60,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { timeTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minutes = timeState.hour * 60 + timeState.minute
                        when (target) {
                            TimeTarget.REMINDER -> viewModel.setDefaultReminderMinutes(minutes)
                            TimeTarget.WORK_START -> viewModel.setWorkStartMinutes(minutes)
                        }
                        timeTarget = null
                    },
                ) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { timeTarget = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
            title = {
                Text(
                    stringResource(
                        when (target) {
                            TimeTarget.REMINDER -> R.string.pick_reminder_time
                            TimeTarget.WORK_START -> R.string.settings_pick_work_start
                        },
                    ),
                )
            },
            text = { TimePicker(state = timeState) },
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CadenceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun AppearanceChip(label: String, selected: Boolean, onClick: () -> Unit) {
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

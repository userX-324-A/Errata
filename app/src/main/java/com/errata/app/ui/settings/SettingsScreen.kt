package com.errata.app.ui.settings

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.material3.Checkbox
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
import com.errata.app.ui.common.isDevice24Hour
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.history.HistoryRetention
import com.errata.app.domain.settings.AppearanceMode
import com.errata.app.reminders.ExactAlarmAccess
import com.errata.app.reminders.NotificationAccess
import com.errata.app.ui.theme.ErrataTopInsets
import com.errata.app.ui.theme.errataContentWidth
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
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
    var confirmPurgeHistory by remember { mutableStateOf(false) }
    var confirmResetTasks by remember { mutableStateOf(false) }
    var alsoClearCloud by remember { mutableStateOf(true) }
    var confirmUnlink by remember { mutableStateOf(false) }
    var confirmWipeCloud by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.completeGoogleConsent(activity, result.data)
        }
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var canExact by remember { mutableStateOf(ExactAlarmAccess.canExact(context)) }
    val exactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        canExact = ExactAlarmAccess.canExact(context)
        viewModel.rescheduleReminders()
    }
    var canNotify by remember { mutableStateOf(NotificationAccess.areEnabled(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        canNotify = NotificationAccess.areEnabled(context)
        viewModel.rescheduleReminders()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canExact = ExactAlarmAccess.canExact(context)
                canNotify = NotificationAccess.areEnabled(context)
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
                .errataContentWidth()
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
            Text(
                text = stringResource(R.string.settings_default_reminder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            Label(stringResource(R.string.settings_notifications))
            Text(
                text = stringResource(
                    if (canNotify) {
                        R.string.settings_notifications_on
                    } else {
                        R.string.settings_notifications_off
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(
                onClick = { notificationLauncher.launch(NotificationAccess.settingsIntent(context)) },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(stringResource(R.string.settings_notifications_open))
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
            Label(stringResource(R.string.settings_google))
            Text(
                text = stringResource(R.string.settings_google_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GoogleSyncBlock(
                state = state,
                onLink = {
                    viewModel.linkGoogle(activity) { sender ->
                        consentLauncher.launch(IntentSenderRequest.Builder(sender).build())
                    }
                },
                onSyncNow = viewModel::syncNow,
                onUnlink = { confirmUnlink = true },
                onWipe = { confirmWipeCloud = true },
            )
            Label(stringResource(R.string.settings_history_retention))
            Text(
                text = stringResource(R.string.settings_history_retention_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppearanceChip(
                    label = stringResource(R.string.settings_retention_90),
                    selected = state.historyRetentionDays == HistoryRetention.DAYS_90,
                    onClick = { viewModel.setHistoryRetentionDays(HistoryRetention.DAYS_90) },
                )
                AppearanceChip(
                    label = stringResource(R.string.settings_retention_1y),
                    selected = state.historyRetentionDays == HistoryRetention.DAYS_YEAR,
                    onClick = { viewModel.setHistoryRetentionDays(HistoryRetention.DAYS_YEAR) },
                )
                AppearanceChip(
                    label = stringResource(R.string.settings_retention_2y),
                    selected = state.historyRetentionDays == HistoryRetention.DAYS_2Y,
                    onClick = { viewModel.setHistoryRetentionDays(HistoryRetention.DAYS_2Y) },
                )
                AppearanceChip(
                    label = stringResource(R.string.settings_retention_all),
                    selected = state.historyRetentionDays == HistoryRetention.KEEP_ALL,
                    onClick = { viewModel.setHistoryRetentionDays(HistoryRetention.KEEP_ALL) },
                )
            }
            TextButton(
                onClick = { confirmPurgeHistory = true },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(stringResource(R.string.settings_purge_history))
            }
            TextButton(
                onClick = { confirmResetTasks = true; alsoClearCloud = true },
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(stringResource(R.string.settings_reset_tasks))
            }

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
            is24Hour = isDevice24Hour(),
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

    if (confirmPurgeHistory) {
        AlertDialog(
            onDismissRequest = { confirmPurgeHistory = false },
            title = { Text(stringResource(R.string.settings_purge_history_title)) },
            text = { Text(stringResource(R.string.settings_purge_history_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.purgeHistory()
                        confirmPurgeHistory = false
                    },
                ) { Text(stringResource(R.string.settings_purge_history_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmPurgeHistory = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (confirmResetTasks) {
        AlertDialog(
            onDismissRequest = { confirmResetTasks = false },
            title = { Text(stringResource(R.string.settings_reset_tasks_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_reset_tasks_body))
                    if (state.googleLinked) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = alsoClearCloud,
                                onCheckedChange = { alsoClearCloud = it },
                            )
                            Text(stringResource(R.string.settings_reset_also_cloud))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetTasks(alsoClearCloud = state.googleLinked && alsoClearCloud)
                        confirmResetTasks = false
                    },
                ) { Text(stringResource(R.string.settings_reset_tasks_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmResetTasks = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (confirmUnlink) {
        AlertDialog(
            onDismissRequest = { confirmUnlink = false },
            title = { Text(stringResource(R.string.settings_google_unlink_title)) },
            text = { Text(stringResource(R.string.settings_google_unlink_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unlink(context, wipeCloud = false)
                        confirmUnlink = false
                    },
                ) { Text(stringResource(R.string.settings_google_unlink_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnlink = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (confirmWipeCloud) {
        AlertDialog(
            onDismissRequest = { confirmWipeCloud = false },
            title = { Text(stringResource(R.string.settings_google_wipe_title)) },
            text = { Text(stringResource(R.string.settings_google_wipe_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.unlink(context, wipeCloud = true)
                        confirmWipeCloud = false
                    },
                ) { Text(stringResource(R.string.settings_google_wipe_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipeCloud = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun GoogleSyncBlock(
    state: SettingsUiState,
    onLink: () -> Unit,
    onSyncNow: () -> Unit,
    onUnlink: () -> Unit,
    onWipe: () -> Unit,
) {
    when {
        !state.googleConfigured -> {
            Text(
                text = stringResource(R.string.settings_google_not_configured),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        !state.playServices -> {
            Text(
                text = stringResource(R.string.settings_google_no_play),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        !state.googleLinked -> {
            TextButton(
                onClick = onLink,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(stringResource(R.string.settings_google_link))
            }
            if (!state.lastSyncError.isNullOrBlank()) {
                Text(
                    text = syncStatusText(state),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        else -> {
            Text(
                text = state.googleEmail.orEmpty(),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = syncStatusText(state),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onSyncNow,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(stringResource(R.string.settings_google_sync_now))
            }
            TextButton(
                onClick = onUnlink,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(stringResource(R.string.settings_google_unlink))
            }
            TextButton(
                onClick = onWipe,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(stringResource(R.string.settings_google_wipe))
            }
        }
    }
}

@Composable
private fun syncStatusText(state: SettingsUiState): String {
    val error = state.lastSyncError
    if (!error.isNullOrBlank()) {
        return stringResource(
            when (error) {
                "auth", "sign_in" -> R.string.settings_google_error_sign_in
                "conflict" -> R.string.settings_google_error_conflict
                "corrupt" -> R.string.settings_google_error_corrupt
                "not_configured" -> R.string.settings_google_not_configured
                "play_services" -> R.string.settings_google_no_play
                else -> R.string.settings_google_error_network
            },
        )
    }
    if (state.lastSyncEpochMs <= 0L) {
        return stringResource(R.string.settings_google_sync_pending)
    }
    val formatted = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(state.lastSyncEpochMs))
    return stringResource(R.string.settings_google_last_sync, formatted)
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

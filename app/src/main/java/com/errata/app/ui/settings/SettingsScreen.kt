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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.errata.app.ui.common.CadenceModeRow
import com.errata.app.ui.common.formatDeviceClock
import com.errata.app.ui.common.formatDeviceDateTime
import com.errata.app.ui.common.isDevice24Hour
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.history.HistoryRetention
import com.errata.app.domain.reminders.DefaultReminderKind
import com.errata.app.domain.settings.AppearanceMode
import com.errata.app.reminders.ExactAlarmAccess
import com.errata.app.reminders.NotificationAccess
import com.errata.app.ui.theme.ErrataTopInsets
import com.errata.app.ui.theme.errataContentWidth
import com.errata.app.ui.theme.errataTopAppBarColors

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
        } else {
            viewModel.cancelGoogleConsent()
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
                val nextExact = ExactAlarmAccess.canExact(context)
                val nextNotify = NotificationAccess.areEnabled(context)
                canExact = nextExact
                canNotify = nextNotify
                viewModel.onResumePermissionFlags(nextNotify, nextExact)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            key(state.appearanceMode) {
                TopAppBar(
                    title = { Text(stringResource(R.string.settings_title)) },
                    windowInsets = ErrataTopInsets,
                    colors = errataTopAppBarColors(),
                )
            }
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

            Label(stringResource(R.string.settings_default_reminder_kind))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppearanceChip(
                    label = stringResource(R.string.reminder_none),
                    selected = state.defaultReminderKind == DefaultReminderKind.NONE,
                    onClick = { viewModel.setDefaultReminderKind(DefaultReminderKind.NONE) },
                )
                AppearanceChip(
                    label = stringResource(R.string.reminder_when_due),
                    selected = state.defaultReminderKind == DefaultReminderKind.WHEN_DUE,
                    onClick = { viewModel.setDefaultReminderKind(DefaultReminderKind.WHEN_DUE) },
                )
                AppearanceChip(
                    label = stringResource(
                        R.string.reminder_at_clock,
                        formatDeviceClock(state.defaultReminderMinutesOfDay),
                    ),
                    selected = state.defaultReminderKind == DefaultReminderKind.CLOCK,
                    onClick = { viewModel.setDefaultReminderKind(DefaultReminderKind.CLOCK) },
                )
            }
            Text(
                text = stringResource(R.string.settings_default_reminder_kind_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingsNavRow(
                title = stringResource(R.string.settings_default_reminder),
                subtitle = stringResource(R.string.settings_default_reminder_hint),
                value = formatDeviceClock(state.defaultReminderMinutesOfDay),
                onClickLabel = stringResource(R.string.pick_time),
                onClick = { timeTarget = TimeTarget.REMINDER },
            )

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
                    Text(
                        text = stringResource(R.string.settings_digest),
                        style = MaterialTheme.typography.bodyLarge,
                    )
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

            SettingsNavRow(
                title = stringResource(R.string.settings_notifications),
                subtitle = stringResource(
                    if (canNotify) {
                        R.string.settings_notifications_on
                    } else {
                        R.string.settings_notifications_off
                    },
                ),
                onClickLabel = stringResource(R.string.settings_notifications_open),
                onClick = { notificationLauncher.launch(NotificationAccess.settingsIntent(context)) },
            )

            if (ExactAlarmAccess.isRelevantSdk()) {
                SettingsNavRow(
                    title = stringResource(R.string.settings_exact_alarms),
                    subtitle = stringResource(
                        if (canExact) R.string.settings_exact_on else R.string.settings_exact_off,
                    ),
                    onClickLabel = stringResource(R.string.settings_exact_open),
                    onClick = { exactLauncher.launch(ExactAlarmAccess.requestIntent(context)) },
                )
            }

            Label(stringResource(R.string.settings_default_cadence))
            CadenceModeRow(
                selected = state.defaultCadenceMode,
                onSelect = viewModel::setDefaultCadenceMode,
            )
            Text(
                text = stringResource(
                    when (state.defaultCadenceMode) {
                        CadenceMode.FROM_COMPLETION -> R.string.settings_cadence_hint_last
                        CadenceMode.FIXED_ANCHOR -> R.string.settings_cadence_hint_fixed
                        CadenceMode.FROM_COMPLETION_CATCH_UP ->
                            R.string.settings_cadence_hint_catch_up
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingsNavRow(
                title = stringResource(R.string.settings_work_start),
                subtitle = stringResource(R.string.settings_work_start_hint),
                value = state.defaultWorkStartMinutesOfDay?.let { formatDeviceClock(it) }
                    ?: stringResource(R.string.settings_work_start_unset),
                onClickLabel = stringResource(R.string.settings_set_time),
                onClick = { timeTarget = TimeTarget.WORK_START },
            )
            if (state.defaultWorkStartMinutesOfDay != null) {
                TextButton(
                    onClick = { viewModel.setWorkStartMinutes(null) },
                ) {
                    Text(stringResource(R.string.settings_clear))
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
            SettingsNavRow(
                title = stringResource(R.string.backup_title),
                subtitle = stringResource(R.string.settings_backup_hint),
                onClick = onOpenBackup,
            )
            SettingsNavRow(
                title = stringResource(R.string.privacy_title),
                subtitle = stringResource(R.string.privacy_hint),
                onClick = onOpenPrivacy,
            )

            Text(
                text = stringResource(R.string.settings_section_remove),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            TextButton(
                onClick = { confirmPurgeHistory = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.settings_purge_history))
            }
            TextButton(
                onClick = { confirmResetTasks = true; alsoClearCloud = true },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.settings_reset_tasks))
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
                DestructiveTextButton(
                    text = stringResource(R.string.settings_purge_history_confirm),
                    onClick = {
                        viewModel.purgeHistory()
                        confirmPurgeHistory = false
                    },
                )
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
                DestructiveTextButton(
                    text = stringResource(R.string.settings_reset_tasks_confirm),
                    onClick = {
                        viewModel.resetTasks(alsoClearCloud = state.googleLinked && alsoClearCloud)
                        confirmResetTasks = false
                    },
                )
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
                DestructiveTextButton(
                    text = stringResource(R.string.settings_google_wipe_confirm),
                    onClick = {
                        viewModel.unlink(context, wipeCloud = true)
                        confirmWipeCloud = false
                    },
                )
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
            ) {
                Text(stringResource(R.string.settings_google_sync_now))
            }
            TextButton(
                onClick = onUnlink,
            ) {
                Text(stringResource(R.string.settings_google_unlink))
            }
            TextButton(
                onClick = onWipe,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
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
                "auth" -> R.string.settings_google_error_auth
                "sign_in" -> R.string.settings_google_error_sign_in
                "wipe" -> R.string.settings_google_error_wipe
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
    val formatted = formatDeviceDateTime(state.lastSyncEpochMs)
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
private fun SettingsNavRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClickLabel = onClickLabel, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DestructiveTextButton(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
    ) {
        Text(text)
    }
}

@Composable
private fun AppearanceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

package com.errata.app.ui.starter

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.errata.app.R
import com.errata.app.domain.starter.StarterCatalog
import com.errata.app.domain.starter.StarterSpec
import com.errata.app.reminders.AfterPinPrompt
import com.errata.app.reminders.ExactAlarmAccess
import com.errata.app.reminders.NotificationAccess
import com.errata.app.reminders.afterPinPrompt
import com.errata.app.ui.common.NotifyPromptDialog
import com.errata.app.ui.theme.ErrataScreenInsets
import com.errata.app.ui.theme.ErrataTopInsets
import com.errata.app.ui.theme.errataContentWidth
import com.errata.app.ui.theme.errataTopAppBarColors

@Composable
fun StarterPackEmpty(
    title: String,
    body: String,
    onBlankTask: () -> Unit,
    onPickStarter: (String) -> Unit,
    onPin: (List<StarterSpec>) -> Unit,
    onRescheduleReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StarterPinPrompts(
        onRescheduleReminders = onRescheduleReminders,
    ) { onPinned ->
        StarterCheckboxPicker(
            modifier = modifier,
            onPickStarter = onPickStarter,
            onPin = { specs ->
                onPin(specs)
                onPinned()
            },
            header = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                CatalogBlankRow(onBlankTask = onBlankTask)
                Text(
                    text = stringResource(R.string.starters_heading),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            },
        )
    }
}

@Composable
fun StarterPackPaused(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.starters_while_editing),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StarterCatalogScreen(
    onBack: () -> Unit,
    onBlankTask: () -> Unit,
    onPickStarter: (String) -> Unit,
    onPin: (List<StarterSpec>) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.catalog_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                colors = errataTopAppBarColors(),
                windowInsets = ErrataTopInsets,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = ErrataScreenInsets,
    ) { innerPadding ->
        StarterCheckboxPicker(
            modifier = Modifier
                .padding(innerPadding)
                .errataContentWidth(),
            onPickStarter = onPickStarter,
            onPin = onPin,
            header = {
                CatalogBlankRow(onBlankTask = onBlankTask)
            },
        )
    }
}

@Composable
private fun StarterPinPrompts(
    onRescheduleReminders: () -> Unit,
    content: @Composable (onPinned: () -> Unit) -> Unit,
) {
    var showExactPrompt by remember { mutableStateOf(false) }
    var showNotifyPrompt by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        onRescheduleReminders()
    }
    val notifyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) onRescheduleReminders()
        if (ExactAlarmAccess.shouldPrompt(context)) {
            showExactPrompt = true
        }
    }
    content {
        when (
            afterPinPrompt(
                NotificationAccess.shouldPrompt(context),
                ExactAlarmAccess.shouldPrompt(context),
            )
        ) {
            AfterPinPrompt.Notifications -> showNotifyPrompt = true
            AfterPinPrompt.Exact -> showExactPrompt = true
            AfterPinPrompt.None -> Unit
        }
    }
    if (showNotifyPrompt) {
        NotifyPromptDialog(
            onAllow = {
                NotificationAccess.markPrompted(context)
                showNotifyPrompt = false
                notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
            onNotNow = {
                NotificationAccess.markPrompted(context)
                showNotifyPrompt = false
                if (ExactAlarmAccess.shouldPrompt(context)) {
                    showExactPrompt = true
                }
            },
        )
    }
    if (showExactPrompt) {
        AlertDialog(
            onDismissRequest = {
                ExactAlarmAccess.markPrompted(context)
                showExactPrompt = false
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
                        showExactPrompt = false
                    },
                ) { Text(stringResource(R.string.exact_prompt_not_now)) }
            },
        )
    }
}

@Composable
private fun StarterCheckboxPicker(
    onPickStarter: (String) -> Unit,
    onPin: (List<StarterSpec>) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val hasSelection = selectedIds.isNotEmpty()
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            header()
            StarterGroupedList(
                onRow = { spec ->
                    StarterCheckboxRow(
                        spec = spec,
                        selected = spec.id in selectedIds,
                        onToggle = {
                            selectedIds = if (spec.id in selectedIds) {
                                selectedIds - spec.id
                            } else {
                                selectedIds + spec.id
                            }
                        },
                        onOpen = { onPickStarter(spec.id) },
                    )
                },
            )
        }
        if (hasSelection) {
            HorizontalDivider()
            Button(
                onClick = {
                    val specs = StarterCatalog.specsByIds(selectedIds)
                    if (specs.isEmpty()) return@Button
                    onPin(specs)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(stringResource(R.string.starters_pin))
            }
        }
    }
}

@Composable
private fun CatalogBlankRow(onBlankTask: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBlankTask)
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.catalog_blank),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.catalog_blank_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StarterGroupedList(
    onRow: @Composable (StarterSpec) -> Unit,
) {
    StarterCatalog.groupedByArea().forEach { (label, rows) ->
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
        )
        rows.forEach { spec -> onRow(spec) }
    }
}

@Composable
private fun StarterCheckboxRow(
    spec: StarterSpec,
    selected: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
        )
        StarterRowCopy(
            spec = spec,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpen) {
            Text(stringResource(R.string.catalog_edit_minutes))
        }
    }
}

@Composable
private fun StarterRowCopy(
    spec: StarterSpec,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = spec.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "${StarterCatalog.cadenceSummary(spec)} · ~${spec.estimateMinutes} min",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

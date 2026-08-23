package com.errata.app.ui.starter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.errata.app.reminders.ExactAlarmAccess

@Composable
fun StarterPackEmpty(
    title: String,
    body: String,
    onAddTask: () -> Unit,
    onPin: (List<StarterSpec>) -> Unit,
    onRescheduleReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    var showExactPrompt by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val exactLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        onRescheduleReminders()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
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
        Text(
            text = stringResource(R.string.starters_heading),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        StarterCatalog.ALL.forEach { spec ->
            StarterRow(
                spec = spec,
                selected = spec.id in selectedIds,
                onToggle = {
                    selectedIds = if (spec.id in selectedIds) {
                        selectedIds - spec.id
                    } else {
                        selectedIds + spec.id
                    }
                },
            )
        }
        Button(
            onClick = {
                val specs = StarterCatalog.specsByIds(selectedIds)
                if (specs.isEmpty()) return@Button
                onPin(specs)
                if (ExactAlarmAccess.shouldPrompt(context)) {
                    showExactPrompt = true
                }
            },
            enabled = selectedIds.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.starters_pin))
        }
        TextButton(onClick = onAddTask) {
            Text(stringResource(R.string.add_task))
        }
        Spacer(modifier = Modifier.height(24.dp))
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
private fun StarterRow(
    spec: StarterSpec,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = { onToggle() },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onToggle),
        ) {
            Text(
                text = spec.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val area = spec.area.orEmpty()
            val detail = buildString {
                if (area.isNotEmpty()) {
                    append(area)
                    append(" · ")
                }
                append(StarterCatalog.cadenceSummary(spec))
                append(" · ~")
                append(spec.estimateMinutes)
                append(" min")
            }
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

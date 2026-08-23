package com.errata.app.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errata.app.ErrataApp
import com.errata.app.R
import com.errata.app.ui.theme.ErrataScreenInsets
import com.errata.app.ui.theme.ErrataTopInsets
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    viewModel: BackupViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshFolder(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.writeExport(uri, context.contentResolver)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
                }
            }
            if (text != null) {
                viewModel.prepareImport(text)
            }
        }
    }

    val folderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setFolder(uri, context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.backup_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { exportLauncher.launch(viewModel.suggestedExportFileName()) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_export))
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_import))
            }

            Text(
                text = stringResource(R.string.backup_folder_heading),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = stringResource(R.string.backup_folder_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.hasFolder) {
                Text(
                    text = state.folderLabel
                        ?: stringResource(R.string.backup_folder_chosen),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            OutlinedButton(
                onClick = { folderLauncher.launch(null) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.backup_choose_folder))
            }
            if (state.hasFolder) {
                Button(
                    onClick = { viewModel.writeToFolder(context) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.backup_write_folder))
                }
                OutlinedButton(
                    onClick = { viewModel.readFromFolder(context) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.backup_read_folder))
                }
                TextButton(
                    onClick = { viewModel.clearFolder(context) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.backup_clear_folder))
                }
            }

            StatusFor(state)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (state.pendingImportJson != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        if (ErrataApp.instance.syncPreferences.isLinked()) {
                            R.string.backup_confirm_body_linked
                        } else {
                            R.string.backup_confirm_body
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) {
                    Text(stringResource(R.string.backup_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun StatusFor(state: BackupUiState) {
    val text = when {
        state.message == "exported" -> stringResource(R.string.backup_export_ok)
        state.message == "imported" -> stringResource(R.string.backup_import_ok)
        state.message == "folder_written" -> stringResource(R.string.backup_folder_written)
        state.message == "folder_cleared" -> stringResource(R.string.backup_folder_cleared)
        state.message == "no_folder" -> stringResource(R.string.backup_no_folder)
        state.message == "folder_unavailable" -> stringResource(R.string.backup_folder_unavailable)
        state.message == "file_missing" -> stringResource(R.string.backup_folder_missing_file)
        state.isError && state.message != null -> state.message
        else -> null
    } ?: return
    StatusText(text, error = state.isError)
}

@Composable
private fun StatusText(text: String, error: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (error) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}

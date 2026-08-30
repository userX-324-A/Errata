package com.errata.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errata.app.R
import com.errata.app.ui.common.AreaFilterChips
import com.errata.app.ui.common.TaskAreaLabel
import com.errata.app.ui.starter.StarterPackEmpty
import com.errata.app.ui.theme.ErrataTopInsets
import com.errata.app.ui.theme.errataContentWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllTasksScreen(
    viewModel: AllTasksViewModel,
    onOpenTask: (Long) -> Unit,
    onAddTask: () -> Unit,
    onPickStarter: (String) -> Unit,
    selectedTaskId: Long? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var archiveTargetId by remember { mutableStateOf<Long?>(null) }
    var pauseTargetId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.library_title)) },
                windowInsets = ErrataTopInsets,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (!state.isEmpty) {
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
        if (state.isEmpty) {
            StarterPackEmpty(
                title = stringResource(R.string.library_empty_title),
                body = stringResource(R.string.library_empty_body),
                onAddTask = onAddTask,
                onPickStarter = onPickStarter,
                onPin = viewModel::pinStarters,
                onRescheduleReminders = viewModel::rescheduleReminders,
                modifier = Modifier.padding(innerPadding).errataContentWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .errataContentWidth(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.availableAreas.isNotEmpty()) {
                    item {
                        AreaFilterChips(
                            usedAreas = state.availableAreas,
                            activeArea = state.activeArea,
                            onSelect = viewModel::setActiveArea,
                        )
                    }
                }
                items(state.items, key = { it.task.id }) { item ->
                    LibraryRow(
                        item = item,
                        selected = item.task.id == selectedTaskId,
                        onOpen = { onOpenTask(item.task.id) },
                        onPause = { pauseTargetId = item.task.id },
                        onResume = { viewModel.resume(item.task.id) },
                        onArchive = { archiveTargetId = item.task.id },
                    )
                }
                if (state.items.isEmpty() && state.activeArea != null) {
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
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }

    pauseTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { pauseTargetId = null },
            title = { Text(stringResource(R.string.pause_confirm_title)) },
            text = { Text(stringResource(R.string.pause_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.pause(id)
                        pauseTargetId = null
                    },
                ) {
                    Text(stringResource(R.string.action_pause))
                }
            },
            dismissButton = {
                TextButton(onClick = { pauseTargetId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    archiveTargetId?.let { id ->
        AlertDialog(
            onDismissRequest = { archiveTargetId = null },
            title = { Text(stringResource(R.string.archive_confirm_title)) },
            text = { Text(stringResource(R.string.archive_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.archive(id)
                        archiveTargetId = null
                    },
                ) {
                    Text(stringResource(R.string.action_archive))
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveTargetId = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun LibraryRow(
    item: LibraryItem,
    selected: Boolean,
    onOpen: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onArchive: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
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
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
            ) {
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
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.menu_more),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    if (item.task.isPaused) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_resume)) },
                            onClick = {
                                menuExpanded = false
                                onResume()
                            },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_pause)) },
                            onClick = {
                                menuExpanded = false
                                onPause()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_archive)) },
                        onClick = {
                            menuExpanded = false
                            onArchive()
                        },
                    )
                }
            }
        }
    }
}

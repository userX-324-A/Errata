package com.errata.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.errata.app.ErrataApp
import com.errata.app.R
import com.errata.app.ui.adaptive.ErrataListDetail
import com.errata.app.ui.adaptive.PaneDest
import com.errata.app.ui.backup.BackupScreen
import com.errata.app.ui.backup.BackupViewModel
import com.errata.app.ui.library.AllTasksScreen
import com.errata.app.ui.library.AllTasksViewModel
import com.errata.app.ui.pending.PendingQueueScreen
import com.errata.app.ui.pending.PendingQueueViewModel
import com.errata.app.ui.privacy.PrivacyScreen
import com.errata.app.ui.settings.SettingsScreen
import com.errata.app.ui.settings.SettingsViewModel
import com.errata.app.ui.starter.StarterCatalogScreen
import com.errata.app.ui.task.TaskEditorScreen
import com.errata.app.ui.task.TaskEditorViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

object Routes {
    const val PENDING = "pending"
    const val LIBRARY = "tasks"
    const val SETTINGS = "settings"
}

private data class TabDest(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val Tabs = listOf(
    TabDest(Routes.PENDING, R.string.nav_pending, Icons.Outlined.CheckCircle),
    TabDest(Routes.LIBRARY, R.string.nav_library, Icons.AutoMirrored.Outlined.List),
    TabDest(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings),
)

@Composable
fun ErrataNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val commands = ErrataApp.instance.taskCommands
    val hasPinnedTasks by remember {
        commands.observeActiveTasks.map { it.isNotEmpty() }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val compactDetailByTab = remember { mutableStateMapOf<String, Boolean>() }
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val defaultSuite = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)
    val coverTab = currentRoute?.takeIf { it in compactDetailByTab.keys }
    val layoutType = if (coverTab != null && compactDetailByTab[coverTab] == true) {
        NavigationSuiteType.None
    } else {
        defaultSuite
    }
    val visibleTabs = if (hasPinnedTasks) Tabs else Tabs.filter { it.route != Routes.LIBRARY }

    LaunchedEffect(hasPinnedTasks, currentRoute) {
        if (!hasPinnedTasks && currentRoute == Routes.LIBRARY) {
            navController.navigate(Routes.PENDING) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = layoutType,
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.background,
            navigationRailContainerColor = MaterialTheme.colorScheme.background,
        ),
        navigationSuiteItems = {
            val dest = backStack?.destination
            visibleTabs.forEach { tab ->
                val selected = dest?.hierarchy?.any { it.route == tab.route } == true
                item(
                    selected = selected,
                    onClick = {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = stringResource(tab.labelRes),
                        )
                    },
                    label = { Text(stringResource(tab.labelRes)) },
                )
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.PENDING,
        ) {
            composable(Routes.PENDING) {
                val vm: PendingQueueViewModel = viewModel(
                    factory = PendingQueueViewModel.factory(commands),
                )
                val pendingState by vm.uiState.collectAsStateWithLifecycle()
                val pendingItems = pendingState.overdue + pendingState.dueToday + pendingState.soon
                val emptyMessage = when {
                    pendingState.hasNoPinnedTasks -> stringResource(R.string.pane_empty_starters)
                    pendingState.isEmpty -> stringResource(R.string.pane_empty_caught_up)
                    else -> stringResource(
                        R.string.pane_empty_pending,
                        pendingItems.size,
                        pendingItems.sumOf { it.task.estimateMinutes },
                    )
                }
                ErrataListDetail(
                    emptyMessage = emptyMessage,
                    onCompactDetailChanged = { covered ->
                        compactDetailByTab[Routes.PENDING] = covered
                    },
                    list = { actions, selectedKey ->
                        PendingQueueScreen(
                            viewModel = vm,
                            onAddTask = { actions.open(PaneDest.CATALOG) },
                            onBlankTask = { actions.open(PaneDest.task(0L)) },
                            onPickStarter = { id -> actions.open(PaneDest.task(0L, id)) },
                            onOpenTask = { id -> actions.open(PaneDest.task(id)) },
                            selectedTaskId = PaneDest.taskId(selectedKey),
                            detailOpen = selectedKey != null,
                        )
                    },
                    detail = { key, actions ->
                        TaskPaneDetail(key = key, actions = actions, commands = commands)
                    },
                )
            }
            composable(Routes.LIBRARY) {
                val vm: AllTasksViewModel = viewModel(
                    factory = AllTasksViewModel.factory(commands),
                )
                ErrataListDetail(
                    emptyMessage = stringResource(R.string.pane_empty_task),
                    onCompactDetailChanged = { covered ->
                        compactDetailByTab[Routes.LIBRARY] = covered
                    },
                    list = { actions, selectedKey ->
                        AllTasksScreen(
                            viewModel = vm,
                            onOpenTask = { id -> actions.open(PaneDest.task(id)) },
                            onAddTask = { actions.open(PaneDest.CATALOG) },
                            onBlankTask = { actions.open(PaneDest.task(0L)) },
                            onPickStarter = { id -> actions.open(PaneDest.task(0L, id)) },
                            selectedTaskId = PaneDest.taskId(selectedKey),
                            detailOpen = selectedKey != null,
                        )
                    },
                    detail = { key, actions ->
                        TaskPaneDetail(key = key, actions = actions, commands = commands)
                    },
                )
            }
            composable(Routes.SETTINGS) {
                val app = ErrataApp.instance
                val vm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.factory(
                        commands,
                        app.syncPreferences,
                        app.syncScheduler,
                        app.syncCoordinator,
                        com.errata.app.sync.GoogleAuth.playServicesAvailable(app),
                        initialNotify = com.errata.app.reminders.NotificationAccess.areEnabled(app),
                        initialExact = com.errata.app.reminders.ExactAlarmAccess.canExact(app),
                    ),
                )
                ErrataListDetail(
                    emptyMessage = stringResource(R.string.pane_empty_settings),
                    onCompactDetailChanged = { covered ->
                        compactDetailByTab[Routes.SETTINGS] = covered
                    },
                    list = { actions, _ ->
                        SettingsScreen(
                            viewModel = vm,
                            onOpenBackup = { actions.open(PaneDest.BACKUP) },
                            onOpenPrivacy = { actions.open(PaneDest.PRIVACY) },
                        )
                    },
                    detail = { key, actions ->
                        when (key) {
                            PaneDest.BACKUP -> {
                                val backupVm: BackupViewModel = viewModel(
                                    factory = BackupViewModel.factory(commands),
                                )
                                BackupScreen(
                                    viewModel = backupVm,
                                    onBack = actions.back,
                                )
                            }
                            PaneDest.PRIVACY -> PrivacyScreen(onBack = actions.back)
                            else -> Unit
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TaskPaneDetail(
    key: String,
    actions: com.errata.app.ui.adaptive.PaneActions,
    commands: com.errata.app.data.TaskCommands,
) {
    when {
        key == PaneDest.CATALOG -> {
            val scope = rememberCoroutineScope()
            StarterCatalogScreen(
                onBack = actions.back,
                onBlankTask = { actions.open(PaneDest.task(0L)) },
                onPickStarter = { id -> actions.open(PaneDest.task(0L, id)) },
                onPin = { specs ->
                    scope.launch {
                        commands.pinStarters(specs)
                        actions.popToList()
                    }
                },
            )
        }
        PaneDest.isTask(key) -> {
            val (taskId, starterId) = PaneDest.parseTask(key) ?: return
            val vm: TaskEditorViewModel = viewModel(
                key = "task-$taskId-$starterId",
                factory = TaskEditorViewModel.factory(commands, taskId, starterId),
            )
            TaskEditorScreen(
                viewModel = vm,
                onBack = actions.back,
                onSaved = actions.popToList,
            )
        }
    }
}

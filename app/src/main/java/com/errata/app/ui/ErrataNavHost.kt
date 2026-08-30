package com.errata.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.errata.app.ErrataApp
import com.errata.app.R
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
import com.errata.app.ui.theme.ErrataBottomInsets

object Routes {
    const val PENDING = "pending"
    const val STARTERS = "starters"
    const val TASK = "task/{taskId}?starter={starterId}"
    const val BACKUP = "backup"
    const val PRIVACY = "privacy"
    const val LIBRARY = "tasks"
    const val SETTINGS = "settings"

    fun task(taskId: Long, starterId: String? = null): String {
        val base = "task/$taskId"
        return if (starterId.isNullOrBlank()) base else "$base?starter=$starterId"
    }
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
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in setOf(Routes.PENDING, Routes.LIBRARY, Routes.SETTINGS)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    windowInsets = ErrataBottomInsets,
                ) {
                    val dest = backStack?.destination
                    Tabs.forEach { tab ->
                        val selected = dest?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
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
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.PENDING,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.PENDING) {
                val vm: PendingQueueViewModel = viewModel(
                    factory = PendingQueueViewModel.factory(commands),
                )
                PendingQueueScreen(
                    viewModel = vm,
                    onAddTask = { navController.navigate(Routes.STARTERS) },
                    onOpenTask = { id -> navController.navigate(Routes.task(id)) },
                )
            }
            composable(Routes.STARTERS) {
                StarterCatalogScreen(
                    onBack = { navController.popBackStack() },
                    onBlankTask = { navController.navigate(Routes.task(0L)) },
                    onPickStarter = { id -> navController.navigate(Routes.task(0L, id)) },
                )
            }
            composable(
                route = Routes.TASK,
                arguments = listOf(
                    navArgument("taskId") { type = NavType.LongType },
                    navArgument("starterId") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId") ?: 0L
                val starterId = entry.arguments?.getString("starterId").orEmpty()
                val vm: TaskEditorViewModel = viewModel(
                    key = "task-$taskId-$starterId",
                    factory = TaskEditorViewModel.factory(commands, taskId, starterId),
                )
                TaskEditorScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onSaved = { EditorSaveNav.popAfterSave(navController) },
                )
            }
            composable(Routes.LIBRARY) {
                val vm: AllTasksViewModel = viewModel(
                    factory = AllTasksViewModel.factory(commands),
                )
                AllTasksScreen(
                    viewModel = vm,
                    onOpenTask = { id -> navController.navigate(Routes.task(id)) },
                    onAddTask = { navController.navigate(Routes.STARTERS) },
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
                    ),
                )
                SettingsScreen(
                    viewModel = vm,
                    onOpenBackup = { navController.navigate(Routes.BACKUP) },
                    onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                )
            }
            composable(Routes.BACKUP) {
                val vm: BackupViewModel = viewModel(
                    factory = BackupViewModel.factory(commands),
                )
                BackupScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.PRIVACY) {
                PrivacyScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

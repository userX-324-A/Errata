package com.errata.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.errata.app.ErrataApp
import com.errata.app.ui.backup.BackupScreen
import com.errata.app.ui.backup.BackupViewModel
import com.errata.app.ui.library.AllTasksScreen
import com.errata.app.ui.library.AllTasksViewModel
import com.errata.app.ui.pending.PendingQueueScreen
import com.errata.app.ui.pending.PendingQueueViewModel
import com.errata.app.ui.settings.SettingsScreen
import com.errata.app.ui.settings.SettingsViewModel
import com.errata.app.ui.task.TaskEditorScreen
import com.errata.app.ui.task.TaskEditorViewModel

object Routes {
    const val PENDING = "pending"
    const val TASK = "task/{taskId}"
    const val BACKUP = "backup"
    const val LIBRARY = "tasks"
    const val SETTINGS = "settings"

    fun task(taskId: Long) = "task/$taskId"
}

@Composable
fun ErrataNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val commands = ErrataApp.instance.taskCommands

    NavHost(
        navController = navController,
        startDestination = Routes.PENDING,
        modifier = modifier,
    ) {
        composable(Routes.PENDING) {
            val vm: PendingQueueViewModel = viewModel(
                factory = PendingQueueViewModel.factory(commands),
            )
            PendingQueueScreen(
                viewModel = vm,
                onAddTask = { navController.navigate(Routes.task(0L)) },
                onOpenTask = { id -> navController.navigate(Routes.task(id)) },
                onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenBackup = { navController.navigate(Routes.BACKUP) },
            )
        }
        composable(
            route = Routes.TASK,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType }),
        ) { entry ->
            val taskId = entry.arguments?.getLong("taskId") ?: 0L
            val vm: TaskEditorViewModel = viewModel(
                factory = TaskEditorViewModel.factory(commands, taskId),
            )
            TaskEditorScreen(
                viewModel = vm,
                onDone = { navController.popBackStack() },
            )
        }
        composable(Routes.LIBRARY) {
            val vm: AllTasksViewModel = viewModel(
                factory = AllTasksViewModel.factory(commands),
            )
            AllTasksScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onOpenTask = { id -> navController.navigate(Routes.task(id)) },
                onAddTask = { navController.navigate(Routes.task(0L)) },
            )
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(
                factory = SettingsViewModel.factory(commands),
            )
            SettingsScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
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
    }
}

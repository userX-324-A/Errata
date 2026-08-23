package com.errata.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.errata.app.data.TaskCommands
import com.errata.app.data.local.TaskEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryItem(
    val task: TaskEntity,
    val subtitle: String,
)

data class AllTasksUiState(
    val items: List<LibraryItem> = emptyList(),
    val isEmpty: Boolean = true,
)

class AllTasksViewModel(
    private val commands: TaskCommands,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val dayFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    val uiState: StateFlow<AllTasksUiState> = commands.observeActiveTasks
        .map { tasks ->
            val items = tasks.map { task ->
                LibraryItem(task = task, subtitle = subtitleFor(task))
            }
            AllTasksUiState(items = items, isEmpty = items.isEmpty())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AllTasksUiState(),
        )

    fun pause(taskId: Long) {
        viewModelScope.launch { commands.pause(taskId) }
    }

    fun resume(taskId: Long) {
        viewModelScope.launch { commands.resume(taskId) }
    }

    fun archive(taskId: Long) {
        viewModelScope.launch { commands.archive(taskId) }
    }

    private fun subtitleFor(task: TaskEntity): String {
        if (task.isPaused) {
            return "Paused · ~${task.estimateMinutes} min"
        }
        val zoned = Instant.ofEpochMilli(task.nextDueAtEpochMs).atZone(zone)
        val date = dayFormatter.format(zoned.toLocalDate())
        val time = timeFormatter.format(zoned.toLocalTime())
        return "Next due $date · $time · ~${task.estimateMinutes} min"
    }

    companion object {
        fun factory(commands: TaskCommands): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AllTasksViewModel(commands) as T
            }
    }
}

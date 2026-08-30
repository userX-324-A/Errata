package com.errata.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.errata.app.data.TaskCommands
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.area.TaskAreas
import com.errata.app.domain.cadence.NthWeekday
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.cadence.Weekdays
import com.errata.app.domain.cadence.Yearly
import com.errata.app.domain.starter.StarterSpec
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryItem(
    val task: TaskEntity,
    val subtitle: String,
)

data class AllTasksUiState(
    val items: List<LibraryItem> = emptyList(),
    val isEmpty: Boolean = true,
    val availableAreas: List<String> = emptyList(),
    /** Null = All. */
    val activeArea: String? = null,
)

class AllTasksViewModel(
    private val commands: TaskCommands,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val dayFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    private val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    private val activeArea = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AllTasksUiState> = combine(
        commands.observeActiveTasks,
        activeArea,
    ) { tasks, requestedArea ->
        val availableAreas = TaskAreas.usedAreas(tasks.map { it.area })
        val selectedArea = requestedArea.takeIf { it != null && it in availableAreas }
        val shown = if (selectedArea == null) {
            tasks
        } else {
            tasks.filter { TaskAreas.normalize(it.area) == selectedArea }
        }
        AllTasksUiState(
            items = shown.map { task ->
                LibraryItem(task = task, subtitle = subtitleFor(task))
            },
            isEmpty = tasks.isEmpty(),
            availableAreas = availableAreas,
            activeArea = selectedArea,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AllTasksUiState(),
    )

    fun setActiveArea(area: String?) {
        activeArea.value = area
    }

    fun pause(taskId: Long) {
        viewModelScope.launch { commands.pause(taskId) }
    }

    fun resume(taskId: Long) {
        viewModelScope.launch { commands.resume(taskId) }
    }

    fun archive(taskId: Long) {
        viewModelScope.launch { commands.archive(taskId) }
    }

    fun pinStarters(specs: List<StarterSpec>) {
        viewModelScope.launch { commands.pinStarters(specs) }
    }

    fun rescheduleReminders() {
        viewModelScope.launch { commands.rescheduleReminders() }
    }

    private fun subtitleFor(task: TaskEntity): String {
        if (task.isPaused) {
            return "Paused · ~${task.estimateMinutes} min"
        }
        val zoned = Instant.ofEpochMilli(task.nextDueAtEpochMs).atZone(zone)
        val date = dayFormatter.format(zoned.toLocalDate())
        val time = timeFormatter.format(zoned.toLocalTime())
        val due = "Next due $date · $time · ~${task.estimateMinutes} min"
        val kind = when (task.scheduleKind) {
            ScheduleKind.INTERVAL -> null
            ScheduleKind.WEEKLY -> "Weekly · ${Weekdays.shortLabels(task.weekdaysMask)}"
            ScheduleKind.MONTHLY -> "Monthly · day ${task.monthDay}"
            ScheduleKind.NTH_WEEKDAY ->
                NthWeekday.summary(task.weekdayOrdinal, task.weekdaysMask)
            ScheduleKind.YEARLY ->
                Yearly.summary(task.seasonMask, task.yearMonthsMask, task.monthDay)
        }
        return if (kind == null) due else "$kind · $due"
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

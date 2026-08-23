package com.errata.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.errata.app.data.TaskCommands
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.area.TaskAreas
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.cadence.Weekdays
import com.errata.app.domain.history.HistoryGlance
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditorUiState(
    val isNew: Boolean = true,
    val title: String = "",
    val notes: String = "",
    val estimateMinutes: String = "15",
    val intervalDays: String = "14",
    val scheduleKind: ScheduleKind = ScheduleKind.INTERVAL,
    val weekdaysMask: Int = 0,
    val monthDay: String = "15",
    val cadenceMode: CadenceMode = CadenceMode.FROM_COMPLETION_CATCH_UP,
    val dueEpochDay: Long = LocalDate.now().toEpochDay(),
    /** Local minutes since midnight for due datetime. */
    val dueMinuteOfDay: Int = 9 * 60,
    val anchorEpochDay: Long = LocalDate.now().toEpochDay(),
    val existingId: Long = 0L,
    val createdAtEpochMs: Long = 0L,
    val lastCompletedAtEpochMs: Long? = null,
    /** null = use app default reminder time */
    val reminderMinutesOfDay: Int? = null,
    val defaultReminderMinutesOfDay: Int = 9 * 60,
    val snoozedUntilEpochMs: Long? = null,
    val area: String? = null,
    val isPaused: Boolean = false,
    val isArchived: Boolean = false,
    val history: HistoryGlance? = null,
    val loaded: Boolean = false,
    val errorMessage: String? = null,
    val saved: Boolean = false,
)

class TaskEditorViewModel(
    private val commands: TaskCommands,
    private val taskId: Long,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskEditorUiState(isNew = taskId == 0L))
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = commands.getSettings()
            if (taskId == 0L) {
                val mode = settings.defaultCadenceMode
                val today = LocalDate.now(zone).toEpochDay()
                val dueMinutes = settings.defaultReminderMinutesOfDay
                _uiState.update {
                    it.copy(
                        cadenceMode = mode,
                        dueEpochDay = today,
                        dueMinuteOfDay = dueMinutes,
                        anchorEpochDay = today,
                        defaultReminderMinutesOfDay = settings.defaultReminderMinutesOfDay,
                        reminderMinutesOfDay = null,
                        loaded = true,
                    )
                }
            } else {
                val task = commands.getTask(taskId)
                if (task == null) {
                    _uiState.update { it.copy(loaded = true, errorMessage = "missing") }
                } else {
                    _uiState.update {
                        it.copy(
                            isNew = false,
                            existingId = task.id,
                            title = task.title,
                            notes = task.notes.orEmpty(),
                            estimateMinutes = task.estimateMinutes.toString(),
                            intervalDays = task.intervalDays.toString(),
                            scheduleKind = task.scheduleKind,
                            weekdaysMask = task.weekdaysMask,
                            monthDay = task.monthDay.takeIf { it in 1..31 }?.toString() ?: "15",
                            cadenceMode = task.cadenceMode,
                            dueEpochDay = CadenceCalculator.epochDayOf(task.nextDueAtEpochMs, zone),
                            dueMinuteOfDay = CadenceCalculator.minutesOfDay(task.nextDueAtEpochMs, zone),
                            anchorEpochDay = task.anchorEpochDay,
                            createdAtEpochMs = task.createdAtEpochMs,
                            lastCompletedAtEpochMs = task.lastCompletedAtEpochMs,
                            reminderMinutesOfDay = task.reminderMinutesOfDay,
                            defaultReminderMinutesOfDay = settings.defaultReminderMinutesOfDay,
                            snoozedUntilEpochMs = task.snoozedUntilEpochMs,
                            area = task.area,
                            isPaused = task.isPaused,
                            isArchived = task.isArchived,
                            history = HistoryGlance.from(
                                commands.completionsFor(taskId).map { row ->
                                    HistoryGlance.Sample(
                                        completedAtEpochMs = row.completedAtEpochMs,
                                        scheduledDueAtEpochMs = row.scheduledDueAtEpochMs,
                                    )
                                },
                                zone,
                            ),
                            loaded = true,
                        )
                    }
                }
            }
        }
    }

    fun updateTitle(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }
    fun updateNotes(value: String) = _uiState.update { it.copy(notes = value) }
    fun updateEstimate(value: String) = _uiState.update {
        it.copy(estimateMinutes = value.filter { c -> c.isDigit() }, errorMessage = null)
    }
    fun updateInterval(value: String) = _uiState.update {
        it.copy(intervalDays = value.filter { c -> c.isDigit() }, errorMessage = null)
    }
    fun updateScheduleKind(kind: ScheduleKind) = _uiState.update { state ->
        when (kind) {
            ScheduleKind.INTERVAL -> state.copy(scheduleKind = kind, errorMessage = null)
            ScheduleKind.WEEKLY -> state.copy(
                scheduleKind = kind,
                weekdaysMask = if (Weekdays.hasAny(state.weekdaysMask)) {
                    state.weekdaysMask
                } else {
                    Weekdays.bit(LocalDate.ofEpochDay(state.dueEpochDay).dayOfWeek)
                },
                errorMessage = null,
            )
            ScheduleKind.MONTHLY -> {
                val existing = state.monthDay.toIntOrNull()
                val day = if (existing in 1..31) {
                    state.monthDay
                } else {
                    LocalDate.ofEpochDay(state.dueEpochDay).dayOfMonth.toString()
                }
                state.copy(scheduleKind = kind, monthDay = day, errorMessage = null)
            }
        }
    }
    fun toggleWeekday(day: DayOfWeek) = _uiState.update {
        it.copy(weekdaysMask = Weekdays.toggle(it.weekdaysMask, day), errorMessage = null)
    }
    fun updateMonthDay(value: String) = _uiState.update {
        it.copy(monthDay = value.filter { c -> c.isDigit() }.take(2), errorMessage = null)
    }
    fun updateCadenceMode(mode: CadenceMode) = _uiState.update { it.copy(cadenceMode = mode) }
    fun updateDueEpochDay(day: Long) = _uiState.update { it.copy(dueEpochDay = day) }
    fun updateDueMinuteOfDay(minutes: Int) = _uiState.update {
        it.copy(dueMinuteOfDay = minutes.coerceIn(0, 24 * 60 - 1))
    }
    fun useDefaultReminder() = _uiState.update { it.copy(reminderMinutesOfDay = null) }
    fun updateReminderMinutes(minutes: Int) = _uiState.update {
        it.copy(reminderMinutesOfDay = minutes.coerceIn(0, 24 * 60 - 1))
    }
    fun updateArea(value: String?) = _uiState.update {
        it.copy(area = TaskAreas.normalize(value))
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val title = state.title.trim()
            val estimate = state.estimateMinutes.toIntOrNull() ?: 0
            val interval = state.intervalDays.toIntOrNull() ?: 0
            val monthDay = state.monthDay.toIntOrNull() ?: 0
            when {
                title.isEmpty() -> _uiState.update { it.copy(errorMessage = "title") }
                estimate < 1 -> _uiState.update { it.copy(errorMessage = "estimate") }
                state.scheduleKind == ScheduleKind.INTERVAL && interval < 1 ->
                    _uiState.update { it.copy(errorMessage = "interval") }
                state.scheduleKind == ScheduleKind.WEEKLY && !Weekdays.hasAny(state.weekdaysMask) ->
                    _uiState.update { it.copy(errorMessage = "weekdays") }
                state.scheduleKind == ScheduleKind.MONTHLY && monthDay !in 1..31 ->
                    _uiState.update { it.copy(errorMessage = "monthDay") }
                else -> {
                    val nextDue = CadenceCalculator.atLocalDateMinutes(
                        state.dueEpochDay,
                        state.dueMinuteOfDay,
                        zone,
                    )
                    val now = System.currentTimeMillis()
                    val anchor = if (state.isNew) state.dueEpochDay else state.anchorEpochDay
                    val storedInterval = if (state.scheduleKind == ScheduleKind.INTERVAL) {
                        interval
                    } else {
                        CadenceCalculator.GRID_INTERVAL_DAYS
                    }
                    val entity = TaskEntity(
                        id = state.existingId,
                        title = title,
                        notes = state.notes.trim().ifEmpty { null },
                        estimateMinutes = estimate,
                        intervalDays = storedInterval,
                        scheduleKind = state.scheduleKind,
                        weekdaysMask = if (state.scheduleKind == ScheduleKind.WEEKLY) {
                            state.weekdaysMask
                        } else {
                            0
                        },
                        monthDay = if (state.scheduleKind == ScheduleKind.MONTHLY) monthDay else 0,
                        cadenceMode = state.cadenceMode,
                        anchorEpochDay = anchor,
                        nextDueAtEpochMs = nextDue,
                        lastCompletedAtEpochMs = state.lastCompletedAtEpochMs,
                        reminderMinutesOfDay = state.reminderMinutesOfDay,
                        snoozedUntilEpochMs = state.snoozedUntilEpochMs,
                        area = TaskAreas.normalize(state.area),
                        isPaused = state.isPaused,
                        isArchived = state.isArchived,
                        createdAtEpochMs = state.createdAtEpochMs.takeIf { it != 0L } ?: now,
                        updatedAtEpochMs = now,
                    )
                    commands.upsert(entity)
                    _uiState.update { it.copy(saved = true, errorMessage = null) }
                }
            }
        }
    }

    fun rescheduleReminders() {
        viewModelScope.launch { commands.rescheduleReminders() }
    }

    companion object {
        fun factory(commands: TaskCommands, taskId: Long): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TaskEditorViewModel(commands, taskId) as T
            }
    }
}

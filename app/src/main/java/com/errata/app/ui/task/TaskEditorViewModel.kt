package com.errata.app.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.errata.app.data.TaskCommands
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.area.TaskAreas
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.NthWeekday
import com.errata.app.domain.cadence.ScheduleKind
import com.errata.app.domain.cadence.Seasons
import com.errata.app.domain.cadence.Weekdays
import com.errata.app.domain.cadence.YearMonths
import com.errata.app.domain.cadence.Yearly
import com.errata.app.domain.history.HistoryGlance
import com.errata.app.domain.reminders.ReminderPolicy
import com.errata.app.domain.starter.StarterCatalog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TaskEditorUiState(
    val isNew: Boolean = true,
    val title: String = "",
    val notes: String = "",
    val estimateMinutes: String = "",
    val intervalDays: String = "14",
    val scheduleKind: ScheduleKind = ScheduleKind.INTERVAL,
    val weekdaysMask: Int = 0,
    val monthDay: String = "15",
    val weekdayOrdinal: Int = 1,
    val yearMonthsMask: Int = 0,
    val seasonMask: Int = 0,
    val cadenceMode: CadenceMode = CadenceMode.FROM_COMPLETION_CATCH_UP,
    val dueEpochDay: Long = LocalDate.now().toEpochDay(),
    /** Local minutes since midnight for due datetime. */
    val dueMinuteOfDay: Int = 9 * 60,
    val anchorEpochDay: Long = LocalDate.now().toEpochDay(),
    val existingId: Long = 0L,
    val existingUuid: String = "",
    val createdAtEpochMs: Long = 0L,
    val lastCompletedAtEpochMs: Long? = null,
    /** null = When due; [ReminderPolicy.NONE] = none; 0–1439 = clock. */
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
    val saving: Boolean = false,
)

/** Fields the editor can change. Used to decide discard-on-back. */
fun TaskEditorUiState.editFingerprint(): List<Any?> = listOf(
    title,
    notes,
    estimateMinutes,
    intervalDays,
    scheduleKind,
    weekdaysMask,
    monthDay,
    weekdayOrdinal,
    yearMonthsMask,
    seasonMask,
    cadenceMode,
    dueEpochDay,
    dueMinuteOfDay,
    reminderMinutesOfDay,
    area,
)

/** Due day, interval, mode, or schedule kind — the FIXED_ANCHOR grid. */
fun TaskEditorUiState.cadenceGridChanged(prior: TaskEditorUiState): Boolean =
    dueEpochDay != prior.dueEpochDay ||
        intervalDays != prior.intervalDays ||
        cadenceMode != prior.cadenceMode ||
        scheduleKind != prior.scheduleKind ||
        weekdaysMask != prior.weekdaysMask ||
        monthDay != prior.monthDay ||
        weekdayOrdinal != prior.weekdayOrdinal ||
        yearMonthsMask != prior.yearMonthsMask ||
        seasonMask != prior.seasonMask

fun TaskEditorUiState.anchorOnSave(prior: TaskEditorUiState?): Long {
    if (isNew || prior == null || cadenceGridChanged(prior)) return dueEpochDay
    return anchorEpochDay
}

fun TaskEditorUiState.snoozeOnSave(prior: TaskEditorUiState?): Long? {
    if (isNew || prior == null) return null
    if (cadenceGridChanged(prior) || dueMinuteOfDay != prior.dueMinuteOfDay) return null
    return snoozedUntilEpochMs
}

/** New blank task: reminder from Settings kind; due clock from Settings default time. */
fun blankFirstDueEpochDay(
    todayEpochDay: Long,
    dueMinutes: Int,
    nowEpochMs: Long,
    zone: ZoneId,
): Long {
    val todayFire = CadenceCalculator.atLocalDateMinutes(todayEpochDay, dueMinutes, zone)
    return if (todayFire > nowEpochMs) todayEpochDay else todayEpochDay + 1
}

fun TaskEditorUiState.withBlankNew(
    cadenceMode: CadenceMode,
    todayEpochDay: Long,
    dueMinutes: Int,
    storedReminderMinutes: Int? = null,
    nowEpochMs: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): TaskEditorUiState {
    val dueDay = blankFirstDueEpochDay(todayEpochDay, dueMinutes, nowEpochMs, zone)
    return copy(
        cadenceMode = cadenceMode,
        dueEpochDay = dueDay,
        dueMinuteOfDay = dueMinutes,
        anchorEpochDay = dueDay,
        defaultReminderMinutesOfDay = dueMinutes,
        reminderMinutesOfDay = storedReminderMinutes,
        loaded = true,
    )
}

fun TaskEditorUiState.shouldSkipSave(): Boolean = saved || saving

fun TaskEditorUiState.adoptSavedRow(
    id: Long,
    uuid: String,
    createdAtEpochMs: Long,
): TaskEditorUiState = copy(
    saved = true,
    saving = false,
    errorMessage = null,
    existingId = id,
    existingUuid = uuid,
    isNew = false,
    createdAtEpochMs = createdAtEpochMs,
)

/**
 * After popToList the keyed VM stays on the list tab. Clear [saved] so the
 * next open of this pane key does not immediately leave. A create (blank or
 * starter) drops the adopted row so the same key is a fresh draft.
 */
fun TaskEditorUiState.releaseAfterLeave(openedAsNew: Boolean): TaskEditorUiState {
    if (!saved) return this
    if (!openedAsNew) return copy(saved = false, saving = false)
    return TaskEditorUiState(isNew = true)
}

class TaskEditorViewModel(
    private val commands: TaskCommands,
    private val taskId: Long,
    private val starterId: String? = null,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskEditorUiState(isNew = taskId == 0L))
    val uiState: StateFlow<TaskEditorUiState> = _uiState.asStateFlow()
    private var baselineFingerprint: List<Any?>? = null
    private var baselineState: TaskEditorUiState? = null
    private val saveGate = AtomicBoolean(false)

    init {
        viewModelScope.launch { loadDraft() }
    }

    /** Call when Save (or after-pin Back / Not now) is about to popToList. */
    fun releaseAfterLeave() {
        if (!_uiState.value.saved) return
        saveGate.set(false)
        val openedAsNew = taskId == 0L
        _uiState.update { it.releaseAfterLeave(openedAsNew) }
        if (openedAsNew) {
            viewModelScope.launch { loadDraft() }
        } else {
            markBaseline()
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
            ScheduleKind.NTH_WEEKDAY -> {
                val mask = if (Weekdays.isSingle(state.weekdaysMask)) {
                    state.weekdaysMask
                } else {
                    Weekdays.bit(DayOfWeek.SATURDAY)
                }
                val ordinal = if (NthWeekday.isValid(state.weekdayOrdinal)) {
                    state.weekdayOrdinal
                } else {
                    1
                }
                state.copy(
                    scheduleKind = kind,
                    weekdaysMask = mask,
                    weekdayOrdinal = ordinal,
                    errorMessage = null,
                )
            }
            ScheduleKind.YEARLY -> {
                val due = LocalDate.ofEpochDay(state.dueEpochDay)
                val hasAny = YearMonths.hasAny(state.yearMonthsMask) ||
                    Seasons.hasAny(state.seasonMask)
                if (hasAny) {
                    state.copy(scheduleKind = kind, errorMessage = null)
                } else {
                    val existing = state.monthDay.toIntOrNull()
                    val day = if (existing in 1..31) {
                        state.monthDay
                    } else {
                        due.dayOfMonth.toString()
                    }
                    state.copy(
                        scheduleKind = kind,
                        yearMonthsMask = YearMonths.bit(due.month),
                        monthDay = day,
                        errorMessage = null,
                    )
                }
            }
        }
    }
    fun toggleWeekday(day: DayOfWeek) = _uiState.update {
        it.copy(weekdaysMask = Weekdays.toggle(it.weekdaysMask, day), errorMessage = null)
    }
    fun selectNthWeekday(day: DayOfWeek) = _uiState.update {
        it.copy(weekdaysMask = Weekdays.bit(day), errorMessage = null)
    }
    fun updateWeekdayOrdinal(ordinal: Int) = _uiState.update {
        it.copy(weekdayOrdinal = ordinal, errorMessage = null)
    }
    fun toggleYearMonth(month: Month) = _uiState.update {
        it.copy(yearMonthsMask = YearMonths.toggle(it.yearMonthsMask, month), errorMessage = null)
    }
    fun toggleSeason(bit: Int) = _uiState.update {
        it.copy(seasonMask = Seasons.toggle(it.seasonMask, bit), errorMessage = null)
    }
    fun updateMonthDay(value: String) = _uiState.update {
        it.copy(monthDay = value.filter { c -> c.isDigit() }.take(2), errorMessage = null)
    }
    fun updateCadenceMode(mode: CadenceMode) = _uiState.update { it.copy(cadenceMode = mode) }
    fun updateDueEpochDay(day: Long) = _uiState.update { it.copy(dueEpochDay = day) }
    fun updateDueMinuteOfDay(minutes: Int) = _uiState.update {
        it.copy(dueMinuteOfDay = minutes.coerceIn(0, 24 * 60 - 1))
    }
    fun useWhenDueReminder() = _uiState.update { it.copy(reminderMinutesOfDay = null) }
    fun useNoneReminder() = _uiState.update {
        it.copy(reminderMinutesOfDay = ReminderPolicy.NONE)
    }
    fun updateReminderMinutes(minutes: Int) = _uiState.update {
        it.copy(reminderMinutesOfDay = minutes.coerceIn(0, 24 * 60 - 1))
    }
    fun updateArea(value: String?) = _uiState.update {
        it.copy(area = TaskAreas.normalize(value))
    }

    fun isDirty(): Boolean {
        val snap = baselineFingerprint ?: return false
        return _uiState.value.editFingerprint() != snap
    }

    private fun markBaseline() {
        val snap = _uiState.value
        baselineFingerprint = snap.editFingerprint()
        baselineState = snap
    }

    private suspend fun loadDraft() {
        val settings = commands.getSettings()
        if (taskId == 0L) {
            val spec = StarterCatalog.specById(starterId)
            if (spec != null) {
                val now = System.currentTimeMillis()
                val entity = StarterCatalog.materialize(
                    spec = spec,
                    cadenceMode = settings.defaultCadenceMode,
                    reminderMinutesOfDay = settings.defaultReminderMinutesOfDay,
                    storedReminderMinutes = ReminderPolicy.storedFor(
                        settings.defaultReminderKind,
                        settings.defaultReminderMinutesOfDay,
                    ),
                    nowEpochMs = now,
                    zone = zone,
                )
                _uiState.update {
                    it.copy(
                        isNew = true,
                        existingId = 0L,
                        existingUuid = "",
                        createdAtEpochMs = 0L,
                        lastCompletedAtEpochMs = null,
                        history = null,
                        saved = false,
                        saving = false,
                        cadenceMode = entity.cadenceMode,
                        title = entity.title,
                        estimateMinutes = entity.estimateMinutes.toString(),
                        intervalDays = entity.intervalDays.toString(),
                        scheduleKind = entity.scheduleKind,
                        weekdaysMask = entity.weekdaysMask,
                        monthDay = entity.monthDay.takeIf { day -> day in 1..31 }?.toString()
                            ?: "15",
                        weekdayOrdinal = entity.weekdayOrdinal.takeIf { NthWeekday.isValid(it) }
                            ?: 1,
                        yearMonthsMask = entity.yearMonthsMask,
                        seasonMask = entity.seasonMask,
                        dueEpochDay = CadenceCalculator.epochDayOf(entity.nextDueAtEpochMs, zone),
                        dueMinuteOfDay = CadenceCalculator.minutesOfDay(
                            entity.nextDueAtEpochMs,
                            zone,
                        ),
                        anchorEpochDay = entity.anchorEpochDay,
                        defaultReminderMinutesOfDay = settings.defaultReminderMinutesOfDay,
                        reminderMinutesOfDay = entity.reminderMinutesOfDay,
                        area = entity.area,
                        loaded = true,
                    )
                }
                markBaseline()
            } else {
                val today = LocalDate.now(zone).toEpochDay()
                val now = System.currentTimeMillis()
                _uiState.update {
                    it.withBlankNew(
                        cadenceMode = settings.defaultCadenceMode,
                        todayEpochDay = today,
                        dueMinutes = settings.defaultReminderMinutesOfDay,
                        storedReminderMinutes = ReminderPolicy.storedFor(
                            settings.defaultReminderKind,
                            settings.defaultReminderMinutesOfDay,
                        ),
                        nowEpochMs = now,
                        zone = zone,
                    ).copy(
                        isNew = true,
                        existingId = 0L,
                        existingUuid = "",
                        createdAtEpochMs = 0L,
                        lastCompletedAtEpochMs = null,
                        history = null,
                        saved = false,
                        saving = false,
                    )
                }
                markBaseline()
            }
        } else {
            val task = commands.getTask(taskId)
            if (task == null) {
                _uiState.update { it.copy(loaded = true, errorMessage = "missing") }
                markBaseline()
            } else {
                _uiState.update {
                    it.copy(
                        isNew = false,
                        existingId = task.id,
                        existingUuid = task.uuid,
                        title = task.title,
                        notes = task.notes.orEmpty(),
                        estimateMinutes = task.estimateMinutes.toString(),
                        intervalDays = task.intervalDays.toString(),
                        scheduleKind = task.scheduleKind,
                        weekdaysMask = task.weekdaysMask,
                        monthDay = task.monthDay.takeIf { it in 1..31 }?.toString() ?: "15",
                        weekdayOrdinal = task.weekdayOrdinal.takeIf { NthWeekday.isValid(it) }
                            ?: 1,
                        yearMonthsMask = task.yearMonthsMask,
                        seasonMask = task.seasonMask,
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
                markBaseline()
            }
        }
    }

    fun save() {
        if (_uiState.value.shouldSkipSave()) return
        if (!saveGate.compareAndSet(false, true)) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
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
                state.scheduleKind == ScheduleKind.NTH_WEEKDAY &&
                    (!Weekdays.isSingle(state.weekdaysMask) ||
                        !NthWeekday.isValid(state.weekdayOrdinal)) ->
                    _uiState.update { it.copy(errorMessage = "nthWeekday") }
                state.scheduleKind == ScheduleKind.YEARLY &&
                    !Yearly.isValid(state.yearMonthsMask, state.seasonMask, monthDay) ->
                    _uiState.update { it.copy(errorMessage = "yearly") }
                else -> {
                    val nextDue = CadenceCalculator.atLocalDateMinutes(
                        state.dueEpochDay,
                        state.dueMinuteOfDay,
                        zone,
                    )
                    val now = System.currentTimeMillis()
                    val anchor = state.anchorOnSave(baselineState)
                    val storedInterval = if (state.scheduleKind == ScheduleKind.INTERVAL) {
                        interval
                    } else {
                        CadenceCalculator.GRID_INTERVAL_DAYS
                    }
                    val entity = TaskEntity(
                        id = state.existingId,
                        uuid = state.existingUuid,
                        title = title,
                        notes = state.notes.trim().ifEmpty { null },
                        estimateMinutes = estimate,
                        intervalDays = storedInterval,
                        scheduleKind = state.scheduleKind,
                        weekdaysMask = when (state.scheduleKind) {
                            ScheduleKind.WEEKLY,
                            ScheduleKind.NTH_WEEKDAY,
                            -> state.weekdaysMask
                            else -> 0
                        },
                        monthDay = when (state.scheduleKind) {
                            ScheduleKind.MONTHLY -> monthDay
                            ScheduleKind.YEARLY ->
                                if (YearMonths.hasAny(state.yearMonthsMask)) monthDay else 0
                            else -> 0
                        },
                        weekdayOrdinal = if (state.scheduleKind == ScheduleKind.NTH_WEEKDAY) {
                            state.weekdayOrdinal
                        } else {
                            0
                        },
                        yearMonthsMask = if (state.scheduleKind == ScheduleKind.YEARLY) {
                            state.yearMonthsMask
                        } else {
                            0
                        },
                        seasonMask = if (state.scheduleKind == ScheduleKind.YEARLY) {
                            state.seasonMask
                        } else {
                            0
                        },
                        cadenceMode = state.cadenceMode,
                        anchorEpochDay = anchor,
                        nextDueAtEpochMs = nextDue,
                        lastCompletedAtEpochMs = state.lastCompletedAtEpochMs,
                        reminderMinutesOfDay = state.reminderMinutesOfDay,
                        snoozedUntilEpochMs = state.snoozeOnSave(baselineState),
                        area = TaskAreas.normalize(state.area),
                        isPaused = state.isPaused,
                        isArchived = state.isArchived,
                        createdAtEpochMs = state.createdAtEpochMs.takeIf { it != 0L } ?: now,
                        updatedAtEpochMs = now,
                    )
                    val id = commands.upsert(entity)
                    val row = commands.getTask(id)
                    _uiState.update {
                        it.adoptSavedRow(
                            id = id,
                            uuid = row?.uuid.orEmpty(),
                            createdAtEpochMs = row?.createdAtEpochMs ?: state.createdAtEpochMs,
                        )
                    }
                    markBaseline()
                }
            }
            } finally {
                if (!_uiState.value.saved) {
                    saveGate.set(false)
                    _uiState.update { it.copy(saving = false) }
                }
            }
        }
    }

    fun rescheduleReminders() {
        viewModelScope.launch { commands.rescheduleReminders() }
    }

    companion object {
        fun factory(
            commands: TaskCommands,
            taskId: Long,
            starterId: String = "",
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TaskEditorViewModel(commands, taskId, starterId) as T
            }
    }
}

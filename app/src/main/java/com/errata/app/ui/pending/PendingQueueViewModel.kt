package com.errata.app.ui.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.errata.app.ErrataApp
import com.errata.app.data.TaskCommands
import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.estimate.EstimateAdjuster
import com.errata.app.domain.estimate.EstimateHonesty
import com.errata.app.domain.reminders.ReminderPolicy
import com.errata.app.domain.freewindow.FreeWindowSelection
import com.errata.app.domain.starter.StarterSpec
import com.errata.app.reminders.ReminderActionGuard
import com.errata.app.ui.common.formatClock
import com.errata.app.ui.snooze.SnoozePreset
import com.errata.app.ui.snooze.SnoozePresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PendingItem(
    val task: TaskEntity,
    val bucket: DueBucket,
    val subtitle: String,
)

data class PendingHonestyPrompt(
    val taskId: Long,
    val title: String,
    val estimateMinutes: Int,
)

/** In-app snooze expected due is captured when the sheet opens, not at confirm. */
data class PendingSnoozeTarget(
    val taskId: Long,
    val expectedNextDueAtEpochMs: Long,
)

data class PendingQueueUiState(
    val overdue: List<PendingItem> = emptyList(),
    val dueToday: List<PendingItem> = emptyList(),
    val soon: List<PendingItem> = emptyList(),
    val isEmpty: Boolean = true,
    /** Null = full pending list. */
    val activeWindowMinutes: Int? = null,
    val fits: List<PendingItem> = emptyList(),
    val leftoverAfterBestMinutes: Int? = null,
    val untilWorkMinutes: Int? = null,
    val untilWorkSelected: Boolean = false,
    /** Until-work / stop-by clock is selected but that time has already passed today. */
    val clockWindowPassed: Boolean = false,
    val workStartMinutesOfDay: Int? = null,
    val customWindowSelected: Boolean = false,
    val honesty: PendingHonestyPrompt? = null,
    val availableAreas: List<String> = emptyList(),
    /** Null = All. */
    val activeArea: String? = null,
    /** True when an area is selected and nothing in that area is pending. */
    val areaFilterEmpty: Boolean = false,
    /** True when there are zero active (non-archived) tasks. */
    val hasNoPinnedTasks: Boolean = true,
    /** Show after pinning starters if the due queue is still empty. */
    val startersPinnedHint: Boolean = false,
    /** Cycle actions in flight for these ids (Done/Skip/Snooze). */
    val busyTaskIds: Set<Long> = emptySet(),
)

class PendingQueueViewModel(
    private val commands: TaskCommands,
    private val appContext: Context,
) : ViewModel() {

    private val nowTick = MutableStateFlow(System.currentTimeMillis())
    private val window = MutableStateFlow<FreeWindowSelection?>(null)
    private val honestyPrompt = MutableStateFlow<PendingHonestyPrompt?>(null)
    private val activeArea = MutableStateFlow<String?>(null)
    private val startersPinnedHint = MutableStateFlow(false)
    private val busyIds = MutableStateFlow<Set<Long>>(emptySet())

    private data class QueueExtras(
        val honesty: PendingHonestyPrompt?,
        val area: String?,
        val hint: Boolean,
        val busy: Set<Long>,
    )

    val uiState: StateFlow<PendingQueueUiState> = combine(
        commands.observeActiveTasks,
        commands.observeSettings,
        nowTick,
        window,
        combine(honestyPrompt, activeArea, startersPinnedHint, busyIds) { honesty, area, hint, busy ->
            QueueExtras(honesty, area, hint, busy)
        },
    ) { tasks, settings, now, window, extras ->
        PendingQueueState.build(
            tasks,
            settings ?: SettingsEntity(),
            now,
            window,
            extras.honesty,
            extras.area,
            extras.hint,
            formatTime = { time -> formatClock(appContext, time.hour * 60 + time.minute) },
        ).copy(busyTaskIds = extras.busy)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PendingQueueUiState(),
    )

    fun refreshNow() {
        nowTick.value = System.currentTimeMillis()
    }

    fun setWindowMinutes(minutes: Int) {
        if (minutes > 0) {
            window.value = FreeWindowSelection.Duration(minutes)
            refreshNow()
        }
    }

    fun setWindowUntilClock(minutesOfDay: Int) {
        window.value = FreeWindowSelection.UntilClock(minutesOfDay)
        refreshNow()
    }

    fun clearWindow() {
        window.value = null
    }

    fun setActiveArea(area: String?) {
        activeArea.value = area
    }

    fun complete(taskId: Long) {
        viewModelScope.launch {
            withTaskBusy(taskId) {
                val task = commands.getTask(taskId) ?: return@withTaskBusy
                val applied = commands.complete(
                    taskId,
                    expectedNextDueAtEpochMs = task.nextDueAtEpochMs,
                )
                if (applied && EstimateAdjuster.shouldAskAfterDone(task.estimateMinutes)) {
                    honestyPrompt.value = PendingHonestyPrompt(
                        taskId = task.id,
                        title = task.title,
                        estimateMinutes = task.estimateMinutes,
                    )
                }
                refreshNow()
            }
        }
    }

    fun applyHonesty(choice: EstimateHonesty) {
        val prompt = honestyPrompt.value ?: return
        viewModelScope.launch {
            val next = EstimateAdjuster.adjust(prompt.estimateMinutes, choice)
            if (choice != EstimateHonesty.SAME && next != prompt.estimateMinutes) {
                commands.updateEstimateMinutes(prompt.taskId, next)
            }
            honestyPrompt.value = null
            refreshNow()
        }
    }

    fun dismissHonesty() {
        honestyPrompt.value = null
    }

    fun snooze(
        taskId: Long,
        preset: SnoozePreset,
        expectedNextDueAtEpochMs: Long,
    ) {
        viewModelScope.launch {
            withTaskBusy(taskId) {
                val task = commands.getTask(taskId)
                val clock = if (task != null) {
                    ReminderPolicy.displayMinutes(
                        task.reminderMinutesOfDay,
                        CadenceCalculator.minutesOfDay(task.nextDueAtEpochMs),
                    )
                } else {
                    SnoozePresets.DEFAULT_TOMORROW_MINUTES
                }
                commands.snooze(
                    taskId,
                    SnoozePresets.untilEpochMs(preset, clockMinutesOfDay = clock),
                    expectedNextDueAtEpochMs = expectedNextDueAtEpochMs,
                )
                refreshNow()
            }
        }
    }

    fun snoozeUntil(
        taskId: Long,
        untilEpochMs: Long,
        expectedNextDueAtEpochMs: Long,
    ) {
        viewModelScope.launch {
            withTaskBusy(taskId) {
                commands.snooze(
                    taskId,
                    untilEpochMs,
                    expectedNextDueAtEpochMs = expectedNextDueAtEpochMs,
                )
                refreshNow()
            }
        }
    }

    fun skip(taskId: Long) {
        viewModelScope.launch {
            withTaskBusy(taskId) {
                val task = commands.getTask(taskId) ?: return@withTaskBusy
                commands.skip(
                    taskId,
                    expectedNextDueAtEpochMs = task.nextDueAtEpochMs,
                )
                refreshNow()
            }
        }
    }

    private suspend fun withTaskBusy(taskId: Long, block: suspend () -> Unit) {
        if (!ReminderActionGuard.tryBegin(taskId)) return
        busyIds.value = busyIds.value + taskId
        try {
            block()
        } finally {
            busyIds.value = busyIds.value - taskId
            ReminderActionGuard.end(taskId)
        }
    }

    fun pinStarters(specs: List<StarterSpec>) {
        viewModelScope.launch {
            if (commands.pinStarters(specs) > 0) {
                startersPinnedHint.value = true
                refreshNow()
            }
        }
    }

    fun rescheduleReminders() {
        viewModelScope.launch { commands.rescheduleReminders() }
    }

    companion object {
        fun factory(commands: TaskCommands): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PendingQueueViewModel(commands, ErrataApp.instance) as T
            }
    }
}

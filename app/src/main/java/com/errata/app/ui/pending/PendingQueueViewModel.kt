package com.errata.app.ui.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.errata.app.data.TaskCommands
import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.area.TaskAreas
import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.due.PendingClassifier
import com.errata.app.domain.estimate.EstimateAdjuster
import com.errata.app.domain.estimate.EstimateHonesty
import com.errata.app.domain.freewindow.FreeWindowRanker
import com.errata.app.domain.starter.StarterSpec
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
    val honesty: PendingHonestyPrompt? = null,
    val availableAreas: List<String> = emptyList(),
    /** Null = All. */
    val activeArea: String? = null,
    /** True when there are zero active (non-archived) tasks. */
    val hasNoPinnedTasks: Boolean = true,
    /** Show after pinning starters if the due queue is still empty. */
    val startersPinnedHint: Boolean = false,
)

class PendingQueueViewModel(
    private val commands: TaskCommands,
) : ViewModel() {

    private val nowTick = MutableStateFlow(System.currentTimeMillis())
    private val activeWindowMinutes = MutableStateFlow<Int?>(null)
    private val honestyPrompt = MutableStateFlow<PendingHonestyPrompt?>(null)
    private val activeArea = MutableStateFlow<String?>(null)
    private val startersPinnedHint = MutableStateFlow(false)

    val uiState: StateFlow<PendingQueueUiState> = combine(
        commands.observeActiveTasks,
        commands.observeSettings,
        nowTick,
        activeWindowMinutes,
        combine(honestyPrompt, activeArea, startersPinnedHint) { honesty, area, hint ->
            Triple(honesty, area, hint)
        },
    ) { tasks, settings, now, window, extras ->
        val (honesty, area, hint) = extras
        buildState(tasks, settings ?: SettingsEntity(), now, window, honesty, area, hint)
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
            activeWindowMinutes.value = minutes
            refreshNow()
        }
    }

    fun clearWindow() {
        activeWindowMinutes.value = null
    }

    fun setActiveArea(area: String?) {
        activeArea.value = area
    }

    fun complete(taskId: Long) {
        viewModelScope.launch {
            val task = commands.getTask(taskId)
            commands.complete(taskId)
            if (task != null) {
                honestyPrompt.value = PendingHonestyPrompt(
                    taskId = task.id,
                    title = task.title,
                    estimateMinutes = task.estimateMinutes,
                )
            }
            refreshNow()
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

    fun snooze(taskId: Long, preset: SnoozePreset) {
        viewModelScope.launch {
            commands.snooze(taskId, SnoozePresets.untilEpochMs(preset))
            refreshNow()
        }
    }

    fun snoozeUntil(taskId: Long, untilEpochMs: Long) {
        viewModelScope.launch {
            commands.snooze(taskId, untilEpochMs)
            refreshNow()
        }
    }

    fun skip(taskId: Long) {
        viewModelScope.launch {
            commands.skip(taskId)
            refreshNow()
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

    private fun buildState(
        tasks: List<TaskEntity>,
        settings: SettingsEntity,
        now: Long,
        windowMinutes: Int?,
        honesty: PendingHonestyPrompt?,
        requestedArea: String?,
        hint: Boolean,
    ): PendingQueueUiState {
        val items = tasks.mapNotNull { task ->
            val bucket = PendingClassifier.classify(
                task = PendingClassifier.ClassifiableTask(
                    nextDueAtEpochMs = task.nextDueAtEpochMs,
                    snoozedUntilEpochMs = task.snoozedUntilEpochMs,
                    isPaused = task.isPaused,
                    isArchived = task.isArchived,
                ),
                nowEpochMs = now,
                soonHorizonDays = settings.soonHorizonDays,
            )
            if (bucket != DueBucket.OVERDUE &&
                bucket != DueBucket.DUE_TODAY &&
                bucket != DueBucket.SOON
            ) {
                return@mapNotNull null
            }
            PendingItem(
                task = task,
                bucket = bucket,
                subtitle = DueCopy.subtitle(
                    bucket = bucket,
                    nextDueAtEpochMs = task.nextDueAtEpochMs,
                    snoozedUntilEpochMs = task.snoozedUntilEpochMs,
                    estimateMinutes = task.estimateMinutes,
                    nowEpochMs = now,
                ),
            )
        }

        fun List<PendingItem>.sortedPending() =
            sortedWith(
                compareBy(
                    {
                        PendingClassifier.effectiveDueEpochMs(
                            it.task.nextDueAtEpochMs,
                            it.task.snoozedUntilEpochMs,
                        )
                    },
                    { it.task.title.lowercase() },
                ),
            )

        val availableAreas = TaskAreas.usedAreas(items.map { it.task.area })
        val selectedArea = requestedArea.takeIf { it != null && it in availableAreas }
        val visible = if (selectedArea == null) {
            items
        } else {
            items.filter { it.task.area == selectedArea }
        }

        val overdue = visible.filter { it.bucket == DueBucket.OVERDUE }.sortedPending()
        val dueToday = visible.filter { it.bucket == DueBucket.DUE_TODAY }.sortedPending()
        val soon = visible.filter { it.bucket == DueBucket.SOON }.sortedPending()
        val isEmpty = items.isEmpty()

        val untilWork = FreeWindowRanker.minutesUntilWorkStart(
            workStartMinutesOfDay = settings.defaultWorkStartMinutesOfDay,
            nowEpochMs = now,
        )

        var fits: List<PendingItem> = emptyList()
        var leftover: Int? = null
        if (windowMinutes != null && visible.isNotEmpty()) {
            val byId = visible.associateBy { it.task.id }
            val ranked = FreeWindowRanker.rank(
                candidates = visible.map {
                    FreeWindowRanker.Candidate(
                        id = it.task.id,
                        title = it.task.title,
                        estimateMinutes = it.task.estimateMinutes,
                        bucket = it.bucket,
                        nextDueAtEpochMs = it.task.nextDueAtEpochMs,
                        snoozedUntilEpochMs = it.task.snoozedUntilEpochMs,
                    )
                },
                availableMinutes = windowMinutes,
            )
            fits = ranked.fits.mapNotNull { byId[it.id] }
            leftover = ranked.leftoverAfterBestMinutes
        }

        return PendingQueueUiState(
            overdue = overdue,
            dueToday = dueToday,
            soon = soon,
            isEmpty = isEmpty,
            activeWindowMinutes = windowMinutes,
            fits = fits,
            leftoverAfterBestMinutes = leftover,
            untilWorkMinutes = untilWork,
            honesty = honesty,
            availableAreas = availableAreas,
            activeArea = selectedArea,
            hasNoPinnedTasks = tasks.isEmpty(),
            startersPinnedHint = hint && items.isEmpty() && tasks.isNotEmpty(),
        )
    }

    companion object {
        fun factory(commands: TaskCommands): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    PendingQueueViewModel(commands) as T
            }
    }
}

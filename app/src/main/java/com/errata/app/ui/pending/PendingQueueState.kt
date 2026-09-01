package com.errata.app.ui.pending

import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.area.AreaFilter
import com.errata.app.domain.area.TaskAreas
import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.due.PendingClassifier
import com.errata.app.domain.freewindow.FreeWindowRanker
import com.errata.app.domain.freewindow.FreeWindowSelection
import com.errata.app.domain.freewindow.remainingMinutes
import java.time.LocalTime
import java.time.ZoneId

object PendingQueueState {

    fun build(
        tasks: List<TaskEntity>,
        settings: SettingsEntity,
        now: Long,
        window: FreeWindowSelection?,
        honesty: PendingHonestyPrompt?,
        requestedArea: String?,
        hint: Boolean,
        formatTime: (LocalTime) -> String = DueCopy::formatTimeDefault,
        zone: ZoneId = ZoneId.systemDefault(),
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
                    formatTime = formatTime,
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

        val usedAreas = TaskAreas.usedAreas(tasks.map { it.area })
        val showAreaFilter = AreaFilter.shouldShow(usedAreas, items.size)
        val availableAreas = if (showAreaFilter) usedAreas else emptyList()
        val selectedArea = requestedArea.takeIf { it != null && it in availableAreas }
        val visible = if (selectedArea == null) {
            items
        } else {
            items.filter { TaskAreas.normalize(it.task.area) == selectedArea }
        }

        val overdue = visible.filter { it.bucket == DueBucket.OVERDUE }.sortedPending()
        val dueToday = visible.filter { it.bucket == DueBucket.DUE_TODAY }.sortedPending()
        val soon = visible.filter { it.bucket == DueBucket.SOON }.sortedPending()

        val untilWork = FreeWindowRanker.minutesUntilWorkStart(
            workStartMinutesOfDay = settings.defaultWorkStartMinutesOfDay,
            nowEpochMs = now,
            zone = zone,
        )
        val windowMinutes = window?.remainingMinutes(now, zone)
        val workStart = settings.defaultWorkStartMinutesOfDay
        val untilWorkSelected =
            window is FreeWindowSelection.UntilClock && window.minutesOfDay == workStart
        val clockWindowPassed =
            window is FreeWindowSelection.UntilClock && windowMinutes == 0
        val customWindowSelected = when (window) {
            is FreeWindowSelection.Duration -> window.minutes !in setOf(15, 30, 45)
            is FreeWindowSelection.UntilClock -> !untilWorkSelected
            null -> false
        }

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
            isEmpty = items.isEmpty(),
            activeWindowMinutes = windowMinutes,
            fits = fits,
            leftoverAfterBestMinutes = leftover,
            untilWorkMinutes = untilWork,
            untilWorkSelected = untilWorkSelected,
            clockWindowPassed = clockWindowPassed,
            workStartMinutesOfDay = workStart,
            customWindowSelected = customWindowSelected,
            honesty = honesty,
            availableAreas = availableAreas,
            activeArea = selectedArea,
            areaFilterEmpty = visible.isEmpty() && selectedArea != null,
            hasNoPinnedTasks = tasks.isEmpty(),
            startersPinnedHint = hint && items.isEmpty() && tasks.isNotEmpty(),
        )
    }
}

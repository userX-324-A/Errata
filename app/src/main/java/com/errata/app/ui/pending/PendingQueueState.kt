package com.errata.app.ui.pending

import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.area.TaskAreas
import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.due.PendingClassifier
import com.errata.app.domain.freewindow.FreeWindowRanker

object PendingQueueState {

    fun build(
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

        val availableAreas = TaskAreas.usedAreas(tasks.map { it.area })
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
            isEmpty = items.isEmpty(),
            activeWindowMinutes = windowMinutes,
            fits = fits,
            leftoverAfterBestMinutes = leftover,
            untilWorkMinutes = untilWork,
            honesty = honesty,
            availableAreas = availableAreas,
            activeArea = selectedArea,
            areaFilterEmpty = visible.isEmpty() && selectedArea != null,
            hasNoPinnedTasks = tasks.isEmpty(),
            startersPinnedHint = hint && items.isEmpty() && tasks.isNotEmpty(),
        )
    }
}

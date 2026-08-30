package com.errata.app.data

import androidx.room.withTransaction
import com.errata.app.data.backup.BACKUP_SCHEMA_VERSION
import com.errata.app.data.backup.BackupFormatException
import com.errata.app.data.backup.CompletionBackup
import com.errata.app.data.backup.ErrataBackup
import com.errata.app.data.backup.SettingsBackup
import com.errata.app.data.backup.TaskBackup
import com.errata.app.data.backup.normalized
import com.errata.app.data.backup.parseAppearanceMode
import com.errata.app.data.backup.parseCadenceMode
import com.errata.app.data.backup.parseDefaultReminderKind
import com.errata.app.data.backup.parseScheduleKind
import com.errata.app.data.local.CompletionEntity
import com.errata.app.data.local.ErrataDatabase
import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.cadence.TaskCycle
import com.errata.app.domain.estimate.EstimateAdjuster
import com.errata.app.domain.history.HistoryGlance
import com.errata.app.domain.history.HistoryRetention
import com.errata.app.domain.reminders.ReminderPolicy
import com.errata.app.domain.starter.StarterCatalog
import com.errata.app.domain.starter.StarterSpec
import com.errata.app.domain.sync.StableIds
import com.errata.app.domain.sync.SyncCompletion
import com.errata.app.domain.sync.SyncMerge
import com.errata.app.domain.sync.SyncSettings
import com.errata.app.domain.sync.SyncSnapshot
import com.errata.app.domain.sync.SyncTask
import com.errata.app.reminders.ReminderActionGuard
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskRepository(
    private val db: ErrataDatabase,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val tasks = db.taskDao()
    private val completions = db.completionDao()
    private val settings = db.settingsDao()

    @Volatile
    private var lastPruneEpochDay: Long? = null

    fun observeActiveTasks(): Flow<List<TaskEntity>> = tasks.observeActiveTasks()

    fun observeSettings(): Flow<SettingsEntity?> = settings.observe()

    suspend fun getTask(id: Long): TaskEntity? = tasks.getById(id)

    suspend fun completionsFor(taskId: Long): List<CompletionEntity> =
        completions.forTaskNewest(taskId, HistoryGlance.MAX_SAMPLES)

    suspend fun listSchedulableTasks(): List<TaskEntity> = tasks.listSchedulable()

    /**
     * Insert or update. For new tasks ([TaskEntity.id] == 0), sets [TaskEntity.anchorEpochDay]
     * from [TaskEntity.nextDueAtEpochMs] if the caller left anchor as the due day (recommended).
     */
    suspend fun upsert(task: TaskEntity): Long {
        val now = System.currentTimeMillis()
        val normalized = if (task.id == 0L) {
            val anchor = task.anchorEpochDay.takeIf { it != 0L }
                ?: CadenceCalculator.epochDayOf(task.nextDueAtEpochMs, zone)
            task.copy(
                uuid = StableIds.orNew(task.uuid),
                anchorEpochDay = anchor,
                createdAtEpochMs = task.createdAtEpochMs.takeIf { it != 0L } ?: now,
                updatedAtEpochMs = now,
            )
        } else {
            val previous = tasks.getById(task.id)
            task.copy(
                uuid = StableIds.orNew(task.uuid.ifBlank { previous?.uuid.orEmpty() }),
                updatedAtEpochMs = now,
            )
        }

        return if (normalized.id == 0L) {
            tasks.upsert(normalized)
        } else {
            tasks.update(normalized)
            normalized.id
        }
    }

    /**
     * Insert new tasks from the empty-state starter pack. Empty [specs] is a no-op.
     * Caller should reschedule reminders once after a non-empty pin.
     */
    suspend fun pinStarters(
        specs: List<StarterSpec>,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Int {
        if (specs.isEmpty()) return 0
        val settings = getSettings()
        val now = nowEpochMs
        val entities = specs.map { spec ->
            val drafted = StarterCatalog.materialize(
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
            val anchor = drafted.anchorEpochDay.takeIf { it != 0L }
                ?: CadenceCalculator.epochDayOf(drafted.nextDueAtEpochMs, zone)
            drafted.copy(
                id = 0,
                uuid = StableIds.new(),
                anchorEpochDay = anchor,
                createdAtEpochMs = drafted.createdAtEpochMs.takeIf { it != 0L } ?: now,
                updatedAtEpochMs = now,
            )
        }
        tasks.upsertAll(entities)
        return entities.size
    }

    suspend fun complete(
        taskId: Long,
        completedAtEpochMs: Long = System.currentTimeMillis(),
        expectedNextDueAtEpochMs: Long? = null,
    ): Boolean {
        val task = tasks.getById(taskId) ?: return false
        if (
            !ReminderActionGuard.shouldComplete(
                task.nextDueAtEpochMs,
                expectedNextDueAtEpochMs,
                isPaused = task.isPaused,
                isArchived = task.isArchived,
            )
        ) {
            return false
        }
        val scheduledDue = task.nextDueAtEpochMs
        val nextDue = CadenceCalculator.nextDueAfterCompletion(
            mode = task.cadenceMode,
            intervalDays = task.intervalDays,
            completedAtEpochMs = completedAtEpochMs,
            scheduledDueAtEpochMs = scheduledDue,
            anchorEpochDay = task.anchorEpochDay,
            zone = zone,
            scheduleKind = task.scheduleKind,
            weekdaysMask = task.weekdaysMask,
            monthDay = task.monthDay,
            weekdayOrdinal = task.weekdayOrdinal,
            yearMonthsMask = task.yearMonthsMask,
            seasonMask = task.seasonMask,
        )
        db.withTransaction {
            completions.insert(
                CompletionEntity(
                    uuid = StableIds.new(),
                    taskId = taskId,
                    completedAtEpochMs = completedAtEpochMs,
                    scheduledDueAtEpochMs = scheduledDue,
                    estimateMinutesAtCompletion = task.estimateMinutes,
                ),
            )
            tasks.update(
                task.copy(
                    lastCompletedAtEpochMs = completedAtEpochMs,
                    nextDueAtEpochMs = nextDue,
                    snoozedUntilEpochMs = null,
                    updatedAtEpochMs = completedAtEpochMs,
                ),
            )
        }
        pruneHistory()
        return true
    }

    suspend fun snooze(
        taskId: Long,
        untilEpochMs: Long,
        expectedNextDueAtEpochMs: Long? = null,
    ): Boolean {
        val task = tasks.getById(taskId) ?: return false
        if (
            !ReminderActionGuard.shouldSnooze(
                task.nextDueAtEpochMs,
                expectedNextDueAtEpochMs,
                isPaused = task.isPaused,
                isArchived = task.isArchived,
            )
        ) {
            return false
        }
        tasks.update(
            task.copy(
                snoozedUntilEpochMs = untilEpochMs,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        return true
    }

    suspend fun updateEstimateMinutes(taskId: Long, estimateMinutes: Int) {
        val task = tasks.getById(taskId) ?: return
        val minutes = estimateMinutes.coerceIn(1, EstimateAdjuster.MAX_ESTIMATE_MINUTES)
        tasks.update(
            task.copy(
                estimateMinutes = minutes,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    /**
     * Abandon this cycle: advance next due without a completion record.
     */
    suspend fun skip(taskId: Long, nowEpochMs: Long = System.currentTimeMillis()) {
        val task = tasks.getById(taskId) ?: return
        val nextDue = CadenceCalculator.nextDueAfterSkip(
            mode = task.cadenceMode,
            intervalDays = task.intervalDays,
            scheduledDueAtEpochMs = task.nextDueAtEpochMs,
            anchorEpochDay = task.anchorEpochDay,
            nowEpochMs = nowEpochMs,
            zone = zone,
            scheduleKind = task.scheduleKind,
            weekdaysMask = task.weekdaysMask,
            monthDay = task.monthDay,
            weekdayOrdinal = task.weekdayOrdinal,
            yearMonthsMask = task.yearMonthsMask,
            seasonMask = task.seasonMask,
        )
        tasks.update(TaskCycle.skipped(task, nextDue, nowEpochMs))
    }

    suspend fun setPaused(taskId: Long, paused: Boolean) {
        val task = tasks.getById(taskId) ?: return
        tasks.update(
            task.copy(
                isPaused = paused,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun archive(taskId: Long) {
        val task = tasks.getById(taskId) ?: return
        tasks.update(
            task.copy(
                isArchived = true,
                isPaused = false,
                snoozedUntilEpochMs = null,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getSettings(): SettingsEntity {
        db.ensureSettings()
        return settings.get() ?: SettingsEntity()
    }

    suspend fun updateSettings(entity: SettingsEntity) {
        val previous = getSettings()
        val sharedChanged = previous.sharedEquals(entity).not()
        val now = System.currentTimeMillis()
        settings.upsert(
            entity.copy(
                id = 1,
                updatedAtEpochMs = if (sharedChanged) now else previous.updatedAtEpochMs,
            ),
        )
    }

    suspend fun defaultCadenceMode(): CadenceMode = getSettings().defaultCadenceMode

    suspend fun exportSnapshot(): ErrataBackup = withContext(Dispatchers.IO) {
        val settingsEntity = getSettings()
        ErrataBackup(
            exportedAtEpochMs = System.currentTimeMillis(),
            settings = settingsEntity.toBackup(),
            tasks = tasks.listAll().map { it.toBackup() },
            completions = completions.listAll().map { it.toBackup() },
        )
    }

    /** Wipe local data and replace with [backup]. */
    suspend fun importReplace(backup: ErrataBackup) {
        if (backup.schemaVersion !in 1..BACKUP_SCHEMA_VERSION) {
            throw BackupFormatException(
                "Unsupported backup version ${backup.schemaVersion}",
            )
        }
        val normalized = backup.normalized()
        val now = System.currentTimeMillis()
        db.withTransaction {
            val previous = settings.get()
            completions.deleteAll()
            tasks.deleteAll()
            settings.deleteAll()
            val imported = normalized.settings.toEntity()
            val follow = SyncMerge.marksAfterLocalReplace(
                previousTasksGeneration = previous?.tasksGeneration ?: 0,
                previousHistoryGeneration = previous?.historyGeneration ?: 0,
                importedTasksGeneration = imported.tasksGeneration,
                importedHistoryGeneration = imported.historyGeneration,
                importedSettingsUpdatedAt = imported.updatedAtEpochMs,
                nowEpochMs = now,
            )
            settings.upsert(
                imported.copy(
                    tasksGeneration = follow.tasksGeneration,
                    tasksResetAtEpochMs = follow.tasksResetAtEpochMs,
                    historyGeneration = follow.historyGeneration,
                    historyPurgedAtEpochMs = follow.historyPurgedAtEpochMs,
                    updatedAtEpochMs = follow.settingsUpdatedAtEpochMs,
                ),
            )
            if (normalized.tasks.isNotEmpty()) {
                tasks.upsertAll(normalized.tasks.map { it.toEntity() })
            }
            if (normalized.completions.isNotEmpty()) {
                completions.insertAll(normalized.completions.map { it.toEntity() })
            }
        }
        db.ensureSettings()
    }

    suspend fun pruneHistory(
        nowEpochMs: Long = System.currentTimeMillis(),
        force: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        val days = getSettings().historyRetentionDays
        val today = CadenceCalculator.epochDayOf(nowEpochMs, zone)
        if (!HistoryRetention.shouldRun(days, lastPruneEpochDay, today, force)) {
            return@withContext
        }
        val cutoff = HistoryRetention.cutoffEpochMs(nowEpochMs, days)
        if (cutoff != null) {
            completions.taskIdsOlderThan(cutoff).forEach { taskId ->
                completions.deleteExpiredBeyondKeep(
                    taskId = taskId,
                    cutoff = cutoff,
                    keepCount = HistoryRetention.KEEP_PER_TASK,
                )
            }
        }
        lastPruneEpochDay = today
    }

    suspend fun purgeHistory() {
        val now = System.currentTimeMillis()
        val current = getSettings()
        db.withTransaction {
            completions.deleteAll()
            settings.upsert(
                current.copy(
                    historyGeneration = current.historyGeneration + 1,
                    historyPurgedAtEpochMs = now,
                ),
            )
        }
    }

    suspend fun resetTasks(alsoClearCloud: Boolean = false) {
        val now = System.currentTimeMillis()
        val current = getSettings()
        db.withTransaction {
            completions.deleteAll()
            tasks.deleteAll()
            if (alsoClearCloud) {
                settings.upsert(
                    current.copy(
                        tasksGeneration = current.tasksGeneration + 1,
                        tasksResetAtEpochMs = now,
                    ),
                )
            }
        }
    }

    suspend fun toSyncSnapshot(nowEpochMs: Long = System.currentTimeMillis()): SyncSnapshot {
        val settingsEntity = getSettings()
        val taskRows = tasks.listAll()
        val idToUuid = taskRows.associate { it.id to it.uuid }
        return SyncSnapshot(
            writtenAtEpochMs = nowEpochMs,
            tasksGeneration = settingsEntity.tasksGeneration,
            tasksResetAtEpochMs = settingsEntity.tasksResetAtEpochMs,
            historyGeneration = settingsEntity.historyGeneration,
            historyPurgedAtEpochMs = settingsEntity.historyPurgedAtEpochMs,
            settings = settingsEntity.toSyncSettings(),
            tasks = taskRows.map { it.toSyncTask() },
            completions = completions.listAll().mapNotNull { row ->
                val taskUuid = idToUuid[row.taskId] ?: return@mapNotNull null
                row.toSyncCompletion(taskUuid)
            },
        )
    }

    suspend fun applySyncSnapshot(snapshot: SyncSnapshot) {
        val current = getSettings()
        val pruned = SyncMerge.pruneCompletions(snapshot, System.currentTimeMillis())
        db.withTransaction {
            val existing = tasks.listAll().associateBy { it.uuid }
            for (remote in pruned.tasks) {
                val local = existing[remote.uuid]
                val entity = remote.toEntity(localId = local?.id ?: 0L)
                if (local == null) {
                    tasks.upsert(entity.copy(id = 0))
                } else {
                    tasks.update(entity)
                }
            }
            val keep = pruned.tasks.map { it.uuid }
            if (keep.isEmpty()) {
                tasks.deleteAll()
            } else {
                tasks.deleteWhereUuidNotIn(keep)
            }
            completions.deleteAll()
            val uuidToId = tasks.listAll().associate { it.uuid to it.id }
            val rows = pruned.completions.mapNotNull { row ->
                val taskId = uuidToId[row.taskUuid] ?: return@mapNotNull null
                row.toEntity(taskId)
            }
            if (rows.isNotEmpty()) {
                completions.insertAll(rows)
            }
            settings.upsert(
                current.copy(
                    defaultCadenceMode = parseCadenceMode(pruned.settings.defaultCadenceMode),
                    defaultReminderKind = parseDefaultReminderKind(
                        pruned.settings.defaultReminderKind,
                    ),
                    defaultReminderMinutesOfDay = pruned.settings.defaultReminderMinutesOfDay,
                    defaultWorkStartMinutesOfDay = pruned.settings.defaultWorkStartMinutesOfDay,
                    soonHorizonDays = pruned.settings.soonHorizonDays,
                    digestEnabled = pruned.settings.digestEnabled,
                    historyRetentionDays = pruned.settings.historyRetentionDays,
                    updatedAtEpochMs = pruned.settings.updatedAtEpochMs,
                    historyGeneration = pruned.historyGeneration,
                    historyPurgedAtEpochMs = pruned.historyPurgedAtEpochMs,
                    tasksGeneration = pruned.tasksGeneration,
                    tasksResetAtEpochMs = pruned.tasksResetAtEpochMs,
                ),
            )
        }
        pruneHistory()
    }
}

private fun SettingsEntity.sharedEquals(other: SettingsEntity): Boolean =
    defaultCadenceMode == other.defaultCadenceMode &&
        defaultReminderKind == other.defaultReminderKind &&
        defaultReminderMinutesOfDay == other.defaultReminderMinutesOfDay &&
        defaultWorkStartMinutesOfDay == other.defaultWorkStartMinutesOfDay &&
        soonHorizonDays == other.soonHorizonDays &&
        digestEnabled == other.digestEnabled &&
        historyRetentionDays == other.historyRetentionDays

private fun SettingsEntity.toBackup() = SettingsBackup(
    defaultCadenceMode = defaultCadenceMode.name,
    defaultReminderKind = defaultReminderKind.name,
    defaultReminderMinutesOfDay = defaultReminderMinutesOfDay,
    defaultWorkStartMinutesOfDay = defaultWorkStartMinutesOfDay,
    soonHorizonDays = soonHorizonDays,
    appearanceMode = appearanceMode.name,
    digestEnabled = digestEnabled,
    historyRetentionDays = historyRetentionDays,
    updatedAtEpochMs = updatedAtEpochMs,
    historyGeneration = historyGeneration,
    historyPurgedAtEpochMs = historyPurgedAtEpochMs,
    tasksGeneration = tasksGeneration,
    tasksResetAtEpochMs = tasksResetAtEpochMs,
)

private fun SettingsBackup.toEntity() = SettingsEntity(
    id = 1,
    defaultCadenceMode = parseCadenceMode(defaultCadenceMode),
    defaultReminderKind = parseDefaultReminderKind(defaultReminderKind),
    defaultReminderMinutesOfDay = defaultReminderMinutesOfDay,
    defaultWorkStartMinutesOfDay = defaultWorkStartMinutesOfDay,
    soonHorizonDays = soonHorizonDays,
    appearanceMode = parseAppearanceMode(appearanceMode),
    digestEnabled = digestEnabled,
    historyRetentionDays = historyRetentionDays,
    updatedAtEpochMs = updatedAtEpochMs,
    historyGeneration = historyGeneration,
    historyPurgedAtEpochMs = historyPurgedAtEpochMs,
    tasksGeneration = tasksGeneration,
    tasksResetAtEpochMs = tasksResetAtEpochMs,
)

private fun SettingsEntity.toSyncSettings() = SyncSettings(
    updatedAtEpochMs = updatedAtEpochMs,
    defaultCadenceMode = defaultCadenceMode.name,
    defaultReminderKind = defaultReminderKind.name,
    defaultReminderMinutesOfDay = defaultReminderMinutesOfDay,
    defaultWorkStartMinutesOfDay = defaultWorkStartMinutesOfDay,
    soonHorizonDays = soonHorizonDays,
    digestEnabled = digestEnabled,
    historyRetentionDays = historyRetentionDays,
)

private fun TaskEntity.toBackup() = TaskBackup(
    id = id,
    uuid = uuid,
    title = title,
    notes = notes,
    estimateMinutes = estimateMinutes,
    intervalDays = intervalDays,
    scheduleKind = scheduleKind.name,
    weekdaysMask = weekdaysMask,
    monthDay = monthDay,
    weekdayOrdinal = weekdayOrdinal,
    yearMonthsMask = yearMonthsMask,
    seasonMask = seasonMask,
    cadenceMode = cadenceMode.name,
    anchorEpochDay = anchorEpochDay,
    nextDueAtEpochMs = nextDueAtEpochMs,
    lastCompletedAtEpochMs = lastCompletedAtEpochMs,
    reminderMinutesOfDay = reminderMinutesOfDay,
    snoozedUntilEpochMs = snoozedUntilEpochMs,
    area = area,
    isPaused = isPaused,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun TaskBackup.toEntity() = TaskEntity(
    id = id,
    uuid = StableIds.orNew(uuid),
    title = title,
    notes = notes,
    estimateMinutes = estimateMinutes,
    intervalDays = intervalDays,
    scheduleKind = parseScheduleKind(scheduleKind),
    weekdaysMask = weekdaysMask,
    monthDay = monthDay,
    weekdayOrdinal = weekdayOrdinal,
    yearMonthsMask = yearMonthsMask,
    seasonMask = seasonMask,
    cadenceMode = parseCadenceMode(cadenceMode),
    anchorEpochDay = anchorEpochDay,
    nextDueAtEpochMs = nextDueAtEpochMs,
    lastCompletedAtEpochMs = lastCompletedAtEpochMs,
    reminderMinutesOfDay = reminderMinutesOfDay,
    snoozedUntilEpochMs = snoozedUntilEpochMs,
    area = area,
    isPaused = isPaused,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun TaskEntity.toSyncTask() = SyncTask(
    uuid = uuid,
    title = title,
    notes = notes,
    estimateMinutes = estimateMinutes,
    intervalDays = intervalDays,
    scheduleKind = scheduleKind.name,
    weekdaysMask = weekdaysMask,
    monthDay = monthDay,
    weekdayOrdinal = weekdayOrdinal,
    yearMonthsMask = yearMonthsMask,
    seasonMask = seasonMask,
    cadenceMode = cadenceMode.name,
    anchorEpochDay = anchorEpochDay,
    nextDueAtEpochMs = nextDueAtEpochMs,
    lastCompletedAtEpochMs = lastCompletedAtEpochMs,
    reminderMinutesOfDay = reminderMinutesOfDay,
    snoozedUntilEpochMs = snoozedUntilEpochMs,
    area = area,
    isPaused = isPaused,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun SyncTask.toEntity(localId: Long) = TaskEntity(
    id = localId,
    uuid = uuid,
    title = title,
    notes = notes,
    estimateMinutes = estimateMinutes,
    intervalDays = intervalDays,
    scheduleKind = parseScheduleKind(scheduleKind),
    weekdaysMask = weekdaysMask,
    monthDay = monthDay,
    weekdayOrdinal = weekdayOrdinal,
    yearMonthsMask = yearMonthsMask,
    seasonMask = seasonMask,
    cadenceMode = parseCadenceMode(cadenceMode),
    anchorEpochDay = anchorEpochDay,
    nextDueAtEpochMs = nextDueAtEpochMs,
    lastCompletedAtEpochMs = lastCompletedAtEpochMs,
    reminderMinutesOfDay = reminderMinutesOfDay,
    snoozedUntilEpochMs = snoozedUntilEpochMs,
    area = area,
    isPaused = isPaused,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun CompletionEntity.toBackup() = CompletionBackup(
    id = id,
    uuid = uuid,
    taskId = taskId,
    completedAtEpochMs = completedAtEpochMs,
    scheduledDueAtEpochMs = scheduledDueAtEpochMs,
    estimateMinutesAtCompletion = estimateMinutesAtCompletion,
)

private fun CompletionBackup.toEntity() = CompletionEntity(
    id = id,
    uuid = StableIds.orNew(uuid),
    taskId = taskId,
    completedAtEpochMs = completedAtEpochMs,
    scheduledDueAtEpochMs = scheduledDueAtEpochMs,
    estimateMinutesAtCompletion = estimateMinutesAtCompletion,
)

private fun CompletionEntity.toSyncCompletion(taskUuid: String) = SyncCompletion(
    uuid = uuid,
    taskUuid = taskUuid,
    completedAtEpochMs = completedAtEpochMs,
    scheduledDueAtEpochMs = scheduledDueAtEpochMs,
    estimateMinutesAtCompletion = estimateMinutesAtCompletion,
)

private fun SyncCompletion.toEntity(taskId: Long) = CompletionEntity(
    id = 0,
    uuid = uuid,
    taskId = taskId,
    completedAtEpochMs = completedAtEpochMs,
    scheduledDueAtEpochMs = scheduledDueAtEpochMs,
    estimateMinutesAtCompletion = estimateMinutesAtCompletion,
)


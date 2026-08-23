package com.errata.app.data

import androidx.room.withTransaction
import com.errata.app.data.backup.BACKUP_SCHEMA_VERSION
import com.errata.app.data.backup.BackupFormatException
import com.errata.app.data.backup.CompletionBackup
import com.errata.app.data.backup.ErrataBackup
import com.errata.app.data.backup.SettingsBackup
import com.errata.app.data.backup.TaskBackup
import com.errata.app.data.backup.parseCadenceMode
import com.errata.app.data.local.CompletionEntity
import com.errata.app.data.local.ErrataDatabase
import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.cadence.CadenceCalculator
import com.errata.app.domain.cadence.CadenceMode
import com.errata.app.domain.estimate.EstimateAdjuster
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val db: ErrataDatabase,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {
    private val tasks = db.taskDao()
    private val completions = db.completionDao()
    private val settings = db.settingsDao()

    fun observeActiveTasks(): Flow<List<TaskEntity>> = tasks.observeActiveTasks()

    fun observeSettings(): Flow<SettingsEntity?> = settings.observe()

    suspend fun getTask(id: Long): TaskEntity? = tasks.getById(id)

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
                anchorEpochDay = anchor,
                createdAtEpochMs = task.createdAtEpochMs.takeIf { it != 0L } ?: now,
                updatedAtEpochMs = now,
            )
        } else {
            task.copy(updatedAtEpochMs = now)
        }

        return if (normalized.id == 0L) {
            tasks.upsert(normalized)
        } else {
            tasks.update(normalized)
            normalized.id
        }
    }

    suspend fun complete(taskId: Long, completedAtEpochMs: Long = System.currentTimeMillis()) {
        val task = tasks.getById(taskId) ?: return
        val scheduledDue = task.nextDueAtEpochMs

        completions.insert(
            CompletionEntity(
                taskId = taskId,
                completedAtEpochMs = completedAtEpochMs,
                scheduledDueAtEpochMs = scheduledDue,
                estimateMinutesAtCompletion = task.estimateMinutes,
            ),
        )

        val nextDue = CadenceCalculator.nextDueAfterCompletion(
            mode = task.cadenceMode,
            intervalDays = task.intervalDays,
            completedAtEpochMs = completedAtEpochMs,
            scheduledDueAtEpochMs = scheduledDue,
            anchorEpochDay = task.anchorEpochDay,
            zone = zone,
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

    suspend fun snooze(taskId: Long, untilEpochMs: Long) {
        val task = tasks.getById(taskId) ?: return
        tasks.update(
            task.copy(
                snoozedUntilEpochMs = untilEpochMs,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
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
        )
        tasks.update(
            task.copy(
                nextDueAtEpochMs = nextDue,
                snoozedUntilEpochMs = null,
                updatedAtEpochMs = nowEpochMs,
            ),
        )
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
        settings.upsert(entity.copy(id = 1))
    }

    suspend fun defaultCadenceMode(): CadenceMode = getSettings().defaultCadenceMode

    suspend fun exportSnapshot(): ErrataBackup {
        val settingsEntity = getSettings()
        return ErrataBackup(
            exportedAtEpochMs = System.currentTimeMillis(),
            settings = settingsEntity.toBackup(),
            tasks = tasks.listAll().map { it.toBackup() },
            completions = completions.listAll().map { it.toBackup() },
        )
    }

    /** Wipe local data and replace with [backup]. */
    suspend fun importReplace(backup: ErrataBackup) {
        if (backup.schemaVersion != BACKUP_SCHEMA_VERSION) {
            throw BackupFormatException(
                "Unsupported backup version ${backup.schemaVersion}",
            )
        }
        db.withTransaction {
            completions.deleteAll()
            tasks.deleteAll()
            settings.deleteAll()
            settings.upsert(backup.settings.toEntity())
            if (backup.tasks.isNotEmpty()) {
                tasks.upsertAll(backup.tasks.map { it.toEntity() })
            }
            if (backup.completions.isNotEmpty()) {
                completions.insertAll(backup.completions.map { it.toEntity() })
            }
        }
        db.ensureSettings()
    }
}

private fun SettingsEntity.toBackup() = SettingsBackup(
    defaultCadenceMode = defaultCadenceMode.name,
    defaultReminderMinutesOfDay = defaultReminderMinutesOfDay,
    defaultWorkStartMinutesOfDay = defaultWorkStartMinutesOfDay,
    soonHorizonDays = soonHorizonDays,
)

private fun SettingsBackup.toEntity() = SettingsEntity(
    id = 1,
    defaultCadenceMode = parseCadenceMode(defaultCadenceMode),
    defaultReminderMinutesOfDay = defaultReminderMinutesOfDay,
    defaultWorkStartMinutesOfDay = defaultWorkStartMinutesOfDay,
    soonHorizonDays = soonHorizonDays,
)

private fun TaskEntity.toBackup() = TaskBackup(
    id = id,
    title = title,
    notes = notes,
    estimateMinutes = estimateMinutes,
    intervalDays = intervalDays,
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
    title = title,
    notes = notes,
    estimateMinutes = estimateMinutes,
    intervalDays = intervalDays,
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
    taskId = taskId,
    completedAtEpochMs = completedAtEpochMs,
    scheduledDueAtEpochMs = scheduledDueAtEpochMs,
    estimateMinutesAtCompletion = estimateMinutesAtCompletion,
)

private fun CompletionBackup.toEntity() = CompletionEntity(
    id = id,
    taskId = taskId,
    completedAtEpochMs = completedAtEpochMs,
    scheduledDueAtEpochMs = scheduledDueAtEpochMs,
    estimateMinutesAtCompletion = estimateMinutesAtCompletion,
)

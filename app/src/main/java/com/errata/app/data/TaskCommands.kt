package com.errata.app.data

import com.errata.app.data.backup.BackupCodec
import com.errata.app.data.backup.ErrataBackup
import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.reminders.ReminderScheduler

/**
 * Mutating facade: repository write then reminder reschedule.
 */
class TaskCommands(
    private val repository: TaskRepository,
    private val scheduler: ReminderScheduler,
) {
    val observeActiveTasks get() = repository.observeActiveTasks()
    val observeSettings get() = repository.observeSettings()

    suspend fun getTask(id: Long) = repository.getTask(id)
    suspend fun getSettings() = repository.getSettings()
    suspend fun defaultCadenceMode() = repository.defaultCadenceMode()

    /**
     * Persist settings. Reschedules all reminders when the global default reminder time changes.
     */
    suspend fun updateSettings(entity: SettingsEntity) {
        val previous = repository.getSettings()
        repository.updateSettings(entity)
        if (previous.defaultReminderMinutesOfDay != entity.defaultReminderMinutesOfDay) {
            scheduler.rescheduleAll()
        }
    }

    suspend fun upsert(task: TaskEntity): Long {
        val id = repository.upsert(task)
        scheduler.rescheduleTask(if (task.id == 0L) id else task.id)
        return id
    }

    suspend fun complete(taskId: Long, completedAtEpochMs: Long = System.currentTimeMillis()) {
        repository.complete(taskId, completedAtEpochMs)
        scheduler.rescheduleTask(taskId)
    }

    suspend fun snooze(taskId: Long, untilEpochMs: Long) {
        repository.snooze(taskId, untilEpochMs)
        scheduler.rescheduleTask(taskId)
    }

    suspend fun updateEstimateMinutes(taskId: Long, estimateMinutes: Int) {
        repository.updateEstimateMinutes(taskId, estimateMinutes)
    }

    suspend fun skip(taskId: Long) {
        repository.skip(taskId)
        scheduler.rescheduleTask(taskId)
    }

    suspend fun pause(taskId: Long) {
        repository.setPaused(taskId, paused = true)
        scheduler.rescheduleTask(taskId)
    }

    suspend fun resume(taskId: Long) {
        repository.setPaused(taskId, paused = false)
        scheduler.rescheduleTask(taskId)
    }

    suspend fun archive(taskId: Long) {
        repository.archive(taskId)
        scheduler.rescheduleTask(taskId)
    }

    suspend fun exportJson(): String = BackupCodec.encode(repository.exportSnapshot())

    suspend fun importJsonReplace(json: String) {
        val backup: ErrataBackup = BackupCodec.decode(json)
        repository.importReplace(backup)
        scheduler.rescheduleAll()
    }
}

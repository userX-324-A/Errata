package com.errata.app.data

import com.errata.app.data.backup.BackupCodec
import com.errata.app.data.backup.ErrataBackup
import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.starter.StarterSpec
import com.errata.app.reminders.ReminderScheduler
import com.errata.app.sync.SyncScheduler
import com.errata.app.widget.WidgetUpdater

/**
 * Mutating facade: repository write then reminder reschedule.
 */
class TaskCommands(
    private val repository: TaskRepository,
    private val scheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
    private val syncScheduler: SyncScheduler? = null,
) {
    val observeActiveTasks get() = repository.observeActiveTasks()
    val observeSettings get() = repository.observeSettings()

    suspend fun getTask(id: Long) = repository.getTask(id)
    suspend fun completionsFor(taskId: Long) = repository.completionsFor(taskId)
    suspend fun getSettings() = repository.getSettings()
    suspend fun defaultCadenceMode() = repository.defaultCadenceMode()

    /**
     * Persist settings. Reschedules reminders when digest or default reminder time changes.
     */
    suspend fun updateSettings(entity: SettingsEntity) {
        val previous = repository.getSettings()
        repository.updateSettings(entity)
        val reminderChanged =
            previous.defaultReminderMinutesOfDay != entity.defaultReminderMinutesOfDay
        val digestChanged = previous.digestEnabled != entity.digestEnabled
        if (reminderChanged || digestChanged) {
            scheduler.rescheduleAll()
        }
        if (previous.historyRetentionDays != entity.historyRetentionDays) {
            repository.pruneHistory()
        }
    }

    suspend fun upsert(task: TaskEntity): Long {
        val id = repository.upsert(task)
        scheduler.rescheduleTask(if (task.id == 0L) id else task.id)
        afterWrite()
        return id
    }

    /** Pin starter specs as real tasks, then one reminder/widget pass. */
    suspend fun pinStarters(specs: List<StarterSpec>): Int {
        val pinned = repository.pinStarters(specs)
        if (pinned > 0) {
            scheduler.rescheduleAll()
        }
        return pinned
    }

    suspend fun complete(taskId: Long, completedAtEpochMs: Long = System.currentTimeMillis()) {
        repository.complete(taskId, completedAtEpochMs)
        scheduler.rescheduleTask(taskId)
        afterWrite()
    }

    suspend fun snooze(taskId: Long, untilEpochMs: Long) {
        repository.snooze(taskId, untilEpochMs)
        scheduler.rescheduleTask(taskId)
        afterWrite()
    }

    suspend fun updateEstimateMinutes(taskId: Long, estimateMinutes: Int) {
        repository.updateEstimateMinutes(taskId, estimateMinutes)
        afterWrite()
    }

    suspend fun skip(taskId: Long) {
        repository.skip(taskId)
        scheduler.rescheduleTask(taskId)
        afterWrite()
    }

    suspend fun pause(taskId: Long) {
        repository.setPaused(taskId, paused = true)
        scheduler.rescheduleTask(taskId)
        afterWrite()
    }

    suspend fun resume(taskId: Long) {
        repository.setPaused(taskId, paused = false)
        scheduler.rescheduleTask(taskId)
        afterWrite()
    }

    suspend fun archive(taskId: Long) {
        repository.archive(taskId)
        scheduler.rescheduleTask(taskId)
        afterWrite()
    }

    suspend fun exportJson(): String = BackupCodec.encode(repository.exportSnapshot())

    suspend fun importJsonReplace(json: String) {
        val backup: ErrataBackup = BackupCodec.decode(json)
        repository.importReplace(backup)
        scheduler.rescheduleAll()
        afterWrite()
    }

    suspend fun pruneHistory() = repository.pruneHistory()

    suspend fun purgeHistory() {
        repository.purgeHistory()
        afterWrite()
    }

    suspend fun resetTasks(alsoClearCloud: Boolean = false) {
        repository.resetTasks(alsoClearCloud)
        scheduler.rescheduleAll()
        afterWrite()
    }

    suspend fun rescheduleReminders() = scheduler.rescheduleAll()

    private suspend fun afterWrite() {
        widgetUpdater.refresh()
        syncScheduler?.requestDebounced()
    }
}

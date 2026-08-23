package com.errata.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.errata.app.data.TaskRepository
import com.errata.app.data.local.SettingsEntity
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.digest.DigestPlanner
import com.errata.app.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderScheduler(
    private val context: Context,
    private val repository: TaskRepository,
    private val widgetUpdater: WidgetUpdater,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun rescheduleAll() = withContext(Dispatchers.IO) {
        val settings = repository.getSettings()
        val tasks = repository.listSchedulableTasks()
        if (settings.digestEnabled) {
            scheduleDigest(settings.defaultReminderMinutesOfDay)
        } else {
            cancelDigest()
        }
        tasks.forEach { task ->
            scheduleTask(task, settings)
        }
        widgetUpdater.refresh()
    }

    suspend fun rescheduleTask(taskId: Long) = withContext(Dispatchers.IO) {
        val task = repository.getTask(taskId) ?: run {
            cancel(taskId)
            return@withContext
        }
        if (task.isArchived || task.isPaused) {
            cancel(taskId)
            return@withContext
        }
        val settings = repository.getSettings()
        scheduleTask(task, settings)
    }

    suspend fun onDigestFired() = withContext(Dispatchers.IO) {
        val settings = repository.getSettings()
        if (!settings.digestEnabled) {
            cancelDigest()
            return@withContext
        }
        val now = System.currentTimeMillis()
        val members = DigestPlanner.members(
            candidates = repository.listSchedulableTasks().map { it.toDigestCandidate() },
            defaultReminderMinutesOfDay = settings.defaultReminderMinutesOfDay,
            nowEpochMs = now,
        )
        when (members.size) {
            0 -> Unit
            1 -> {
                val task = repository.getTask(members.single().id)
                if (task != null) {
                    NotificationHelper.showDueReminder(context, task)
                }
            }
            else -> NotificationHelper.showDigest(
                context,
                count = members.size,
                totalMinutes = DigestPlanner.totalMinutes(members),
            )
        }
        scheduleDigest(settings.defaultReminderMinutesOfDay, nowEpochMs = now, afterFire = true)
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(alarmPendingIntent(taskId))
    }

    private fun scheduleTask(task: TaskEntity, settings: SettingsEntity) {
        cancel(task.id)
        val now = System.currentTimeMillis()
        if (settings.digestEnabled &&
            DigestPlanner.coveredByDigest(
                candidate = task.toDigestCandidate(),
                defaultReminderMinutesOfDay = settings.defaultReminderMinutesOfDay,
                nowEpochMs = now,
            )
        ) {
            return
        }
        val fireAt = ReminderTimeCalculator.nextFireEpochMs(
            task,
            settings.defaultReminderMinutesOfDay,
            nowEpochMs = now,
        ) ?: return
        setWakeup(fireAt, alarmPendingIntent(task.id))
    }

    private fun scheduleDigest(
        defaultReminderMinutes: Int,
        nowEpochMs: Long = System.currentTimeMillis(),
        afterFire: Boolean = false,
    ) {
        val fireAt = if (afterFire) {
            DigestPlanner.nextDigestAfterFire(
                defaultReminderMinutesOfDay = defaultReminderMinutes,
                nowEpochMs = nowEpochMs,
            )
        } else {
            DigestPlanner.nextDigestEpochMs(
                defaultReminderMinutesOfDay = defaultReminderMinutes,
                nowEpochMs = nowEpochMs,
            )
        }
        setWakeup(fireAt, digestPendingIntent())
    }

    private fun cancelDigest() {
        alarmManager.cancel(digestPendingIntent())
    }

    private fun setWakeup(fireAt: Long, pi: PendingIntent) {
        if (canExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pi)
        }
    }

    private fun canExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    private fun alarmPendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun digestPendingIntent(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_DIGEST
        }
        return PendingIntent.getBroadcast(
            context,
            DIGEST_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_ALARM = "com.errata.app.action.REMINDER_ALARM"
        const val ACTION_DIGEST = "com.errata.app.action.REMINDER_DIGEST"
        const val EXTRA_TASK_ID = "task_id"
        const val DIGEST_REQUEST_CODE = 0x7E11A701
        const val DIGEST_NOTIFICATION_ID = 0x7E11A702

        fun requestCode(taskId: Long): Int = taskId.toInt()
    }
}

private fun TaskEntity.toDigestCandidate() = DigestPlanner.Candidate(
    id = id,
    estimateMinutes = estimateMinutes,
    reminderMinutesOfDay = reminderMinutesOfDay,
    nextDueAtEpochMs = nextDueAtEpochMs,
    snoozedUntilEpochMs = snoozedUntilEpochMs,
    isPaused = isPaused,
    isArchived = isArchived,
)

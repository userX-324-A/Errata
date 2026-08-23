package com.errata.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.errata.app.data.TaskRepository
import com.errata.app.data.local.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderScheduler(
    private val context: Context,
    private val repository: TaskRepository,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun rescheduleAll() = withContext(Dispatchers.IO) {
        val settings = repository.getSettings()
        val tasks = repository.listSchedulableTasks()
        tasks.forEach { task ->
            scheduleTask(task, settings.defaultReminderMinutesOfDay)
        }
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
        scheduleTask(task, settings.defaultReminderMinutesOfDay)
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(alarmPendingIntent(taskId))
    }

    private fun scheduleTask(task: TaskEntity, defaultReminderMinutes: Int) {
        cancel(task.id)
        val fireAt = ReminderTimeCalculator.nextFireEpochMs(task, defaultReminderMinutes)
            ?: return
        val pi = alarmPendingIntent(task.id)
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

    companion object {
        const val ACTION_ALARM = "com.errata.app.action.REMINDER_ALARM"
        const val EXTRA_TASK_ID = "task_id"

        fun requestCode(taskId: Long): Int = taskId.toInt()
    }
}

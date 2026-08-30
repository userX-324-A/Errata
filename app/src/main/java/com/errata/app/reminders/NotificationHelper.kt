package com.errata.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.errata.app.MainActivity
import com.errata.app.R
import com.errata.app.data.local.TaskEntity

object NotificationHelper {
    const val CHANNEL_ID = "due_reminders"
    private const val CHANNEL_NAME = "Due reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminders for tasks that are due"
        }
        manager.createNotificationChannel(channel)
    }

    fun showDueReminder(context: Context, task: TaskEntity) {
        if (!NotificationAccess.areEnabled(context)) return
        ensureChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            ReminderScheduler.requestCode(task.id),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(task.title)
            .setContentText(context.getString(R.string.notification_duration, task.estimateMinutes))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.action_done), doneIntent(context, task))
            .addAction(0, context.getString(R.string.action_snooze), snoozeIntent(context, task))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId(task.id), notification)
    }

    fun showDigest(context: Context, count: Int, totalMinutes: Int) {
        if (!NotificationAccess.areEnabled(context)) return
        ensureChannel(context)
        val contentIntent = PendingIntent.getActivity(
            context,
            ReminderScheduler.DIGEST_REQUEST_CODE,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.digest_title, count))
            .setContentText(context.getString(R.string.digest_body, totalMinutes))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context)
            .notify(ReminderScheduler.DIGEST_NOTIFICATION_ID, notification)
    }

    fun dismiss(context: Context, taskId: Long) {
        NotificationManagerCompat.from(context).cancel(notificationId(taskId))
    }

    fun dismissDigest(context: Context) {
        NotificationManagerCompat.from(context).cancel(ReminderScheduler.DIGEST_NOTIFICATION_ID)
    }

    fun cancelActions(context: Context, taskId: Long) {
        doneIntent(context, taskId = taskId, scheduledDueAtEpochMs = 0L).cancel()
        snoozeIntent(context, taskId = taskId, scheduledDueAtEpochMs = 0L).cancel()
    }

    fun notificationId(taskId: Long): Int = taskId.toInt()

    private fun doneIntent(context: Context, task: TaskEntity): PendingIntent =
        doneIntent(context, task.id, task.nextDueAtEpochMs)

    private fun doneIntent(
        context: Context,
        taskId: Long,
        scheduledDueAtEpochMs: Long,
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_DONE
            putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
            putExtra(ReminderScheduler.EXTRA_SCHEDULED_DUE, scheduledDueAtEpochMs)
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderScheduler.requestCode(taskId) + 100_000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun snoozeIntent(context: Context, task: TaskEntity): PendingIntent =
        snoozeIntent(context, task.id, task.nextDueAtEpochMs)

    private fun snoozeIntent(
        context: Context,
        taskId: Long,
        scheduledDueAtEpochMs: Long = 0L,
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
            putExtra(ReminderScheduler.EXTRA_TASK_ID, taskId)
            putExtra(ReminderScheduler.EXTRA_SCHEDULED_DUE, scheduledDueAtEpochMs)
        }
        return PendingIntent.getBroadcast(
            context,
            ReminderScheduler.requestCode(taskId) + 200_000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

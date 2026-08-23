package com.errata.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.errata.app.ErrataApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderScheduler.ACTION_ALARM) return
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId < 0) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ErrataApp
                val task = app.taskRepository.getTask(taskId) ?: return@launch
                if (task.isArchived || task.isPaused) return@launch
                NotificationHelper.showDueReminder(context, task)
                // Schedule the next nudge (e.g. tomorrow) so overdue keeps a single alarm.
                app.reminderScheduler.rescheduleTask(taskId)
            } finally {
                pending.finish()
            }
        }
    }
}

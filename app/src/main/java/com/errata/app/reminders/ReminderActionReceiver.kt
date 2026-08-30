package com.errata.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.errata.app.ErrataApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        if (!ReminderActionGuard.tryBegin(taskId)) return

        NotificationHelper.dismiss(context, taskId)
        NotificationHelper.cancelActions(context, taskId)

        val expectedDue = intent.getLongExtra(ReminderScheduler.EXTRA_SCHEDULED_DUE, Long.MIN_VALUE)
            .takeIf { it != Long.MIN_VALUE }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ErrataApp
                when (action) {
                    ACTION_DONE -> app.taskCommands.complete(
                        taskId,
                        expectedNextDueAtEpochMs = expectedDue,
                    )
                    ACTION_SNOOZE -> {
                        val until = System.currentTimeMillis() + 60L * 60L * 1000L
                        app.taskCommands.snooze(
                            taskId,
                            until,
                            expectedNextDueAtEpochMs = expectedDue,
                        )
                    }
                }
            } finally {
                ReminderActionGuard.end(taskId)
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.errata.app.action.REMINDER_DONE"
        const val ACTION_SNOOZE = "com.errata.app.action.REMINDER_SNOOZE"
    }
}

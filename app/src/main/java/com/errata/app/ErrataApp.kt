package com.errata.app

import android.app.Application
import com.errata.app.data.TaskCommands
import com.errata.app.data.TaskRepository
import com.errata.app.data.local.ErrataDatabase
import com.errata.app.reminders.NotificationHelper
import com.errata.app.reminders.ReminderScheduler
import com.errata.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ErrataApp : Application() {
    val database: ErrataDatabase by lazy { ErrataDatabase.create(this) }
    val taskRepository: TaskRepository by lazy { TaskRepository(database) }
    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(this, taskRepository, widgetUpdater)
    }
    val widgetUpdater: WidgetUpdater by lazy {
        WidgetUpdater(this, taskRepository)
    }
    val taskCommands: TaskCommands by lazy {
        TaskCommands(taskRepository, reminderScheduler, widgetUpdater)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureChannel(this)
        CoroutineScope(Dispatchers.IO).launch {
            database.ensureSettings()
        }
    }

    companion object {
        lateinit var instance: ErrataApp
            private set
    }
}

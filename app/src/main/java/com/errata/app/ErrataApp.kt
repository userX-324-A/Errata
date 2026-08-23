package com.errata.app

import android.app.Application
import com.errata.app.data.TaskCommands
import com.errata.app.data.TaskRepository
import com.errata.app.data.local.ErrataDatabase
import com.errata.app.reminders.NotificationHelper
import com.errata.app.reminders.ReminderScheduler
import com.errata.app.sync.DriveAppDataClient
import com.errata.app.sync.GoogleAuth
import com.errata.app.sync.SyncCoordinator
import com.errata.app.sync.SyncPreferences
import com.errata.app.sync.SyncScheduler
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
    val syncPreferences: SyncPreferences by lazy { SyncPreferences(this) }
    val driveClient: DriveAppDataClient by lazy {
        DriveAppDataClient(
            tokenProvider = { GoogleAuth.accessToken(this) },
            fileIdStore = { syncPreferences.setFileId(it) },
            currentFileId = { syncPreferences.snapshot().fileId },
        )
    }
    val syncScheduler: SyncScheduler by lazy { SyncScheduler(this, syncPreferences) }
    val syncCoordinator: SyncCoordinator by lazy {
        SyncCoordinator(
            repository = taskRepository,
            store = driveClient,
            prefs = syncPreferences,
            scheduler = reminderScheduler,
            widgetUpdater = widgetUpdater,
        )
    }
    val taskCommands: TaskCommands by lazy {
        TaskCommands(taskRepository, reminderScheduler, widgetUpdater, syncScheduler)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.ensureChannel(this)
        CoroutineScope(Dispatchers.IO).launch {
            database.ensureSettings()
            taskRepository.pruneHistory()
            if (syncPreferences.isLinked()) {
                syncScheduler.ensurePeriodic()
                syncScheduler.requestNow()
            }
        }
    }

    companion object {
        lateinit var instance: ErrataApp
            private set
    }
}

package com.errata.app.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.errata.app.ErrataApp
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? ErrataApp ?: return Result.success()
        if (!app.syncPreferences.isLinked()) return Result.success()
        return when (app.syncCoordinator.sync()) {
            true -> Result.success()
            false -> Result.retry()
        }
    }
}

class SyncScheduler(
    context: Context,
    private val prefs: SyncPreferences,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    fun requestDebounced() {
        if (!prefs.isLinked()) return
        workManager.enqueueUniqueWork(
            DEBOUNCE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setInitialDelay(DEBOUNCE_SECONDS, TimeUnit.SECONDS)
                .setConstraints(connected())
                .build(),
        )
    }

    fun requestNow() {
        if (!prefs.isLinked()) return
        workManager.enqueueUniqueWork(
            NOW_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(connected())
                .build(),
        )
    }

    fun ensurePeriodic() {
        if (!prefs.isLinked()) return
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(connected())
                .build(),
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(DEBOUNCE_WORK)
        workManager.cancelUniqueWork(NOW_WORK)
        workManager.cancelUniqueWork(PERIODIC_WORK)
    }

    private fun connected() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private companion object {
        const val DEBOUNCE_SECONDS = 45L
        const val DEBOUNCE_WORK = "errata-sync-debounce"
        const val NOW_WORK = "errata-sync-now"
        const val PERIODIC_WORK = "errata-sync-daily"
    }
}

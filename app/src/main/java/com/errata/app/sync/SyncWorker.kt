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
import com.errata.app.domain.sync.SyncErrorPolicy
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? ErrataApp ?: return Result.success()
        if (!app.syncPreferences.isLinked()) return Result.success()
        return when (app.syncCoordinator.sync()) {
            SyncOutcome.Ok -> {
                if (app.syncPreferences.isLinked()) {
                    app.syncScheduler.ensurePeriodic()
                }
                Result.success()
            }
            SyncOutcome.Retryable -> Result.retry()
            SyncOutcome.Auth, SyncOutcome.Corrupt -> {
                app.syncScheduler.cancelAll()
                Result.success()
            }
        }
    }
}

class SyncScheduler(
    context: Context,
    private val prefs: SyncPreferences,
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    private fun allowsBackgroundSync(): Boolean =
        prefs.isLinked() && !SyncErrorPolicy.blocksBackground(prefs.snapshot().lastError)

    fun requestDebounced() {
        if (!allowsBackgroundSync()) return
        retireLegacyOneShots()
        workManager.enqueueUniqueWork(
            ONE_SHOT_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setInitialDelay(DEBOUNCE_SECONDS, TimeUnit.SECONDS)
                .setConstraints(connected())
                .build(),
        )
    }

    fun requestNow(force: Boolean = false) {
        if (!prefs.isLinked()) return
        if (!force && SyncErrorPolicy.blocksBackground(prefs.snapshot().lastError)) return
        retireLegacyOneShots()
        workManager.enqueueUniqueWork(
            ONE_SHOT_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(connected())
                .build(),
        )
    }

    fun ensurePeriodic() {
        if (!allowsBackgroundSync()) return
        retireLegacyOneShots()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<SyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(connected())
                .build(),
        )
    }

    fun cancelAll() {
        retireLegacyOneShots()
        workManager.cancelUniqueWork(ONE_SHOT_WORK)
        workManager.cancelUniqueWork(PERIODIC_WORK)
    }

    private fun retireLegacyOneShots() {
        workManager.cancelUniqueWork(LEGACY_DEBOUNCE_WORK)
        workManager.cancelUniqueWork(LEGACY_NOW_WORK)
    }

    private fun connected() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private companion object {
        const val DEBOUNCE_SECONDS = 45L
        const val ONE_SHOT_WORK = "errata-sync"
        const val PERIODIC_WORK = "errata-sync-daily"
        const val LEGACY_DEBOUNCE_WORK = "errata-sync-debounce"
        const val LEGACY_NOW_WORK = "errata-sync-now"
    }
}

package com.errata.app.sync

import android.util.Log
import com.errata.app.data.TaskRepository
import com.errata.app.domain.sync.SyncMerge
import com.errata.app.domain.sync.SyncRound
import com.errata.app.domain.sync.SyncRoundResult
import com.errata.app.reminders.ReminderScheduler
import com.errata.app.widget.WidgetUpdater
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncCoordinator(
    private val repository: TaskRepository,
    private val store: DriveAppDataClient,
    private val prefs: SyncPreferences,
    private val scheduler: ReminderScheduler,
    private val widgetUpdater: WidgetUpdater,
) {
    private val lock = Mutex()

    suspend fun sync(): Boolean = lock.withLock {
        if (!prefs.isLinked()) return true
        val local = repository.toSyncSnapshot()
        val result = try {
            SyncRound.run(local, store, System.currentTimeMillis())
        } catch (_: DriveAppDataClient.AuthRequiredException) {
            prefs.markError("auth")
            return false
        } catch (_: DriveAppDataClient.NetworkException) {
            prefs.markError("network")
            return false
        } catch (e: Exception) {
            Log.w(TAG, "sync", e)
            prefs.markError("network")
            return false
        }
        return when (result) {
            is SyncRoundResult.Applied -> {
                val current = repository.toSyncSnapshot()
                if (SyncMerge.localMoved(local, current)) {
                    return false
                }
                repository.applySyncSnapshot(result.snapshot)
                scheduler.rescheduleAll()
                widgetUpdater.refresh()
                prefs.markSynced(System.currentTimeMillis())
                true
            }
            is SyncRoundResult.Failed -> {
                prefs.markError(result.reason)
                result.reason != "auth"
            }
        }
    }

    suspend fun wipeCloud(): Boolean = store.delete()

    private companion object {
        const val TAG = "ErrataSync"
    }
}

package com.errata.app.sync

import android.util.Log
import com.errata.app.data.TaskRepository
import com.errata.app.domain.sync.SyncMerge
import com.errata.app.domain.sync.SyncRound
import com.errata.app.domain.sync.SyncRoundResult
import com.errata.app.reminders.ReminderScheduler
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SyncOutcome {
    Ok,
    Retryable,
    Auth,
    Corrupt,
}

fun syncOutcomeForFailure(reason: String): SyncOutcome = when (reason) {
    "auth" -> SyncOutcome.Auth
    "corrupt" -> SyncOutcome.Corrupt
    else -> SyncOutcome.Retryable
}

class SyncCoordinator(
    private val repository: TaskRepository,
    private val store: DriveAppDataClient,
    private val prefs: SyncPreferences,
    private val scheduler: ReminderScheduler,
) {
    private val lock = Mutex()

    suspend fun sync(): SyncOutcome = lock.withLock {
        if (!prefs.isLinked()) return@withLock SyncOutcome.Ok
        val local = repository.toSyncSnapshot()
        val result = try {
            SyncRound.run(local, store, System.currentTimeMillis())
        } catch (_: DriveAppDataClient.AuthRequiredException) {
            prefs.markError("auth")
            return@withLock SyncOutcome.Auth
        } catch (_: DriveAppDataClient.NetworkException) {
            prefs.markError("network")
            return@withLock SyncOutcome.Retryable
        } catch (e: Exception) {
            Log.w(TAG, "sync", e)
            prefs.markError("network")
            return@withLock SyncOutcome.Retryable
        }
        when (result) {
            is SyncRoundResult.Applied -> {
                val current = repository.toSyncSnapshot()
                if (SyncMerge.localMoved(local, current)) {
                    return@withLock SyncOutcome.Retryable
                }
                repository.applySyncSnapshot(result.snapshot)
                scheduler.rescheduleAll()
                prefs.markSynced(System.currentTimeMillis())
                SyncOutcome.Ok
            }
            is SyncRoundResult.Failed -> {
                prefs.markError(result.reason)
                syncOutcomeForFailure(result.reason)
            }
        }
    }

    suspend fun wipeCloud(): Boolean = store.delete()

    private companion object {
        const val TAG = "ErrataSync"
    }
}

package com.errata.app.domain.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object SyncCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(snapshot: SyncSnapshot): String = json.encodeToString(snapshot)

    fun decode(text: String): SyncSnapshot {
        val snapshot = json.decodeFromString<SyncSnapshot>(text)
        if (snapshot.schemaVersion != SYNC_SCHEMA_VERSION) {
            throw IllegalArgumentException("unsupported_sync_schema")
        }
        return snapshot
    }
}

data class CloudDocument(
    val snapshot: SyncSnapshot?,
    val fileId: String?,
    val etag: String?,
    val unreadable: Boolean = false,
)

sealed class CloudSaveResult {
    data class Written(val fileId: String, val etag: String) : CloudSaveResult()
    data object Stale : CloudSaveResult()
    data class Failed(val reason: String) : CloudSaveResult()
}

interface CloudStore {
    suspend fun load(): CloudDocument
    suspend fun save(
        snapshot: SyncSnapshot,
        fileId: String?,
        etag: String?,
    ): CloudSaveResult

    suspend fun delete(): Boolean
}

sealed class SyncRoundResult {
    data class Applied(val snapshot: SyncSnapshot) : SyncRoundResult()
    data class Failed(val reason: String) : SyncRoundResult()
}

object SyncRound {
    const val MAX_ATTEMPTS = 3

    suspend fun run(
        local: SyncSnapshot,
        store: CloudStore,
        nowEpochMs: Long,
        maxAttempts: Int = MAX_ATTEMPTS,
    ): SyncRoundResult {
        repeat(maxAttempts) {
            val loaded = store.load()
            if (loaded.unreadable) return SyncRoundResult.Failed("corrupt")
            val remote = loaded.snapshot ?: SyncSnapshot()
            val merged = SyncMerge.merge(local, remote, nowEpochMs)
            when (val saved = store.save(merged, loaded.fileId, loaded.etag)) {
                is CloudSaveResult.Written -> return SyncRoundResult.Applied(merged)
                CloudSaveResult.Stale -> Unit
                is CloudSaveResult.Failed -> return SyncRoundResult.Failed(saved.reason)
            }
        }
        return SyncRoundResult.Failed("conflict")
    }
}

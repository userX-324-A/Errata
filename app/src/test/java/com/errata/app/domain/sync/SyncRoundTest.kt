package com.errata.app.domain.sync

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRoundTest {

    @Test
    fun retriesOnStaleEtagThenWrites() = runTest {
        val store = FakeCloudStore(staleCount = 2)
        val local = SyncSnapshot(
            writtenAtEpochMs = 5,
            tasks = listOf(
                SyncTask(
                    uuid = "t1",
                    title = "Filters",
                    estimateMinutes = 15,
                    intervalDays = 14,
                    scheduleKind = "INTERVAL",
                    cadenceMode = "FROM_COMPLETION_CATCH_UP",
                    anchorEpochDay = 1,
                    nextDueAtEpochMs = 1,
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 5,
                ),
            ),
        )
        val result = SyncRound.run(local, store, nowEpochMs = 10)
        assertTrue(result is SyncRoundResult.Applied)
        assertEquals(3, store.saveCalls)
        assertEquals("t1", (result as SyncRoundResult.Applied).snapshot.tasks.single().uuid)
    }

    @Test
    fun givesUpAfterMaxStale() = runTest {
        val store = FakeCloudStore(staleCount = 10)
        val result = SyncRound.run(SyncSnapshot(), store, nowEpochMs = 1, maxAttempts = 3)
        assertEquals("conflict", (result as SyncRoundResult.Failed).reason)
        assertEquals(3, store.saveCalls)
    }

    @Test
    fun unreadableRemote_failsWithoutSave() = runTest {
        val store = object : CloudStore {
            override suspend fun load() = CloudDocument(
                snapshot = null,
                fileId = "file1",
                etag = "e1",
                unreadable = true,
            )
            override suspend fun save(
                snapshot: SyncSnapshot,
                fileId: String?,
                etag: String?,
            ) = error("must not save")
            override suspend fun delete(): Boolean = true
        }
        val result = SyncRound.run(SyncSnapshot(), store, nowEpochMs = 1)
        assertEquals("corrupt", (result as SyncRoundResult.Failed).reason)
    }

    private class FakeCloudStore(private val staleCount: Int) : CloudStore {
        var saveCalls: Int = 0
            private set

        override suspend fun load(): CloudDocument = CloudDocument(
            snapshot = SyncSnapshot(),
            fileId = "file1",
            etag = "etag-$saveCalls",
        )

        override suspend fun save(
            snapshot: SyncSnapshot,
            fileId: String?,
            etag: String?,
        ): CloudSaveResult {
            saveCalls += 1
            return if (saveCalls <= staleCount) {
                CloudSaveResult.Stale
            } else {
                CloudSaveResult.Written(fileId = "file1", etag = "ok")
            }
        }

        override suspend fun delete(): Boolean = true
    }
}

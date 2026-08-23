package com.errata.app.domain.sync

import com.errata.app.domain.history.HistoryRetention
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {

    private val now = 1_800_000_000_000L
    private val day = Duration.ofDays(1).toMillis()

    @Test
    fun independentTasks_areBothKept() {
        val a = snapshot(tasks = listOf(task("phone", "Filters", updated = 10)))
        val b = snapshot(tasks = listOf(task("tablet", "Filters", updated = 10)))
        val out = SyncMerge.merge(a, b)
        assertEquals(setOf("phone", "tablet"), out.tasks.map { it.uuid }.toSet())
    }

    @Test
    fun taskLww_laterUpdatedWins() {
        val a = snapshot(tasks = listOf(task("t1", "Old", updated = 10)))
        val b = snapshot(tasks = listOf(task("t1", "New", updated = 20)))
        val out = SyncMerge.merge(a, b)
        assertEquals("New", out.tasks.single().title)
        val reversed = SyncMerge.merge(b, a)
        assertEquals("New", reversed.tasks.single().title)
    }

    @Test
    fun doneOnA_editTitleOnB_laterFieldWinsWholeRow() {
        val a = snapshot(
            tasks = listOf(
                task("t1", "Filters", updated = 50, lastCompleted = 50, nextDue = 80),
            ),
        )
        val b = snapshot(
            tasks = listOf(task("t1", "HVAC filters", updated = 40, nextDue = 30)),
        )
        val out = SyncMerge.merge(a, b).tasks.single()
        assertEquals("Filters", out.title)
        assertEquals(50L, out.lastCompletedAtEpochMs)
        assertEquals(80L, out.nextDueAtEpochMs)
    }

    @Test
    fun completionsUnion_sameGeneration() {
        val a = snapshot(completions = listOf(done("c1", "t1", at = 10)))
        val b = snapshot(completions = listOf(done("c2", "t1", at = 20)))
        val out = SyncMerge.merge(a, b)
        assertEquals(setOf("c1", "c2"), out.completions.map { it.uuid }.toSet())
    }

    @Test
    fun yearlySparse_survives90DayRetentionAfterMerge() {
        val rows = (1..8).map { n ->
            done("c$n", "yearly", at = now - (8 - n) * 365L * day)
        }
        val a = snapshot(
            settings = SyncSettings(historyRetentionDays = HistoryRetention.DAYS_90),
            tasks = listOf(task("yearly", "Gutters")),
            completions = rows,
        )
        val b = snapshot(
            settings = SyncSettings(historyRetentionDays = HistoryRetention.DAYS_90),
        )
        val out = SyncMerge.merge(a, b, nowEpochMs = now)
        assertEquals(8, out.completions.size)
    }

    @Test
    fun purgeOnA_thenDoneOnB_keepsNewDone() {
        val old = done("old", "t1", at = 10)
        val a = snapshot(
            historyGeneration = 2,
            historyPurgedAtEpochMs = 100,
            completions = emptyList(),
        )
        val b = snapshot(
            historyGeneration = 1,
            historyPurgedAtEpochMs = 0,
            completions = listOf(old, done("new", "t1", at = 200)),
        )
        val out = SyncMerge.merge(a, b)
        assertEquals(2, out.historyGeneration)
        assertEquals(listOf("new"), out.completions.map { it.uuid })
    }

    @Test
    fun resetGeneration_dropsOldTasks_keepsNewerCreated() {
        val a = snapshot(
            tasksGeneration = 2,
            tasksResetAtEpochMs = 100,
            tasks = emptyList(),
        )
        val b = snapshot(
            tasksGeneration = 1,
            tasks = listOf(
                task("old", "Old", created = 50, updated = 50),
                task("fresh", "Fresh", created = 150, updated = 150),
            ),
        )
        val out = SyncMerge.merge(a, b)
        assertEquals(2, out.tasksGeneration)
        assertEquals(listOf("fresh"), out.tasks.map { it.uuid })
    }

    @Test
    fun settingsLww_ignoresAppearanceByOmission() {
        val a = snapshot(
            settings = SyncSettings(
                updatedAtEpochMs = 10,
                defaultReminderMinutesOfDay = 7 * 60,
                digestEnabled = false,
            ),
        )
        val b = snapshot(
            settings = SyncSettings(
                updatedAtEpochMs = 20,
                defaultReminderMinutesOfDay = 9 * 60,
                digestEnabled = true,
            ),
        )
        val out = SyncMerge.merge(a, b)
        assertEquals(9 * 60, out.settings.defaultReminderMinutesOfDay)
        assertTrue(out.settings.digestEnabled)
    }

    @Test
    fun tenOldCompletions_prunedToEight() {
        val rows = (1..10).map { n ->
            done("c$n", "t1", at = now - (100L + n) * day)
        }
        val a = snapshot(
            settings = SyncSettings(historyRetentionDays = HistoryRetention.DAYS_90),
            tasks = listOf(task("t1", "X")),
            completions = rows,
        )
        val out = SyncMerge.merge(a, snapshot(), nowEpochMs = now)
        assertEquals(8, out.completions.size)
        assertTrue(out.completions.none { it.uuid == "c9" || it.uuid == "c10" })
    }

    private fun snapshot(
        tasksGeneration: Int = 0,
        tasksResetAtEpochMs: Long = 0,
        historyGeneration: Int = 0,
        historyPurgedAtEpochMs: Long = 0,
        settings: SyncSettings = SyncSettings(),
        tasks: List<SyncTask> = emptyList(),
        completions: List<SyncCompletion> = emptyList(),
    ) = SyncSnapshot(
        writtenAtEpochMs = 1,
        tasksGeneration = tasksGeneration,
        tasksResetAtEpochMs = tasksResetAtEpochMs,
        historyGeneration = historyGeneration,
        historyPurgedAtEpochMs = historyPurgedAtEpochMs,
        settings = settings,
        tasks = tasks,
        completions = completions,
    )

    private fun task(
        uuid: String,
        title: String,
        updated: Long = 1,
        created: Long = 1,
        lastCompleted: Long? = null,
        nextDue: Long = 1,
    ) = SyncTask(
        uuid = uuid,
        title = title,
        estimateMinutes = 15,
        intervalDays = 14,
        scheduleKind = "INTERVAL",
        cadenceMode = "FROM_COMPLETION_CATCH_UP",
        anchorEpochDay = 1,
        nextDueAtEpochMs = nextDue,
        lastCompletedAtEpochMs = lastCompleted,
        createdAtEpochMs = created,
        updatedAtEpochMs = updated,
    )

    private fun done(uuid: String, taskUuid: String, at: Long) = SyncCompletion(
        uuid = uuid,
        taskUuid = taskUuid,
        completedAtEpochMs = at,
        scheduledDueAtEpochMs = at,
        estimateMinutesAtCompletion = 15,
    )
}

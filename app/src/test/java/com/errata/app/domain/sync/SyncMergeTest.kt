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
    fun marksAfterLocalReplace_beatsPreviousAndImportedGens() {
        val marks = SyncMerge.marksAfterLocalReplace(
            previousTasksGeneration = 3,
            previousHistoryGeneration = 1,
            importedTasksGeneration = 2,
            importedHistoryGeneration = 4,
            importedSettingsUpdatedAt = 50,
            nowEpochMs = 200,
        )
        assertEquals(4, marks.tasksGeneration)
        assertEquals(5, marks.historyGeneration)
        assertEquals(200L, marks.tasksResetAtEpochMs)
        assertEquals(200L, marks.historyPurgedAtEpochMs)
        assertEquals(200L, marks.settingsUpdatedAtEpochMs)
    }

    @Test
    fun importReplace_dropsCloudOnlyTasksAndOldHistory() {
        val now = 500L
        val marks = SyncMerge.marksAfterLocalReplace(
            previousTasksGeneration = 1,
            previousHistoryGeneration = 1,
            importedTasksGeneration = 0,
            importedHistoryGeneration = 0,
            importedSettingsUpdatedAt = 10,
            nowEpochMs = now,
        )
        val local = snapshot(
            tasksGeneration = marks.tasksGeneration,
            tasksResetAtEpochMs = marks.tasksResetAtEpochMs,
            historyGeneration = marks.historyGeneration,
            historyPurgedAtEpochMs = marks.historyPurgedAtEpochMs,
            settings = SyncSettings(updatedAtEpochMs = marks.settingsUpdatedAtEpochMs),
            tasks = listOf(task("kept", "From backup", created = 10, updated = 10)),
            completions = listOf(done("kept-done", "kept", at = 20)),
        )
        val cloud = snapshot(
            tasksGeneration = 1,
            historyGeneration = 1,
            settings = SyncSettings(updatedAtEpochMs = 100),
            tasks = listOf(
                task("deleted", "Should vanish", created = 10, updated = 400),
                task("kept", "Cloud newer title", created = 10, updated = 400),
            ),
            completions = listOf(
                done("cloud-old", "deleted", at = 30),
                done("kept-done", "kept", at = 20),
            ),
        )
        val out = SyncMerge.merge(local, cloud)
        assertEquals(setOf("kept"), out.tasks.map { it.uuid }.toSet())
        assertEquals("From backup", out.tasks.single().title)
        assertEquals(listOf("kept-done"), out.completions.map { it.uuid })
        assertEquals(marks.tasksGeneration, out.tasksGeneration)
        assertEquals(marks.historyGeneration, out.historyGeneration)
        assertEquals(marks.settingsUpdatedAtEpochMs, out.settings.updatedAtEpochMs)
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

    @Test
    fun localMoved_taskEditOrNewDone() {
        val before = snapshot(tasks = listOf(task("t1", "Filters", updated = 10)))
        assertTrue(
            SyncMerge.localMoved(
                before,
                snapshot(tasks = listOf(task("t1", "Filters", updated = 20))),
            ),
        )
        assertTrue(
            SyncMerge.localMoved(
                before,
                snapshot(
                    tasks = listOf(task("t1", "Filters", updated = 10)),
                    completions = listOf(done("c1", "t1", 11)),
                ),
            ),
        )
        assertTrue(!SyncMerge.localMoved(before, snapshot(tasks = listOf(task("t1", "Filters", updated = 10)))))
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

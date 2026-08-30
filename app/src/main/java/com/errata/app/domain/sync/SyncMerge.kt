package com.errata.app.domain.sync

import com.errata.app.domain.history.HistoryRetention

/**
 * Merge two Drive snapshots. Pure JVM; commutative for equal [SyncSnapshot.writtenAtEpochMs]
 * ties via field-level rules below.
 */
object SyncMerge {
    fun merge(a: SyncSnapshot, b: SyncSnapshot, nowEpochMs: Long = 0): SyncSnapshot {
        val tasks = mergeTasks(a, b)
        val completions = mergeCompletions(a, b)
        val (tasksGeneration, tasksResetAt) = maxGeneration(
            a.tasksGeneration,
            a.tasksResetAtEpochMs,
            b.tasksGeneration,
            b.tasksResetAtEpochMs,
        )
        val (historyGeneration, historyPurgedAt) = maxGeneration(
            a.historyGeneration,
            a.historyPurgedAtEpochMs,
            b.historyGeneration,
            b.historyPurgedAtEpochMs,
        )
        val settings = mergeSettings(a.settings, b.settings)
        val writtenAt = maxOf(a.writtenAtEpochMs, b.writtenAtEpochMs, nowEpochMs)
        val merged = SyncSnapshot(
            schemaVersion = SYNC_SCHEMA_VERSION,
            writtenAtEpochMs = writtenAt,
            tasksGeneration = tasksGeneration,
            tasksResetAtEpochMs = tasksResetAt,
            historyGeneration = historyGeneration,
            historyPurgedAtEpochMs = historyPurgedAt,
            settings = settings,
            tasks = tasks,
            completions = completions,
        )
        return if (nowEpochMs > 0) pruneCompletions(merged, nowEpochMs) else merged
    }

    data class CloudFollowMarks(
        val tasksGeneration: Int,
        val tasksResetAtEpochMs: Long,
        val historyGeneration: Int,
        val historyPurgedAtEpochMs: Long,
        val settingsUpdatedAtEpochMs: Long,
    )

    /**
     * After a local replace-all import, bump generations so the next Drive merge
     * drops cloud-only tasks and completions older than this import.
     */
    fun marksAfterLocalReplace(
        previousTasksGeneration: Int,
        previousHistoryGeneration: Int,
        importedTasksGeneration: Int,
        importedHistoryGeneration: Int,
        importedSettingsUpdatedAt: Long,
        nowEpochMs: Long,
    ): CloudFollowMarks = CloudFollowMarks(
        tasksGeneration = maxOf(previousTasksGeneration, importedTasksGeneration) + 1,
        tasksResetAtEpochMs = nowEpochMs,
        historyGeneration = maxOf(previousHistoryGeneration, importedHistoryGeneration) + 1,
        historyPurgedAtEpochMs = nowEpochMs,
        settingsUpdatedAtEpochMs = maxOf(importedSettingsUpdatedAt, nowEpochMs),
    )

    /**
     * True when local data changed after the snapshot used for a merge.
     * Apply would overwrite those edits.
     */
    fun localMoved(before: SyncSnapshot, after: SyncSnapshot): Boolean {
        if (before.tasksGeneration != after.tasksGeneration) return true
        if (before.historyGeneration != after.historyGeneration) return true
        if (before.settings.updatedAtEpochMs != after.settings.updatedAtEpochMs) return true
        if (before.tasks.size != after.tasks.size) return true
        if (before.completions.size != after.completions.size) return true
        val taskTimes = before.tasks.associate { it.uuid to it.updatedAtEpochMs }
        val afterTimes = after.tasks.associate { it.uuid to it.updatedAtEpochMs }
        if (taskTimes != afterTimes) return true
        return before.completions.map { it.uuid }.toSet() !=
            after.completions.map { it.uuid }.toSet()
    }

    fun pruneCompletions(snapshot: SyncSnapshot, nowEpochMs: Long): SyncSnapshot {
        val gone = HistoryRetention.sampleIdsToDelete(
            snapshot.completions.map { row ->
                HistoryRetention.Sample(
                    id = row.uuid,
                    taskKey = row.taskUuid,
                    completedAtEpochMs = row.completedAtEpochMs,
                )
            },
            nowEpochMs,
            snapshot.settings.historyRetentionDays,
        ).toSet()
        if (gone.isEmpty()) return snapshot
        return snapshot.copy(completions = snapshot.completions.filter { it.uuid !in gone })
    }

    private fun mergeSettings(a: SyncSettings, b: SyncSettings): SyncSettings {
        val cmp = a.updatedAtEpochMs.compareTo(b.updatedAtEpochMs)
        if (cmp > 0) return a
        if (cmp < 0) return b
        return if (settingsTieBreak(a) >= settingsTieBreak(b)) a else b
    }

    private fun settingsTieBreak(s: SyncSettings): String =
        listOf(
            s.defaultCadenceMode,
            s.defaultReminderMinutesOfDay.toString(),
            s.defaultWorkStartMinutesOfDay?.toString().orEmpty(),
            s.soonHorizonDays.toString(),
            s.digestEnabled.toString(),
            s.historyRetentionDays.toString(),
        ).joinToString("|")

    private fun mergeTasks(a: SyncSnapshot, b: SyncSnapshot): List<SyncTask> {
        val (_, winResetAt) = maxGeneration(
            a.tasksGeneration,
            a.tasksResetAtEpochMs,
            b.tasksGeneration,
            b.tasksResetAtEpochMs,
        )
        val winning = if (a.tasksGeneration >= b.tasksGeneration) a else b
        val losing = if (winning === a) b else a
        val seed = if (a.tasksGeneration == b.tasksGeneration) {
            a.tasks + b.tasks
        } else {
            winning.tasks + losing.tasks.filter { task ->
                winResetAt == 0L || task.createdAtEpochMs >= winResetAt
            }
        }
        val byUuid = LinkedHashMap<String, SyncTask>()
        for (task in seed) {
            val existing = byUuid[task.uuid]
            byUuid[task.uuid] = if (existing == null) {
                task
            } else {
                pickTask(existing, task)
            }
        }
        return byUuid.values.sortedBy { it.uuid }
    }

    private fun pickTask(a: SyncTask, b: SyncTask): SyncTask {
        val cmp = a.updatedAtEpochMs.compareTo(b.updatedAtEpochMs)
        if (cmp > 0) return a
        if (cmp < 0) return b
        return if (a.uuid >= b.uuid) a else b
    }

    private fun mergeCompletions(a: SyncSnapshot, b: SyncSnapshot): List<SyncCompletion> {
        val (_, winPurgedAt) = maxGeneration(
            a.historyGeneration,
            a.historyPurgedAtEpochMs,
            b.historyGeneration,
            b.historyPurgedAtEpochMs,
        )
        val winning = if (a.historyGeneration >= b.historyGeneration) a else b
        val losing = if (winning === a) b else a
        val seed = if (a.historyGeneration == b.historyGeneration) {
            a.completions + b.completions
        } else {
            winning.completions + losing.completions.filter { row ->
                winPurgedAt == 0L || row.completedAtEpochMs >= winPurgedAt
            }
        }
        val byUuid = LinkedHashMap<String, SyncCompletion>()
        val byLegacy = LinkedHashMap<String, SyncCompletion>()
        for (row in seed) {
            val uuid = row.uuid.ifBlank { legacyKey(row) }
            val key = if (row.uuid.isNotBlank()) uuid else legacyKey(row)
            if (row.uuid.isNotBlank()) {
                byUuid.putIfAbsent(row.uuid, row)
            } else {
                byLegacy.putIfAbsent(key, row)
            }
        }
        val merged = ArrayList<SyncCompletion>(byUuid.size + byLegacy.size)
        merged.addAll(byUuid.values)
        for (row in byLegacy.values) {
            val duplicate = byUuid.values.any { existing ->
                existing.taskUuid == row.taskUuid &&
                    existing.completedAtEpochMs == row.completedAtEpochMs
            }
            if (!duplicate) merged.add(row)
        }
        return merged.sortedBy { it.uuid.ifBlank { legacyKey(it) } }
    }

    private fun legacyKey(row: SyncCompletion): String =
        "${row.taskUuid}|${row.completedAtEpochMs}"

    private fun maxGeneration(
        genA: Int,
        atA: Long,
        genB: Int,
        atB: Long,
    ): Pair<Int, Long> = when {
        genA > genB -> genA to atA
        genB > genA -> genB to atB
        else -> maxOf(genA, genB) to maxOf(atA, atB)
    }
}

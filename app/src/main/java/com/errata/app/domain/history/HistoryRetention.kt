package com.errata.app.domain.history

import java.time.Duration

/**
 * Which completion rows to drop so history does not grow forever.
 *
 * Always keep the newest [KEEP_PER_TASK] rows per task (glance). Also drop rows
 * older than [retentionDays], except those kept. [KEEP_ALL] skips age trim.
 */
object HistoryRetention {
    const val KEEP_PER_TASK = HistoryGlance.MAX_SAMPLES
    const val KEEP_ALL = 0
    const val DAYS_90 = 90
    const val DAYS_YEAR = 365
    const val DAYS_2Y = 730
    const val DEFAULT_DAYS = DAYS_2Y

    data class Row(
        val id: Long,
        val taskId: Long,
        val completedAtEpochMs: Long,
    )

    data class Sample<T : Comparable<T>>(
        val id: T,
        val taskKey: T,
        val completedAtEpochMs: Long,
    )

    fun idsToDelete(
        rows: List<Row>,
        nowEpochMs: Long,
        retentionDays: Int,
    ): List<Long> = sampleIdsToDelete(
        rows.map { Sample(it.id, it.taskId, it.completedAtEpochMs) },
        nowEpochMs,
        retentionDays,
    )

    fun cutoffEpochMs(nowEpochMs: Long, retentionDays: Int): Long? {
        if (retentionDays <= KEEP_ALL) return null
        return nowEpochMs - Duration.ofDays(retentionDays.toLong()).toMillis()
    }

    /**
     * Age-trim at most once per local day unless [force] (retention setting changed).
     * [KEEP_ALL] never scans.
     */
    fun shouldRun(
        retentionDays: Int,
        lastPruneEpochDay: Long?,
        todayEpochDay: Long,
        force: Boolean,
    ): Boolean {
        if (retentionDays <= KEEP_ALL) return false
        if (force) return true
        return lastPruneEpochDay != todayEpochDay
    }

    fun <T : Comparable<T>> sampleIdsToDelete(
        rows: List<Sample<T>>,
        nowEpochMs: Long,
        retentionDays: Int,
    ): List<T> {
        val cutoff = cutoffEpochMs(nowEpochMs, retentionDays) ?: return emptyList()
        if (rows.isEmpty()) return emptyList()
        val protectedIds = rows
            .groupBy { it.taskKey }
            .flatMap { (_, group) ->
                group.sortedWith(
                    compareByDescending<Sample<T>> { it.completedAtEpochMs }
                        .thenByDescending { it.id },
                ).take(KEEP_PER_TASK).map { it.id }
            }
            .toSet()
        return rows
            .filter { it.id !in protectedIds && it.completedAtEpochMs < cutoff }
            .map { it.id }
    }
}

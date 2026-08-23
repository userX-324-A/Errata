package com.errata.app.domain.widget

import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.due.PendingClassifier
import java.time.Instant
import java.time.ZoneId

/**
 * Overdue + due-today counts for the home-screen widget. Soon is excluded.
 */
data class WidgetSnapshot(
    val count: Int,
    val totalMinutes: Int,
) {
    val isEmpty: Boolean get() = count == 0

    companion object {
        fun from(
            items: List<Item>,
            nowEpochMs: Long,
            zone: ZoneId = ZoneId.systemDefault(),
        ): WidgetSnapshot {
            val due = items.filter { item ->
                val bucket = PendingClassifier.classify(
                    task = PendingClassifier.ClassifiableTask(
                        nextDueAtEpochMs = item.nextDueAtEpochMs,
                        snoozedUntilEpochMs = item.snoozedUntilEpochMs,
                        isPaused = item.isPaused,
                        isArchived = item.isArchived,
                    ),
                    nowEpochMs = nowEpochMs,
                    zone = zone,
                )
                bucket == DueBucket.OVERDUE || bucket == DueBucket.DUE_TODAY
            }
            return WidgetSnapshot(
                count = due.size,
                totalMinutes = due.sumOf { it.estimateMinutes },
            )
        }

        fun nextLocalMidnightEpochMs(
            nowEpochMs: Long = System.currentTimeMillis(),
            zone: ZoneId = ZoneId.systemDefault(),
        ): Long {
            val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
            var next = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            if (next <= nowEpochMs) {
                next = today.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
            }
            return next
        }
    }

    data class Item(
        val estimateMinutes: Int,
        val nextDueAtEpochMs: Long,
        val snoozedUntilEpochMs: Long? = null,
        val isPaused: Boolean = false,
        val isArchived: Boolean = false,
    )
}

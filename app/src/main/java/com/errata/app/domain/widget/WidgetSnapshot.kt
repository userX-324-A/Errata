package com.errata.app.domain.widget

import com.errata.app.domain.due.DueBucket
import com.errata.app.domain.due.PendingClassifier
import java.time.Instant
import java.time.ZoneId

/**
 * Overdue + due-today counts for the home-screen widget. Soon is excluded.
 * [titles] are the first [TITLE_LIMIT] due rows (effective due, then title).
 */
data class WidgetSnapshot(
    val count: Int,
    val totalMinutes: Int,
    val titles: List<String> = emptyList(),
    val overflowCount: Int = 0,
) {
    val isEmpty: Boolean get() = count == 0

    companion object {
        const val TITLE_LIMIT = 4

        /** Two-cell min height is 110dp; one-cell is 40dp. Unknown (0) follows the default tall tile. */
        const val TALL_MIN_HEIGHT_DP = 100

        fun titleSlots(minHeightDp: Int): Int =
            if (minHeightDp in 1 until TALL_MIN_HEIGHT_DP) 0 else TITLE_LIMIT

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
            }.sortedWith(
                compareBy(
                    {
                        PendingClassifier.effectiveDueEpochMs(
                            it.nextDueAtEpochMs,
                            it.snoozedUntilEpochMs,
                        )
                    },
                    { it.title.lowercase() },
                ),
            )
            return WidgetSnapshot(
                count = due.size,
                totalMinutes = due.sumOf { it.estimateMinutes },
                titles = due.take(TITLE_LIMIT).map { it.title },
                overflowCount = (due.size - TITLE_LIMIT).coerceAtLeast(0),
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
        val title: String = "",
    )
}

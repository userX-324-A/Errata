package com.errata.app.domain.history

import com.errata.app.domain.cadence.CadenceCalculator
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor

sealed class TypicalLateness {
    data object OnDueDay : TypicalLateness()
    data class DaysAfter(val days: Int) : TypicalLateness()
    data class DaysBefore(val days: Int) : TypicalLateness()
}

data class HistoryGlance(
    val lastCompletedEpochMs: Long,
    val typical: TypicalLateness?,
) {
    companion object {
        const val MAX_SAMPLES = 8
        const val MIN_FOR_TYPICAL = 3

        fun from(
            samples: List<Sample>,
            zone: ZoneId = ZoneId.systemDefault(),
        ): HistoryGlance? {
            if (samples.isEmpty()) return null
            val newestFirst = samples.sortedByDescending { it.completedAtEpochMs }
            val window = newestFirst.take(MAX_SAMPLES)
            val typical = if (window.size >= MIN_FOR_TYPICAL) {
                val deltas = window.map { sample ->
                    (
                        CadenceCalculator.epochDayOf(sample.completedAtEpochMs, zone) -
                            CadenceCalculator.epochDayOf(sample.scheduledDueAtEpochMs, zone)
                        ).toInt()
                }
                typicalFromMedian(medianHalfUp(deltas))
            } else {
                null
            }
            return HistoryGlance(
                lastCompletedEpochMs = newestFirst.first().completedAtEpochMs,
                typical = typical,
            )
        }

        private fun typicalFromMedian(medianDays: Int): TypicalLateness = when {
            medianDays == 0 -> TypicalLateness.OnDueDay
            medianDays > 0 -> TypicalLateness.DaysAfter(medianDays)
            else -> TypicalLateness.DaysBefore(-medianDays)
        }

        private fun medianHalfUp(values: List<Int>): Int {
            val sorted = values.sorted()
            val n = sorted.size
            if (n % 2 == 1) return sorted[n / 2]
            val mid = (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0
            return roundHalfUp(mid)
        }

        /** Nearest int; halfway cases away from zero. */
        internal fun roundHalfUp(value: Double): Int =
            if (value >= 0) {
                floor(value + 0.5).toInt()
            } else {
                ceil(value - 0.5).toInt()
            }
    }

    data class Sample(
        val completedAtEpochMs: Long,
        val scheduledDueAtEpochMs: Long,
    )
}

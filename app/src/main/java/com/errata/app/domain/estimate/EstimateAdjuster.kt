package com.errata.app.domain.estimate

enum class EstimateHonesty {
    SHORTER,
    SAME,
    LONGER,
}

/**
 * Lightweight estimate nudge after Done — no stopwatch, no guilt.
 */
object EstimateAdjuster {

    const val MAX_ESTIMATE_MINUTES = 480 // 8 hours soft ceiling
    /** Skip the post-Done nudge for chores shorter than this (glasses, lint, compost). */
    const val HONESTY_MIN_MINUTES = 10

    fun shouldAskAfterDone(estimateMinutes: Int): Boolean =
        estimateMinutes >= HONESTY_MIN_MINUTES

    fun adjust(estimateMinutes: Int, choice: EstimateHonesty): Int {
        val base = estimateMinutes.coerceAtLeast(1)
        return when (choice) {
            EstimateHonesty.SAME -> base
            EstimateHonesty.SHORTER -> maxOf(1, base * 3 / 4)
            EstimateHonesty.LONGER -> {
                val bumped = base + maxOf(5, base / 4)
                minOf(MAX_ESTIMATE_MINUTES, bumped)
            }
        }
    }
}

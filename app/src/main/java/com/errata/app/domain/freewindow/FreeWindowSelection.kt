package com.errata.app.domain.freewindow

import java.time.ZoneId

/**
 * Pending free-window: a fixed pocket of minutes, or a clock that must not be overrun.
 */
sealed class FreeWindowSelection {
    data class Duration(val minutes: Int) : FreeWindowSelection()
    data class UntilClock(val minutesOfDay: Int) : FreeWindowSelection()
}

fun FreeWindowSelection.remainingMinutes(
    nowEpochMs: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): Int = when (this) {
    is FreeWindowSelection.Duration -> minutes
    is FreeWindowSelection.UntilClock ->
        FreeWindowRanker.minutesUntilStopBy(minutesOfDay, nowEpochMs, zone) ?: 0
}

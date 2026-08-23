package com.errata.app.domain.cadence

/**
 * Shape of the recurrence. Orthogonal to [CadenceMode] (after-Done math).
 *
 * Weekly and monthly ignore after-Done mode: next due is the next matching
 * local calendar day strictly after Done or Skip, keeping the open due's time.
 */
enum class ScheduleKind {
    INTERVAL,
    WEEKLY,
    MONTHLY,
}

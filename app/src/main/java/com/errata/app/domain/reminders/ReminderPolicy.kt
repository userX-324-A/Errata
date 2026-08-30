package com.errata.app.domain.reminders

/**
 * Per-task reminder encoding on [com.errata.app.data.local.TaskEntity.reminderMinutesOfDay]:
 * - `null` — fire at the due clock (When due)
 * - [NONE] (`-1`) — no alarm; still due on the pending list
 * - `0`–`1439` — fire at that clock
 *
 * Do not overload null for silence; it already means When due.
 */
enum class DefaultReminderKind {
    NONE,
    WHEN_DUE,
    CLOCK,
}

object ReminderPolicy {
    const val NONE = -1
    private const val LAST_MINUTE = 24 * 60 - 1

    fun isNone(minutes: Int?): Boolean = minutes == NONE

    fun isClock(minutes: Int?): Boolean = minutes != null && minutes in 0..LAST_MINUTE

    fun storedFor(kind: DefaultReminderKind, clockMinutes: Int): Int? = when (kind) {
        DefaultReminderKind.NONE -> NONE
        DefaultReminderKind.WHEN_DUE -> null
        DefaultReminderKind.CLOCK -> clockMinutes.coerceIn(0, LAST_MINUTE)
    }

    /** Clock shown next to When due / None; custom clocks use the stored minutes. */
    fun displayMinutes(stored: Int?, dueMinutes: Int): Int =
        if (isClock(stored)) stored!! else dueMinutes.coerceIn(0, LAST_MINUTE)
}

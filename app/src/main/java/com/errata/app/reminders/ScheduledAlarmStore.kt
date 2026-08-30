package com.errata.app.reminders

import android.content.Context

/**
 * Task ids that currently have a per-task [android.app.AlarmManager] wakeup.
 * Updated on [ReminderScheduler.rescheduleTask] as well as [ReminderScheduler.rescheduleAll].
 * Orphans (import, reset, sync prune) are previous ∪ this-process ids minus remaining.
 */
object ScheduledAlarmStore {
    private const val PREFS = "errata_scheduled_alarms"
    private const val KEY_IDS = "task_ids"

    fun load(context: Context): Set<Long> =
        prefs(context).getStringSet(KEY_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

    fun save(context: Context, ids: Set<Long>) {
        prefs(context).edit()
            .putStringSet(KEY_IDS, ids.map { it.toString() }.toSet())
            .apply()
    }

    fun add(context: Context, id: Long) {
        save(context, load(context) + id)
    }

    fun remove(context: Context, id: Long) {
        save(context, load(context) - id)
    }

    fun orphans(
        previous: Set<Long>,
        remaining: Set<Long>,
        session: Set<Long> = emptySet(),
    ): Set<Long> = (previous + session) - remaining

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

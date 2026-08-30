package com.errata.app.reminders

import android.content.Context

/**
 * Task ids that currently have a per-task [android.app.AlarmManager] wakeup.
 * [ReminderScheduler.rescheduleAll] cancels ids that dropped off this set
 * (import, reset, sync prune, archive) so deleted chores do not keep waking the device.
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

    fun orphans(previous: Set<Long>, remaining: Set<Long>): Set<Long> = previous - remaining

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

package com.errata.app.reminders

import android.content.Context

/**
 * Local calendar day the morning digest (or its miss-replay) last posted.
 * Device-only — not backup/sync. Prevents a second card after a real fire.
 */
object DigestNotifyStore {
    private const val PREFS = "errata_digest_notify"
    private const val KEY_DAY = "last_notified_epoch_day"

    fun lastNotifiedEpochDay(context: Context): Long? {
        val prefs = prefs(context)
        if (!prefs.contains(KEY_DAY)) return null
        return prefs.getLong(KEY_DAY, Long.MIN_VALUE)
    }

    fun markNotified(context: Context, epochDay: Long) {
        prefs(context).edit().putLong(KEY_DAY, epochDay).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

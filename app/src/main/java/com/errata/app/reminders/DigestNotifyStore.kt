package com.errata.app.reminders

import android.content.Context

/**
 * Local calendar day the morning digest (or its miss-replay) last posted,
 * plus task ids that already got a same-day fallback card.
 * Device-only — not backup/sync.
 */
object DigestNotifyStore {
    private const val PREFS = "errata_digest_notify"
    private const val KEY_DAY = "last_notified_epoch_day"
    private const val KEY_FALLBACK_DAY = "fallback_epoch_day"
    private const val KEY_FALLBACK_IDS = "fallback_task_ids"

    fun lastNotifiedEpochDay(context: Context): Long? {
        val prefs = prefs(context)
        if (!prefs.contains(KEY_DAY)) return null
        return prefs.getLong(KEY_DAY, Long.MIN_VALUE)
    }

    fun markNotified(context: Context, epochDay: Long) {
        prefs(context).edit().putLong(KEY_DAY, epochDay).apply()
    }

    fun fallbackIds(context: Context, todayEpochDay: Long): Set<Long> {
        val prefs = prefs(context)
        if (!prefs.contains(KEY_FALLBACK_DAY)) return emptySet()
        return idsForDay(
            storedDay = prefs.getLong(KEY_FALLBACK_DAY, Long.MIN_VALUE),
            storedIds = parseIds(prefs.getStringSet(KEY_FALLBACK_IDS, emptySet())),
            todayEpochDay = todayEpochDay,
        )
    }

    fun alreadyPostedFallback(context: Context, todayEpochDay: Long, taskId: Long): Boolean =
        taskId in fallbackIds(context, todayEpochDay)

    fun markFallbackPosted(context: Context, todayEpochDay: Long, ids: Collection<Long>) {
        if (ids.isEmpty()) return
        val merged = fallbackIds(context, todayEpochDay) + ids
        prefs(context).edit()
            .putLong(KEY_FALLBACK_DAY, todayEpochDay)
            .putStringSet(KEY_FALLBACK_IDS, merged.map { it.toString() }.toSet())
            .apply()
    }

    fun idsForDay(storedDay: Long?, storedIds: Set<Long>, todayEpochDay: Long): Set<Long> =
        if (storedDay == todayEpochDay) storedIds else emptySet()

    private fun parseIds(raw: Set<String>?): Set<Long> =
        raw?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

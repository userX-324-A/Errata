package com.errata.app.reminders

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Exact-alarm special access (API 31+). Not a runtime permission; not required to schedule.
 */
object ExactAlarmAccess {
    private const val PREFS = "errata_exact_alarm"
    private const val KEY_PROMPTED = "prompted"

    fun isRelevantSdk(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= Build.VERSION_CODES.S

    fun canExact(context: Context): Boolean {
        if (!isRelevantSdk()) return true
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return manager.canScheduleExactAlarms()
    }

    fun shouldPrompt(sdkInt: Int, canExact: Boolean, prompted: Boolean): Boolean =
        isRelevantSdk(sdkInt) && !canExact && !prompted

    fun shouldPrompt(context: Context): Boolean = shouldPrompt(
        sdkInt = Build.VERSION.SDK_INT,
        canExact = canExact(context),
        prompted = isPrompted(context),
    )

    fun requestIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun isPrompted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PROMPTED, false)

    fun markPrompted(context: Context) {
        prefs(context).edit().putBoolean(KEY_PROMPTED, true).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

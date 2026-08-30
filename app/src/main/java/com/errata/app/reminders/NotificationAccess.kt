package com.errata.app.reminders

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Whether due reminders can actually appear. Skip AlarmManager wakeups when they cannot.
 * Runtime [android.Manifest.permission.POST_NOTIFICATIONS] is API 33+; do not ask on cold start.
 */
object NotificationAccess {
    private const val PREFS = "errata_notifications"
    private const val KEY_PROMPTED = "prompted"

    fun areEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun shouldSkipWakeup(notificationsEnabled: Boolean): Boolean = !notificationsEnabled

    fun isRuntimeSdk(sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        sdkInt >= Build.VERSION_CODES.TIRAMISU

    fun shouldPrompt(
        sdkInt: Int,
        notificationsEnabled: Boolean,
        prompted: Boolean,
    ): Boolean = isRuntimeSdk(sdkInt) && !notificationsEnabled && !prompted

    fun shouldPrompt(context: Context): Boolean = shouldPrompt(
        sdkInt = Build.VERSION.SDK_INT,
        notificationsEnabled = areEnabled(context),
        prompted = isPrompted(context),
    )

    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }

    fun isPrompted(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PROMPTED, false)

    fun markPrompted(context: Context) {
        prefs(context).edit().putBoolean(KEY_PROMPTED, true).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

enum class AfterPinPrompt {
    Notifications,
    Exact,
    None,
}

fun afterPinPrompt(notifyShouldPrompt: Boolean, exactShouldPrompt: Boolean): AfterPinPrompt =
    when {
        notifyShouldPrompt -> AfterPinPrompt.Notifications
        exactShouldPrompt -> AfterPinPrompt.Exact
        else -> AfterPinPrompt.None
    }

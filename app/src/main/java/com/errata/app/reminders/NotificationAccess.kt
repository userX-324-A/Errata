package com.errata.app.reminders

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Whether due reminders can actually appear. Skip AlarmManager wakeups when they cannot.
 */
object NotificationAccess {
    fun areEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun shouldSkipWakeup(notificationsEnabled: Boolean): Boolean = !notificationsEnabled

    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
}

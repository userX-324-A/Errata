package com.errata.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.errata.app.ErrataApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Restores alarms after boot, clock/timezone change, and package replace.
 * Not direct-boot aware — Room needs credential storage.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in ACTIONS) return
        if (!shouldRun(action, System.currentTimeMillis())) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ErrataApp
                app.reminderScheduler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_QUICKBOOT,
            ACTION_HTC_QUICKBOOT,
        )

        const val ACTION_QUICKBOOT = "android.intent.action.QUICKBOOT_POWERON"
        const val ACTION_HTC_QUICKBOOT = "com.htc.intent.action.QUICKBOOT_POWERON"
        const val CLOCK_DEBOUNCE_MS = 2_000L

        @Volatile
        private var lastClockRescheduleMs = 0L

        fun shouldRun(action: String, nowMs: Long): Boolean {
            if (!isClockAction(action)) return true
            if (nowMs - lastClockRescheduleMs < CLOCK_DEBOUNCE_MS) return false
            lastClockRescheduleMs = nowMs
            return true
        }

        fun isClockAction(action: String): Boolean =
            action == Intent.ACTION_TIME_CHANGED || action == Intent.ACTION_TIMEZONE_CHANGED

        fun shouldDebounceClock(lastMs: Long, nowMs: Long, windowMs: Long = CLOCK_DEBOUNCE_MS): Boolean =
            nowMs - lastMs < windowMs
    }
}

package com.errata.app.ui.common

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar

@Composable
fun isDevice24Hour(): Boolean = DateFormat.is24HourFormat(LocalContext.current)

/** Wall-clock label using the device 12/24 setting (same as TimePicker). */
fun formatClock(context: Context, minutesOfDay: Int): String {
    val clamped = minutesOfDay.coerceIn(0, 24 * 60 - 1)
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, clamped / 60)
    cal.set(Calendar.MINUTE, clamped % 60)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return DateFormat.getTimeFormat(context).format(cal.time)
}

fun formatDateTime(context: Context, epochMs: Long): String =
    DateUtils.formatDateTime(
        context,
        epochMs,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_SHOW_YEAR,
    )

@Composable
fun formatDeviceClock(minutesOfDay: Int): String =
    formatClock(LocalContext.current, minutesOfDay)

@Composable
fun formatDeviceDateTime(epochMs: Long): String =
    formatDateTime(LocalContext.current, epochMs)

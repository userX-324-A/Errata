package com.errata.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.errata.app.MainActivity
import com.errata.app.R
import com.errata.app.data.TaskRepository
import com.errata.app.data.local.TaskEntity
import com.errata.app.domain.widget.WidgetSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetUpdater(
    private val context: Context,
    private val repository: TaskRepository,
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, ErrataWidgetProvider::class.java),
        )
        if (ids.isEmpty()) {
            cancelMidnight()
            return@withContext
        }
        val snapshot = WidgetSnapshot.from(
            items = repository.listSchedulableTasks().map { it.toWidgetItem() },
            nowEpochMs = System.currentTimeMillis(),
        )
        val views = buildViews(context, snapshot)
        ids.forEach { id -> manager.updateAppWidget(id, views) }
        scheduleMidnight()
    }

    private fun scheduleMidnight() {
        val fireAt = WidgetSnapshot.nextLocalMidnightEpochMs()
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            fireAt,
            midnightPendingIntent(),
        )
    }

    private fun cancelMidnight() {
        alarmManager.cancel(midnightPendingIntent())
    }

    private fun midnightPendingIntent(): PendingIntent {
        val intent = Intent(context, ErrataWidgetProvider::class.java).apply {
            action = ErrataWidgetProvider.ACTION_MIDNIGHT
        }
        return PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val MIDNIGHT_REQUEST_CODE = 0x7E11A703

        fun buildViews(context: Context, snapshot: WidgetSnapshot): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.errata_widget)
            val line = if (snapshot.isEmpty) {
                context.getString(R.string.widget_empty)
            } else {
                context.getString(R.string.widget_line, snapshot.count, snapshot.totalMinutes)
            }
            views.setTextViewText(R.id.widget_line, line)
            val tap = PendingIntent.getActivity(
                context,
                TAP_REQUEST_CODE,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, tap)
            return views
        }

        private const val TAP_REQUEST_CODE = 0x7E11A704
    }
}

private fun TaskEntity.toWidgetItem() = WidgetSnapshot.Item(
    estimateMinutes = estimateMinutes,
    nextDueAtEpochMs = nextDueAtEpochMs,
    snoozedUntilEpochMs = snoozedUntilEpochMs,
    isPaused = isPaused,
    isArchived = isArchived,
)

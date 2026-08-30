package com.errata.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.Gravity
import android.view.View
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
        ids.forEach { id ->
            val minHeightDp = manager.getAppWidgetOptions(id)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            manager.updateAppWidget(id, buildViews(context, snapshot, minHeightDp))
        }
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

        fun buildViews(
            context: Context,
            snapshot: WidgetSnapshot,
            minHeightDp: Int = 0,
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.errata_widget)
            val line = if (snapshot.isEmpty) {
                context.getString(R.string.widget_empty)
            } else {
                context.getString(R.string.widget_line, snapshot.count, snapshot.totalMinutes)
            }
            views.setTextViewText(R.id.widget_line, line)
            val slots = WidgetSnapshot.titleSlots(minHeightDp)
            val tall = slots > 0
            views.setTextViewTextSize(
                R.id.widget_wordmark,
                TypedValue.COMPLEX_UNIT_SP,
                if (tall) 14f else 12f,
            )
            views.setTextViewTextSize(
                R.id.widget_line,
                TypedValue.COMPLEX_UNIT_SP,
                if (tall) 20f else 16f,
            )
            views.setInt(
                R.id.widget_root,
                "setGravity",
                if (tall) Gravity.TOP else Gravity.CENTER_VERTICAL,
            )
            val shown = if (tall && !snapshot.isEmpty) snapshot.titles.take(slots) else emptyList()
            TITLE_IDS.forEachIndexed { index, viewId ->
                val title = shown.getOrNull(index)
                if (title == null) {
                    views.setViewVisibility(viewId, View.GONE)
                } else {
                    views.setViewVisibility(viewId, View.VISIBLE)
                    views.setTextViewText(viewId, title)
                }
            }
            val overflow = if (tall && shown.isNotEmpty()) snapshot.overflowCount else 0
            if (overflow > 0) {
                views.setViewVisibility(R.id.widget_more, View.VISIBLE)
                views.setTextViewText(
                    R.id.widget_more,
                    context.getString(R.string.widget_more, overflow),
                )
            } else {
                views.setViewVisibility(R.id.widget_more, View.GONE)
            }
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

        private val TITLE_IDS = intArrayOf(
            R.id.widget_title_1,
            R.id.widget_title_2,
            R.id.widget_title_3,
            R.id.widget_title_4,
        )

        private const val TAP_REQUEST_CODE = 0x7E11A704
    }
}

private fun TaskEntity.toWidgetItem() = WidgetSnapshot.Item(
    estimateMinutes = estimateMinutes,
    nextDueAtEpochMs = nextDueAtEpochMs,
    snoozedUntilEpochMs = snoozedUntilEpochMs,
    isPaused = isPaused,
    isArchived = isArchived,
    title = title,
)

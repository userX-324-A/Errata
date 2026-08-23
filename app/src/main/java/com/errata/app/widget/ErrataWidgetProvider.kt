package com.errata.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.errata.app.ErrataApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ErrataWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_MIDNIGHT) {
            refreshAsync(context)
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        refreshAsync(context)
    }

    override fun onEnabled(context: Context) {
        refreshAsync(context)
    }

    override fun onDisabled(context: Context) {
        refreshAsync(context)
    }

    private fun refreshAsync(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as ErrataApp
                app.widgetUpdater.refresh()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_MIDNIGHT = "com.errata.app.action.WIDGET_MIDNIGHT"
    }
}

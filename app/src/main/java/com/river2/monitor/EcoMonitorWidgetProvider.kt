package com.river2.monitor

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.graphics.toColorInt

class EcoMonitorWidgetProvider : AppWidgetProvider() {
    companion object {

        private fun statusColor(status: String, connected: Boolean): Int {
            if (!connected) return "#94A3B8".toColorInt()
            return when {
                status.contains("charging", true) -> "#38BDF8".toColorInt()
                status.contains("low", true) || status.contains("crit", true) -> "#F87171".toColorInt()
                status.contains("full", true) -> "#34D399".toColorInt()
                else -> "#A7F3D0".toColorInt()
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val store = Store(context.applicationContext)
        for (appWidgetId in appWidgetIds) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val layoutId = if (minWidth >= 220) R.layout.widget_status_large else R.layout.widget_status
            val views = RemoteViews(context.packageName, layoutId)
            bind(context, views, store)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun bind(context: Context, views: RemoteViews, store: Store) {
        val soc = store.widgetSoc
        val connected = store.widgetConnected
        val status = store.widgetStatus.ifBlank { "EcoMonitor" }
        val inW = store.widgetInW
        val outW = store.widgetOutW
        val solar = store.widgetSolar
        val savedWh = store.widgetSavedWh
        val statusTint = statusColor(status, connected)

        val powerLabel = if (connected) "LIVE" else "OFFLINE"
        val socLabel = if (connected) "$soc%" else "--%"
        val savedText = if (savedWh >= 1000.0) {
            "Saved ${String.format(java.util.Locale.US, "%.2f kWh", savedWh / 1000.0)}"
        } else {
            "Saved ${String.format(java.util.Locale.US, "%.0f Wh", savedWh)}"
        }

        views.setTextViewText(R.id.widgetTitle, powerLabel)
        views.setTextViewText(R.id.widgetStatus, status)
        views.setTextViewText(R.id.widgetSoc, socLabel)
        views.setTextViewText(R.id.widgetFlow, "In ${inW}W • Out ${outW}W")
        views.setTextViewText(R.id.widgetSolar, "Solar ${solar}W")
        views.setTextViewText(R.id.widgetSaved, savedText)
        views.setProgressBar(R.id.widgetBatteryBar, 100, soc.coerceIn(0, 100), false)
        views.setInt(R.id.widgetTitle, "setTextColor", statusTint)
        views.setInt(R.id.widgetStatus, "setTextColor", statusTint)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)
    }

    override fun onEnabled(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, EcoMonitorWidgetProvider::class.java)
        onUpdate(context, appWidgetManager, appWidgetManager.getAppWidgetIds(componentName))
    }
}

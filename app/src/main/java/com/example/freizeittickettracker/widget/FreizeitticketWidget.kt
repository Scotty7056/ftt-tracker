package com.example.freizeittickettracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.freizeittickettracker.R
import com.example.freizeittickettracker.data.AppDatabase
import com.example.freizeittickettracker.data.TicketConfig
import com.example.freizeittickettracker.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class FreizeitticketWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val db = AppDatabase.getDatabase(context)
        val visitDao = db.visitDao()
        val configDao = db.ticketConfigDao()

        CoroutineScope(Dispatchers.IO).launch {
            val visits = visitDao.getAllVisits().firstOrNull() ?: emptyList()
            val configEntity = configDao.getConfig().firstOrNull()
            val config = configEntity?.toModel() ?: TicketConfig()

            val saved = visits.sumOf { it.savedAmount }
            val price = config.getPrice()
            val progress = if (price > 0) ((saved / price) * 100).toInt().coerceAtMost(100) else 0

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_layout)
                views.setTextViewText(R.id.widget_saved_value, "%.2f €".format(saved))
                views.setTextViewText(R.id.widget_progress_text, "$progress% amortisiert")
                views.setProgressBar(R.id.widget_progress_bar, 100, progress, false)

                val intent = Intent(context, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
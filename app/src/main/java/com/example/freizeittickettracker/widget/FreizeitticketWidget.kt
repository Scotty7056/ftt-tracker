package com.example.freizeittickettracker.widget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.room.Room
import com.example.freizeittickettracker.data.AppDatabase
import com.example.freizeittickettracker.data.TicketType
import com.example.freizeittickettracker.ui.MainActivity
import kotlinx.coroutines.flow.firstOrNull

class FreizeitticketWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "freizeitticket_db").build()
        val total = db.checkInDao().getTotalSaved().firstOrNull() ?: 0.0
        val cost = TicketType.U23.presalePrice
        val progress = if (cost > 0) (total / cost).toFloat().coerceIn(0f, 1f) else 0f

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surfaceVariant).cornerRadius(16.dp).padding(12.dp).clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Freizeitticket Tirol", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant))
                    Spacer(GlanceModifier.height(6.dp))
                    LinearProgressIndicator(progress = progress, modifier = GlanceModifier.fillMaxWidth().height(8.dp).cornerRadius(4.dp))
                    Spacer(GlanceModifier.height(6.dp))
                    Text("€ %.0f / € %.0f (%.0f%%)".format(total, cost, progress * 100), style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

class FreizeitticketWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FreizeitticketWidget()
}
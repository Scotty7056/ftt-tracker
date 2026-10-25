package com.example.freizeittickettracker.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.freizeittickettracker.R
import com.example.freizeittickettracker.data.AppDatabase
import com.example.freizeittickettracker.data.VenueCatalog
import com.example.freizeittickettracker.data.formatEuro
import com.example.freizeittickettracker.ui.MainActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val triggeredGeofences = event.triggeringGeofences ?: return
        val venueId = triggeredGeofences.firstOrNull()?.requestId ?: return
        val venue = VenueCatalog.findById(venueId) ?: return

        val db = AppDatabase.getDatabase(context)
        val today = LocalDate.now()

        CoroutineScope(Dispatchers.IO).launch {
            val visits = db.visitDao().getAllVisits().first()
            val alreadyVisited = visits.any { it.venueId == venue.id && it.date == today }
            if (alreadyVisited) return@launch

            showInteractiveNotification(context, venue.id, venue.name, venue.normalTicketPrice)
        }
    }

    private fun showInteractiveNotification(
        context: Context,
        venueId: String,
        venueName: String,
        price: Double
    ) {
        val channelId = "freizeitticket_geofence"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Talstation-Erkennung",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Erinnert an das Einbuchen beim Erreichen einer Lift- oder Freizeitstation"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val bookIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "ACTION_BOOK_VISIT"
            putExtra("EXTRA_VENUE_ID", venueId)
        }
        val bookPendingIntent = PendingIntent.getBroadcast(
            context,
            venueId.hashCode(),
            bookIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Bist du bei $venueName?")
            .setContentText("Jetzt Ausflug einbuchen (+${price.formatEuro()} Ersparnis)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "✓ Jetzt einbuchen", bookPendingIntent)
            .build()

        notificationManager.notify(venueId.hashCode(), notification)
    }
}
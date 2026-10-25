package com.example.freizeittickettracker.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.freizeittickettracker.data.AppDatabase
import com.example.freizeittickettracker.data.VenueCatalog
import com.example.freizeittickettracker.data.VisitEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "ACTION_BOOK_VISIT") {
            val venueId = intent.getStringExtra("EXTRA_VENUE_ID") ?: return
            val venue = VenueCatalog.findById(venueId) ?: return

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(venueId.hashCode())

            val db = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                val visit = VisitEntity(
                    venueId = venue.id,
                    venueName = venue.name,
                    category = venue.category.name,
                    date = LocalDate.now(),
                    savedAmount = venue.normalTicketPrice,
                    notes = "Automatisch per Geotagging erfasst"
                )
                db.visitDao().insertVisit(visit)
            }
        }
    }
}
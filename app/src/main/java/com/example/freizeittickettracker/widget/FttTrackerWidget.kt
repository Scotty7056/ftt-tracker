package com.example.freizeittickettracker.widget

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.freizeittickettracker.data.*
import com.example.freizeittickettracker.ui.MainActivity
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate

class FttTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val configEntity = db.ticketConfigDao().getConfig().firstOrNull()
        val visits = db.visitDao().getAllVisits().firstOrNull() ?: emptyList()

        val category = try {
            TarifCategory.valueOf(configEntity?.categoryName ?: TarifCategory.U23.name)
        } catch (_: Exception) {
            TarifCategory.U23
        }
        val isEarlyBird = configEntity?.isEarlyBird ?: true
        val purchasePrice = if (isEarlyBird) category.earlyBirdPrice else category.normalPrice
        val totalSaved = visits.sumOf { it.savedAmount }
        val remaining = (purchasePrice - totalSaved).coerceAtLeast(0.0)
        val progress = if (purchasePrice > 0) (totalSaved / purchasePrice).toFloat().coerceIn(0f, 1f) else 0f
        val isAmortized = totalSaved >= purchasePrice

        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color(0xFF0F1E2E))
                        .cornerRadius(20.dp)
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Header mit Klick zur App
                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity<MainActivity>()),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FTT Tracker",
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = TextStyle(
                                    color = ColorProvider(
                                        if (isAmortized) androidx.compose.ui.graphics.Color(0xFF10B981)
                                        else androidx.compose.ui.graphics.Color(0xFF00B4D8)
                                    ),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // Betrag und 1-Tap Einbuchen Button
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .clickable(actionStartActivity<MainActivity>())
                            ) {
                                Text(
                                    text = totalSaved.formatEuro(),
                                    style = TextStyle(
                                        color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = if (isAmortized) "🎉 Amortisiert!" else "Noch ${remaining.formatEuro()}",
                                    style = TextStyle(
                                        color = ColorProvider(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)),
                                        fontSize = 10.sp
                                    )
                                )
                            }

                            // 1-Tap Action Button
                            Box(
                                modifier = GlanceModifier
                                    .background(androidx.compose.ui.graphics.Color(0xFF0077B6))
                                    .cornerRadius(12.dp)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .clickable(actionRunCallback<QuickBookNearbyActionCallback>()),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚡",
                                        style = TextStyle(fontSize = 11.sp)
                                    )
                                    Spacer(modifier = GlanceModifier.width(4.dp))
                                    Text(
                                        text = "+ Einbuchen",
                                        style = TextStyle(
                                            color = ColorProvider(androidx.compose.ui.graphics.Color.White),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // Fortschrittsbalken
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .cornerRadius(3.dp),
                            color = ColorProvider(
                                if (isAmortized) androidx.compose.ui.graphics.Color(0xFF10B981)
                                else androidx.compose.ui.graphics.Color(0xFF00B4D8)
                            ),
                            backgroundColor = ColorProvider(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f))
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        Row(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .clickable(actionStartActivity<MainActivity>())
                        ) {
                            Text(
                                text = "${visits.size} erfasste Ausflüge",
                                style = TextStyle(
                                    color = ColorProvider(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

class QuickBookNearbyActionCallback : ActionCallback {
    @SuppressLint("MissingPermission")
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = try {
            client.lastLocation.await()
        } catch (_: Exception) {
            null
        }

        // Nächstgelegenes Skigebiet (oder Venue) ermitteln
        val targetVenue = if (location != null) {
            VenueCatalog.venues
                .filter { it.category == VenueCategory.SKI }
                .minByOrNull { v ->
                    calculateDistanceKm(location.latitude, location.longitude, v.latitude, v.longitude)
                } ?: VenueCatalog.venues.first()
        } else {
            // Fallback wenn kein Standort-Fix vorliegt: Nordkette Innsbruck
            VenueCatalog.venues.firstOrNull { it.id == "ski_nordkette" } ?: VenueCatalog.venues.first()
        }

        val db = AppDatabase.getDatabase(context)
        val today = LocalDate.now()

        // Prüfen, ob für heute schon ein Eintrag für dieses Gebiet existiert
        val existingVisits = db.visitDao().getAllVisits().firstOrNull() ?: emptyList()
        val alreadyBookedToday = existingVisits.any { it.venueId == targetVenue.id && it.date == today }

        if (alreadyBookedToday) {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "${targetVenue.name} wurde heute bereits verbucht!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        // Besuch einbuchen
        db.visitDao().insertVisit(
            VisitEntity(
                venueId = targetVenue.id,
                venueName = targetVenue.name,
                category = targetVenue.category.name,
                date = today,
                savedAmount = targetVenue.normalTicketPrice,
                notes = "1-Tap Schnellbuchung via Homescreen-Widget"
            )
        )

        // Widget live aktualisieren
        FttTrackerWidget().updateAll(context)

        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "✅ ${targetVenue.name} (+${targetVenue.normalTicketPrice.formatEuro()}) eingebucht!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

class FttTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FttTrackerWidget()
}
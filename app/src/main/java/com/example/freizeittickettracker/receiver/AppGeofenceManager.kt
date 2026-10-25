package com.example.freizeittickettracker.receiver

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.freizeittickettracker.data.VenueCatalog
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class AppGeofenceManager(private val context: Context) {
    private val client = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun registerAllVenues(onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val geofences = VenueCatalog.venues.map { venue ->
            Geofence.Builder()
                .setRequestId(venue.id)
                .setCircularRegion(venue.latitude, venue.longitude, venue.geofenceRadiusMeters.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                // Smarte Entprellung: Benachrichtigung erst nach 15 Minuten Verweildauer
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(900000) // 15 Min Verweildauer (verhindert Autobahn-Fehlalarme)
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(geofences)
            .build()

        client.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    fun removeAllVenues() {
        client.removeGeofences(geofencePendingIntent)
    }
}
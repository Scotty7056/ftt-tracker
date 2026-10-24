package com.example.freizeittickettracker.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.freizeittickettracker.data.Venue
import com.example.freizeittickettracker.data.VenueCatalog
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationTracker(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getNearbyVenue(): Pair<Venue, Float>? = try {
        val loc = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token).await()
            ?: client.lastLocation.await() ?: null
        if (loc == null) null else {
            var nearest: Venue? = null
            var minDistance = Float.MAX_VALUE
            for (venue in VenueCatalog.venues) {
                if (venue.latitude == null || venue.longitude == null) continue
                val dist = FloatArray(1)
                Location.distanceBetween(loc.latitude, loc.longitude, venue.latitude, venue.longitude, dist)
                if (dist[0] <= venue.radiusMeters && dist[0] < minDistance) {
                    minDistance = dist[0]
                    nearest = venue
                }
            }
            nearest?.let { Pair(it, minDistance) }
        }
    } catch (e: Exception) { null }

    @SuppressLint("MissingPermission")
    fun getLocationFlow(): Flow<Location?> = callbackFlow {
        val req = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000L).build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) { trySend(res.lastLocation) }
        }
        client.lastLocation.addOnSuccessListener { if (it != null) trySend(it) }
        client.requestLocationUpdates(req, cb, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(cb) }
    }

    companion object {
        fun distanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val res = FloatArray(1)
            Location.distanceBetween(lat1, lon1, lat2, lon2, res)
            return res[0] / 1000f
        }
    }
}
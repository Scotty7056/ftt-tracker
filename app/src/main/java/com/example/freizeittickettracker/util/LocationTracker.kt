package com.example.freizeittickettracker.util

import android.location.Location
import com.example.freizeittickettracker.data.Venue
import com.example.freizeittickettracker.ui.calculateDistanceKm

object LocationTracker {
    fun findNearbyVenue(userLocation: Location, venues: List<Venue>): Venue? {
        return venues.firstOrNull { venue ->
            val distanceKm = calculateDistanceKm(
                userLocation.latitude,
                userLocation.longitude,
                venue.latitude,
                venue.longitude
            )
            (distanceKm * 1000.0) <= venue.geofenceRadiusMeters
        }
    }
}
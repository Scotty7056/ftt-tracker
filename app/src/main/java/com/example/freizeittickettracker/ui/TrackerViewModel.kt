package com.example.freizeittickettracker.ui

import android.annotation.SuppressLint
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.freizeittickettracker.data.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UserLocation(val latitude: Double, val longitude: Double)

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val visitDao = db.visitDao()
    private val configDao = db.configDao()

    val allVisits: Flow<List<VisitEntity>> = visitDao.getAllVisits()
    val recentVisits: Flow<List<VisitEntity>> = visitDao.getAllVisits().map { it.take(10) }

    val ticketConfig: Flow<TicketConfig> = configDao.getConfig().map { entity ->
        entity?.toModel() ?: TicketConfig()
    }

    private val _currentLocation = MutableStateFlow<UserLocation?>(null)
    val currentLocation: StateFlow<UserLocation?> = _currentLocation.asStateFlow()

    val dashboardStats: Flow<DashboardStats> = combine(allVisits, ticketConfig) { visits, config ->
        val totalSaved = visits.sumOf { it.savedAmount }
        val price = config.getPrice()
        val remaining = (price - totalSaved).coerceAtLeast(0.0)
        val netSavings = (totalSaved - price).coerceAtLeast(0.0)
        val progress = if (price > 0) (totalSaved / price).toFloat() else 0f
        val isAmortized = totalSaved >= price

        DashboardStats(
            tarifTitle = "${config.category.label} (${if (config.isEarlyBird) "Vorverkauf" else "Normal"})",
            purchasePrice = price,
            totalValueSaved = totalSaved,
            remainingToAmortize = remaining,
            netSavings = netSavings,
            amortizationProgress = progress,
            isAmortized = isAmortized,
            totalVisits = visits.size
        )
    }

    fun saveTicketConfig(config: TicketConfig) {
        viewModelScope.launch {
            configDao.saveConfig(config.toEntity())
        }
    }

    fun recordVisit(venue: Venue, date: LocalDate, notes: String = "") {
        viewModelScope.launch {
            val visit = VisitEntity(
                venueId = venue.id,
                venueName = venue.name,
                category = venue.category.name,
                date = date,
                savedAmount = venue.normalTicketPrice,
                notes = notes
            )
            visitDao.insertVisit(visit)
        }
    }

    fun updateVisit(visit: VisitEntity) {
        viewModelScope.launch {
            visitDao.updateVisit(visit)
        }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch {
            visitDao.deleteVisit(visit)
        }
    }

    @SuppressLint("MissingPermission")
    fun updateLocation() {
        val client = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
        client.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                _currentLocation.value = UserLocation(loc.latitude, loc.longitude)
            }
        }
    }
}
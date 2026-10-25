package com.example.freizeittickettracker.ui

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.freizeittickettracker.data.*
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    val ticketConfig: Flow<TicketConfig> = database.ticketConfigDao().getConfig()
        .map { it?.toModel() ?: TicketConfig() }

    val allVisits: Flow<List<VisitEntity>> = database.visitDao().getAllVisits()

    val recentVisits: Flow<List<VisitEntity>> = database.visitDao().getRecentVisits()

    val dashboardStats: Flow<DashboardStats> = combine(ticketConfig, allVisits) { config, visits ->
        val purchasePrice = config.getPrice()
        val totalSaved = visits.sumOf { it.savedAmount }
        val remaining = (purchasePrice - totalSaved).coerceAtLeast(0.0)
        val netSavings = (totalSaved - purchasePrice).coerceAtLeast(0.0)
        val progress = if (purchasePrice > 0) (totalSaved / purchasePrice).toFloat() else 0f

        DashboardStats(
            tarifTitle = "${config.category.label} (${if (config.isEarlyBird) "Vorverkauf" else "Normal"})",
            purchasePrice = purchasePrice,
            totalValueSaved = totalSaved,
            remainingToAmortize = remaining,
            netSavings = netSavings,
            amortizationProgress = progress,
            isAmortized = totalSaved >= purchasePrice,
            totalVisits = visits.size
        )
    }

    init {
        updateLocation()
    }

    @SuppressLint("MissingPermission")
    fun updateLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    _currentLocation.value = loc
                }
            }
        } catch (_: SecurityException) {}
    }

    fun saveTicketConfig(config: TicketConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            database.ticketConfigDao().saveConfig(config.toEntity())
        }
    }

    fun recordVisit(venue: Venue, date: LocalDate = LocalDate.now(), notes: String = "", customAmount: Double? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val visit = VisitEntity(
                venueId = venue.id,
                venueName = venue.name,
                category = venue.category.name,
                date = date,
                savedAmount = customAmount ?: venue.normalTicketPrice,
                notes = notes
            )
            database.visitDao().insertVisit(visit)
        }
    }

    fun updateVisit(visit: VisitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.visitDao().updateVisit(visit)
        }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.visitDao().deleteVisit(visit)
        }
    }
}
package com.example.freizeittickettracker.ui

import android.annotation.SuppressLint
import android.app.Application
import android.location.Location
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.freizeittickettracker.data.*
import com.example.freizeittickettracker.widget.FttTrackerWidget
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

    private val _selectedSeason = MutableStateFlow(determineSeason(LocalDate.now()))
    val selectedSeason: StateFlow<String> = _selectedSeason.asStateFlow()

    val ticketConfig: Flow<TicketConfig> = database.ticketConfigDao().getConfig()
        .map { it?.toModel() ?: TicketConfig() }

    val allVisits: Flow<List<VisitEntity>> = database.visitDao().getAllVisits()

    val seasonVisits: Flow<List<VisitEntity>> = combine(allVisits, _selectedSeason) { visits, season ->
        visits.filter { it.season == season }
    }

    val availableSeasons: Flow<List<String>> = allVisits.map { visits ->
        val current = determineSeason(LocalDate.now())
        (visits.map { it.season } + current).distinct().sortedDescending()
    }

    val dashboardStats: Flow<DashboardStats> = combine(ticketConfig, seasonVisits, _selectedSeason, availableSeasons) { config, visits, season, seasons ->
        val purchasePrice = config.getPrice()
        val totalSaved = visits.sumOf { it.savedAmount }
        val remaining = (purchasePrice - totalSaved).coerceAtLeast(0.0)
        val netSavings = (totalSaved - purchasePrice).coerceAtLeast(0.0)
        val progress = if (purchasePrice > 0) (totalSaved / purchasePrice).toFloat() else 0f

        DashboardStats(
            season = season,
            tarifTitle = "${config.category.label} (${if (config.isEarlyBird) "Vorverkauf" else "Normal"})",
            purchasePrice = purchasePrice,
            totalValueSaved = totalSaved,
            remainingToAmortize = remaining,
            netSavings = netSavings,
            amortizationProgress = progress,
            isAmortized = totalSaved >= purchasePrice,
            totalVisits = visits.size,
            availableSeasons = seasons
        )
    }

    init {
        updateLocation()
    }

    fun selectSeason(season: String) {
        _selectedSeason.value = season
    }

    @SuppressLint("MissingPermission")
    fun updateLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) _currentLocation.value = loc
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
                notes = notes,
                season = determineSeason(date)
            )
            database.visitDao().insertVisit(visit)
            triggerAutoBackup()
            updateGlanceWidget()
        }
    }

    fun updateVisit(visit: VisitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.visitDao().updateVisit(visit.copy(season = determineSeason(visit.date)))
            triggerAutoBackup()
            updateGlanceWidget()
        }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.visitDao().deleteVisit(visit)
            triggerAutoBackup()
            updateGlanceWidget()
        }
    }

    private suspend fun triggerAutoBackup() {
        val all = database.visitDao().getAllVisits().first()
        AutoBackupManager.performAutoBackup(getApplication(), all)
    }

    private suspend fun updateGlanceWidget() {
        try {
            FttTrackerWidget().updateAll(getApplication())
        } catch (_: Exception) {}
    }
}
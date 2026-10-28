package com.example.freizeittickettracker.data

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

fun getSeasonForDate(date: LocalDate): String {
    val year = date.year
    return if (date.monthValue >= 10) {
        "$year/${year + 1}"
    } else {
        "${year - 1}/$year"
    }
}

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TrackerRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = TrackerRepository(db.visitDao(), db.ticketConfigDao())
    }

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    private val _selectedSeason = MutableStateFlow(getSeasonForDate(LocalDate.now()))
    val selectedSeason: StateFlow<String> = _selectedSeason.asStateFlow()

    val ticketConfig: Flow<TicketConfig> = repository.ticketConfigFlow

    val allVisits: Flow<List<VisitEntity>> = repository.allVisits

    val availableSeasons: StateFlow<List<String>> = repository.allVisits
        .map { visits ->
            val fromVisits = visits.map { it.season }.distinct()
            val currentYear = LocalDate.now().year
            val defaultSeasons = (currentYear downTo currentYear - 3).map { "$it/${it + 1}" }
            (defaultSeasons + fromVisits).distinct().sortedDescending()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            listOf("2026/2027", "2025/2026", "2024/2025", "2023/2024")
        )

    val seasonVisits: Flow<List<VisitEntity>> = combine(repository.allVisits, _selectedSeason) { visits, season ->
        visits.filter { it.season == season }.sortedByDescending { it.date }
    }

    val dashboardStats: Flow<DashboardStats> = combine(seasonVisits, ticketConfig, _selectedSeason) { visits, config, season ->
        val totalValue = visits.sumOf { it.savedAmount }
        val purchasePrice = if (config.isEarlyBird) config.category.earlyBirdPrice else config.category.normalPrice
        val remaining = (purchasePrice - totalValue).coerceAtLeast(0.0)
        val netSavings = (totalValue - purchasePrice).coerceAtLeast(0.0)
        val progress = if (purchasePrice > 0.0) (totalValue / purchasePrice).toFloat() else 0f

        DashboardStats(
            season = season,
            totalVisits = visits.size,
            totalValueSaved = totalValue,
            purchasePrice = purchasePrice,
            remainingToAmortize = remaining,
            netSavings = netSavings,
            amortizationProgress = progress,
            isAmortized = totalValue >= purchasePrice,
            tarifTitle = config.category.label + (if (config.isEarlyBird) " (Vorverkauf)" else "")
        )
    }

    fun selectSeason(season: String) {
        _selectedSeason.value = season
    }

    fun saveTicketConfig(config: TicketConfig) {
        viewModelScope.launch {
            repository.saveTicketConfig(config)
        }
    }

    fun recordVisit(venue: Venue, date: LocalDate = LocalDate.now(), notes: String = "", customAmount: Double? = null) {
        viewModelScope.launch {
            val amount = customAmount ?: venue.normalTicketPrice
            val calculatedSeason = getSeasonForDate(date)
            repository.insertVisit(
                VisitEntity(
                    venueId = venue.id,
                    venueName = venue.name,
                    category = venue.category.name,
                    date = date,
                    season = calculatedSeason,
                    savedAmount = amount,
                    notes = notes
                )
            )
        }
    }

    fun updateVisit(visit: VisitEntity) {
        viewModelScope.launch {
            val updatedSeason = getSeasonForDate(visit.date)
            repository.updateVisit(visit.copy(season = updatedSeason))
        }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch {
            repository.deleteVisit(visit)
        }
    }

    fun importVisitsFromCsv(csvText: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val visits = AutoBackupManager.parseCsvToVisits(csvText)
            for (v in visits) {
                repository.insertVisit(v)
            }
            onComplete(visits.size)
        }
    }

    fun checkAutoPriceSync() {
        viewModelScope.launch {
            PriceSyncManager.syncPrices(getApplication(), force = false)
        }
    }

    fun manualPriceSync(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = PriceSyncManager.syncPrices(getApplication(), force = true)
            result.onSuccess { count ->
                onResult(true, "$count Tarife erfolgreich aktualisiert!")
            }.onFailure { err ->
                onResult(false, "Fehler beim Laden: ${err.localizedMessage}")
            }
        }
    }

    fun updateLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    _currentLocation.value = loc
                }
            }
        } catch (_: SecurityException) {}
    }
}
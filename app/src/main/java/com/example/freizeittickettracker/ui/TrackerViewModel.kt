package com.example.freizeittickettracker.ui

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.freizeittickettracker.data.*
import com.example.freizeittickettracker.data.weather.VenueWeather
import com.example.freizeittickettracker.data.weather.WeatherRepository
import com.example.freizeittickettracker.util.LocationTracker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class UiState(
    val selectedTicketType: TicketType = TicketType.U23,
    val isPresale: Boolean = true,
    val checkIns: List<CheckInEntity> = emptyList(),
    val totalSaved: Double = 0.0,
    val nearbyVenue: Pair<Venue, Float>? = null,
    val currentLocation: Location? = null,
    val winterStats: SeasonStats = SeasonStats(Season.WINTER, 0.0, 0, emptyMap()),
    val summerStats: SeasonStats = SeasonStats(Season.SUMMER, 0.0, 0, emptyMap())
) {
    val ticketCost: Double get() = if (isPresale) selectedTicketType.presalePrice else selectedTicketType.regularPrice
    val balance: Double get() = totalSaved - ticketCost
    val progress: Float get() = if (ticketCost > 0) (totalSaved / ticketCost).toFloat().coerceIn(0f, 1f) else 0f
}

class TrackerViewModel(
    private val dao: CheckInDao,
    private val locationTracker: LocationTracker
) : ViewModel() {
    private val weatherRepo = WeatherRepository()
    private val _userTicket = MutableStateFlow(Pair(TicketType.U23, true))
    private val _nearbyVenue = MutableStateFlow<Pair<Venue, Float>?>(null)
    private val _currentVenueWeather = MutableStateFlow<VenueWeather?>(null)
    val currentVenueWeather: StateFlow<VenueWeather?> = _currentVenueWeather.asStateFlow()

    val uiState: StateFlow<UiState> = combine(
        _userTicket,
        dao.getAllCheckIns(),
        dao.getTotalSaved(),
        _nearbyVenue,
        locationTracker.getLocationFlow()
    ) { (ticket, isPresale), checkIns, totalSaved, nearby, location ->
        val winterList = checkIns.filter { Season.fromTimestamp(it.timestamp) == Season.WINTER }
        val summerList = checkIns.filter { Season.fromTimestamp(it.timestamp) == Season.SUMMER }
        fun stats(s: Season, l: List<CheckInEntity>) = SeasonStats(
            s, l.sumOf { it.savedAmount }, l.size,
            l.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.savedAmount } }
        )
        UiState(
            selectedTicketType = ticket, isPresale = isPresale, checkIns = checkIns,
            totalSaved = totalSaved ?: 0.0, nearbyVenue = nearby, currentLocation = location,
            winterStats = stats(Season.WINTER, winterList), summerStats = stats(Season.SUMMER, summerList)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun checkCurrentLocation() {
        viewModelScope.launch { _nearbyVenue.value = locationTracker.getNearbyVenue() }
    }
    fun dismissNearbyVenue() { _nearbyVenue.value = null }

    fun recordVisit(venue: Venue) {
        viewModelScope.launch {
            dao.insertCheckIn(CheckInEntity(0, venue.id, venue.name, venue.category.displayName, venue.getPriceFor(_userTicket.value.first)))
            if (_nearbyVenue.value?.first?.id == venue.id) _nearbyVenue.value = null
        }
    }

    fun recordVisitForDate(venue: Venue, date: LocalDate) {
        viewModelScope.launch {
            val ts = date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            dao.insertCheckIn(CheckInEntity(0, venue.id, venue.name, venue.category.displayName, venue.getPriceFor(_userTicket.value.first), ts))
        }
    }

    suspend fun isAlreadyCheckedInToday(venueId: String): Boolean = isAlreadyCheckedInOnDate(venueId, LocalDate.now())

    suspend fun isAlreadyCheckedInOnDate(venueId: String, date: LocalDate): Boolean {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return dao.countVisitsForVenueOnDay(venueId, start, end) > 0
    }

    fun removeVisit(checkIn: CheckInEntity) { viewModelScope.launch { dao.deleteCheckIn(checkIn) } }
    fun updateTicket(type: TicketType, isPresale: Boolean) { _userTicket.value = Pair(type, isPresale) }

    fun loadWeatherForVenue(venue: Venue) {
        if (venue.latitude == null || venue.longitude == null) { _currentVenueWeather.value = null; return }
        viewModelScope.launch { _currentVenueWeather.value = weatherRepo.getVenueWeather(venue.latitude, venue.longitude).getOrNull() }
    }
    fun clearWeather() { _currentVenueWeather.value = null }
}
package com.example.freizeittickettracker.ui

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.freizeittickettracker.data.*
import com.example.freizeittickettracker.widget.FreizeitticketWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val visitDao = db.visitDao()
    private val configDao = db.configDao()

    val allVisits: StateFlow<List<VisitEntity>> = visitDao.getAllVisits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentVisits: StateFlow<List<VisitEntity>> = visitDao.getRecentVisits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ticketConfig: StateFlow<TicketConfig> = configDao.getConfig()
        .map { it?.toModel() ?: TicketConfig() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TicketConfig())

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    val dashboardStats: StateFlow<DashboardStats> = combine(allVisits, ticketConfig) { visits, config ->
        val price = config.getPrice()
        val saved = visits.sumOf { it.savedAmount }
        val remaining = (price - saved).coerceAtLeast(0.0)
        val net = (saved - price).coerceAtLeast(0.0)
        val progress = if (price > 0) (saved / price).toFloat() else 0f

        DashboardStats(
            tarifTitle = config.category.label + if (config.isEarlyBird) " (Vorverkauf)" else "",
            purchasePrice = price,
            totalValueSaved = saved,
            remainingToAmortize = remaining,
            netSavings = net,
            amortizationProgress = progress,
            isAmortized = saved >= price,
            totalVisits = visits.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    fun recordVisit(venue: Venue, date: LocalDate = LocalDate.now(), notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val visit = VisitEntity(
                venueId = venue.id,
                venueName = venue.name,
                category = venue.category.name,
                date = date,
                savedAmount = venue.normalTicketPrice,
                notes = notes
            )
            visitDao.insertVisit(visit)
            notifyWidgetUpdate()
        }
    }

    fun deleteVisit(visit: VisitEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            visitDao.deleteVisit(visit)
            notifyWidgetUpdate()
        }
    }

    fun saveTicketConfig(newConfig: TicketConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            configDao.saveConfig(newConfig.toEntity())
            notifyWidgetUpdate()
        }
    }

    fun updateLocation() {
        // GPS-Aktualisierung
    }

    private fun notifyWidgetUpdate() {
        val context = getApplication<Application>()
        val intent = Intent(context, FreizeitticketWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, FreizeitticketWidget::class.java)
            )
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        }
        context.sendBroadcast(intent)
    }
}

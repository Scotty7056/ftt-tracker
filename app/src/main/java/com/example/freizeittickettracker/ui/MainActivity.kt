package com.example.freizeittickettracker.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.freizeittickettracker.data.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: TrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF1E88E5),
                secondary = Color(0xFF26A69A),
                tertiary = Color(0xFFFFA000)
            )) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: TrackerViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTarifDialog by remember { mutableStateOf(false) }
    val ticketConfig by viewModel.ticketConfig.collectAsState(initial = TicketConfig())

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.updateLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.updateLocation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Freizeitticket Tracker", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { showTarifDialog = true }) {
                        Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mein Ticket")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Kalender") },
                    label = { Text("Kalender") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Karte") },
                    label = { Text("Karte") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.List, contentDescription = "Partner") },
                    label = { Text("Partner") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardView(viewModel, onOpenTarifDialog = { showTarifDialog = true })
                1 -> CalendarView(viewModel)
                2 -> MapOverviewView(viewModel)
                3 -> PartnerListView(viewModel)
            }
        }

        if (showTarifDialog) {
            TarifSelectionDialog(
                currentConfig = ticketConfig,
                onDismiss = { showTarifDialog = false },
                onSave = { newConfig ->
                    viewModel.saveTicketConfig(newConfig)
                    showTarifDialog = false
                }
            )
        }
    }
}

// ==========================================
// 1. DASHBOARD
// ==========================================
@Composable
fun DashboardView(viewModel: TrackerViewModel, onOpenTarifDialog: () -> Unit) {
    val stats by viewModel.dashboardStats.collectAsState(initial = DashboardStats())
    val visits by viewModel.recentVisits.collectAsState(initial = emptyList())
    var showAddVisitDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${stats.tarifTitle} (${stats.purchasePrice.formatEuro()})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = onOpenTarifDialog) {
                            Icon(Icons.Default.Edit, contentDescription = "Tarif ändern")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { stats.amortizationProgress.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = if (stats.isAmortized) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Eingefahren:", style = MaterialTheme.typography.bodySmall)
                            Text(stats.totalValueSaved.formatEuro(), fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (stats.isAmortized) "Reiner Gewinn:" else "Noch offen:", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (stats.isAmortized) "+${stats.netSavings.formatEuro()}" else stats.remainingToAmortize.formatEuro(),
                                fontWeight = FontWeight.Bold,
                                color = if (stats.isAmortized) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Besuche", style = MaterialTheme.typography.bodyMedium)
                        Text("${stats.totalVisits}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Quote", style = MaterialTheme.typography.bodyMedium)
                        Text("${(stats.amortizationProgress * 100).toInt()}%", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Button(
                onClick = { showAddVisitDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddLocationAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Besuch manuell erfassen")
            }
        }

        item {
            Text("Letzte Aktivitäten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (visits.isEmpty()) {
            item {
                Text(
                    "Noch keine Besuche erfasst. Besuche ein Skigebiet oder Hallenbad oder trage manuell einen Ausflug ein!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            items(visits) { visit ->
                VisitItemCard(visit = visit, onDelete = { viewModel.deleteVisit(visit) })
            }
        }
    }

    if (showAddVisitDialog) {
        ManualVisitDialog(
            venues = VenueCatalog.venues,
            onDismiss = { showAddVisitDialog = false },
            onConfirm = { venue, date, notes ->
                viewModel.recordVisit(venue, date, notes)
                showAddVisitDialog = false
            }
        )
    }
}

// ==========================================
// 2. KALENDER
// ==========================================
@Composable
fun CalendarView(viewModel: TrackerViewModel) {
    val visits by viewModel.allVisits.collectAsState(initial = emptyList())
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val daysInMonth = remember(currentMonth) {
        val firstDay = currentMonth.atDay(1)
        val firstDayOfWeek = firstDay.dayOfWeek.value // 1 = Mo, 7 = So
        val length = currentMonth.lengthOfMonth()
        val days = mutableListOf<LocalDate?>()
        for (i in 1 until firstDayOfWeek) days.add(null)
        for (i in 1..length) days.add(currentMonth.atDay(i))
        days
    }

    val visitDates = remember(visits) {
        visits.map { it.date }.toSet()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Vormonat")
            }
            Text(
                text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Nächster Monat")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach {
                Text(it, fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(daysInMonth) { date ->
                if (date != null) {
                    val hasVisit = visitDates.contains(date)
                    val isSelected = date == selectedDate
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    hasVisit -> Color(0xFFC8E6C9)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable { selectedDate = date },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${date.dayOfMonth}",
                            fontWeight = if (hasVisit || isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (hasVisit) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.aspectRatio(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider()
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Besuche am ${selectedDate.format(DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN))}:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        val selectedVisits = visits.filter { it.date == selectedDate }
        if (selectedVisits.isEmpty()) {
            Text("Keine Einträge für diesen Tag.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                items(selectedVisits) { visit ->
                    VisitItemCard(visit = visit, onDelete = { viewModel.deleteVisit(visit) })
                }
            }
        }
    }
}

// ==========================================
// 3. GOOGLE MAPS KARTENÜBERSICHT
// ==========================================
@Composable
fun MapOverviewView(viewModel: TrackerViewModel) {
    val venues = VenueCatalog.venues
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize()) { mv ->
        mv.getMapAsync { googleMap ->
            googleMap.uiSettings.isZoomControlsEnabled = true
            val innsbruck = LatLng(47.2692, 11.4041)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(innsbruck, 9.5f))

            googleMap.clear()
            venues.forEach { venue ->
                val markerColor = when (venue.category) {
                    VenueCategory.SKI -> BitmapDescriptorFactory.HUE_AZURE
                    VenueCategory.POOL -> BitmapDescriptorFactory.HUE_BLUE
                    VenueCategory.ICE -> BitmapDescriptorFactory.HUE_CYAN
                    VenueCategory.CULTURE -> BitmapDescriptorFactory.HUE_ORANGE
                }
                googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(venue.latitude, venue.longitude))
                        .title(venue.name)
                        .snippet("Tagestarif: ${venue.normalTicketPrice.formatEuro()}")
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                )
            }
        }
    }
}

// ==========================================
// 4. PARTNERLISTE & WETTER
// ==========================================
@Composable
fun PartnerListView(viewModel: TrackerViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<VenueCategory?>(null) }
    val userLocation by viewModel.currentLocation.collectAsState(initial = null)

    val filteredVenues = remember(searchQuery, selectedCategory) {
        VenueCatalog.venues.filter { venue ->
            (searchQuery.isEmpty() || venue.name.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategory == null || venue.category == selectedCategory)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Skigebiet, Bad oder Museum suchen...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("Alle") }
            )
            FilterChip(
                selected = selectedCategory == VenueCategory.SKI,
                onClick = { selectedCategory = VenueCategory.SKI },
                label = { Text("Ski") }
            )
            FilterChip(
                selected = selectedCategory == VenueCategory.POOL,
                onClick = { selectedCategory = VenueCategory.POOL },
                label = { Text("Bäder") }
            )
            FilterChip(
                selected = selectedCategory == VenueCategory.CULTURE,
                onClick = { selectedCategory = VenueCategory.CULTURE },
                label = { Text("Kultur") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredVenues) { venue ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(venue.name, fontWeight = FontWeight.Bold)
                            Text("Normalpreis: ${venue.normalTicketPrice.formatEuro()}", style = MaterialTheme.typography.bodySmall)
                            userLocation?.let { loc ->
                                val dist = calculateDistanceKm(loc.latitude, loc.longitude, venue.latitude, venue.longitude)
                                Text("Entfernung: ${"%.1f".format(dist)} km", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// HILFSKOMPONENTEN & DIALOGE
// ==========================================
@Composable
fun VisitItemCard(visit: VisitEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(visit.venueName, fontWeight = FontWeight.SemiBold)
                Text(visit.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("+${visit.savedAmount.formatEuro()}", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Löschen", tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun ManualVisitDialog(
    venues: List<Venue>,
    onDismiss: () -> Unit,
    onConfirm: (Venue, LocalDate, String) -> Unit
) {
    var selectedVenue by remember { mutableStateOf(venues.first()) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Besuch manuell eintragen") },
        text = {
            Column {
                Text("Zielort wählen:")
                venues.take(6).forEach { v ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedVenue = v }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedVenue.id == v.id, onClick = { selectedVenue = v })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(v.name, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedVenue, LocalDate.now(), notes) }) {
                Text("Eintragen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@Composable
fun TarifSelectionDialog(
    currentConfig: TicketConfig,
    onDismiss: () -> Unit,
    onSave: (TicketConfig) -> Unit
) {
    var isEarlyBird by remember { mutableStateOf(currentConfig.isEarlyBird) }
    var selectedCategory by remember { mutableStateOf(currentConfig.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ticket Tarif wählen") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isEarlyBird, onCheckedChange = { isEarlyBird = it })
                    Text("Vorverkaufstarif")
                }
                Spacer(modifier = Modifier.height(8.dp))
                TarifCategory.values().forEach { category ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { selectedCategory = category }.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedCategory == category, onClick = { selectedCategory = category })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(category.label, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(TicketConfig(category = selectedCategory, isEarlyBird = isEarlyBird)) }) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

fun Double.formatEuro(): String = "%.2f €".format(this)

fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}
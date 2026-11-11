package com.example.freizeittickettracker.ui






import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.freizeittickettracker.domain.FamilyTarifEngine

import com.example.freizeittickettracker.ui.components.FamilyProfilesDialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.example.freizeittickettracker.ui.components.StoryShareDialog
import com.example.freizeittickettracker.domain.StorySummaryData

import com.example.freizeittickettracker.ui.components.FamilyCheckInSelector
import com.example.freizeittickettracker.data.model.PersonProfile


import androidx.compose.foundation.BorderStroke
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.freizeittickettracker.data.*
import com.example.freizeittickettracker.receiver.AppGeofenceManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth
import java.util.Locale

val AlpineDark = Color(0xFF0F1E2E)
val AlpineNavy = Color(0xFF1B2E4B)
val GlacierBlue = Color(0xFF0077B6)
val FrostTeal = Color(0xFF00B4D8)
val EmeraldGain = Color(0xFF10B981)
val CoralWarning = Color(0xFFE53935)
val CardSurface = Color(0xFFF8FAFC)

@Serializable
data class OpenMeteoResponse(val current: CurrentWeather? = null)

@Serializable
data class CurrentWeather(
    val temperature_2m: Double = 0.0,
    val relative_humidity_2m: Double = 0.0,
    val apparent_temperature: Double = 0.0,
    val weather_code: Int = 0,
    val wind_speed_10m: Double = 0.0
)

class MainActivity : ComponentActivity() {
    private val viewModel: TrackerViewModel by viewModels()
    private val venueToOpenFromIntent = mutableStateOf<Venue?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val osmConfig = Configuration.getInstance()
        osmConfig.load(this, PreferenceManager.getDefaultSharedPreferences(this))
        osmConfig.userAgentValue = "FTTTracker/1.2 (Android; contact: info@ftt-tracker.local)"

        val osmCacheDir = File(cacheDir, "osm_topo_tiles")
        if (!osmCacheDir.exists()) osmCacheDir.mkdirs()
        osmConfig.osmdroidBasePath = osmCacheDir
        osmConfig.osmdroidTileCache = osmCacheDir

        extractVenueFromIntent(intent)

        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = GlacierBlue,
                    secondary = FrostTeal,
                    surface = CardSurface,
                    background = Color(0xFFF1F5F9)
                )
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        viewModel = viewModel,
                        initialDetailVenue = venueToOpenFromIntent.value,
                        onClearInitialVenue = { venueToOpenFromIntent.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractVenueFromIntent(intent)
    }

    private fun extractVenueFromIntent(intent: Intent?) {
        val venueId = intent?.getStringExtra("EXTRA_OPEN_VENUE_ID")
        if (!venueId.isNullOrBlank()) {
            VenueCatalog.findById(venueId)?.let { venue ->
                venueToOpenFromIntent.value = venue
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: TrackerViewModel,
    initialDetailVenue: Venue? = null,
    onClearInitialVenue: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var targetMapVenue by remember { mutableStateOf<Venue?>(null) }
    var selectedDetailVenue by remember { mutableStateOf<Venue?>(null) }

    LaunchedEffect(initialDetailVenue) {
        if (initialDetailVenue != null) {
            selectedDetailVenue = initialDetailVenue
            onClearInitialVenue()
        }
    }

    var showTarifDialog by remember { mutableStateOf(false) }
    var showFamilyDialog by remember { mutableStateOf(false) }
    var visitPendingDeletion by remember { mutableStateOf<VisitEntity?>(null) }
    var visitPendingEdit by remember { mutableStateOf<VisitEntity?>(null) }
    var showBackgroundRationaleDialog by remember { mutableStateOf(false) }

    val ticketConfig by viewModel.ticketConfig.collectAsState(initial = TicketConfig())
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val availableSeasons by viewModel.availableSeasons.collectAsState(initial = listOf(selectedSeason))
    val allVisits by viewModel.allVisits.collectAsState(initial = emptyList())
        val context = LocalContext.current
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { targetUri ->
            pendingCsvContent?.let { content ->
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "\u2705 CSV erfolgreich gespeichert", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "\u274C Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val geofenceManager = remember { AppGeofenceManager(context) }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) geofenceManager.registerAllVenues()
    }

    val basePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            viewModel.updateLocation()
            viewModel.checkAutoPriceSync()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasBackground = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                if (!hasBackground) showBackgroundRationaleDialog = true else geofenceManager.registerAllVenues()
            } else {
                geofenceManager.registerAllVenues()
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (permissionsToRequest.isNotEmpty()) {
            basePermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            viewModel.updateLocation()
            viewModel.checkAutoPriceSync()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    showBackgroundRationaleDialog = true
                } else {
                    geofenceManager.registerAllVenues()
                }
            } else {
                geofenceManager.registerAllVenues()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(end = 4.dp)
                    ) {
                        Text(
                            "🎿 FTT Tracker",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        var expandedSeason by remember { mutableStateOf(false) }
                        Box {
                            FilledTonalButton(
                                onClick = { expandedSeason = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.22f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(selectedSeason, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            DropdownMenu(expanded = expandedSeason, onDismissRequest = { expandedSeason = false }) {
                                availableSeasons.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("Saison $s", fontWeight = if (s == selectedSeason) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            viewModel.selectSeason(s)
                                            expandedSeason = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        FilledTonalButton(
                            onClick = { showTarifDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Tarif", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        FilledTonalButton(
                            onClick = {
                                showFamilyDialog = true
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Einstellungen", modifier = Modifier.size(15.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AlpineNavy)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val items = listOf(
                    Triple(0, "Dashboard", Icons.Rounded.Dashboard),
                    Triple(1, "Kalender", Icons.Rounded.CalendarMonth),
                    Triple(2, "Statistik", Icons.Rounded.BarChart),
                    Triple(3, "Karte", Icons.Rounded.Map),
                    Triple(4, "Partner", Icons.Rounded.Explore)
                )
                items.forEach { (index, label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, softWrap = false) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GlacierBlue,
                            indicatorColor = GlacierBlue.copy(alpha = 0.12f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> DashboardView(
                    viewModel = viewModel,
                    onOpenTarifDialog = { showTarifDialog = true },
                    onRequestEditVisit = { visitPendingEdit = it },
                    onRequestDeleteVisit = { visitPendingDeletion = it }
                )
                1 -> CalendarView(
                    viewModel = viewModel,
                    onRequestEditVisit = { visitPendingEdit = it },
                    onRequestDeleteVisit = { visitPendingDeletion = it }
                )
                2 -> StatisticsView(viewModel = viewModel)
                3 -> OsmMapOverviewView(viewModel = viewModel, focusVenue = targetMapVenue)
                4 -> PartnerListView(
                    viewModel = viewModel,
                    onNavigateToMap = { venue ->
                        targetMapVenue = venue
                        selectedTab = 3
                    },
                    onOpenDetail = { venue ->
                        selectedDetailVenue = venue
                    }
                )
            }
        }

        selectedDetailVenue?.let { venue ->
            VenueDetailBottomSheet(
                venue = venue,
                existingVisits = allVisits,
                onDismiss = { selectedDetailVenue = null },
                onNavigateToMap = {
                    targetMapVenue = venue
                    selectedDetailVenue = null
                    selectedTab = 3
                },
                onBookVisit = { chosenDate, customPrice, note ->
                    viewModel.recordVisit(venue, chosenDate, note, customPrice)
                    Toast.makeText(
                        context,
                        "✅ ${venue.name} (${chosenDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}) eingebucht!",
                        Toast.LENGTH_SHORT
                    ).show()
                    selectedDetailVenue = null
                }
            )
        }

        if (showTarifDialog) {
            val seasonConfig = viewModel.getTicketConfigForSeason(selectedSeason, ticketConfig)
            TarifSelectionDialog(
                season = selectedSeason,
                currentConfig = seasonConfig,
                viewModel = viewModel,
                onDismiss = { showTarifDialog = false },
                onSave = { newConfig ->
                    viewModel.saveTicketConfig(newConfig, selectedSeason)
                    showTarifDialog = false
                }
            )
        }

        if (showFamilyDialog) {
            val personsList by viewModel.allPersons.collectAsState(initial = emptyList())
            val currentSeason by viewModel.selectedSeason.collectAsState(initial = "2025/2026")
            FamilySettingsDialog(
                viewModel = viewModel,
                persons = personsList,
                season = currentSeason,
                onDismiss = { showFamilyDialog = false }
            )
        }

        if (showBackgroundRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showBackgroundRationaleDialog = false },
                icon = { Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = GlacierBlue, modifier = Modifier.size(36.dp)) },
                title = { Text("Standorterfassung im Hintergrund", fontWeight = FontWeight.Bold, color = AlpineDark) },
                text = {
                    Text("FTT Tracker erfasst deinen Standort auch dann, wenn die App geschlossen ist oder nicht aktiv genutzt wird, um automatische Besuche an teilnehmenden Bergbahnen und Partnergebieten nach ca. 15 Minuten Verweildauer zu erkennen.\n\nDeine Standortdaten werden ausschließlich lokal auf deinem Gerät verarbeitet und niemals weitergegeben.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showBackgroundRationaleDialog = false
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue)
                    ) {
                        Text("Verstanden & Weiter")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackgroundRationaleDialog = false }) { Text("Nicht jetzt", color = Color.Gray) }
                }
            )
        }

        visitPendingEdit?.let { visit ->
            val allPersonsList by viewModel.allPersons.collectAsState(initial = emptyList())
            val crossRefs by viewModel.allCrossRefs.collectAsState(initial = emptyList())
            val initialPersonIds = remember(crossRefs, visit.id) {
                crossRefs.filter { it.visitId == visit.id }.map { it.personId }
            }

            EditVisitDialog(
                visit = visit,
                allPersons = allPersonsList,
                initialPersonIds = initialPersonIds,
                onDismiss = { visitPendingEdit = null },
                onSave = { updatedDate, updatedAmount, updatedNotes, updatedPersonIds ->
                    val updated = visit.copy(
                        date = updatedDate,
                        savedAmount = updatedAmount,
                        notes = updatedNotes
                    )
                    viewModel.saveVisitWithPersons(updated, updatedPersonIds)
                    visitPendingEdit = null
                }
            )
        }

        visitPendingDeletion?.let { visit ->
            AlertDialog(
                onDismissRequest = { visitPendingDeletion = null },
                icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = CoralWarning, modifier = Modifier.size(36.dp)) },
                title = { Text("Eintrag l\u00F6schen?", fontWeight = FontWeight.Bold, color = AlpineDark) },
                text = { Text("Möchtest du '${visit.venueName}' vom ${visit.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} (+${visit.savedAmount.formatEuro()}) wirklich entfernen?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteVisit(visit)
                            visitPendingDeletion = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralWarning)
                    ) {
                        Text("l\u00F6schen", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { visitPendingDeletion = null }) { Text("Abbrechen", color = AlpineNavy) }
                }
            )
        }
    }
}

// ==========================================
// 1. DASHBOARD
// ==========================================
@Composable
fun DashboardView(
    viewModel: TrackerViewModel,
    onOpenTarifDialog: () -> Unit,
    onRequestEditVisit: (VisitEntity) -> Unit,
    onRequestDeleteVisit: (VisitEntity) -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsState(initial = DashboardStats())
    val visits by viewModel.seasonVisits.collectAsState(initial = emptyList())
    val allPersonsForFilter by viewModel.allPersons.collectAsState(initial = emptyList())
    val crossRefs by viewModel.allCrossRefs.collectAsState(initial = emptyList())
    var selectedPersonFilterId by remember { mutableStateOf<Long?>(null) }

    val displayVisits = remember(visits, crossRefs, selectedPersonFilterId) {
        if (selectedPersonFilterId == null) {
            visits
        } else {
            val matchingVisitIds = crossRefs.filter { it.personId == selectedPersonFilterId }.map { it.visitId }.toSet()
            visits.filter { it.id in matchingVisitIds }
        }
    }

    val selectedPerson = remember(allPersonsForFilter, selectedPersonFilterId) {
        allPersonsForFilter.find { it.id == selectedPersonFilterId }
    }

    val effectiveTicketPrice = remember(selectedPerson, stats.purchasePrice, stats.season) {
        if (selectedPerson != null) {
            FamilyTarifEngine.calculateTicketPrice(selectedPerson, stats.season)
        } else {
            stats.purchasePrice
        }
    }

    val effectiveSavedAmount = remember(displayVisits) {
        displayVisits.sumOf { it.savedAmount }
    }

    val effectiveProgress = remember(effectiveSavedAmount, effectiveTicketPrice) {
        if (effectiveTicketPrice > 0) (effectiveSavedAmount / effectiveTicketPrice).toFloat().coerceIn(0f, 1f) else 0f
    }

    val effectiveRemaining = remember(effectiveSavedAmount, effectiveTicketPrice) {
        (effectiveTicketPrice - effectiveSavedAmount).coerceAtLeast(0.0)
    }

    val effectiveNetGain = remember(effectiveSavedAmount, effectiveTicketPrice) {
        (effectiveSavedAmount - effectiveTicketPrice).coerceAtLeast(0.0)
    }
    val allVisits by viewModel.allVisits.collectAsState(initial = emptyList())
    var showAddVisitDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
                if (allPersonsForFilter.isNotEmpty()) {
            item {
                // PersonFilterChipRow
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    items(allPersonsForFilter) { person ->
                        FilterChip(
                            selected = selectedPersonFilterId == person.id,
                            onClick = {
                                selectedPersonFilterId = person.id
                            },
                            label = { Text(person.name, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlacierBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = if (effectiveSavedAmount >= effectiveTicketPrice && effectiveTicketPrice > 0) listOf(Color(0xFF0F3E2E), EmeraldGain)
                                else listOf(AlpineDark, AlpineNavy, GlacierBlue)
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "SAISON ${stats.season} • ${stats.tarifTitle.uppercase(Locale.GERMAN)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Kaufpreis: ${effectiveTicketPrice.formatEuro()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onOpenTarifDialog,
                                modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.15f)).size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Bearbeiten", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("EINGEFAHRENER WERT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f), letterSpacing = 1.sp)
                                Text(effectiveSavedAmount.formatEuro(), fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White, softWrap = false)
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (effectiveSavedAmount >= effectiveTicketPrice && effectiveTicketPrice > 0) EmeraldGain else Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "${if (effectiveTicketPrice > 0) ((effectiveSavedAmount / effectiveTicketPrice) * 100).toInt() else 0}%",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { effectiveProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = if (effectiveSavedAmount >= effectiveTicketPrice && effectiveTicketPrice > 0) EmeraldGain else FrostTeal,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (effectiveSavedAmount >= effectiveTicketPrice && effectiveTicketPrice > 0) "🎉 Ticket amortisiert!" else "Noch offen zum Break-Even:",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (effectiveSavedAmount >= effectiveTicketPrice && effectiveTicketPrice > 0) "+${effectiveNetGain.formatEuro()} Plus" else effectiveRemaining.formatEuro(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (effectiveSavedAmount >= effectiveTicketPrice && effectiveTicketPrice > 0) EmeraldGain else Color(0xFFFFD166),
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val personVisitsCount = displayVisits.size
                MetricTile("aktivitäten", personVisitsCount.toString(), Icons.Rounded.EventAvailable, GlacierBlue, Modifier.weight(1f))
                val avg = if (personVisitsCount > 0) effectiveSavedAmount / personVisitsCount else 0.0
                val (rightTitle, rightValue) = if (effectiveSavedAmount >= effectiveTicketPrice && effectiveTicketPrice > 0) {
                    "Reingewinn" to ("+" + effectiveNetGain.formatEuro())
                } else {
                    val needed = if (avg > 0.0) kotlin.math.ceil(effectiveRemaining / avg).toInt() else 0
                    "Noch nötig" to ("ca. " + needed + "x")
                }
                MetricTile(rightTitle, rightValue, Icons.Rounded.TrendingUp, EmeraldGain, Modifier.weight(1f))
            }
        }

        item {
            Button(
                onClick = { showAddVisitDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Icon(Icons.Rounded.AddLocationAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Besuch manuell Eintr\u00E4gen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Aktivitäten",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlpineDark
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GlacierBlue.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${displayVisits.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlacierBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Saison ${stats.season}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
            }
        }

        if (displayVisits.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(28.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.Hiking, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Keine Ausflüge in Saison ${stats.season}", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    }
                }
            }
        } else {
            items(displayVisits) { visit ->
                
                    ModernVisitCard(visit = visit,
                        onEdit = { onRequestEditVisit(visit) },
                        onDelete = { onRequestDeleteVisit(visit) }
                    )
            }
        }

        item {
            val currentContext = LocalContext.current
            val vName = remember(currentContext) {
                try {
                    currentContext.packageManager.getPackageInfo(currentContext.packageName, 0)?.versionName ?: "1.2"
                } catch (_: Exception) {
                    "1.2"
                }
            }

            Text(
                text = "FTT Tracker v$vName",
                fontSize = 11.sp,
                color = Color.LightGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showAddVisitDialog) {
        val allPersonsList by viewModel.allPersons.collectAsState(initial = emptyList())
        ManualVisitDialog(
            venues = VenueCatalog.venues,
            allPersons = allPersonsList,
            existingVisits = allVisits,
            onDismiss = { showAddVisitDialog = false },
            onConfirm = { venue, date, amount, notes, personIds ->
                val season = getSeasonForDate(date)
                val newVisit = VisitEntity(
                    venueId = venue.id,
                    venueName = venue.name,
                    category = venue.category.name,
                    date = date,
                    savedAmount = amount,
                    season = season,
                    notes = notes
                )
                viewModel.saveVisitWithPersons(newVisit, personIds)
                showAddVisitDialog = false
            }
        )
    }
}

// ==========================================
// 2. STATISTIKEN, EXPORT & IMPORT
// ==========================================
enum class StoryTheme(val title: String, val subtitle: String) {
    ALPINE_DARK("Alpine Dark", "Dunkler High-Contrast Modus"),
    GLACIER_SNOW("Glacier Light", "Heller Schnee-Look"),
    SUNSET_PEAK("Sunset Peak", "Warmer Alpenglühen-Verlauf")
}

data class StoryColors(
    val bgTop: String,
    val bgBottom: String,
    val cardBg: String,
    val textPrimary: String,
    val textSecondary: String,
    val accentColor: String
)

fun generateStoryShareImage(
    context: android.content.Context,
    season: String,
    personName: String,
    visitsCount: Int,
    savedAmount: Double,
    ticketPrice: Double,
    theme: StoryTheme = StoryTheme.ALPINE_DARK
): android.net.Uri? {
    return try {
        val width = 1080
        val height = 1920
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Theme Farben definieren
        val colors = when (theme) {
            StoryTheme.ALPINE_DARK -> StoryColors(
                bgTop = "#0F172A", bgBottom = "#1E293B", cardBg = "#1E293B",
                textPrimary = "#FFFFFF", textSecondary = "#94A3B8", accentColor = "#38BDF8"
            )
            StoryTheme.GLACIER_SNOW -> StoryColors(
                bgTop = "#F8FAFC", bgBottom = "#E2E8F0", cardBg = "#FFFFFF",
                textPrimary = "#0F172A", textSecondary = "#64748B", accentColor = "#0284C7"
            )
            StoryTheme.SUNSET_PEAK -> StoryColors(
                bgTop = "#31103F", bgBottom = "#1A0B2E", cardBg = "#2D1B4E",
                textPrimary = "#FFFFFF", textSecondary = "#F9A8D4", accentColor = "#F43F5E"
            )
        }
        val bgTop = colors.bgTop
        val bgBottom = colors.bgBottom
        val cardBg = colors.cardBg
        val textPrimary = colors.textPrimary
        val textSecondary = colors.textSecondary
        val accentColor = colors.accentColor

        // Hintergrund mit linearem Farbverlauf
        val bgPaint = android.graphics.Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                android.graphics.Color.parseColor(bgTop),
                android.graphics.Color.parseColor(bgBottom),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Brand / Tagline
        val brandPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor(accentColor)
            textSize = 38f
            isFakeBoldText = true
            letterSpacing = 0.12f
        }
        canvas.drawText("FREIZEITTICKET TRACKER", 100f, 220f, brandPaint)

        // Titel
        val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor(textPrimary)
            textSize = 76f
            isFakeBoldText = true
        }
        canvas.drawText("Saison-Rückblick", 100f, 320f, titlePaint)

        // Saison & Name
        val seasonPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor(textSecondary)
            textSize = 46f
        }
        val personLabel = if (personName.isNotBlank()) " • $personName" else ""
        canvas.drawText("Saison $season$personLabel", 100f, 390f, seasonPaint)

        // Card 1: Eingefahrener Wert
        val cardPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor(cardBg)
        }
        val cardBorder = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = if (theme == StoryTheme.GLACIER_SNOW) android.graphics.Color.parseColor("#CBD5E1") else android.graphics.Color.parseColor("#334155")
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = 3f
        }
        val cardRect = android.graphics.RectF(100f, 480f, 980f, 940f)
        canvas.drawRoundRect(cardRect, 44f, 44f, cardPaint)
        canvas.drawRoundRect(cardRect, 44f, 44f, cardBorder)

        val cardLabelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor(textSecondary)
            textSize = 34f
            isFakeBoldText = true
            letterSpacing = 0.08f
        }
        canvas.drawText("EINGEFAHRENER GESAMTWERT", 160f, 580f, cardLabelPaint)

        val valuePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor(textPrimary)
            textSize = 96f
            isFakeBoldText = true
        }
        canvas.drawText(String.format(java.util.Locale.GERMAN, "%.2f €", savedAmount), 160f, 700f, valuePaint)

        val isAmortized = savedAmount >= ticketPrice && ticketPrice > 0
        val statusText = if (isAmortized) {
            String.format(java.util.Locale.GERMAN, "🎉 Break-Even erreicht (+%.2f €)", (savedAmount - ticketPrice))
        } else {
            String.format(java.util.Locale.GERMAN, "Noch %.2f € bis zum Break-Even", (ticketPrice - savedAmount).coerceAtLeast(0.0))
        }
        val statusPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isAmortized) android.graphics.Color.parseColor("#10B981") else android.graphics.Color.parseColor(accentColor)
            textSize = 38f
            isFakeBoldText = true
        }
        canvas.drawText(statusText, 160f, 830f, statusPaint)

        // Card 2: Aktivitäten
        val statCardRect = android.graphics.RectF(100f, 1000f, 980f, 1280f)
        canvas.drawRoundRect(statCardRect, 44f, 44f, cardPaint)
        canvas.drawRoundRect(statCardRect, 44f, 44f, cardBorder)

        canvas.drawText("ABSOLVIERTE AUSFLÜGE", 160f, 1100f, cardLabelPaint)
        canvas.drawText("$visitsCount Aktivitäten", 160f, 1200f, valuePaint)

        // Footer
        val footerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor(textSecondary)
            textSize = 34f
        }
        canvas.drawText("Getrackt mit dem Freizeitticket Tracker", 100f, 1800f, footerPaint)

        val cacheFolder = java.io.File(context.cacheDir, "story_shares").apply { mkdirs() }
        val file = java.io.File(cacheFolder, "ftt_story_${System.currentTimeMillis()}.png")
        val stream = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        android.util.Log.e("FTT_SHARE", "Fehler bei Story-Grafik", e)
        null
    }
}

@Composable
fun StatisticsView(viewModel: TrackerViewModel) {
    
    val stats by viewModel.dashboardStats.collectAsState(initial = DashboardStats())
    val visits by viewModel.seasonVisits.collectAsState(initial = emptyList())
    val allPersonsForFilter by viewModel.allPersons.collectAsState(initial = emptyList())
    val crossRefs by viewModel.allCrossRefs.collectAsState(initial = emptyList())
    var selectedPersonFilterId by remember { mutableStateOf<Long?>(null) }
    var showThemeChooserDialog by remember { mutableStateOf(false) }



    val displayVisits = remember(visits, crossRefs, selectedPersonFilterId) {
        if (selectedPersonFilterId == null) {
            visits
        } else {
            val matchingVisitIds = crossRefs.filter { it.personId == selectedPersonFilterId }.map { it.visitId }.toSet()
            visits.filter { it.id in matchingVisitIds }
        }
    }

    val selectedPerson = remember(allPersonsForFilter, selectedPersonFilterId) {
        allPersonsForFilter.find { it.id == selectedPersonFilterId }
    }

    val effectiveTicketPrice = remember(selectedPerson, stats.purchasePrice, stats.season) {
        if (selectedPerson != null) {
            FamilyTarifEngine.calculateTicketPrice(selectedPerson, stats.season)
        } else {
            stats.purchasePrice
        }
    }

    val effectiveSavedAmount = remember(displayVisits) {
        displayVisits.sumOf { it.savedAmount }
    }

    val effectiveProgress = remember(effectiveSavedAmount, effectiveTicketPrice) {
        if (effectiveTicketPrice > 0) (effectiveSavedAmount / effectiveTicketPrice).toFloat().coerceIn(0f, 1f) else 0f
    }

    val effectiveRemaining = remember(effectiveSavedAmount, effectiveTicketPrice) {
        (effectiveTicketPrice - effectiveSavedAmount).coerceAtLeast(0.0)
    }

    val effectiveNetGain = remember(effectiveSavedAmount, effectiveTicketPrice) {
        (effectiveSavedAmount - effectiveTicketPrice).coerceAtLeast(0.0)
    }
        val context = LocalContext.current
                    Toast.makeText(context, "\u2705 CSV erfolgreich gespeichert", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "\u274C Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    viewModel.importVisitsFromCsv(text) { count ->
                        Toast.makeText(context, "✅ $count Eintr\u00E4ge erfolgreich importiert!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Fehler beim Import: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val categoryStats = remember(displayVisits) {
        val total = displayVisits.sumOf { it.savedAmount }
        VenueCategory.entries.mapNotNull { cat ->
            val catVisits = displayVisits.filter { it.category == cat.name }
            if (catVisits.isEmpty()) null
            else {
                val sum = catVisits.sumOf { it.savedAmount }
                val pct = if (total > 0) (sum / total).toFloat() else 0f
                CategoryShareStat(cat, catVisits.size, sum, pct)
            }
        }.sortedByDescending { it.totalSaved }
    }

    val topVenues = remember(displayVisits) { displayVisits.groupBy { it.venueName }
            .map { (name, list) -> VenueTopStat(name, list.size, list.sumOf { it.savedAmount }) }
            .sortedByDescending { it.count }
            .take(3)
    }

    val costPerSkiDay = remember(effectiveTicketPrice, displayVisits) {
        val skiVisits = displayVisits.count { it.category == VenueCategory.SKI.name }
        if (skiVisits > 0) effectiveTicketPrice / skiVisits else 0.0
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (allPersonsForFilter.isNotEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    items(allPersonsForFilter) { person ->
                        FilterChip(
                            selected = selectedPersonFilterId == person.id,
                            onClick = { selectedPersonFilterId = person.id },
                            label = { Text(person.name, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GlacierBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("FTT WRAPPED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GlacierBlue, letterSpacing = 1.sp)
                            Text("Saison-Rückblick ${stats.season}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AlpineDark)
                        }
                        IconButton(
                        onClick = { showThemeChooserDialog = true },
                            modifier = Modifier.clip(CircleShape).background(GlacierBlue.copy(alpha = 0.1f)).size(40.dp)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Teilen", tint = GlacierBlue, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Rechnerischer Preis pro Skitag:", fontSize = 13.sp, color = Color.Gray)
                        Text(if (costPerSkiDay > 0) costPerSkiDay.formatEuro() else "–", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineDark)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Kategorie-Aufteilung", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AlpineDark)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (categoryStats.isEmpty()) {
                        Text("Noch keine Daten für diese Saison.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        categoryStats.forEach { stat ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${stat.category.iconLabel} (${stat.count}x)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("${stat.totalSaved.formatEuro()} (${(stat.percentage * 100).toInt()}%)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GlacierBlue)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { stat.percentage },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = when (stat.category) {
                                        VenueCategory.SKI -> GlacierBlue
                                        VenueCategory.SLED -> Color(0xFFE65100)
                                        VenueCategory.ICE -> Color(0xFF7B1FA2)
                                        VenueCategory.POOL -> Color(0xFF00838F)
                                        VenueCategory.CULTURE -> Color(0xFFD84315)
                                    },
                                    trackColor = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Top Favoriten (${stats.season})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AlpineDark)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (topVenues.isEmpty()) {
                        Text("Noch keine Gebiete eingetragen.", fontSize = 13.sp, color = Color.Gray)
                    } else {
                        topVenues.forEachIndexed { idx, venue ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${idx + 1}. ${venue.venueName}", fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Text("${venue.count} Besuche (${venue.totalSaved.formatEuro()})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = EmeraldGain)
                            }
                        }
                    }
                }
            }
        }

            }
        }
    }
    if (showThemeChooserDialog) {
        val pName = selectedPerson?.name ?: ""
        StoryPreviewCarouselDialog(
            season = stats.season,
            personName = pName,
            visitsCount = displayVisits.size,
            savedAmount = effectiveSavedAmount,
            ticketPrice = effectiveTicketPrice,
            onDismiss = { showThemeChooserDialog = false },
            onShare = { bitmap ->
                showThemeChooserDialog = false
                val imageUri = saveBitmapToCache(context.applicationContext, bitmap)
                if (imageUri != null) {
                    shareStoryImage(context, imageUri)
                } else {
                    android.widget.Toast.makeText(context, "Fehler beim Erstellen der Grafik", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
// ==========================================
// 3. KALENDER & KARTE
// ==========================================
@Composable
fun CalendarView(
    viewModel: TrackerViewModel,
    onRequestEditVisit: (VisitEntity) -> Unit,
    onRequestDeleteVisit: (VisitEntity) -> Unit
) {
    val visits by viewModel.allVisits.collectAsState(initial = emptyList())
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val daysInMonth = remember(currentMonth) {
        val firstDay = currentMonth.atDay(1)
        val firstDayOfWeek = firstDay.dayOfWeek.value
        val length = currentMonth.lengthOfMonth()
        val days = mutableListOf<LocalDate?>()
        for (i in 1 until firstDayOfWeek) days.add(null)
        for (i in 1..length) days.add(currentMonth.atDay(i))
        days
    }

    val visitDates = remember(visits) { visits.map { it.date }.toSet() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Zur\u00FCck")
                    }
                    Text(
                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.GERMAN)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AlpineDark
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Vor")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So").forEach {
                        Text(it, fontWeight = FontWeight.SemiBold, color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.fillMaxWidth()) {
                    items(daysInMonth) { date ->
                        if (date != null) {
                            val hasVisit = visitDates.contains(date)
                            val isSelected = date == selectedDate
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .padding(3.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> GlacierBlue
                                            hasVisit -> EmeraldGain.copy(alpha = 0.25f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable { selectedDate = date },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${date.dayOfMonth}",
                                    fontWeight = if (hasVisit || isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> Color.White
                                        hasVisit -> EmeraldGain
                                        else -> AlpineDark
                                    },
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.aspectRatio(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Ausflüge am ${selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy", Locale.GERMAN))}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = AlpineDark
        )

        val selectedVisits = visits.filter { it.date == selectedDate }
        if (selectedVisits.isEmpty()) {
            Text("Kein Ausflug an diesem Tag eingetragen.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectedVisits) { visit ->
                    
                    ModernVisitCard(visit = visit,
                        onEdit = { onRequestEditVisit(visit) },
                        onDelete = { onRequestDeleteVisit(visit) }
                    )
                }
            }
        }
    }
}

@Composable
fun OsmMapOverviewView(
    viewModel: TrackerViewModel,
    focusVenue: Venue? = null
) {
    val venues = VenueCatalog.venues
        val context = LocalContext.current
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { targetUri ->
            pendingCsvContent?.let { content ->
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "\u2705 CSV erfolgreich gespeichert", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "\u274C Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val mapView = remember { MapView(context) }
    val markerMap = remember { mutableMapOf<String, Marker>() }
    val userLocation by viewModel.currentLocation.collectAsState(initial = null)
    var userLocationMarker by remember { mutableStateOf<Marker?>(null) }
    val allVisits by viewModel.allVisits.collectAsState(initial = emptyList())
    val today = remember { LocalDate.now() }
    val visitedVenueIdsToday = remember(allVisits) {
        allVisits.filter { it.date == today }.map { it.venueId }.toSet()
    }

    val openTopoTileSource = remember {
        XYTileSource(
            "OpenTopoMap",
            1,
            17,
            256,
            ".png",
            arrayOf("https://a.tile.opentopomap.org/", "https://b.tile.opentopomap.org/", "https://c.tile.opentopomap.org/"),
            "Kartendaten: © OpenStreetMap-Mitwirkende, SRTM | Darstellung: © OpenTopoMap (CC-BY-SA)"
        )
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    LaunchedEffect(focusVenue) {
        focusVenue?.let { v ->
            mapView.controller.animateTo(GeoPoint(v.latitude, v.longitude), 14.0, 1000L)
            markerMap[v.id]?.showInfoWindow()
        }
    }

    LaunchedEffect(userLocation) {
        userLocation?.let { loc ->
            val geoPoint = GeoPoint(loc.latitude, loc.longitude)
            if (userLocationMarker == null) {
                val marker = Marker(mapView).apply {
                    position = geoPoint
                    title = "Mein Standort"
                    icon = createMyLocationMarkerDrawable(context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                mapView.overlays.add(marker)
                userLocationMarker = marker
            } else {
                userLocationMarker?.position = geoPoint
            }
            mapView.invalidate()
        }
    }

    LaunchedEffect(visitedVenueIdsToday) {
        venues.forEach { v ->
            markerMap[v.id]?.let { m ->
                val isVisited = visitedVenueIdsToday.contains(v.id)
                val currentPrice = PriceSyncManager.getCustomPrice(context, v.id, v.normalTicketPrice)
                val (pinColor, emoji) = when (v.category) {
                    VenueCategory.SKI -> android.graphics.Color.parseColor("#0077B6") to "⛷️"
                    VenueCategory.SLED -> android.graphics.Color.parseColor("#E65100") to "🛷"
                    VenueCategory.ICE -> android.graphics.Color.parseColor("#7B1FA2") to "⛸️"
                    VenueCategory.POOL -> android.graphics.Color.parseColor("#00838F") to "🏊"
                    VenueCategory.CULTURE -> android.graphics.Color.parseColor("#D84315") to "🏛️"
                }
                m.icon = createOsmMarkerDrawable(context, emoji, pinColor, isVisited)
                m.snippet = if (isVisited) {
                    "${v.category.displayName} • ${currentPrice.formatEuro()}\n✅ Heute bereits erfasst"
                } else {
                    "${v.category.displayName} • ${currentPrice.formatEuro()}\n👉 Pin oder Text gedrückt halten zum Einbuchen"
                }
            }
        }
        mapView.invalidate()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    tileProvider.clearTileCache()
                    setTileSource(openTopoTileSource)
                    setMultiTouchControls(true)
                    controller.setZoom(9.5)
                    controller.setCenter(GeoPoint(47.2692, 11.4041))

                    overlays.clear()
                    markerMap.clear()
                    userLocationMarker = null

                    venues.forEach { venue ->
                        val isVisited = visitedVenueIdsToday.contains(venue.id)
                        val effectivePrice = PriceSyncManager.getCustomPrice(context, venue.id, venue.normalTicketPrice)

                        val marker = object : Marker(this) {
                            override fun onLongPress(event: android.view.MotionEvent?, mapView: MapView?): Boolean {
                                val touched = hitTest(event, mapView)
                                if (touched) {
                                    val isAlreadyDone = allVisits.any { it.venueId == venue.id && it.date == LocalDate.now() }
                                    if (isAlreadyDone) {
                                        Toast.makeText(
                                            context,
                                            "⚠️ ${venue.name} wurde heute bereits verbucht!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return true
                                    }

                                    mapView?.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                    viewModel.recordVisit(
                                        venue = venue,
                                        date = LocalDate.now(),
                                        notes = "Per Schnellbuchung (Pin) erfasst",
                                        customAmount = effectivePrice
                                    )
                                    Toast.makeText(
                                        context,
                                        "⚡ ${venue.name} (+${effectivePrice.formatEuro()}) direkt eingebucht!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    closeInfoWindow()
                                    return true
                                }
                                return super.onLongPress(event, mapView)
                            }
                        }

                        marker.position = GeoPoint(venue.latitude, venue.longitude)
                        marker.title = venue.name
                        marker.snippet = if (isVisited) {
                            "${venue.category.displayName} • ${effectivePrice.formatEuro()}\n✅ Heute bereits erfasst"
                        } else {
                            "${venue.category.displayName} • ${effectivePrice.formatEuro()}\n👉 Pin oder Text gedrückt halten zum Einbuchen"
                        }

                        val (pinColor, emoji) = when (venue.category) {
                            VenueCategory.SKI -> android.graphics.Color.parseColor("#0077B6") to "⛷️"
                            VenueCategory.SLED -> android.graphics.Color.parseColor("#E65100") to "🛷"
                            VenueCategory.ICE -> android.graphics.Color.parseColor("#7B1FA2") to "⛸️"
                            VenueCategory.POOL -> android.graphics.Color.parseColor("#00838F") to "🏊"
                            VenueCategory.CULTURE -> android.graphics.Color.parseColor("#D84315") to "🏛️"
                        }
                        marker.icon = createOsmMarkerDrawable(context, emoji, pinColor, isVisited)
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                        marker.infoWindow = object : org.osmdroid.views.overlay.infowindow.BasicInfoWindow(
                            org.osmdroid.library.R.layout.bonuspack_bubble,
                            this
                        ) {
                            override fun onOpen(item: Any?) {
                                super.onOpen(item)
                                mView?.isClickable = true
                                mView?.isLongClickable = true

                                val triggerBooking = {
                                    val isAlreadyDone = allVisits.any { it.venueId == venue.id && it.date == LocalDate.now() }
                                    if (isAlreadyDone) {
                                        Toast.makeText(
                                            context,
                                            "⚠️ ${venue.name} wurde heute bereits verbucht!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        mView?.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                                        viewModel.recordVisit(
                                            venue = venue,
                                            date = LocalDate.now(),
                                            notes = "Per Schnellbuchung (Bubble) erfasst",
                                            customAmount = effectivePrice
                                        )
                                        Toast.makeText(
                                            context,
                                            "✅ ${venue.name} (+${effectivePrice.formatEuro()}) eingebucht!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        close()
                                    }
                                }

                                mView?.setOnLongClickListener {
                                    triggerBooking()
                                    true
                                }

                                val bubbleText = mView?.findViewById<android.widget.TextView>(org.osmdroid.library.R.id.bubble_description)
                                bubbleText?.isLongClickable = true
                                bubbleText?.setOnLongClickListener {
                                    triggerBooking()
                                    true
                                }
                            }
                        }

                        overlays.add(marker)
                        markerMap[venue.id] = marker
                    }
                    invalidate()
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        FloatingActionButton(
            onClick = {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                val isGpsEnabled = locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                                   locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true

                if (!isGpsEnabled) {
                    Toast.makeText(context, "Standortdienste sind deaktiviert. Bitte aktiviere GPS.", Toast.LENGTH_SHORT).show()
                    try {
                        context.startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    } catch (_: Exception) {}
                    return@FloatingActionButton
                }

                viewModel.updateLocation()
                userLocation?.let { loc ->
                    mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude), 13.5, 900L)
                } ?: run {
                    Toast.makeText(context, "Suche GPS-Signal...", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 24.dp),
            containerColor = Color.White,
            contentColor = GlacierBlue,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = "Mein Standort", modifier = Modifier.size(24.dp))
        }
    }
}

// ==========================================
// 4. PARTNERLISTE & DETAILS
// ==========================================
@Composable
fun PartnerListView(
    viewModel: TrackerViewModel,
    onNavigateToMap: (Venue) -> Unit,
    onOpenDetail: (Venue) -> Unit
) {
        val context = LocalContext.current
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { targetUri ->
            pendingCsvContent?.let { content ->
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "\u2705 CSV erfolgreich gespeichert", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "\u274C Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<VenueCategory?>(null) }
    val userLocation by viewModel.currentLocation.collectAsState(initial = null)

    val filteredVenues = remember(searchQuery, selectedCategory) {
        VenueCatalog.venues.filter { venue ->
            (searchQuery.isEmpty() || venue.name.contains(searchQuery, ignoreCase = true) || venue.subtitle.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategory == null || venue.category == selectedCategory)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Gebiet, Bahn, Bad oder Rodelbahn...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = GlacierBlue) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = GlacierBlue
            )
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                label = { Text("Alle (${VenueCatalog.venues.size})", fontSize = 12.sp, fontWeight = FontWeight.Medium, softWrap = false) },
                shape = RoundedCornerShape(12.dp)
            )
            for (cat in VenueCategory.entries) {
                val count = VenueCatalog.venues.count { it.category == cat }
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text("${cat.iconLabel} ($count)", fontSize = 12.sp, fontWeight = FontWeight.Medium, softWrap = false) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filteredVenues) { venue ->
                val effectivePrice = PriceSyncManager.getCustomPrice(context, venue.id, venue.normalTicketPrice)
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDetail(venue) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadge(category = venue.category)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(venue.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (venue.subtitle.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(venue.subtitle, fontSize = 11.sp, color = Color.DarkGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Regulärer Tarif: ${effectivePrice.formatEuro()}", fontSize = 12.sp, color = Color.Gray, softWrap = false)
                        }
                        userLocation?.let { loc ->
                            Spacer(modifier = Modifier.width(8.dp))
                            val dist = calculateDistanceKm(loc.latitude, loc.longitude, venue.latitude, venue.longitude)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GlacierBlue.copy(alpha = 0.12f),
                                modifier = Modifier.clickable { onNavigateToMap(venue) }
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = "Auf Karte zeigen", tint = GlacierBlue, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${"%.0f".format(dist)} km", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GlacierBlue, softWrap = false)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueDetailBottomSheet(
    venue: Venue,
    existingVisits: List<VisitEntity> = emptyList(),
    onDismiss: () -> Unit,
    onNavigateToMap: () -> Unit,
    onBookVisit: (LocalDate, Double, String) -> Unit
) {
        val context = LocalContext.current
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { targetUri ->
            pendingCsvContent?.let { content ->
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "\u2705 CSV erfolgreich gespeichert", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "\u274C Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val effectivePrice = remember(venue) { PriceSyncManager.getCustomPrice(context, venue.id, venue.normalTicketPrice) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var priceText by remember(effectivePrice) { mutableStateOf(String.format(Locale.US, "%.2f", effectivePrice)) }
    var visitNotes by remember { mutableStateOf("") }
    var showDuplicateWarning by remember { mutableStateOf(false) }

    var weatherData by remember { mutableStateOf<CurrentWeather?>(null) }
    var isLoadingWeather by remember { mutableStateOf(true) }

    LaunchedEffect(venue) {
        isLoadingWeather = true
        weatherData = try {
            withContext(Dispatchers.IO) {
                val client = HttpClient(Android) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                }
                val response: OpenMeteoResponse = client.get("https://api.open-meteo.com/v1/forecast") {
                    parameter("latitude", venue.latitude)
                    parameter("longitude", venue.longitude)
                    parameter("current", "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m")
                }.body()
                client.close()
                response.current
            }
        } catch (_: Exception) {
            null
        } finally {
            isLoadingWeather = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(category = venue.category, size = 40.dp, emojiSize = 20.sp, cornerRadius = 12.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(venue.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = AlpineDark)
                    if (venue.subtitle.isNotEmpty()) Text(venue.subtitle, fontSize = 13.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text("Betriebs- & Pisteninformation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineDark)
            Spacer(modifier = Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardSurface)) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Status", color = Color.Gray, fontSize = 13.sp)
                        Text("● Regulärer Betrieb", color = EmeraldGain, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Öffnungszeiten", color = Color.Gray, fontSize = 13.sp)
                        Text("08:30 – 16:30 Uhr", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (venue.category == VenueCategory.SKI) "Schneelage (Berg/Tal)" else "Bedingungen", color = Color.Gray, fontSize = 13.sp)
                        Text(if (venue.category == VenueCategory.SKI) "125 cm / 40 cm" else "Sehr gut präpariert", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFE2E8F0))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Einzelticket (Normalpreis)", color = Color.Gray, fontSize = 13.sp)
                        Text(effectivePrice.formatEuro(), fontWeight = FontWeight.Bold, color = GlacierBlue, fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text("Live-Wetter vor Ort (Open-Meteo)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineDark)
            Spacer(modifier = Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = GlacierBlue.copy(alpha = 0.08f))) {
                if (isLoadingWeather) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GlacierBlue, modifier = Modifier.size(28.dp))
                    }
                } else if (weatherData != null) {
                    val w = weatherData!!
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${"%.1f".format(w.temperature_2m)} °C", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AlpineDark)
                            Text("Gefühlt ${"%.1f".format(w.apparent_temperature)} °C", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val condition = when (w.weather_code) {
                                0 -> "☀️ Klar"
                                1, 2, 3 -> "⛅ Leicht bewölkt"
                                45, 48 -> "🌫️ Nebel"
                                51, 53, 55, 61, 63, 65 -> "🌧️ Regen"
                                71, 73, 75, 77, 85, 86 -> "❄️ Schneefall"
                                else -> "☁️ Bedeckt"
                            }
                            Text(condition, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GlacierBlue)
                            Text("Wind: ${"%.0f".format(w.wind_speed_10m)} km/h", fontSize = 11.sp, color = Color.Gray)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Feuchte", fontSize = 11.sp, color = Color.Gray)
                            Text("${"%.0f".format(w.relative_humidity_2m)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AlpineDark)
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("Wetterdaten aktuell nicht abrufbar", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text("Besuchsdatum wählen", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineDark)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.outlinedCardColors(containerColor = CardSurface)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GlacierBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = GlacierBlue, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedDate == LocalDate.now()) "Heute" else selectedDate.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN)),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy", Locale.GERMAN)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = AlpineDark
                        )
                        Text(
                            text = "Zugeordnete Saison: ${getSeasonForDate(selectedDate)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GlacierBlue
                        )
                    }
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Datum ändern", fontSize = 12.sp, color = GlacierBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = priceText,
                onValueChange = { input ->
                    val sanitized = input.replace(',', '.')
                    if (sanitized.isEmpty() || sanitized.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                        priceText = sanitized
                    }
                },
                label = { Text("Angerechneter Tarif (€)") },
                trailingIcon = { Text("€", modifier = Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold, color = GlacierBlue) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (effectivePrice != (priceText.toDoubleOrNull() ?: 0.0)) {
                Text(
                    text = "Zur\u00FCcksetzen auf Standardtarif (${effectivePrice.formatEuro()})",
                    fontSize = 11.sp,
                    color = GlacierBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { priceText = String.format(Locale.US, "%.2f", effectivePrice) }
                        .padding(top = 4.dp, start = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = visitNotes,
                onValueChange = { visitNotes = it },
                placeholder = { Text("Optionale Notiz (z. B. 'Halbtageskarte ab 12h')", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            val isAlreadyBookedOnDate = existingVisits.any { it.venueId == venue.id && it.date == selectedDate }
            if (isAlreadyBookedOnDate) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CoralWarning.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = CoralWarning, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Bereits am ${selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} eingebucht!",
                            color = CoralWarning,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onNavigateToMap,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.Map, contentDescription = null, tint = GlacierBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auf Karte", color = GlacierBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        if (isAlreadyBookedOnDate && !showDuplicateWarning) {
                            showDuplicateWarning = true
                        } else {
                            val finalPrice = priceText.toDoubleOrNull() ?: effectivePrice
                            val finalNote = if (visitNotes.isNotBlank()) visitNotes.trim() else "Über Partneransicht erfasst"
                            onBookVisit(selectedDate, finalPrice, finalNote)
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAlreadyBookedOnDate && showDuplicateWarning) CoralWarning else GlacierBlue
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAlreadyBookedOnDate && showDuplicateWarning) "Trotzdem buchen" else "Einbuchen",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                            showDuplicateWarning = false
                        }
                        showDatePicker = false
                    }) { Text("Übernehmen") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

// ==========================================
// 5. BADGES, CARDS & DIALOGE
// ==========================================
@Composable
fun CategoryBadge(
    category: VenueCategory,
    size: Dp = 28.dp,
    emojiSize: TextUnit = 15.sp,
    cornerRadius: Dp = 8.dp
) {
    val (emoji, bg) = when (category) {
        VenueCategory.SKI -> "⛷️" to Color(0xFFE0F2FE)
        VenueCategory.SLED -> "🛷" to Color(0xFFFFE0B2)
        VenueCategory.ICE -> "⛸️" to Color(0xFFEDE7F6)
        VenueCategory.POOL -> "🏊" to Color(0xFFE0F7FA)
        VenueCategory.CULTURE -> "🏛️" to Color(0xFFFBE9E7)
    }
    Surface(shape = RoundedCornerShape(cornerRadius), color = bg, modifier = Modifier.size(size)) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = emoji, fontSize = emojiSize, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ModernVisitCard(
    visit: VisitEntity, persons: List<PersonProfile> = emptyList(),
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(EmeraldGain.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldGain, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(visit.venueName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(visit.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), fontSize = 12.sp, color = Color.Gray, softWrap = false)
                    if (visit.notes.isNotEmpty()) {
                        Text(" • ${visit.notes}", fontSize = 12.sp, color = GlacierBlue, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                Text("+${visit.savedAmount.formatEuro()}", fontWeight = FontWeight.Black, color = EmeraldGain, fontSize = 15.sp, softWrap = false)
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp).clip(CircleShape).background(GlacierBlue.copy(alpha = 0.10f))) {
                    Icon(Icons.Rounded.EditCalendar, contentDescription = "Bearbeiten", tint = GlacierBlue, modifier = Modifier.size(17.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).clip(CircleShape).background(CoralWarning.copy(alpha = 0.10f))) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "l\u00F6schen", tint = CoralWarning, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

@Composable
fun MetricTile(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AlpineDark, softWrap = false)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualVisitDialog(
    venues: List<Venue>,
    allPersons: List<PersonProfile> = emptyList(),
    existingVisits: List<VisitEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (Venue, LocalDate, Double, String, List<Long>) -> Unit
) {
    val context = LocalContext.current
    var selectedVenue by remember { mutableStateOf(venues.first()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var priceText by remember {
        val initialEffective = PriceSyncManager.getCustomPrice(context, venues.first().id, venues.first().normalTicketPrice)
        mutableStateOf(String.format(Locale.US, "%.2f", initialEffective))
    }
    var notes by remember { mutableStateOf("") }
    var selectedPersonIds by remember(allPersons) {
        mutableStateOf(allPersons.filter { it.isDefaultSelected }.map { it.id }.toSet())
    }
    var filterText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showDuplicateWarning by remember { mutableStateOf(false) }

    LaunchedEffect(selectedVenue) {
        val effective = PriceSyncManager.getCustomPrice(context, selectedVenue.id, selectedVenue.normalTicketPrice)
        priceText = String.format(Locale.US, "%.2f", effective)
    }

    val displayedVenues = remember(filterText) {
        if (filterText.isEmpty()) venues.take(5)
        else venues.filter { it.name.contains(filterText, ignoreCase = true) || it.subtitle.contains(filterText, ignoreCase = true) }.take(5)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        showDuplicateWarning = false
                    }
                    showDatePicker = false
                }) { Text("Übernehmen") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Besuch manuell Eintr\u00E4gen", fontWeight = FontWeight.Bold, color = AlpineDark) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = GlacierBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Datum des Ausflugs", fontSize = 11.sp, color = Color.Gray)
                            Text(selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy", Locale.GERMAN)), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AlpineDark)
                            Text("Zugeordnete Saison: ${getSeasonForDate(selectedDate)}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GlacierBlue)
                        }
                        Text("ändern", fontSize = 12.sp, color = GlacierBlue, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("Gebiet oder Aktivität suchen...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))
                displayedVenues.forEach { v ->
                    val effective = PriceSyncManager.getCustomPrice(context, v.id, v.normalTicketPrice)
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { selectedVenue = v }.padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedVenue.id == v.id, onClick = { selectedVenue = v })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(v.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${v.category.displayName} • ${effective.formatEuro()}", fontSize = 11.sp, color = Color.Gray, softWrap = false)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val isAlreadyBooked = existingVisits.any { it.venueId == selectedVenue.id && it.date == selectedDate }
                if (isAlreadyBooked) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = CoralWarning.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, contentDescription = null, tint = CoralWarning, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Hinweis: Dieses Ziel wurde am gewählten Tag bereits gebucht!",
                                color = CoralWarning,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                val currentSelectedEffective = PriceSyncManager.getCustomPrice(context, selectedVenue.id, selectedVenue.normalTicketPrice)
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { input ->
                        val sanitized = input.replace(',', '.')
                        if (sanitized.isEmpty() || sanitized.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                            priceText = sanitized
                        }
                    },
                    label = { Text("G\u00FCltiger Tarif (€)") },
                    trailingIcon = { Text("€", modifier = Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold, color = GlacierBlue) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (currentSelectedEffective != (priceText.toDoubleOrNull() ?: 0.0)) {
                    Text(
                        "Zur\u00FCcksetzen auf Standard (${currentSelectedEffective.formatEuro()})",
                        fontSize = 11.sp,
                        color = GlacierBlue,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { priceText = String.format(Locale.US, "%.2f", currentSelectedEffective) }.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Optionale Notiz (z. B. 'Halbtageskarte', 'Kaiserwetter')", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (allPersons.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FamilyCheckInSelector(
                        allPersons = allPersons,
                        selectedPersonIds = selectedPersonIds,
                        onTogglePerson = { pId ->
                            selectedPersonIds = if (selectedPersonIds.contains(pId)) {
                                selectedPersonIds - pId
                            } else {
                                selectedPersonIds + pId
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val alreadyBooked = existingVisits.any { it.venueId == selectedVenue.id && it.date == selectedDate }
                    if (alreadyBooked && !showDuplicateWarning) {
                        showDuplicateWarning = true
                    } else {
                        val currentEffective = PriceSyncManager.getCustomPrice(context, selectedVenue.id, selectedVenue.normalTicketPrice)
                        val finalAmount = priceText.toDoubleOrNull() ?: currentEffective
                        onConfirm(selectedVenue, selectedDate, finalAmount, notes, selectedPersonIds.toList())
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (existingVisits.any { it.venueId == selectedVenue.id && it.date == selectedDate } && showDuplicateWarning) CoralWarning else GlacierBlue
                )
            ) {
                Text(if (existingVisits.any { it.venueId == selectedVenue.id && it.date == selectedDate } && showDuplicateWarning) "Trotzdem buchen" else "Jetzt einbuchen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVisitDialog(
    visit: VisitEntity,
    allPersons: List<PersonProfile> = emptyList(),
    initialPersonIds: List<Long> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double, String, List<Long>) -> Unit
) {
    var editedDate by remember { mutableStateOf(visit.date) }
    var editedNotes by remember { mutableStateOf(visit.notes) }
    var editedPriceText by remember { mutableStateOf(String.format(Locale.US, "%.2f", visit.savedAmount)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedPersonIds by remember(initialPersonIds) {
        mutableStateOf(initialPersonIds.toSet())
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = editedDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        editedDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("Übernehmen", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Abbrechen") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GlacierBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = GlacierBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = visit.venueName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlpineDark,
                        maxLines = 1
                    )
                    Text(
                        text = "Eintrag anpassen",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Schlanke Datumskachel in Kurzform
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Black.copy(alpha = 0.03f),
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = GlacierBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Datum",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = GlacierBlue.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = editedDate.format(DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy", Locale.GERMAN)),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = GlacierBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Personen-Auswahl
                if (allPersons.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        
                        FamilyCheckInSelector(
                            allPersons = allPersons,
                            selectedPersonIds = selectedPersonIds,
                            onTogglePerson = { pId ->
                                selectedPersonIds = if (selectedPersonIds.contains(pId)) {
                                    selectedPersonIds - pId
                                } else {
                                    selectedPersonIds + pId
                                }
                            }
                        )
                    }
                }

                // Tarif & Notiz
                OutlinedTextField(
                    value = editedPriceText,
                    onValueChange = { input ->
                        val sanitized = input.replace(',', '.')
                        if (sanitized.isEmpty() || sanitized.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                            editedPriceText = sanitized
                        }
                    },
                    label = { Text("Angerechneter Tarif") },
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        Text("€", modifier = Modifier.padding(end = 12.dp), fontWeight = FontWeight.Bold, color = GlacierBlue)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    label = { Text("Notiz (optional)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalAmount = editedPriceText.toDoubleOrNull() ?: visit.savedAmount
                    onSave(editedDate, finalAmount, editedNotes, selectedPersonIds.toList())
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text("Speichern", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Abbrechen", color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    )
}

@Composable
fun TarifSelectionDialog(
    season: String = getSeasonForDate(LocalDate.now()),
    currentConfig: TicketConfig,
    viewModel: TrackerViewModel,
    onDismiss: () -> Unit,
    onSave: (TicketConfig) -> Unit
) {
        val context = LocalContext.current
    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { targetUri ->
            pendingCsvContent?.let { content ->
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "\u2705 CSV erfolgreich gespeichert", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "\u274C Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val currentSeasonStr = getSeasonForDate(LocalDate.now())
    val isHistorical = season != currentSeasonStr
    var isEarlyBird by remember { mutableStateOf(currentConfig.isEarlyBird) }
    var selectedCategory by remember { mutableStateOf(currentConfig.category) }

    val defaultPrice = if (currentConfig.isEarlyBird) currentConfig.category.earlyBirdPrice else currentConfig.category.normalPrice
    var customPriceText by remember(season) {
        val initialPrice = viewModel.getHistoricalSeasonPrice(season, defaultPrice)
        mutableStateOf(String.format(java.util.Locale.US, "%.2f", initialPrice))
    }
    var customLabelText by remember(season) {
        val initialLabel = viewModel.getHistoricalSeasonLabel(
            season,
            currentConfig.category.label + (if (currentConfig.isEarlyBird) " (Vorverkauf)" else "")
        )
        mutableStateOf(initialLabel)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isHistorical) "Tarif Saison $season anpassen" else "Ticket-Tarif anpassen",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = AlpineDark
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isHistorical) {
                    Text(
                        text = "F\u00FCr vergangene Saisons gelten individuelle Ticketpreise. Gib hier deinen tats\u00E4chlichen damaligen Kaufpreis ein:",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = customPriceText,
                        onValueChange = { customPriceText = it },
                        label = { Text("Kaufpreis (€)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    )
                    OutlinedTextField(
                        value = customLabelText,
                        onValueChange = { customLabelText = it },
                        label = { Text("Tarifbezeichnung (z. B. U23 Vorverkauf)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedButton(
                        onClick = {
                            viewModel.manualPriceSync { _, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Partnertarife jetzt online abgleichen", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = GlacierBlue.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isEarlyBird,
                                onCheckedChange = { isEarlyBird = it },
                                colors = CheckboxDefaults.colors(checkedColor = GlacierBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Vorverkaufstarif", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineDark)
                                Text("Verg\u00FCnstigter Preis vor Saisonstart", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TarifCategory.values().forEach { cat ->
                            val isSelected = cat == selectedCategory
                            val price = if (isEarlyBird) cat.earlyBirdPrice else cat.normalPrice
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCategory = cat },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) GlacierBlue.copy(alpha = 0.12f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.5.dp, GlacierBlue) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedCategory = cat },
                                        colors = RadioButtonDefaults.colors(selectedColor = GlacierBlue)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        cat.label,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                        color = AlpineDark,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        String.format(java.util.Locale.GERMAN, "%.2f €", price),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AlpineDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isHistorical) {
                        val parsed = customPriceText.replace(",", ".").toDoubleOrNull() ?: defaultPrice
                        val lbl = customLabelText.ifBlank { "Freizeitticket" }
                        viewModel.saveHistoricalSeasonConfig(season, parsed, lbl)
                        onDismiss()
                    } else {
                        onSave(TicketConfig(category = selectedCategory, isEarlyBird = isEarlyBird))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Speichern", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = Color.Gray)
            }
        }
    )
}

fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

fun createOsmMarkerDrawable(context: Context, emoji: String, backgroundColor: Int, isVisitedToday: Boolean = false): BitmapDrawable {
    val sizePx = 92
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isVisitedToday) android.graphics.Color.parseColor("#10B981") else android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2f, borderPaint)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 7f, bgPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 42f
        textAlign = Paint.Align.CENTER
    }
    val yPos = (canvas.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f)
    canvas.drawText(emoji, canvas.width / 2f, yPos, textPaint)

    if (isVisitedToday) {
        val badgeRadius = 14f
        val badgeX = sizePx - badgeRadius - 2f
        val badgeY = badgeRadius + 2f

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawCircle(badgeX, badgeY, badgeRadius + 2f, badgeBgPaint)

        val checkCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#10B981")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(badgeX, badgeY, badgeRadius, checkCirclePaint)

        val checkTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val checkY = badgeY - ((checkTextPaint.descent() + checkTextPaint.ascent()) / 2f)
        canvas.drawText("✓", badgeX, checkY, checkTextPaint)
    }

    return BitmapDrawable(context.resources, bitmap)
}

fun createMyLocationMarkerDrawable(context: Context): BitmapDrawable {
    val sizePx = 64
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#330077B6"); style = Paint.Style.FILL }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - 2f, auraPaint)

    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.WHITE; style = Paint.Style.FILL }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, 16f, borderPaint)

    val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.parseColor("#0077B6"); style = Paint.Style.FILL }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, 11f, centerPaint)

    return BitmapDrawable(context.resources, bitmap)
}
// ==========================================
// FAMILIENMITGLIEDER & EINSTELLUNGEN DIALOG
// ==========================================
@Composable
fun FamilySettingsDialog(
    viewModel: TrackerViewModel,
    persons: List<PersonProfile>,
    season: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val visits by viewModel.allVisits.collectAsState(initial = emptyList())
    val crossRefs by viewModel.allCrossRefs.collectAsState(initial = emptyList())

    var nameInput by remember { mutableStateOf("") }
    var birthYearInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    var personToDelete by remember { mutableStateOf<PersonProfile?>(null) }

    var pendingCsvContent by remember { mutableStateOf<String?>(null) }
    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { targetUri ->
            pendingCsvContent?.let { content ->
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { stream ->
                        stream.write(content.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, "\u2705 CSV erfolgreich gespeichert", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "\u274C Fehler beim Speichern", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    viewModel.importVisitsFromCsv(text) { count ->
                        Toast.makeText(context, "\u2705 $count Einträge erfolgreich importiert!", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Fehler beim Import: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = GlacierBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Einstellungen & Daten", fontWeight = FontWeight.Bold, color = AlpineDark, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ABSCHNITT 1: FAMILIENMITGLIEDER
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.People, contentDescription = null, tint = AlpineNavy, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Familienmitglieder (${persons.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineNavy)
                }

                Text(
                    "Tarife für Saison $season werden automatisch anhand des Geburtsdatums ermittelt.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                if (persons.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        persons.forEach { person ->
                            val tarifPrice = FamilyTarifEngine.calculateTicketPrice(person, season)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GlacierBlue.copy(alpha = 0.08f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(person.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineDark)
                                        Text(
                                            "Geb: ${person.birthYear}  •  ${tarifPrice.formatEuro()}",
                                            fontSize = 12.sp,
                                            color = GlacierBlue,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    IconButton(
                                        onClick = { personToDelete = person },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Löschen",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; inputError = null },
                    label = { Text("Name (z.B. Jakob)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = birthYearInput,
                    onValueChange = { birthYearInput = it; inputError = null },
                    label = { Text("Geburtsjahr (z.B. 2008)", fontSize = 12.sp) },
                    placeholder = { Text("2008", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                inputError?.let { err ->
                    Text(err, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        val trimmedName = nameInput.trim()
                        val trimmedYear = birthYearInput.trim()
                        val parsedYear = trimmedYear.toIntOrNull()
                        if (parsedYear == null || parsedYear !in 1920..2026) {
                            inputError = "Bitte ein gültiges 4-stelliges Geburtsjahr eingeben (z.B. 2008)."
                            return@Button
                        }
                        viewModel.addPerson(name = trimmedName, birthYear = parsedYear)
                        nameInput = ""
                        birthYearInput = ""
                        inputError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue)
                ) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Profil anlegen", fontSize = 12.sp)
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.4f))

                // ABSCHNITT 2: DATENSICHERUNG & EXPORT
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Storage, contentDescription = null, tint = AlpineNavy, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Datensicherung & Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineNavy)
                }

                Text(
                    "Besuche als CSV sichern, Notizen importieren oder ein lokales Backup erstellen.",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val personMap = persons.associateBy { it.id }
                            val personsByVisit = crossRefs.groupBy { it.visitId }.mapValues { (_, refs) ->
                                refs.mapNotNull { personMap[it.personId]?.name }
                            }
                            val csvData = AutoBackupManager.exportCsv(visits, personsByVisit)
pendingCsvContent = csvData
val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
csvExportLauncher.launch("ftt_visits_$timestamp.csv")
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("text/*", "text/comma-separated-values", "text/csv", "application/csv"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                AutoBackupManager.performAutoBackup(context, visits)
                                Toast.makeText(context, "\u2705 Lokales Backup gespeichert", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Backup", fontSize = 11.sp, maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            val restored = AutoBackupManager.restoreAutoBackup(context)
                            if (restored.isNotEmpty()) {
                                viewModel.restoreVisits(restored) { count ->
                                    Toast.makeText(context, "\u2705 $count Einträge wiederhergestellt!", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "\u26A0\uFE0F Kein lokales Backup vorhanden", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Aus lokalem Backup wiederherstellen", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen", fontWeight = FontWeight.Bold, color = AlpineNavy)
            }
        }
    )
    if (personToDelete != null) {
        val target = personToDelete!!
        AlertDialog(
            onDismissRequest = { personToDelete = null },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "${target.name} entfernen?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlpineDark
                )
            },
            text = {
                Text(
                    text = "Möchtest du das Profil von \"${target.name}\" wirklich löschen? Verknüpfte Ausflüge bleiben in der Gesamtsumme erhalten.",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePerson(target)
                        personToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.85f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Löschen", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { personToDelete = null }) {
                    Text("Abbrechen", color = Color.Gray)
                }
            }
        )
    }
}


// ==========================================
// Onboarding / Erstinstallation Dialog
// ==========================================
@Composable
fun InitialSetupDialog(viewModel: TrackerViewModel) {
    var nameInput by remember { mutableStateOf("") }
    var birthYearInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { /* Nicht abbrechbar bei Erstinstallation */ },
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Willkommen!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AlpineDark
                )
                Text(
                    text = "Bitte lege dein erstes Ticket-Profil an, um die Ersparnis exakt berechnen zu können.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; inputError = null },
                    label = { Text("Name (z.B. Jakob)", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = birthYearInput,
                    onValueChange = { birthYearInput = it; inputError = null },
                    label = { Text("Geburtsjahr (z.B. 2008)", fontSize = 12.sp) },
                    placeholder = { Text("2008", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                inputError?.let { err ->
                    Text(err, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedName = nameInput.trim()
                    val trimmedYear = birthYearInput.trim()
                    if (trimmedName.isBlank()) {
                        inputError = "Bitte gib einen Namen ein."
                        return@Button
                    }
                    val parsedYear = trimmedYear.toIntOrNull()
                    if (parsedYear == null || parsedYear !in 1920..2026) {
                        inputError = "Bitte ein gültiges Geburtsjahr eingeben."
                        return@Button
                    }
                    viewModel.addPerson(name = trimmedName, birthYear = parsedYear)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue)
            ) {
                Text("Profil anlegen & Starten", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    )
}

fun shareStoryImage(context: android.content.Context, uri: android.net.Uri) {
    val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        clipData = android.content.ClipData.newUri(context.contentResolver, "FTT Story", uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = android.content.Intent.createChooser(sendIntent, "Saison-Story teilen").apply {
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(chooser)
}

@Composable
fun StoryThemeChooserDialog(
    onDismiss: () -> Unit,
    onThemeSelected: (StoryTheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Story-Design wählen",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AlpineDark
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Text("Wähle den Look für deinen Social Media Rückblick:", fontSize = 13.sp, color = Color.Gray)

                val themes = listOf(
                    Triple(StoryTheme.ALPINE_DARK, "Alpine Dark", Color(0xFF0F172A)),
                    Triple(StoryTheme.GLACIER_SNOW, "Glacier Snow", Color(0xFF0284C7)),
                    Triple(StoryTheme.SUNSET_PEAK, "Sunset Peak", Color(0xFFF43F5E))
                )

                themes.forEach { (theme, label, dotColor) ->
                    Surface(
                        onClick = { onThemeSelected(theme) },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.Black.copy(alpha = 0.04f),
                        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = AlpineDark)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen", color = Color.Gray)
            }
        }
    )
}

fun generateStoryBitmap(
    season: String,
    personName: String,
    visitsCount: Int,
    savedAmount: Double,
    ticketPrice: Double,
    theme: StoryTheme
): android.graphics.Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)

    data class ThemeVisuals(
        val bgTop: Int, val bgBottom: Int, val mountainFar: Int, val mountainNear: Int,
        val cardBg: Int, val cardBorder: Int, val textPrimary: Int, val textSecondary: Int,
        val accent: Int, val progressTrack: Int
    )

    val visuals = when (theme) {
        StoryTheme.ALPINE_DARK -> ThemeVisuals(
            bgTop = android.graphics.Color.parseColor("#070B19"),
            bgBottom = android.graphics.Color.parseColor("#0F172A"),
            mountainFar = android.graphics.Color.argb(70, 30, 41, 59),
            mountainNear = android.graphics.Color.argb(140, 15, 23, 42),
            cardBg = android.graphics.Color.argb(190, 30, 41, 59),
            cardBorder = android.graphics.Color.argb(100, 56, 189, 248),
            textPrimary = android.graphics.Color.parseColor("#F8FAFC"),
            textSecondary = android.graphics.Color.parseColor("#94A3B8"),
            accent = android.graphics.Color.parseColor("#38BDF8"),
            progressTrack = android.graphics.Color.argb(80, 255, 255, 255)
        )
        StoryTheme.GLACIER_SNOW -> ThemeVisuals(
            bgTop = android.graphics.Color.parseColor("#E0F2FE"),
            bgBottom = android.graphics.Color.parseColor("#BAE6FD"),
            mountainFar = android.graphics.Color.argb(90, 186, 230, 253),
            mountainNear = android.graphics.Color.argb(170, 255, 255, 255),
            cardBg = android.graphics.Color.argb(230, 255, 255, 255),
            cardBorder = android.graphics.Color.argb(80, 2, 132, 199),
            textPrimary = android.graphics.Color.parseColor("#0F172A"),
            textSecondary = android.graphics.Color.parseColor("#475569"),
            accent = android.graphics.Color.parseColor("#0284C7"),
            progressTrack = android.graphics.Color.argb(50, 0, 0, 0)
        )
        StoryTheme.SUNSET_PEAK -> ThemeVisuals(
            bgTop = android.graphics.Color.parseColor("#2E0836"),
            bgBottom = android.graphics.Color.parseColor("#6B1D4F"),
            mountainFar = android.graphics.Color.argb(90, 74, 18, 62),
            mountainNear = android.graphics.Color.argb(160, 41, 8, 38),
            cardBg = android.graphics.Color.argb(190, 56, 15, 52),
            cardBorder = android.graphics.Color.argb(120, 244, 63, 94),
            textPrimary = android.graphics.Color.parseColor("#FFFFFF"),
            textSecondary = android.graphics.Color.parseColor("#FBCFE8"),
            accent = android.graphics.Color.parseColor("#F59E0B"),
            progressTrack = android.graphics.Color.argb(80, 255, 255, 255)
        )
    }

    val bgPaint = android.graphics.Paint().apply {
        shader = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            visuals.bgTop, visuals.bgBottom,
            android.graphics.Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    fun drawMountain(pathYBase: Float, p1: android.graphics.PointF, p2: android.graphics.PointF, p3: android.graphics.PointF, paint: android.graphics.Paint) {
        val path = android.graphics.Path().apply {
            moveTo(0f, pathYBase)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            lineTo(p3.x, p3.y)
            lineTo(width.toFloat(), pathYBase)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        canvas.drawPath(path, paint)
    }

    val farMountainPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.mountainFar
        style = android.graphics.Paint.Style.FILL
    }
    drawMountain(1550f, android.graphics.PointF(280f, 1320f), android.graphics.PointF(640f, 1410f), android.graphics.PointF(920f, 1350f), farMountainPaint)

    val nearMountainPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.mountainNear
        style = android.graphics.Paint.Style.FILL
    }
    drawMountain(1700f, android.graphics.PointF(190f, 1500f), android.graphics.PointF(520f, 1580f), android.graphics.PointF(860f, 1470f), nearMountainPaint)

    val brandPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.accent
        textSize = 34f
        isFakeBoldText = true
        letterSpacing = 0.12f
    }
    canvas.drawText("FREIZEITTICKET TRACKER", 90f, 190f, brandPaint)

    val titlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.textPrimary
        textSize = 76f
        isFakeBoldText = true
    }
    canvas.drawText("Saison-Rückblick", 90f, 280f, titlePaint)

    val subTitlePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.textSecondary
        textSize = 38f
    }
    val nameTag = if (personName.isNotBlank()) " • $personName" else ""
    canvas.drawText("Saison $season$nameTag", 90f, 340f, subTitlePaint)

    val cardPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.cardBg
        style = android.graphics.Paint.Style.FILL
    }
    val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.cardBorder
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
    }

    val heroRect = android.graphics.RectF(90f, 430f, 990f, 940f)
    canvas.drawRoundRect(heroRect, 38f, 38f, cardPaint)
    canvas.drawRoundRect(heroRect, 38f, 38f, borderPaint)

    val labelPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.textSecondary
        textSize = 28f
        letterSpacing = 0.1f
        isFakeBoldText = true
    }
    canvas.drawText("EINGEFAHRENER GESAMTWERT", 140f, 510f, labelPaint)

    val amountPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.textPrimary
        textSize = 92f
        isFakeBoldText = true
    }
    canvas.drawText(String.format(java.util.Locale.GERMAN, "%.2f €", savedAmount), 140f, 620f, amountPaint)

    val isAmortized = ticketPrice > 0 && savedAmount >= ticketPrice
    val progressRatio = if (ticketPrice > 0) (savedAmount / ticketPrice).coerceIn(0.0, 1.0).toFloat() else 1.0f
    val percent = if (ticketPrice > 0) ((savedAmount / ticketPrice) * 100).toInt() else 100

    val barLeft = 140f
    val barRight = 940f
    val barY = 700f
    val barHeight = 22f

    val trackPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.progressTrack
        style = android.graphics.Paint.Style.FILL
    }
    canvas.drawRoundRect(android.graphics.RectF(barLeft, barY, barRight, barY + barHeight), 11f, 11f, trackPaint)

    val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isAmortized) android.graphics.Color.parseColor("#10B981") else visuals.accent
        style = android.graphics.Paint.Style.FILL
    }
    val fillWidth = (barRight - barLeft) * progressRatio
    if (fillWidth > 0) {
        canvas.drawRoundRect(android.graphics.RectF(barLeft, barY, barLeft + fillWidth, barY + barHeight), 11f, 11f, fillPaint)
    }

    val statusText = if (isAmortized) {
        val gain = savedAmount - ticketPrice
        "🎉 Break-Even erreicht! +${String.format(java.util.Locale.GERMAN, "%.2f €", gain)} Reingewinn ($percent%)"
    } else {
        val remain = (ticketPrice - savedAmount).coerceAtLeast(0.0)
        "Noch ${String.format(java.util.Locale.GERMAN, "%.2f €", remain)} bis zum Break-Even ($percent%)"
    }

    val statusPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isAmortized) android.graphics.Color.parseColor("#10B981") else visuals.accent
        textSize = 34f
        isFakeBoldText = true
    }
    canvas.drawText(statusText, 140f, 800f, statusPaint)

    val statRect = android.graphics.RectF(90f, 990f, 990f, 1310f)
    canvas.drawRoundRect(statRect, 38f, 38f, cardPaint)
    canvas.drawRoundRect(statRect, 38f, 38f, borderPaint)

    canvas.drawText("ABSOLVIERTE AUSFLÜGE", 140f, 1070f, labelPaint)

    val countText = if (visitsCount == 1) "1 Ausflug" else "$visitsCount Ausflüge"
    val countPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.textPrimary
        textSize = 72f
        isFakeBoldText = true
    }
    canvas.drawText(countText, 140f, 1175f, countPaint)

    val countSubText = if (visitsCount > 0) {
        val avg = savedAmount / visitsCount
        "Ø Ersparnis: ${String.format(java.util.Locale.GERMAN, "%.2f €", avg)} pro Tag"
    } else {
        "Noch keine Ausflüge eingetragen"
    }
    val avgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.textSecondary
        textSize = 32f
    }
    canvas.drawText(countSubText, 140f, 1245f, avgPaint)

    val footerPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = visuals.textSecondary
        textSize = 30f
    }
    canvas.drawText("🎿 Freizeitticket Tracker • Saison $season", 90f, 1820f, footerPaint)

    return bitmap
}

fun saveBitmapToCache(context: android.content.Context, bitmap: android.graphics.Bitmap): android.net.Uri? {
    return try {
        val cacheFolder = java.io.File(context.cacheDir, "story_shares").apply { mkdirs() }
        val file = java.io.File(cacheFolder, "ftt_story_${System.currentTimeMillis()}.png")
        val stream = java.io.FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        android.util.Log.e("FTT_SHARE", "Fehler beim Speichern der Story", e)
        null
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StoryPreviewCarouselDialog(
    season: String,
    personName: String,
    visitsCount: Int,
    savedAmount: Double,
    ticketPrice: Double,
    onDismiss: () -> Unit,
    onShare: (android.graphics.Bitmap) -> Unit
) {
    val themes = listOf(
        Pair(StoryTheme.ALPINE_DARK, "Alpine Dark"),
        Pair(StoryTheme.GLACIER_SNOW, "Glacier Snow"),
        Pair(StoryTheme.SUNSET_PEAK, "Sunset Peak")
    )

    // Rendern der drei Bitmaps im Speicher für verzögerungsfreie Vorschau
    val bitmaps = remember(season, personName, visitsCount, savedAmount, ticketPrice) {
        themes.map { (theme, _) ->
            generateStoryBitmap(season, personName, visitsCount, savedAmount, ticketPrice, theme)
        }
    }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { themes.size })

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.90f),
            shape = RoundedCornerShape(24.dp),
            color = AlpineNavy
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header mit Titel und Schließen-Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Story-Grafik wählen", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Wische nach links/rechts", fontSize = 12.sp, color = Color.LightGray)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Schließen", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // HorizontalPager für das Wischen durch die 3 Grafiken
                androidx.compose.foundation.pager.HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val bmp = bitmaps[page]
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            modifier = Modifier
                                .fillMaxHeight()
                                .aspectRatio(9f / 16f)
                        ) {
                            androidx.compose.foundation.Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = themes[page].second,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Theme-Name & Pager-Indikatorpunkte
                Text(
                    text = themes[pagerState.currentPage].second,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(themes.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 10.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) GlacierBlue else Color.White.copy(alpha = 0.3f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Button zum Teilen der aktuellen Auswahl
                Button(
                    onClick = {
                        val chosenBitmap = bitmaps[pagerState.currentPage]
                        onShare(chosenBitmap)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Diese Grafik teilen", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
            }
        }
    }
}

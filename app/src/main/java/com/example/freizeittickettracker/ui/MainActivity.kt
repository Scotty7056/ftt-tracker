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

// Farbschema: Alpin modern
val AlpineDark = Color(0xFF0F1E2E)
val AlpineNavy = Color(0xFF1B2E4B)
val GlacierBlue = Color(0xFF0077B6)
val FrostTeal = Color(0xFF00B4D8)
val EmeraldGain = Color(0xFF10B981)
val CoralWarning = Color(0xFFF43F5E)
val CardSurface = Color(0xFFF8FAFC)

class MainActivity : ComponentActivity() {
    private val viewModel: TrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        } else {
            viewModel.updateLocation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.DownhillSkiing,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Freizeitticket Tracker",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 19.sp
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showTarifDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Rounded.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tarif", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                    Triple(2, "Karte", Icons.Rounded.Map),
                    Triple(3, "Partner", Icons.Rounded.Explore)
                )
                items.forEach { (index, label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
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
// 1. DASHBOARD REDESIGN
// ==========================================
@Composable
fun DashboardView(viewModel: TrackerViewModel, onOpenTarifDialog: () -> Unit) {
    val stats by viewModel.dashboardStats.collectAsState(initial = DashboardStats())
    val visits by viewModel.recentVisits.collectAsState(initial = emptyList())
    var showAddVisitDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Hero-Amortisationskarte mit Farbverlauf
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
                                colors = if (stats.isAmortized)
                                    listOf(Color(0xFF0F3E2E), EmeraldGain)
                                else
                                    listOf(AlpineDark, AlpineNavy, GlacierBlue)
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
                            Column {
                                Text(
                                    text = stats.tarifTitle.uppercase(Locale.GERMAN),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Kaufpreis: ${stats.purchasePrice.formatEuro()}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = onOpenTarifDialog,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .size(36.dp)
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Bearbeiten", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Großes zentrales Ersparnis-Highlight
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "EINGEFAHRENER WERT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.75f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    stats.totalValueSaved.formatEuro(),
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (stats.isAmortized) EmeraldGain else Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    "${(stats.amortizationProgress * 100).toInt()}%",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        LinearProgressIndicator(
                            progress = { stats.amortizationProgress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (stats.isAmortized) EmeraldGain else FrostTeal,
                            trackColor = Color.White.copy(alpha = 0.25f)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                if (stats.isAmortized) "🎉 Ticket voll amortisiert!" else "Noch offen zum Break-Even:",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Text(
                                if (stats.isAmortized) "+${stats.netSavings.formatEuro()} Plus" else stats.remainingToAmortize.formatEuro(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (stats.isAmortized) EmeraldGain else Color(0xFFFFD166)
                            )
                        }
                    }
                }
            }
        }

        // Schnelle KPI-Kacheln
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricTile(
                    title = "Ausflüge",
                    value = "${stats.totalVisits}",
                    icon = Icons.Rounded.EventAvailable,
                    tint = GlacierBlue,
                    modifier = Modifier.weight(1f)
                )
                val avg = if (stats.totalVisits > 0) stats.totalValueSaved / stats.totalVisits else 0.0
                MetricTile(
                    title = "Ø Ersparnis/Tag",
                    value = avg.formatEuro(),
                    icon = Icons.Rounded.TrendingUp,
                    tint = EmeraldGain,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Primärer Call-to-Action
        item {
            Button(
                onClick = { showAddVisitDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Icon(Icons.Rounded.AddLocationAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Besuch manuell eintragen", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Text("Letzte Aktivitäten", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AlpineDark)
        }

        if (visits.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.Hiking, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Noch keine Ausflüge erfasst",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                        Text(
                            "Tippe oben auf 'Besuch manuell eintragen'",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        } else {
            items(visits) { visit ->
                ModernVisitCard(visit = visit, onDelete = { viewModel.deleteVisit(visit) })
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

@Composable
fun MetricTile(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AlpineDark)
            }
        }
    }
}

// ==========================================
// 2. KALENDER REDESIGN
// ==========================================
@Composable
fun CalendarView(viewModel: TrackerViewModel) {
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
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Zurück")
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
            "Ausflüge am ${selectedDate.format(DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN))}",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = AlpineDark
        )

        val selectedVisits = visits.filter { it.date == selectedDate }
        if (selectedVisits.isEmpty()) {
            Text("Kein Ausflug an diesem Tag eingetragen.", color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedVisits) { visit ->
                    ModernVisitCard(visit = visit, onDelete = { viewModel.deleteVisit(visit) })
                }
            }
        }
    }
}

// ==========================================
// 3. GOOGLE MAPS
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
                    VenueCategory.POOL -> BitmapDescriptorFactory.HUE_CYAN
                    VenueCategory.ICE -> BitmapDescriptorFactory.HUE_VIOLET
                    VenueCategory.CULTURE -> BitmapDescriptorFactory.HUE_ORANGE
                }
                googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(venue.latitude, venue.longitude))
                        .title(venue.name)
                        .snippet("Normaltarif: ${venue.normalTicketPrice.formatEuro()}")
                        .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                )
            }
        }
    }
}

// ==========================================
// 4. PARTNERLISTE REDESIGN
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
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Gebiet, Bad oder Museum suchen...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = GlacierBlue) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = GlacierBlue
            )
        )

        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filters = listOf(
                null to "Alle",
                VenueCategory.SKI to "⛷ Ski",
                VenueCategory.POOL to "🏊 Bäder",
                VenueCategory.CULTURE to "🏛 Kultur"
            )
            filters.forEach { (cat, label) ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredVenues) { venue ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                CategoryBadge(venue.category)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(venue.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineDark)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Tageskarte: ${venue.normalTicketPrice.formatEuro()}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        userLocation?.let { loc ->
                            val dist = calculateDistanceKm(loc.latitude, loc.longitude, venue.latitude, venue.longitude)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    "${"%.0f".format(dist)} km",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GlacierBlue,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// KARTEN & BADGES
// ==========================================
@Composable
fun CategoryBadge(category: VenueCategory) {
    val (label, bg, fg) = when (category) {
        VenueCategory.SKI -> Triple("Ski", Color(0xFFE0F2FE), GlacierBlue)
        VenueCategory.POOL -> Triple("Bad", Color(0xFFE0F7FA), Color(0xFF00838F))
        VenueCategory.ICE -> Triple("Eis", Color(0xFFEDE7F6), Color(0xFF5E35B1))
        VenueCategory.CULTURE -> Triple("Kultur", Color(0xFFFFF3E0), Color(0xFFD84315))
    }
    Surface(shape = RoundedCornerShape(6.dp), color = bg) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ModernVisitCard(visit: VisitEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(EmeraldGain.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldGain, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(visit.venueName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AlpineDark)
                    Text(
                        visit.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "+${visit.savedAmount.formatEuro()}",
                    fontWeight = FontWeight.Black,
                    color = EmeraldGain,
                    fontSize = 15.sp
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Löschen", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ==========================================
// DIALOGE
// ==========================================
@Composable
fun ManualVisitDialog(
    venues: List<Venue>,
    onDismiss: () -> Unit,
    onConfirm: (Venue, LocalDate, String) -> Unit
) {
    var selectedVenue by remember { mutableStateOf(venues.first()) }
    var notes by remember { mutableStateOf("") }
    var filterText by remember { mutableStateOf("") }

    val displayedVenues = remember(filterText) {
        if (filterText.isEmpty()) venues.take(6)
        else venues.filter { it.name.contains(filterText, ignoreCase = true) }.take(6)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Besuch manuell eintragen", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("Gebiet suchen...", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                displayedVenues.forEach { v ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedVenue = v }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedVenue.id == v.id, onClick = { selectedVenue = v })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(v.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            Text("Ersparnis: ${v.normalTicketPrice.formatEuro()}", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedVenue, LocalDate.now(), notes) },
                colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue)
            ) {
                Text("Jetzt einbuchen")
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
        title = { Text("Ticket-Tarif anpassen", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = GlacierBlue.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEarlyBird = !isEarlyBird }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isEarlyBird, onCheckedChange = { isEarlyBird = it })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Vorverkaufstarif", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Günstigerer Preis bei Kauf vor Saisonstart", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                for (category in TarifCategory.entries) {
                    val price = if (isEarlyBird) category.earlyBirdPrice else category.normalPrice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCategory = category }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedCategory == category, onClick = { selectedCategory = category })
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(category.label, fontSize = 13.sp)
                            Text(price.formatEuro(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AlpineDark)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(TicketConfig(category = selectedCategory, isEarlyBird = isEarlyBird)) },
                colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue)
            ) {
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
package com.example.freizeittickettracker.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.example.freizeittickettracker.R
import com.example.freizeittickettracker.data.*
import com.example.freizeittickettracker.receiver.AppGeofenceManager
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
import java.time.format.DateTimeFormatter
import java.util.Locale

val AlpineDark = Color(0xFF0F1E2E)
val AlpineNavy = Color(0xFF1B2E4B)
val GlacierBlue = Color(0xFF0077B6)
val FrostTeal = Color(0xFF00B4D8)
val EmeraldGain = Color(0xFF10B981)
val CoralWarning = Color(0xFFE53935)
val CardSurface = Color(0xFFF8FAFC)

class MainActivity : ComponentActivity() {
    private val viewModel: TrackerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val osmConfig = Configuration.getInstance()
        osmConfig.load(this, PreferenceManager.getDefaultSharedPreferences(this))
        osmConfig.userAgentValue = "${packageName}/1.1 (Android; https://github.com/Scotty7056/freizeitticket-tracker)"
        
        val osmCacheDir = File(cacheDir, "osm_tiles")
        if (!osmCacheDir.exists()) osmCacheDir.mkdirs()
        osmConfig.osmdroidBasePath = osmCacheDir
        osmConfig.osmdroidTileCache = osmCacheDir

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
    var visitPendingDeletion by remember { mutableStateOf<VisitEntity?>(null) }
    var visitPendingEdit by remember { mutableStateOf<VisitEntity?>(null) }
    var showBackgroundRationaleDialog by remember { mutableStateOf(false) }

    val ticketConfig by viewModel.ticketConfig.collectAsState(initial = TicketConfig())
    val context = LocalContext.current
    val geofenceManager = remember { AppGeofenceManager(context) }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            geofenceManager.registerAllVenues()
        }
    }

    val basePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (fineLocationGranted) {
            viewModel.updateLocation()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasBackground = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasBackground) {
                    showBackgroundRationaleDialog = true
                } else {
                    geofenceManager.registerAllVenues()
                }
            } else {
                geofenceManager.registerAllVenues()
            }
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()

        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotification = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!hasNotification) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            basePermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            viewModel.updateLocation()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasBackground = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasBackground) {
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
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.DownhillSkiing,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Freizeitticket Tracker",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = { showTarifDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(18.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Rounded.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tarif", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, softWrap = false)
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
                    Triple(2, "Karte", Icons.Rounded.Map),
                    Triple(3, "Partner", Icons.Rounded.Explore)
                )
                items.forEach { (index, label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, softWrap = false) },
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
                2 -> OsmMapOverviewView()
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

        if (showBackgroundRationaleDialog) {
            AlertDialog(
                onDismissRequest = { showBackgroundRationaleDialog = false },
                icon = {
                    Icon(
                        Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = GlacierBlue,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = { Text("Automatische Erkennung im Hintergrund", fontWeight = FontWeight.Bold, color = AlpineDark) },
                text = {
                    Text(
                        "Damit dich die App beim Erreichen einer Liftstation oder eines Bades per Push-Nachricht an das Einbuchen erinnern kann, wähle im nächsten Schritt bitte 'Immer zulassen'.\n\nDie Erkennung erfolgt akkuschonend über Geofences."
                    )
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
                        Text("Weiter zu 'Immer zulassen'")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackgroundRationaleDialog = false }) {
                        Text("Später", color = Color.Gray)
                    }
                }
            )
        }

        visitPendingEdit?.let { visit ->
            EditVisitDialog(
                visit = visit,
                onDismiss = { visitPendingEdit = null },
                onSave = { updatedDate, updatedNotes ->
                    viewModel.updateVisit(visit.copy(date = updatedDate, notes = updatedNotes))
                    visitPendingEdit = null
                }
            )
        }

        visitPendingDeletion?.let { visit ->
            AlertDialog(
                onDismissRequest = { visitPendingDeletion = null },
                icon = {
                    Icon(
                        Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = CoralWarning,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = { Text("Eintrag löschen?", fontWeight = FontWeight.Bold, color = AlpineDark) },
                text = {
                    Text("Möchtest du '${visit.venueName}' vom ${visit.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))} (+${visit.savedAmount.formatEuro()}) wirklich entfernen?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteVisit(visit)
                            visitPendingDeletion = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralWarning)
                    ) {
                        Text("Löschen", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { visitPendingDeletion = null }) {
                        Text("Abbrechen", color = AlpineNavy)
                    }
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
    val visits by viewModel.recentVisits.collectAsState(initial = emptyList())
    var showAddVisitDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stats.tarifTitle.uppercase(Locale.GERMAN),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.7f),
                                    letterSpacing = 1.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Kaufpreis: ${stats.purchasePrice.formatEuro()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    softWrap = false
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
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

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "EINGEFAHRENER WERT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.75f),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    stats.totalValueSaved.formatEuro(),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    softWrap = false
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
                                    fontSize = 14.sp,
                                    softWrap = false
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
                                if (stats.isAmortized) "🎉 Ticket amortisiert!" else "Noch offen zum Break-Even:",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (stats.isAmortized) "+${stats.netSavings.formatEuro()} Plus" else stats.remainingToAmortize.formatEuro(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (stats.isAmortized) EmeraldGain else Color(0xFFFFD166),
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Letzte Aktivitäten", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AlpineDark)
                if (visits.isNotEmpty()) {
                    Text("✏️ Ändern • 🗑️ Löschen", fontSize = 12.sp, color = Color.Gray)
                }
            }
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
                        Text("Noch keine Ausflüge erfasst", fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Text("Tippe oben auf 'Besuch manuell eintragen'", fontSize = 12.sp, color = Color.LightGray)
                    }
                }
            }
        } else {
            items(visits) { visit ->
                ModernVisitCard(
                    visit = visit,
                    onEdit = { onRequestEditVisit(visit) },
                    onDelete = { onRequestDeleteVisit(visit) }
                )
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
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AlpineDark, softWrap = false)
            }
        }
    }
}

// ==========================================
// 2. KALENDER
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
                    ModernVisitCard(
                        visit = visit,
                        onEdit = { onRequestEditVisit(visit) },
                        onDelete = { onRequestDeleteVisit(visit) }
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. OPENSTREETMAP KARTENANSICHT
// ==========================================
@Composable
fun OsmMapOverviewView() {
    val venues = VenueCatalog.venues
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    val cartoTileSource = remember {
        XYTileSource(
            "CartoVoyager",
            0,
            19,
            256,
            ".png",
            arrayOf(
                "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
                "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
            ),
            "© OpenStreetMap contributors © CARTO"
        )
    }

    DisposableEffect(mapView) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = {
            mapView.apply {
                tileProvider.clearTileCache()
                setTileSource(cartoTileSource)
                setMultiTouchControls(true)
                controller.setZoom(9.5)
                controller.setCenter(GeoPoint(47.2692, 11.4041))

                overlays.clear()
                venues.forEach { venue ->
                    val marker = Marker(this)
                    marker.position = GeoPoint(venue.latitude, venue.longitude)
                    marker.title = venue.name
                    marker.snippet = "${venue.category.displayName} • ${venue.normalTicketPrice.formatEuro()}"

                    val (pinColor, iconRes) = when (venue.category) {
                        VenueCategory.SKI -> android.graphics.Color.parseColor("#0077B6") to R.drawable.ic_ski
                        VenueCategory.SLED -> android.graphics.Color.parseColor("#E65100") to R.drawable.ic_sled
                        VenueCategory.ICE -> android.graphics.Color.parseColor("#7B1FA2") to R.drawable.ic_ice
                        VenueCategory.POOL -> android.graphics.Color.parseColor("#00838F") to R.drawable.ic_pool
                        VenueCategory.CULTURE -> android.graphics.Color.parseColor("#D84315") to R.drawable.ic_culture
                    }
                    marker.icon = createOsmMarkerDrawable(context, iconRes, pinColor)
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    overlays.add(marker)
                }
                invalidate()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ==========================================
// 4. PARTNERLISTE
// ==========================================
@Composable
fun PartnerListView(viewModel: TrackerViewModel) {
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
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
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
                                Text(
                                    venue.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AlpineDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (venue.subtitle.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    venue.subtitle,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Regulärer Tarif: ${venue.normalTicketPrice.formatEuro()}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                softWrap = false
                            )
                        }
                        userLocation?.let { loc ->
                            Spacer(modifier = Modifier.width(8.dp))
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
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    softWrap = false
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
// KARTEN & BADGES (ICON-BADGE STATT TEXT)
// ==========================================
@Composable
fun CategoryBadge(category: VenueCategory) {
    val (iconRes, bg, fg) = when (category) {
        VenueCategory.SKI -> Triple(R.drawable.ic_ski, Color(0xFFE0F2FE), GlacierBlue)
        VenueCategory.SLED -> Triple(R.drawable.ic_sled, Color(0xFFFFE0B2), Color(0xFFE65100))
        VenueCategory.ICE -> Triple(R.drawable.ic_ice, Color(0xFFEDE7F6), Color(0xFF7B1FA2))
        VenueCategory.POOL -> Triple(R.drawable.ic_pool, Color(0xFFE0F7FA), Color(0xFF00838F))
        VenueCategory.CULTURE -> Triple(R.drawable.ic_culture, Color(0xFFFBE9E7), Color(0xFFD84315))
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bg,
        modifier = Modifier.size(26.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = category.displayName,
                tint = fg,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ModernVisitCard(
    visit: VisitEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(EmeraldGain.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = EmeraldGain, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visit.venueName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = AlpineDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = visit.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        softWrap = false
                    )
                    if (visit.notes.isNotEmpty()) {
                        Text(
                            text = " • ${visit.notes}",
                            fontSize = 12.sp,
                            color = GlacierBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "+${visit.savedAmount.formatEuro()}",
                    fontWeight = FontWeight.Black,
                    color = EmeraldGain,
                    fontSize = 15.sp,
                    softWrap = false
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GlacierBlue.copy(alpha = 0.10f))
                ) {
                    Icon(
                        Icons.Rounded.EditCalendar,
                        contentDescription = "Datum bearbeiten",
                        tint = GlacierBlue,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CoralWarning.copy(alpha = 0.10f))
                ) {
                    Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = "Eintrag löschen",
                        tint = CoralWarning,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// DIALOGE (MANUELL EINTRAGEN & DATUM BEARBEITEN)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualVisitDialog(
    venues: List<Venue>,
    onDismiss: () -> Unit,
    onConfirm: (Venue, LocalDate, String) -> Unit
) {
    var selectedVenue by remember { mutableStateOf(venues.first()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }
    var filterText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    val displayedVenues = remember(filterText) {
        if (filterText.isEmpty()) venues.take(6)
        else venues.filter { it.name.contains(filterText, ignoreCase = true) || it.subtitle.contains(filterText, ignoreCase = true) }.take(6)
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
                    }
                    showDatePicker = false
                }) {
                    Text("Übernehmen")
                }
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
        title = { Text("Besuch manuell eintragen", fontWeight = FontWeight.Bold, color = AlpineDark) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = GlacierBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Datum des Ausflugs", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                selectedDate.format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlpineDark
                            )
                        }
                        Text("Ändern", fontSize = 12.sp, color = GlacierBlue, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    placeholder = { Text("Gebiet oder Aktivität suchen...", fontSize = 13.sp) },
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
                            .padding(vertical = 5.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedVenue.id == v.id, onClick = { selectedVenue = v })
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = v.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${v.category.displayName} • ${v.normalTicketPrice.formatEuro()}",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                softWrap = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Optionale Notiz (z. B. 'Neuschnee, Kaiserwetter')", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedVenue, selectedDate, notes) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditVisitDialog(
    visit: VisitEntity,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String) -> Unit
) {
    var editedDate by remember { mutableStateOf(visit.date) }
    var editedNotes by remember { mutableStateOf(visit.notes) }
    var showDatePicker by remember { mutableStateOf(false) }

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
                }) {
                    Text("Übernehmen")
                }
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
            Column {
                Text("Eintrag bearbeiten", fontWeight = FontWeight.Bold, color = AlpineDark)
                Text(visit.venueName, fontSize = 13.sp, color = GlacierBlue, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = GlacierBlue, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Datum anpassen", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                editedDate.format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN)),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlpineDark
                            )
                        }
                        Text("Ändern", fontSize = 12.sp, color = GlacierBlue, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    label = { Text("Notiz") },
                    placeholder = { Text("z. B. Skitour, Nachmittags...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(editedDate, editedNotes) },
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
        title = { Text("Ticket-Tarif anpassen", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = AlpineDark) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vorverkaufstarif", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Vergünstigter Preis vor Saisonstart", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                for (category in TarifCategory.entries) {
                    val price = if (isEarlyBird) category.earlyBirdPrice else category.normalPrice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCategory = category }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedCategory == category, onClick = { selectedCategory = category })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = category.label,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = price.formatEuro(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AlpineDark,
                            softWrap = false
                        )
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

fun createOsmMarkerDrawable(context: Context, vectorResId: Int, backgroundColor: Int): BitmapDrawable {
    val sizePx = 80
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    val radius = sizePx / 2f
    canvas.drawCircle(radius, radius, radius - 3f, paint)

    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawCircle(radius, radius, radius - 3f, strokePaint)

    val drawable = ContextCompat.getDrawable(context, vectorResId)
    drawable?.let {
        it.setTint(android.graphics.Color.WHITE)
        val pad = 18
        it.setBounds(pad, pad, sizePx - pad, sizePx - pad)
        it.draw(canvas)
    }

    return BitmapDrawable(context.resources, bitmap)
}
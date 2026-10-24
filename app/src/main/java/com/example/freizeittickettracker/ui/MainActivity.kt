package com.example.freizeittickettracker.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.room.Room
import com.example.freizeittickettracker.data.*
import com.example.freizeittickettracker.util.LocationTracker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "freizeitticket_db").build()
        val locationTracker = LocationTracker(applicationContext)
        val viewModel = TrackerViewModel(db.checkInDao(), locationTracker)

        setContent {
            MaterialTheme {
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
    val state by viewModel.uiState.collectAsState()
    var currentTab by remember { mutableStateOf(0) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.checkCurrentLocation()
    }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Freizeitticket Tracker") },
                actions = {
                    TextButton(onClick = { showSettingsDialog = true }) {
                        Text("Mein Ticket", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = currentTab == 0, onClick = { currentTab = 0 }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Dashboard") })
                NavigationBarItem(selected = currentTab == 1, onClick = { currentTab = 1 }, icon = { Icon(Icons.Default.Place, null) }, label = { Text("Partner") })
            }
        }
    ) { padding ->
        if (currentTab == 0) {
            DashboardTab(viewModel, state, Modifier.padding(padding))
        } else {
            PartnerListTab(viewModel, state, Modifier.padding(padding))
        }
    }

    if (showSettingsDialog) {
        TicketSettingsDialog(state.selectedTicketType, state.isPresale, onDismiss = { showSettingsDialog = false }) { t, p ->
            viewModel.updateTicket(t, p)
            showSettingsDialog = false
        }
    }
}

@Composable
fun DashboardTab(viewModel: TrackerViewModel, state: UiState, modifier: Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        state.nearbyVenue?.let { (venue, dist) ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("In deiner NÃ¤he (ca. %.0fm): %s".format(dist, venue.name), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.recordVisit(venue) }) { Text("Jetzt fÃ¼r â‚¬ %.2f einbuchen".format(venue.getPriceFor(state.selectedTicketType))) }
                    }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${state.selectedTicketType.label} (Kaufpreis: â‚¬ %.2f)".format(state.ticketCost), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(10.dp))
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Genutzt: â‚¬ %.2f".format(state.totalSaved), fontWeight = FontWeight.Bold)
                        Text(if (state.balance >= 0) "Gewinn: â‚¬ %.2f".format(state.balance) else "Offen: â‚¬ %.2f".format(kotlin.math.abs(state.balance)), color = if (state.balance >= 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("Letzte Besuche", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        items(state.checkIns) { checkIn ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(checkIn.venueName, fontWeight = FontWeight.Bold)
                        Text(checkIn.category, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("+ â‚¬ %.2f".format(checkIn.savedAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    IconButton(onClick = { viewModel.removeVisit(checkIn) }) { Icon(Icons.Default.Delete, "LÃ¶schen") }
                }
            }
        }
    }
}

@Composable
fun PartnerListTab(viewModel: TrackerViewModel, state: UiState, modifier: Modifier) {
    var search by remember { mutableStateOf("") }
    val filtered = remember(search) { VenueCatalog.venues.filter { it.name.contains(search, true) } }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Partner suchen...") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { venue ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(venue.name, fontWeight = FontWeight.Bold)
                            Text("${venue.category.displayName} â€¢ â‚¬ %.2f".format(venue.getPriceFor(state.selectedTicketType)), style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { viewModel.recordVisit(venue) }) { Text("Einbuchen") }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketSettingsDialog(current: TicketType, presale: Boolean, onDismiss: () -> Unit, onSave: (TicketType, Boolean) -> Unit) {
    var type by remember { mutableStateOf(current) }
    var ps by remember { mutableStateOf(presale) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ticket Tarif wÃ¤hlen") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = ps, onCheckedChange = { ps = it })
                    Text("Vorverkaufstarif")
                }
                TicketType.values().forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == t, onClick = { type = t })
                        Text(t.label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(type, ps) }) { Text("Speichern") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } }
    )
}
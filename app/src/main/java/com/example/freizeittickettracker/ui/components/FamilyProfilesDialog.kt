package com.example.freizeittickettracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.freizeittickettracker.data.model.PersonProfile
import com.example.freizeittickettracker.domain.FamilyTarifEngine
import com.example.freizeittickettracker.ui.AlpineDark
import com.example.freizeittickettracker.ui.GlacierBlue

@Composable
fun FamilyProfilesDialog(
    persons: List<PersonProfile>,
    currentSeason: String,
    onAddPerson: (name: String, birthYear: Int, isEarlyBird: Boolean, customPrice: Double?) -> Unit,
    onDeletePerson: (PersonProfile) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddFields by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newBirthYear by remember { mutableStateOf("2010") }
    var isEarlyBird by remember { mutableStateOf(true) }
    var customPriceText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Familien-Profile verwalten",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AlpineDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Schließen")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Liste bestehender Profile
                if (persons.isEmpty()) {
                    Text(
                        text = "Noch keine weiteren Personen angelegt. Lege Profile für Partner oder Kinder an, um gemeinsame Skitage zu tracken.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(persons) { person ->
                            val category = FamilyTarifEngine.resolveCategory(person.birthYear, currentSeason)
                            val calcPrice = FamilyTarifEngine.calculateTicketPrice(person, currentSeason)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Person,
                                            contentDescription = null,
                                            tint = GlacierBlue,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(person.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(
                                                "${person.birthYear} \u2022 ${category.label.substringBefore(" ")} \u2022 ${calcPrice.toInt()} €",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeletePerson(person) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Löschen",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Eingabebereich für neue Person
                if (showAddFields) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Name / Vorname") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = newBirthYear,
                                onValueChange = { if (it.length <= 4) newBirthYear = it },
                                label = { Text("Geburtsjahr") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = customPriceText,
                                onValueChange = { customPriceText = it },
                                label = { Text("Individueller Preis (€)") },
                                placeholder = { Text("Auto") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Checkbox(
                                checked = isEarlyBird,
                                onCheckedChange = { isEarlyBird = it }
                            )
                            Text("Vorverkaufspreis (Early-Bird)", fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val year = newBirthYear.toIntOrNull() ?: 2010
                                val customPrice = customPriceText.replace(',', '.').toDoubleOrNull()
                                if (newName.isNotBlank()) {
                                    onAddPerson(newName.trim(), year, isEarlyBird, customPrice)
                                    newName = ""
                                    customPriceText = ""
                                    showAddFields = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GlacierBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Profil speichern")
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showAddFields = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Neue Person hinzufügen", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
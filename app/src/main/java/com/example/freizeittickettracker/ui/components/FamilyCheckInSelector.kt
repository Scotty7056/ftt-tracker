package com.example.freizeittickettracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.freizeittickettracker.data.model.PersonProfile
import com.example.freizeittickettracker.ui.AlpineDark
import com.example.freizeittickettracker.ui.GlacierBlue

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FamilyCheckInSelector(
    allPersons: List<PersonProfile>,
    selectedPersonIds: Set<Long>,
    onTogglePerson: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (allPersons.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Wer war dabei?",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AlpineDark,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            allPersons.forEach { person ->
                val isSelected = selectedPersonIds.contains(person.id)
                FilterChip(
                    selected = isSelected,
                    onClick = { onTogglePerson(person.id) },
                    label = { Text(person.name, fontSize = 13.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GlacierBlue.copy(alpha = 0.2f),
                        selectedLabelColor = GlacierBlue
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }
    }
}
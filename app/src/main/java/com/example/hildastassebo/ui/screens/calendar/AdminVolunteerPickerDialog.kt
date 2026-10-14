package com.example.hildastassebo.ui.screens.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hildastassebo.data.model.Volunteer
import com.example.hildastassebo.data.model.ShiftType
import androidx.compose.ui.Alignment
private val DarkSage = Color(0xFF47796C)

@Composable
fun AdminVolunteerPickerDialog(
    volunteers: List<Volunteer>,
    onDismiss: () -> Unit,
    onVolunteerSelected: (Volunteer, ShiftType, Boolean) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var selectedVolunteer by remember { mutableStateOf<Volunteer?>(null) }
    var selectedShiftType by remember {
        mutableStateOf(ShiftType.MORNING)
    }
    var wantsToWorkAlone by remember {
        mutableStateOf(false)
    }

    val filteredVolunteers = volunteers.filter { volunteer ->
        volunteer.fullName.contains(searchText, ignoreCase = true) ||
                volunteer.email.contains(searchText, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                text = "Lägg till volontär",
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Välj arbetspass",
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShiftType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedShiftType == type,
                            onClick = { selectedShiftType = type },
                            label = { Text(type.label) }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = wantsToWorkAlone,
                        onCheckedChange = { wantsToWorkAlone = it }
                    )

                    Text(
                        text = "Volontären önskar arbeta ensam"
                    )
                }
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Sök namn eller e-post") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (filteredVolunteers.isEmpty()) {
                    Text("Inga volontärer hittades.")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(
                            items = filteredVolunteers,
                            key = { it.id }
                        ) { volunteer ->

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedVolunteer = volunteer
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment =
                                    androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedVolunteer?.id == volunteer.id,
                                    onClick = {
                                        selectedVolunteer = volunteer
                                    }
                                )

                                Spacer(Modifier.width(8.dp))

                                Column {
                                    Text(
                                        text = volunteer.fullName,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Text(
                                        text = volunteer.email,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {
            TextButton(
                enabled = selectedVolunteer != null,
                onClick = {
                    selectedVolunteer?.let { volunteer ->
                        onVolunteerSelected(
                            volunteer,
                            selectedShiftType,
                            wantsToWorkAlone
                        )
                    }
                }
            ) {
                Text("Lägg till", color = DarkSage)
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Avbryt")
            }
        }
    )
}
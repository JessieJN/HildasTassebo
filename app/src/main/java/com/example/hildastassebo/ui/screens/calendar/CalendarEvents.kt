package com.example.hildastassebo.ui.screens.calendar

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hildastassebo.data.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID


private val DarkSage = Color(0xFF47796C)
private val Muted = Color(0xFF7D8793)
private val MissingRed = Color(0xFFE66A68)

// Dialoger för att skapa, redigera och ta bort evenemang.
@Composable
internal fun CalendarEventDialogs(
    selectedDate: LocalDate,
    dateFormatter: DateTimeFormatter,
    events: MutableMap<LocalDate, List<CalendarEvent>>,
    showEventDialog: Boolean,
    onShowEventDialogChange: (Boolean) -> Unit,
    eventTitle: String,
    onEventTitleChange: (String) -> Unit,
    eventDescription: String,
    onEventDescriptionChange: (String) -> Unit,
    eventTime: String,
    onEventTimeChange: (String) -> Unit,
    editingEvent: CalendarEvent?,
    onEditingEventChange: (CalendarEvent?) -> Unit,
    eventToDelete: CalendarEvent?,
    onEventToDeleteChange: (CalendarEvent?) -> Unit
) {


    // ------------------------------------------------------------
    // DIALOG: LÄGG TILL EVENEMANG
    // ------------------------------------------------------------
    if (showEventDialog) {
        AlertDialog(
            onDismissRequest = { onShowEventDialogChange(false) },
            title = {
                Text("Lägg till evenemang")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = selectedDate.format(dateFormatter)
                            .replaceFirstChar { it.uppercase() },
                        color = Muted
                    )

                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = { onEventTitleChange(it) },
                        label = { Text("Evenemangets namn") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = eventTime,
                        onValueChange = { onEventTimeChange(it) },
                        label = { Text("Tid (t.ex. 13:00–16:00)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = eventDescription,
                        onValueChange = { onEventDescriptionChange(it) },
                        label = { Text("Beskrivning") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = eventTitle.isNotBlank(),

                    onClick = {
                        val existing = events[selectedDate].orEmpty()
                        val edit = editingEvent

                        if (edit == null) {
                            // Skapa ett nytt evenemang
                            val newEvent = CalendarEvent(
                                id = UUID.randomUUID().toString(),
                                date = selectedDate,
                                title = eventTitle.trim(),
                                description = eventDescription.trim(),
                                time = eventTime.trim()
                            )

                            events[selectedDate] = existing + newEvent
                        } else {
                            // Uppdatera det befintliga evenemanget
                            events[selectedDate] = existing.map { event ->
                                if (event.id == edit.id) {
                                    event.copy(
                                        title = eventTitle.trim(),
                                        description = eventDescription.trim(),
                                        time = eventTime.trim()
                                    )
                                } else {
                                    event
                                }
                            }
                        }

                        onShowEventDialogChange(false)
                        onEditingEventChange(null)
                    }

                ) {
                    Text("Spara", color = DarkSage)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onShowEventDialogChange(false) }
                ) {
                    Text("Avbryt")
                }
            }
        )
    }


    // ----------------------------------------------------
    // BEKRÄFTELSE INNAN ETT EVENEMANG RADERAS
    // ----------------------------------------------------
    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = {
                onEventToDeleteChange(null)
            },
            title = {
                Text("Ta bort evenemang?")
            },
            text = {
                Text("Vill du verkligen ta bort evenemanget \"${event.title}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        events[event.date] = events[event.date]
                            .orEmpty()
                            .filterNot { it.id == event.id }

                        onEventToDeleteChange(null)
                    }
                ) {
                    Text(
                        text = "Ta bort",
                        color = MissingRed
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onEventToDeleteChange(null)
                    }
                ) {
                    Text("Avbryt")
                }
            }
        )
    }

}


package com.example.hildastassebo.ui.screens.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.hildastassebo.data.model.*
import com.example.hildastassebo.data.repository.EventRepository
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val DarkSage = Color(0xFF47796C)
private val Muted = Color(0xFF7D8793)
private val MissingRed = Color(0xFFE66A68)


@OptIn(ExperimentalMaterial3Api::class)
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
    onEventToDeleteChange: (CalendarEvent?) -> Unit,
    eventRepository: EventRepository,
    onEventError: (Exception) -> Unit
) {

    // ------------------------------------------------------------
    // DIALOG: SKAPA ELLER REDIGERA EVENEMANG
    // ------------------------------------------------------------
    if (showEventDialog) {

        // Läs in befintliga tider vid redigering.
        val initialTimes = remember(editingEvent, eventTime) {
            readEventTimes(eventTime)
        }

        var startTime by remember(editingEvent) {
            mutableStateOf(initialTimes?.first)
        }

        var endTime by remember(editingEvent) {
            mutableStateOf(initialTimes?.second)
        }

        // true = starttid, false = sluttid, null = stängd.
        var selectingStartTime by remember {
            mutableStateOf<Boolean?>(null)
        }

        // Förhindra dubbla sparningar.
        var isSaving by remember {
            mutableStateOf(false)
        }

        AlertDialog(
            onDismissRequest = {
                if (!isSaving) onShowEventDialogChange(false)
            },
            title = {
                Text(
                    if (editingEvent == null) {
                        "Lägg till evenemang"
                    } else {
                        "Redigera evenemang"
                    }
                )
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

                    // Evenemangets namn.
                    OutlinedTextField(
                        value = eventTitle,
                        onValueChange = onEventTitleChange,
                        label = { Text("Evenemangets namn") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        )
                    )

                    // Välj start- och sluttid med klockan.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        OutlinedButton(
                            onClick = { selectingStartTime = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 12.dp
                            )
                        ) {
                            Icon(
                                Icons.Outlined.AccessTime,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                startTime?.format(eventTimeFormatter)
                                    ?: "Starttid"
                            )
                        }

                        OutlinedButton(
                            onClick = { selectingStartTime = false },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(
                                horizontal = 8.dp,
                                vertical = 12.dp
                            )
                        ) {
                            Icon(
                                Icons.Outlined.AccessTime,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                endTime?.format(eventTimeFormatter)
                                    ?: "Sluttid"
                            )
                        }
                    }

                    // Tider är frivilliga.
                    if (startTime != null || endTime != null) {
                        TextButton(
                            onClick = {
                                startTime = null
                                endTime = null
                            }
                        ) {
                            Text("Ta bort tider", color = DarkSage)
                        }
                    }

                    // Beskrivning av evenemanget.
                    OutlinedTextField(
                        value = eventDescription,
                        onValueChange = onEventDescriptionChange,
                        label = { Text("Beskrivning") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = eventTitle.isNotBlank() && !isSaving,
                    onClick = {

                        // Kontrollera att tiderna är giltiga.
                        val invalidTimes =
                            (startTime == null) != (endTime == null) ||
                                    (
                                            startTime != null &&
                                                    endTime != null &&
                                                    !endTime!!.isAfter(startTime)
                                            )

                        if (invalidTimes) {
                            onEventError(
                                IllegalArgumentException(
                                    "Välj både start- och sluttid. " +
                                            "Sluttiden måste vara senare än starttiden."
                                )
                            )
                        } else {

                            isSaving = true

                            // Stäng dialogen efter lyckad sparning.
                            val onSuccess = {
                                isSaving = false
                                onShowEventDialogChange(false)
                                onEditingEventChange(null)
                                onEventTitleChange("")
                                onEventDescriptionChange("")
                                onEventTimeChange("")
                            }

                            val onError: (Exception) -> Unit = { exception ->
                                isSaving = false
                                onEventError(exception)
                            }

                            val event = editingEvent

                            // Skapa eller uppdatera i Firestore.
                            if (event == null) {
                                eventRepository.addEvent(
                                    date = selectedDate,
                                    title = eventTitle,
                                    description = eventDescription,
                                    startTime = startTime,
                                    endTime = endTime,
                                    onSuccess = onSuccess,
                                    onError = onError
                                )
                            } else {
                                eventRepository.updateEvent(
                                    event = event,
                                    title = eventTitle,
                                    description = eventDescription,
                                    startTime = startTime,
                                    endTime = endTime,
                                    onSuccess = onSuccess,
                                    onError = onError
                                )
                            }
                        }
                    }
                ) {
                    Text("Spara", color = DarkSage)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { onShowEventDialogChange(false) }
                ) {
                    Text("Avbryt")
                }
            }
        )

        // ------------------------------------------------------------
        // KLOCKVÄLJARE FÖR START- OCH SLUTTID
        // ------------------------------------------------------------
        selectingStartTime?.let { isStart ->

            val initialTime = if (isStart) startTime else endTime

            val pickerState = rememberTimePickerState(
                initialHour = initialTime?.hour ?: 12,
                initialMinute = initialTime?.minute ?: 0,
                is24Hour = true
            )

            AlertDialog(
                onDismissRequest = {
                    selectingStartTime = null
                },
                title = {
                    Text(
                        if (isStart) "Välj starttid" else "Välj sluttid"
                    )
                },
                text = {
                    TimePicker(state = pickerState)
                },
                confirmButton = {
                    TextButton(
                        onClick = {

                            val chosenTime = LocalTime.of(
                                pickerState.hour,
                                pickerState.minute
                            )

                            if (isStart) {
                                startTime = chosenTime
                            } else {
                                endTime = chosenTime
                            }

                            selectingStartTime = null
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            selectingStartTime = null
                        }
                    ) {
                        Text("Avbryt")
                    }
                }
            )
        }
    }

    // ------------------------------------------------------------
    // BEKRÄFTELSE INNAN ETT EVENEMANG RADERAS
    // ------------------------------------------------------------
    eventToDelete?.let { event ->

        var isDeleting by remember(event.id) {
            mutableStateOf(false)
        }

        AlertDialog(
            onDismissRequest = {
                if (!isDeleting) onEventToDeleteChange(null)
            },
            title = {
                Text("Ta bort evenemang?")
            },
            text = {
                Text(
                    "Vill du verkligen ta bort evenemanget \"${event.title}\"?"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isDeleting,
                    onClick = {

                        isDeleting = true

                        // Radera evenemanget från Firestore.
                        eventRepository.deleteEvent(
                            event = event,
                            onSuccess = {
                                isDeleting = false
                                onEventToDeleteChange(null)
                            },
                            onError = { exception ->
                                isDeleting = false
                                onEventError(exception)
                            }
                        )
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
                    enabled = !isDeleting,
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

// Format för visning av tider.
private val eventTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")

// Läs befintliga tider när ett evenemang redigeras.
private fun readEventTimes(
    value: String
): Pair<LocalTime?, LocalTime?>? {

    if (value.isBlank()) {
        return Pair(null, null)
    }

    val parts = value.trim().split(Regex("\\s*[–-]\\s*"))

    if (parts.size != 2) {
        return null
    }

    return try {
        Pair(
            LocalTime.parse(parts[0].trim()),
            LocalTime.parse(parts[1].trim())
        )
    } catch (exception: Exception) {
        null
    }
}

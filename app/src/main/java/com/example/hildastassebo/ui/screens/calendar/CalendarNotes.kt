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

// Dialoger för att skapa, redigera och ta bort anteckningar.
@Composable
internal fun CalendarNoteDialogs(
    selectedDate: LocalDate,
    dateFormatter: DateTimeFormatter,
    notes: MutableMap<LocalDate, List<CalendarNote>>,
    showNoteDialog: Boolean,
    onShowNoteDialogChange: (Boolean) -> Unit,
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    editingNote: CalendarNote?,
    onEditingNoteChange: (CalendarNote?) -> Unit,
    noteToDelete: CalendarNote?,
    onNoteToDeleteChange: (CalendarNote?) -> Unit,
    currentUserId: String,
    currentUserName: String,
    isAdmin: Boolean
) {


    // ------------------------------------------------------------
    // DIALOG: SKAPA ELLER REDIGERA EN ANTECKNING
    // ------------------------------------------------------------
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { onShowNoteDialogChange(false) },
            title = {
                Text(if (editingNote == null) "Lägg till anteckning" else "Redigera anteckning")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        selectedDate.format(dateFormatter)
                            .replaceFirstChar { it.uppercase() },
                        color = Muted
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { onNoteTextChange(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Anteckning") },
                        minLines = 3,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = noteText.isNotBlank() && currentUserId.isNotEmpty(),
                    onClick = {
                        val existing = notes[selectedDate].orEmpty()
                        val edit = editingNote
                        if (edit == null) {
                            notes[selectedDate] = existing + CalendarNote(
                                id = UUID.randomUUID().toString(),
                                text = noteText.trim(),
                                authorId = currentUserId,
                                authorName = currentUserName,
                                createdAt = LocalDateTime.now()
                            )
                        } else if (edit.authorId == currentUserId || isAdmin) {
                            // Behåll ursprunglig signatur och tid vid redigering.
                            notes[selectedDate] = existing.map { note ->
                                if (note.id == edit.id) note.copy(text = noteText.trim())
                                else note
                            }
                        }
                        onShowNoteDialogChange(false)
                        onEditingNoteChange(null)
                        onNoteTextChange("")
                    }
                ) {
                    Text("Spara")
                }
            },
            dismissButton = {
                TextButton(onClick = { onShowNoteDialogChange(false) }) {
                    Text("Avbryt")
                }
            }
        )
    }

    // ------------------------------------------------------------
    // BEKRÄFTELSE INNAN EN ANTECKNING RADERAS
    // ------------------------------------------------------------
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { onNoteToDeleteChange(null) },
            title = { Text("Ta bort anteckning?") },
            text = { Text("Vill du verkligen ta bort den här anteckningen?") },
            confirmButton = {
                TextButton(onClick = {
                    if (note.authorId == currentUserId || isAdmin) {
                        notes[selectedDate] = notes[selectedDate].orEmpty()
                            .filterNot { it.id == note.id }
                    }
                    onNoteToDeleteChange(null)
                }) {
                    Text("Ta bort", color = MissingRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { onNoteToDeleteChange(null) }) {
                    Text("Avbryt")
                }
            }
        )
    }

}

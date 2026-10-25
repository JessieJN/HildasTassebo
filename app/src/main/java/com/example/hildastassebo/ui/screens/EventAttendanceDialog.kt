
package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hildastassebo.data.model.CalendarEvent
import com.example.hildastassebo.data.repository.EventAttendance
import com.example.hildastassebo.data.repository.EventAttendanceRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)

private data class AttendanceUser(
    val id: String,
    val name: String
)

@Composable
fun EventAttendanceDialog(
    event: CalendarEvent,
    onDismiss: () -> Unit
) {
    val repository = remember { EventAttendanceRepository() }
    val userId = FirebaseAuth.getInstance().currentUser?.uid

    var attendance by remember(event.id) {
        mutableStateOf<List<EventAttendance>>(emptyList())
    }

    var users by remember(event.id) {
        mutableStateOf<List<AttendanceUser>>(emptyList())
    }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Hämta evenemangets svar i realtid.
    DisposableEffect(event.id) {
        val listener = repository.observeAttendance(
            eventId = event.id,
            onUpdate = { attendance = it },
            onError = {
                errorMessage = "Kunde inte hämta svaren."
            }
        )

        onDispose { listener.remove() }
    }

    // Hämta alla användare för deltagarlistorna.
    DisposableEffect(event.id) {
        val listener = FirebaseFirestore.getInstance()
            .collection("användare")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "Kunde inte hämta användarna."
                } else {
                    users = snapshot?.documents.orEmpty().map { document ->
                        val firstName = document.getString("förnamn").orEmpty()
                        val lastName = document.getString("efternamn").orEmpty()

                        AttendanceUser(
                            id = document.id,
                            name = "$firstName $lastName".trim()
                                .ifBlank { "Okänd användare" }
                        )
                    }.sortedBy { it.name.lowercase() }
                }
            }

        onDispose { listener.remove() }
    }

    val myStatus = attendance
        .find { it.userId == userId }
        ?.status

    val coming = users.filter { user ->
        attendance.any {
            it.userId == user.id && it.status == "kommer"
        }
    }

    val notComing = users.filter { user ->
        attendance.any {
            it.userId == user.id && it.status == "kommer_inte"
        }
    }

    val unanswered = users.filter { user ->
        attendance.none { it.userId == user.id }
    }

    fun saveAnswer(status: String) {
        if (isSaving) return

        isSaving = true

        repository.saveAttendance(
            eventId = event.id,
            status = status,
            onSuccess = {
                isSaving = false
            },
            onError = {
                isSaving = false
                errorMessage = "Kunde inte spara ditt svar."
            }
        )
    }

    val dateText = event.date.format(
        DateTimeFormatter.ofPattern(
            "d MMMM yyyy",
            Locale.forLanguageTag("sv-SE")
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),
        title = {
            Column {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "$dateText ${event.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Kommer du?",
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { saveAnswer("kommer") },
                        enabled = !isSaving && userId != null,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (myStatus == "kommer") {
                                DarkSage
                            } else {
                                PaleSage
                            },
                            contentColor = if (myStatus == "kommer") {
                                Color.White
                            } else {
                                DarkSage
                            }
                        )
                    ) {
                        Text("Kommer")
                    }

                    Button(
                        onClick = { saveAnswer("kommer_inte") },
                        enabled = !isSaving && userId != null,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (myStatus == "kommer_inte") {
                                DarkSage
                            } else {
                                PaleSage
                            },
                            contentColor = if (myStatus == "kommer_inte") {
                                Color.White
                            } else {
                                DarkSage
                            }
                        )
                    ) {
                        Text("Kommer inte")
                    }
                }

                HorizontalDivider()

                AttendanceSection(
                    title = "Kommer",
                    people = coming
                )

                AttendanceSection(
                    title = "Kommer inte",
                    people = notComing
                )

                AttendanceSection(
                    title = "Inte svarat",
                    people = unanswered
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Stäng", color = DarkSage)
            }
        }
    )
}

@Composable
private fun AttendanceSection(
    title: String,
    people: List<AttendanceUser>
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "$title (${people.size})",
            fontWeight = FontWeight.SemiBold,
            color = DarkSage
        )

        if (people.isEmpty()) {
            Text(
                text = "Ingen ännu",
                color = Muted,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            people.forEach { user ->
                Text(
                    text = "• ${user.name}",
                    color = Ink,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

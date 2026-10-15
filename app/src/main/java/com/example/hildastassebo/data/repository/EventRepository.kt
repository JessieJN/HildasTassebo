
package com.example.hildastassebo.data.repository

import com.example.hildastassebo.data.model.CalendarEvent
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

class EventRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val eventsCollection = db.collection("evenemang")
    private val usersCollection = db.collection("användare")

    private val zone = ZoneId.of("Europe/Stockholm")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Hämta evenemang och uppdatera kalendern i realtid.
    fun observeEvents(
        onUpdate: (Map<LocalDate, List<CalendarEvent>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return eventsCollection.addSnapshotListener { snapshot, error ->

            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val events = snapshot?.documents.orEmpty().mapNotNull { document ->

                val dateTimestamp = document.getTimestamp("datum")
                    ?: return@mapNotNull null

                val date = dateTimestamp.toDate()
                    .toInstant()
                    .atZone(zone)
                    .toLocalDate()

                val start = document.getTimestamp("starttid")
                    ?.toDate()
                    ?.toInstant()
                    ?.atZone(zone)
                    ?.toLocalTime()

                val end = document.getTimestamp("sluttid")
                    ?.toDate()
                    ?.toInstant()
                    ?.atZone(zone)
                    ?.toLocalTime()

                // Formatera start- och sluttid för kalendern.
                val timeText = if (start != null && end != null) {
                    "${start.format(timeFormatter)}–${end.format(timeFormatter)}"
                } else {
                    ""
                }

                CalendarEvent(
                    id = document.id,
                    date = date,
                    title = document.getString("titel").orEmpty(),
                    description = document.getString("beskrivning").orEmpty(),
                    time = timeText,

                    // Läs vem som skapade evenemanget.
                    creatorId = document.getString("skapad_av").orEmpty()
                )
            }

            onUpdate(events.groupBy { it.date })
        }
    }

    // Skapa ett evenemang i Firestore.
    fun addEvent(
        date: LocalDate,
        title: String,
        description: String,
        startTime: LocalTime?,
        endTime: LocalTime?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onError(IllegalStateException("Du måste vara inloggad."))
            return
        }

        if (title.isBlank()) {
            onError(IllegalArgumentException("Ange ett namn på evenemanget."))
            return
        }

        if (!validTimes(startTime, endTime)) {
            onError(IllegalArgumentException("Kontrollera start- och sluttiden."))
            return
        }

        val data = hashMapOf<String, Any?>(
            "datum" to toTimestamp(date.atStartOfDay()),
            "titel" to title.trim(),
            "beskrivning" to description.trim(),
            "skapad_av" to userId,
            "starttid" to startTime?.let {
                toTimestamp(date.atTime(it))
            },
            "sluttid" to endTime?.let {
                toTimestamp(date.atTime(it))
            }
        )

        eventsCollection.add(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    // Redigera ett befintligt evenemang.
    fun updateEvent(
        event: CalendarEvent,
        title: String,
        description: String,
        startTime: LocalTime?,
        endTime: LocalTime?,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (title.isBlank()) {
            onError(IllegalArgumentException("Ange ett namn på evenemanget."))
            return
        }

        if (!validTimes(startTime, endTime)) {
            onError(IllegalArgumentException("Kontrollera start- och sluttiden."))
            return
        }

        checkPermission(
            eventId = event.id,
            onAllowed = {
                val updates = mapOf(
                    "titel" to title.trim(),
                    "beskrivning" to description.trim(),
                    "starttid" to startTime?.let {
                        toTimestamp(event.date.atTime(it))
                    },
                    "sluttid" to endTime?.let {
                        toTimestamp(event.date.atTime(it))
                    }
                )

                eventsCollection.document(event.id)
                    .update(updates)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it) }
            },
            onError = onError
        )
    }

    // Radera ett evenemang från Firestore.
    fun deleteEvent(
        event: CalendarEvent,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        checkPermission(
            eventId = event.id,
            onAllowed = {
                eventsCollection.document(event.id)
                    .delete()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onError(it) }
            },
            onError = onError
        )
    }

    // Kontrollera om användaren är skapare eller administratör.
    private fun checkPermission(
        eventId: String,
        onAllowed: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onError(IllegalStateException("Du måste vara inloggad."))
            return
        }

        eventsCollection.document(eventId).get()
            .addOnSuccessListener { eventDocument ->

                if (!eventDocument.exists()) {
                    onError(IllegalStateException("Evenemanget finns inte längre."))
                    return@addOnSuccessListener
                }

                if (eventDocument.getString("skapad_av") == userId) {
                    onAllowed()
                    return@addOnSuccessListener
                }

                usersCollection.document(userId).get()
                    .addOnSuccessListener { userDocument ->
                        if (userDocument.getLong("roll") == 1L) {
                            onAllowed()
                        } else {
                            onError(
                                SecurityException(
                                    "Du får bara ändra dina egna evenemang."
                                )
                            )
                        }
                    }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    // Kontrollera att tiderna är giltiga.
    private fun validTimes(
        start: LocalTime?,
        end: LocalTime?
    ): Boolean {
        return (start == null && end == null) ||
                (start != null && end != null && end.isAfter(start))
    }

    // Omvandla lokal tid till Firestore Timestamp.
    private fun toTimestamp(dateTime: LocalDateTime): Timestamp {
        val instant: Instant = dateTime.atZone(zone).toInstant()
        return Timestamp(Date.from(instant))
    }
}

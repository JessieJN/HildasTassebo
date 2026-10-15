package com.example.hildastassebo.data.repository


import com.example.hildastassebo.data.model.CalendarNote
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class NoteRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val notesCollection = db.collection("anteckningar")

    // Hämtar alla anteckningar och uppdaterar när något ändras.
    fun observeNotes(
        onUpdate: (Map<LocalDate, List<CalendarNote>>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return notesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val notesByDate = mutableMapOf<LocalDate, MutableList<CalendarNote>>()

            snapshot?.documents?.forEach { document ->
                try {
                    val date = LocalDate.parse(
                        document.getString("datum") ?: return@forEach
                    )

                    val createdAt = document.getTimestamp("skapad_tidpunkt")
                        ?.toDate()
                        ?.toInstant()
                        ?.atZone(ZoneId.of("Europe/Stockholm"))
                        ?.toLocalDateTime()
                        ?: LocalDateTime.now()

                    val note = CalendarNote(
                        id = document.id,
                        text = document.getString("text").orEmpty(),
                        authorId = document.getString("författar_id").orEmpty(),
                        authorName = document.getString("författarnamn").orEmpty(),
                        createdAt = createdAt,
                        isAdminNote = document.getBoolean("admin_anteckning") ?: false
                    )

                    notesByDate.getOrPut(date) { mutableListOf() }.add(note)
                } catch (exception: Exception) {
                    android.util.Log.e(
                        "NoteRepository",
                        "Kunde inte läsa anteckning ${document.id}",
                        exception
                    )
                }
            }

            onUpdate(notesByDate)
        }
    }

    // Skapar en ny anteckning.
    fun addNote(
        date: LocalDate,
        text: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onError(IllegalStateException("Du måste vara inloggad."))
            return
        }

        if (text.isBlank()) {
            onError(IllegalArgumentException("Anteckningen får inte vara tom."))
            return
        }

        val userRef = db.collection("användare").document(userId)

        userRef.get()
            .addOnSuccessListener { user ->
                if (!user.exists()) {
                    onError(IllegalStateException("Användaren finns inte."))
                    return@addOnSuccessListener
                }

                val firstName = user.getString("förnamn").orEmpty()
                val lastName = user.getString("efternamn").orEmpty()
                val fullName = "$firstName $lastName".trim()
                    .ifBlank { "Okänd användare" }

                notesCollection.add(
                    mapOf(
                        "datum" to date.toString(),
                        "text" to text.trim(),
                        "författar_id" to userId,
                        "författarnamn" to fullName,
                        "skapad_tidpunkt" to FieldValue.serverTimestamp(),
                        "admin_anteckning" to (user.getLong("roll") == 1L)
                    )
                ).addOnSuccessListener {
                    onSuccess()
                }.addOnFailureListener(onError)
            }
            .addOnFailureListener(onError)
    }

    // Redigerar en anteckning.
    fun updateNote(
        note: CalendarNote,
        text: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (text.isBlank()) {
            onError(IllegalArgumentException("Anteckningen får inte vara tom."))
            return
        }

        checkPermission(note, onError) {
            notesCollection.document(note.id)
                .update("text", text.trim())
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener(onError)
        }
    }

    // Tar bort en anteckning.
    fun deleteNote(
        note: CalendarNote,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        checkPermission(note, onError) {
            notesCollection.document(note.id)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener(onError)
        }
    }

    // Kontrollerar om användaren är volontär eller admin.
    private fun checkPermission(
        note: CalendarNote,
        onError: (Exception) -> Unit,
        onAllowed: () -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onError(IllegalStateException("Du måste vara inloggad."))
            return
        }

        db.collection("användare").document(userId)
            .get()
            .addOnSuccessListener { user ->
                val isAdmin = user.getLong("roll") == 1L

                if (note.authorId == userId || isAdmin) {
                    onAllowed()
                } else {
                    onError(
                        IllegalStateException(
                            "Du får bara ändra dina egna anteckningar."
                        )
                    )
                }
            }
            .addOnFailureListener(onError)
    }
}

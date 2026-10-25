
package com.example.hildastassebo.data.repository

import com.example.hildastassebo.data.model.HomeAnnouncementModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class AnnouncementRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val announcementsCollection = db.collection("information")
    private val usersCollection = db.collection("användare")
    private val zone = ZoneId.of("Europe/Stockholm")

    // Hämtar aktuella meddelanden i realtid.
    fun observeAnnouncements(
        onUpdate: (List<HomeAnnouncementModel>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return announcementsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(error)
                return@addSnapshotListener
            }

            val now = LocalDateTime.now(zone)

            val announcements = snapshot?.documents.orEmpty()
                .mapNotNull { document ->

                    val createdAt = document.getTimestamp("skapad_tidpunkt")
                        ?.toLocalDateTime() ?: return@mapNotNull null

                    val expiresAt = document.getTimestamp("utgår_tidpunkt")
                        ?.toLocalDateTime() ?: return@mapNotNull null

                    // Dölj meddelanden som har gått ut.
                    if (!expiresAt.isAfter(now)) return@mapNotNull null

                    HomeAnnouncementModel(
                        id = document.id,
                        title = document.getString("titel").orEmpty(),
                        message = document.getString("text").orEmpty(),
                        authorId = document.getString("författar_id").orEmpty(),
                        authorName = document.getString("författarnamn").orEmpty(),
                        createdAt = createdAt,
                        updatedAt = document.getTimestamp("redigerad_tidpunkt")
                            ?.toLocalDateTime(),
                        expiresAt = expiresAt
                    )
                }
                .sortedByDescending { it.createdAt }

            onUpdate(announcements)
        }
    }

    // Publicerar ett nytt meddelande.
    fun addAnnouncement(
        title: String,
        message: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (title.isBlank() || message.isBlank()) {
            onError(IllegalArgumentException("Fyll i rubrik och meddelande."))
            return
        }

        checkAdmin(onError) { userId, authorName ->

            val expiresAt = LocalDateTime.now(zone).plusMonths(1)

            announcementsCollection.add(
                mapOf(
                    "titel" to title.trim(),
                    "text" to message.trim(),
                    "författar_id" to userId,
                    "författarnamn" to authorName,
                    "skapad_tidpunkt" to FieldValue.serverTimestamp(),
                    "redigerad_tidpunkt" to null,
                    "utgår_tidpunkt" to Timestamp(
                        Date.from(expiresAt.atZone(zone).toInstant())
                    )
                )
            ).addOnSuccessListener {
                onSuccess()
            }.addOnFailureListener(onError)
        }
    }

    // Redigerar text utan att ändra publiceringsdatum.
    fun updateAnnouncement(
        announcement: HomeAnnouncementModel,
        title: String,
        message: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (title.isBlank() || message.isBlank()) {
            onError(IllegalArgumentException("Fyll i rubrik och meddelande."))
            return
        }

        checkAdmin(onError) { _, _ ->

            announcementsCollection.document(announcement.id)
                .update(
                    mapOf(
                        "titel" to title.trim(),
                        "text" to message.trim(),
                        "redigerad_tidpunkt" to FieldValue.serverTimestamp()
                    )
                )
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener(onError)
        }
    }

    // Administratören kan när som helst ta bort ett meddelande.
    fun deleteAnnouncement(
        announcement: HomeAnnouncementModel,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        checkAdmin(onError) { _, _ ->

            announcementsCollection.document(announcement.id)
                .delete()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener(onError)
        }
    }

    // Raderar utgångna meddelanden när en admin använder appen.
    fun deleteExpiredAnnouncements(
        onError: (Exception) -> Unit
    ) {
        checkAdmin(onError) { _, _ ->

            // Hämta bara meddelanden vars utgångsdatum passerat.
            announcementsCollection
                .whereLessThanOrEqualTo(
                    "utgår_tidpunkt",
                    Timestamp.now()
                )
                .get()
                .addOnSuccessListener { snapshot ->

                    // Radera varje utgånget meddelande.
                    snapshot.documents.forEach { document ->
                        document.reference.delete()
                            .addOnFailureListener(onError)
                    }
                }
                .addOnFailureListener(onError)
        }
    }

    // Kontrollerar adminrollen och hämtar administratörens namn.
    private fun checkAdmin(
        onError: (Exception) -> Unit,
        onAllowed: (String, String) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onError(IllegalStateException("Du måste vara inloggad."))
            return
        }

        usersCollection.document(userId).get()
            .addOnSuccessListener { user ->

                if (!user.exists() || user.getLong("roll") != 1L) {
                    onError(
                        SecurityException(
                            "Endast administratörer får ändra information."
                        )
                    )
                    return@addOnSuccessListener
                }

                val firstName = user.getString("förnamn").orEmpty()
                val lastName = user.getString("efternamn").orEmpty()
                val authorName = "$firstName $lastName".trim()
                    .ifBlank { "Administratör" }

                onAllowed(userId, authorName)
            }
            .addOnFailureListener(onError)
    }

    // Omvandlar Firestore Timestamp till svensk tid.
    private fun Timestamp.toLocalDateTime(): LocalDateTime {
        return toDate().toInstant().atZone(zone).toLocalDateTime()
    }
}

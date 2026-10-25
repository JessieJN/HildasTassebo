
package com.example.hildastassebo.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue

// Ett svar på ett evenemang.
data class EventAttendance(
    val userId: String,
    val status: String
)

class EventAttendanceRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val eventsCollection = db.collection("evenemang")

    // =====================================
    // SPARA ELLER ÄNDRA SVAR
    // =====================================
    fun saveAttendance(
        eventId: String,
        status: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onError(IllegalStateException("Du måste vara inloggad."))
            return
        }

        if (status != "kommer" && status != "kommer_inte") {
            onError(IllegalArgumentException("Ogiltigt svar."))
            return
        }

        val data = mapOf(
            "userId" to userId,
            "status" to status,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        eventsCollection
            .document(eventId)
            .collection("svar")
            .document(userId)
            .set(data)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }

    // =====================================
    // HÄMTA ALLA SVAR I REALTID
    // =====================================
    fun observeAttendance(
        eventId: String,
        onUpdate: (List<EventAttendance>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {

        return eventsCollection
            .document(eventId)
            .collection("svar")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val attendance = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->

                        val status = document.getString("status")
                            ?: return@mapNotNull null

                        if (status != "kommer" && status != "kommer_inte") {
                            return@mapNotNull null
                        }

                        EventAttendance(
                            userId = document.id,
                            status = status
                        )
                    }

                onUpdate(attendance)
            }
    }
}

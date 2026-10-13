
package com.example.hildastassebo.data.repository

import com.example.hildastassebo.data.model.Volunteer
import com.google.firebase.firestore.FirebaseFirestore

class VolunteerRepository {

    private val db = FirebaseFirestore.getInstance()

    fun getVolunteers(
        onSuccess: (List<Volunteer>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("användare")
            .get()
            .addOnSuccessListener { result ->

                val volunteers = result.documents.map { document ->

                    Volunteer(
                        id = document.id,
                        firstName = document.getString("förnamn") ?: "",
                        lastName = document.getString("efternamn") ?: "",
                        email = document.getString("epost") ?: "",
                        role = document.getLong("roll") ?: 0L
                    )
                }

                onSuccess(volunteers)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
    fun updateUser(
        userId: String,
        firstName: String,
        lastName: String,
        role: Long,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("användare")
            .document(userId)
            .update(
                mapOf(
                    "förnamn" to firstName,
                    "efternamn" to lastName,
                    "roll" to role
                )
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it) }
    }
}

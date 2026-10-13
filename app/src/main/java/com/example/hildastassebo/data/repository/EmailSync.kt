package com.example.hildastassebo.data.repository


import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

fun syncUserEmail() {
    val user = FirebaseAuth.getInstance().currentUser ?: return

    user.reload()
        .addOnSuccessListener {
            val currentUser = FirebaseAuth.getInstance().currentUser
                ?: return@addOnSuccessListener

            val verifiedEmail = currentUser.email ?: return@addOnSuccessListener

            FirebaseFirestore.getInstance()
                .collection("användare")
                .document(currentUser.uid)
                .update("epost", verifiedEmail)
                .addOnFailureListener { exception ->
                    Log.e(
                        "EMAIL_SYNC",
                        "Kunde inte synkronisera e-postadressen",
                        exception
                    )
                }
        }
        .addOnFailureListener { exception ->
            Log.e(
                "EMAIL_SYNC",
                "Kunde inte uppdatera Firebase-användaren",
                exception
            )
        }
}

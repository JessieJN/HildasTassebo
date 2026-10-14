package com.example.hildastassebo.data.repository

import com.example.hildastassebo.data.model.ShiftType
import com.example.hildastassebo.data.model.Volunteer
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

class ShiftRepository {

    private val db = FirebaseFirestore.getInstance()

    fun addVolunteerToShift(
        volunteer: Volunteer,
        date: LocalDate,
        shiftType: ShiftType,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val bookingId = "${date}_${shiftType.name}_${volunteer.id}"

        val booking = hashMapOf(
            "volunteerId" to volunteer.id,
            "volunteerName" to volunteer.fullName,
            "date" to date.toString(),
            "shiftType" to shiftType.name,
            "wantsToWorkAlone" to false
        )

        val bookingRef = db.collection("passbokningar")
            .document(bookingId)

        db.runTransaction { transaction ->
            val existingBooking = transaction.get(bookingRef)

            if (existingBooking.exists()) {
                throw IllegalStateException(
                    "Volontären är redan bokad på detta pass."
                )
            }

            transaction.set(bookingRef, booking)
            null
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { exception ->
            onError(exception)
        }
    }
}
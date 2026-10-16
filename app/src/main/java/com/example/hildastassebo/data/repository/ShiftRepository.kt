
package com.example.hildastassebo.data.repository

import com.example.hildastassebo.data.model.ShiftType
import com.example.hildastassebo.data.model.Volunteer
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

data class SavedShiftBooking(
    val id: String,
    val userId: String,
    val date: LocalDate,
    val shiftType: ShiftType,
    val wantsToWorkAlone: Boolean
)

class ShiftRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun currentUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Du måste vara inloggad.")

    // =====================================
    // BOKA EGET ARBETSPASS
    // =====================================

    // Vanlig bokning i kalendern, för både admin och volontär.
    fun bookOwnShift(
        date: LocalDate,
        shiftType: ShiftType,
        wantsToWorkAlone: Boolean,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = try {
            currentUserId()
        } catch (e: Exception) {
            onError(e)
            return
        }

        saveBooking(
            userId, date, shiftType, wantsToWorkAlone,
            requireAdmin = false, onSuccess, onError
        )
    }

    // =====================================
    // ADMIN BOKAR ARBETSPASS
    // =====================================

    // Admin bokar ett pass åt en annan användare.
    fun adminBookShift(
        userId: String,
        date: LocalDate,
        shiftType: ShiftType,
        wantsToWorkAlone: Boolean,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        saveBooking(
            userId, date, shiftType, wantsToWorkAlone,
            requireAdmin = true, onSuccess, onError
        )
    }

    // Behåller det befintliga anropet från CalendarScreen.
    fun addVolunteerToShift(
        volunteer: Volunteer,
        date: LocalDate,
        shiftType: ShiftType,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
        wantsToWorkAlone: Boolean = false
    ) {
        adminBookShift(
            volunteer.id, date, shiftType, wantsToWorkAlone,
            onSuccess, onError
        )
    }

    // =====================================
    // SPARA BOKNING
    // =====================================

    private fun saveBooking(
        userId: String,
        date: LocalDate,
        shiftType: ShiftType,
        wantsToWorkAlone: Boolean,
        requireAdmin: Boolean,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val actorId = try {
            currentUserId()
        } catch (e: Exception) {
            onError(e)
            return
        }
        val today = LocalDate.now(ZoneId.of("Europe/Stockholm"))

        if (date.isBefore(today)) {
            onError(
                IllegalStateException(
                    "Du kan inte boka arbetspass på datum som redan har passerat."
                )
            )
            return
        }

        val dateId = date.toString()
        val dayRef = db.collection("arbetspass").document(dateId)
        val bookingRef = db.collection("bokningar")
            .document("${dateId}_${shiftType.name}_$userId")

        val actorRef = db.collection("användare").document(actorId)
        val targetRef = db.collection("användare").document(userId)

        val bookedField = if (shiftType == ShiftType.MORNING)
            "förmiddag_bokade" else "eftermiddag_bokade"

        val aloneField = if (shiftType == ShiftType.MORNING)
            "förmiddag_ensam" else "eftermiddag_ensam"

        db.runTransaction { transaction ->

            // Alla läsningar måste ske före skrivningarna.
            val actor = transaction.get(actorRef)
            val target = transaction.get(targetRef)
            val day = transaction.get(dayRef)
            val existing = transaction.get(bookingRef)

            val isAdmin = actor.getLong("roll") == 1L

            // =====================================
            // SKYDDA KONTON UNDER RADERING
            // =====================================

            // Stoppa ändringar om den inloggade
            // användarens profil saknas.
            if (!actor.exists()) {
                throw IllegalStateException(
                    "Ditt användarkonto finns inte."
                )
            }

            // Stoppa bokning om den inloggade
            // användaren håller på att radera kontot.
            if (
                actor.getBoolean("kontoradering_pågår") == true
            ) {
                throw IllegalStateException(
                    "Ditt konto håller på att raderas. " +
                            "Du kan inte boka nya arbetspass."
                )
            }

            // Stoppa bokning om personen som ska
            // bokas håller på att radera sitt konto.
            // Gäller även när admin bokar åt någon annan.
            if (
                target.getBoolean("kontoradering_pågår") == true
            ) {
                throw IllegalStateException(
                    "Den här användaren håller på att " +
                            "radera sitt konto och kan inte bokas."
                )
            }

            // =====================================
            // BEFINTLIGA BEHÖRIGHETSKONTROLLER
            // =====================================

            if (requireAdmin && !isAdmin) {
                throw IllegalStateException(
                    "Endast administratörer får boka åt andra."
                )
            }

            if (!isAdmin && actorId != userId) {
                throw IllegalStateException(
                    "Du får bara boka pass åt dig själv."
                )
            }

            if (!target.exists()) {
                throw IllegalStateException(
                    "Användaren finns inte."
                )
            }

            if (existing.exists()) {
                throw IllegalStateException(
                    "Personen är redan bokad på detta pass."
                )
            }

            val bookedIds = (day.get(bookedField) as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()

            val someoneWorksAlone =
                day.getBoolean(aloneField) ?: false

            if (userId in bookedIds) {
                throw IllegalStateException(
                    "Personen är redan bokad på detta pass."
                )
            }

            if (someoneWorksAlone) {
                throw IllegalStateException(
                    "Passet är reserverat för ensamarbete."
                )
            }

            if (wantsToWorkAlone && bookedIds.isNotEmpty()) {
                throw IllegalStateException(
                    "Det finns redan andra bokade på passet."
                )
            }

            // =====================================
            // SKAPA ARBETSPASS VID BEHOV
            // =====================================

            if (!day.exists()) {
                val timestamp = Date.from(
                    date.atStartOfDay(
                        ZoneId.of("Europe/Stockholm")
                    ).toInstant()
                )

                transaction.set(
                    dayRef,
                    mapOf(
                        "datum" to timestamp,
                        "förmiddag" to "",
                        "eftermiddag" to ""
                    )
                )
            }

            // =====================================
            // SPARA ARBETSPASS OCH BOKNING
            // =====================================

            transaction.set(
                dayRef,
                mapOf(
                    bookedField to FieldValue.arrayUnion(userId),
                    aloneField to wantsToWorkAlone
                ),
                SetOptions.merge()
            )

            transaction.set(
                bookingRef,
                mapOf(
                    "användar_id" to userId,
                    "arbetspass_id" to dateId,
                    "passtyp" to shiftType.name,
                    "bokad_tidpunkt" to FieldValue.serverTimestamp(),
                    "önskar_arbeta_själv" to wantsToWorkAlone
                )
            )

            null
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener(onError)
    }

    // =====================================
    // AVBOKA EGET ARBETSPASS
    // =====================================

    fun cancelOwnShift(
        date: LocalDate,
        shiftType: ShiftType,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = try {
            currentUserId()
        } catch (e: Exception) {
            onError(e)
            return
        }

        removeBooking(
            userId, date, shiftType,
            requireAdmin = false, onSuccess, onError
        )
    }

    // =====================================
    // ADMIN AVBOKAR ARBETSPASS
    // =====================================

    fun adminCancelShift(
        userId: String,
        date: LocalDate,
        shiftType: ShiftType,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        removeBooking(
            userId, date, shiftType,
            requireAdmin = true, onSuccess, onError
        )
    }

    // =====================================
    // TA BORT BOKNING
    // =====================================

    private fun removeBooking(
        userId: String,
        date: LocalDate,
        shiftType: ShiftType,
        requireAdmin: Boolean,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val actorId = try {
            currentUserId()
        } catch (e: Exception) {
            onError(e)
            return
        }

        val dateId = date.toString()
        val dayRef = db.collection("arbetspass").document(dateId)
        val bookingRef = db.collection("bokningar")
            .document("${dateId}_${shiftType.name}_$userId")
        val actorRef = db.collection("användare").document(actorId)
        val targetRef = db.collection("användare").document(userId)

        val bookedField = if (shiftType == ShiftType.MORNING)
            "förmiddag_bokade" else "eftermiddag_bokade"

        val aloneField = if (shiftType == ShiftType.MORNING)
            "förmiddag_ensam" else "eftermiddag_ensam"

        db.runTransaction { transaction ->

            // Alla läsningar måste ske före skrivningarna.
            val actor = transaction.get(actorRef)
            val target = transaction.get(targetRef)
            val day = transaction.get(dayRef)
            val booking = transaction.get(bookingRef)

            val isAdmin = actor.getLong("roll") == 1L

            // =====================================
            // SKYDDA KONTON UNDER RADERING
            // =====================================

            // Ingen vanlig avbokning ska ske från
            // ett konto som håller på att raderas.
            if (!actor.exists()) {
                throw IllegalStateException(
                    "Ditt användarkonto finns inte."
                )
            }

            if (
                actor.getBoolean("kontoradering_pågår") == true
            ) {
                throw IllegalStateException(
                    "Ditt konto håller på att raderas. " +
                            "Du kan inte ändra arbetspass."
                )
            }

            // Stoppa vanliga ändringar av bokningar
            // för en användare som håller på att raderas.
            // Själva kontoraderingen använder en
            // separat funktion i AccountDeletionRepository.
            if (
                target.getBoolean("kontoradering_pågår") == true
            ) {
                throw IllegalStateException(
                    "Den här användarens konto håller på " +
                            "att raderas."
                )
            }

            // =====================================
            // BEFINTLIGA BEHÖRIGHETSKONTROLLER
            // =====================================

            if (requireAdmin && !isAdmin) {
                throw IllegalStateException(
                    "Endast administratörer får ta bort andras pass."
                )
            }

            if (!isAdmin && actorId != userId) {
                throw IllegalStateException(
                    "Du får bara avboka dina egna pass."
                )
            }

            if (!isAdmin &&
                !date.isAfter(
                    LocalDate.now(
                        ZoneId.of("Europe/Stockholm")
                    ).plusDays(7)
                )
            ) {
                throw IllegalStateException(
                    "Pass kan inte avbokas inom 7 dagar."
                )
            }

            if (!booking.exists() || !day.exists()) {
                throw IllegalStateException(
                    "Bokningen finns inte."
                )
            }

            val bookedIds = (day.get(bookedField) as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()

            if (userId !in bookedIds) {
                throw IllegalStateException(
                    "Bokningsinformationen stämmer inte."
                )
            }

            // =====================================
            // UPPDATERA ARBETSPASS OCH BOKNING
            // =====================================

            transaction.update(
                dayRef,
                mapOf(
                    bookedField to FieldValue.arrayRemove(userId),
                    aloneField to if (bookedIds.size == 1)
                        false
                    else
                        day.getBoolean(aloneField) ?: false
                )
            )

            transaction.delete(bookingRef)

            null
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener(onError)
    }

    // =====================================
    // LYSSNA PÅ BOKNINGAR
    // =====================================

    // Lyssnar på riktiga bokningar och
    // uppdaterar kalendern vid ändringar.
    fun observeBookings(
        onUpdate: (List<SavedShiftBooking>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("bokningar")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val bookings = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        try {
                            val userId =
                                document.getString("användar_id")
                                    ?: return@mapNotNull null

                            val date = LocalDate.parse(
                                document.getString("arbetspass_id")
                                    ?: return@mapNotNull null
                            )

                            val type = ShiftType.valueOf(
                                document.getString("passtyp")
                                    ?: return@mapNotNull null
                            )

                            SavedShiftBooking(
                                id = document.id,
                                userId = userId,
                                date = date,
                                shiftType = type,
                                wantsToWorkAlone =
                                    document.getBoolean(
                                        "önskar_arbeta_själv"
                                    ) ?: false
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }

                onUpdate(bookings)
            }
    }
}


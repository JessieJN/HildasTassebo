
package com.example.hildastassebo.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source

class AccountDeletionRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Hur många dokument vi behandlar åt gången.
    // Vi fortsätter automatiskt med nästa omgång.
    private val batchSize = 100L

    // Förhindrar att två raderingsprocesser körs
    // samtidigt i samma repository-instans.
    private var deletionInProgress = false

    // ==========================================
    // STARTA ELLER FORTSÄTT KONTORADERING
    // ==========================================

    // Denna funktion anropas efter att användaren
    // har bekräftat sitt lösenord.
    //
    // onProgress skickar statusmeddelanden till skärmen.
    // onSuccess betyder att Firestore-rensningen är klar.
    // onError betyder att något gick fel.
    //
    // Vid ett avbrott kan funktionen anropas igen.
    fun deleteUserFirestoreData(
        onProgress: (String) -> Unit,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onError(
                IllegalStateException("Du måste vara inloggad.")
            )
            return
        }

        if (deletionInProgress) {
            onError(
                IllegalStateException(
                    "Kontoraderingen pågår redan."
                )
            )
            return
        }

        deletionInProgress = true

        // Återställ status om raderingen misslyckas.
        val fail: (Exception) -> Unit = { exception ->
            deletionInProgress = false
            onError(exception)
        }

        // Återställ status när Firestore är färdigt.
        val finish: () -> Unit = {
            deletionInProgress = false
            onSuccess()
        }

        onProgress("Förbereder kontoradering...")

        // Spara att kontoraderingen har påbörjats.
        // Denna markering används för att upptäcka
        // och återuppta en avbruten radering.
        val profileRef = db.collection("användare")
            .document(userId)

        profileRef.get(Source.SERVER)
            .addOnSuccessListener { profile ->

                if (!profile.exists()) {

                    // Profilen kan redan vara borttagen
                    // efter ett tidigare försök.
                    // Fortsätt ändå att kontrollera övrig data.
                    deleteAllUserData(
                        userId = userId,
                        onProgress = onProgress,
                        onSuccess = finish,
                        onError = fail
                    )

                    return@addOnSuccessListener
                }

                profileRef.update(
                    "kontoradering_pågår",
                    true
                ).addOnSuccessListener {

                    deleteAllUserData(
                        userId = userId,
                        onProgress = onProgress,
                        onSuccess = finish,
                        onError = fail
                    )

                }.addOnFailureListener { exception ->
                    fail(exception)
                }

            }.addOnFailureListener { exception ->
                fail(exception)
            }
    }

    // ==========================================
    // ORDNINGEN FÖR RADERING
    // ==========================================

    private fun deleteAllUserData(
        userId: String,
        onProgress: (String) -> Unit,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // STEG 1: Anteckningar.
        onProgress("Raderar anteckningar...")

        deleteCollectionDocuments(
            collection = "anteckningar",
            field = "författar_id",
            userId = userId,
            onSuccess = {

                // STEG 2: Evenemang.
                onProgress("Raderar evenemang...")

                deleteCollectionDocuments(
                    collection = "evenemang",
                    field = "skapad_av",
                    userId = userId,
                    onSuccess = {

                        // STEG 3: Kalenderns arbetspass.
                        onProgress("Uppdaterar arbetspass...")

                        removeUserFromAllShifts(
                            userId = userId,
                            onSuccess = {

                                // STEG 4: Bokningsdokument.
                                onProgress("Raderar bokningar...")

                                deleteCollectionDocuments(
                                    collection = "bokningar",
                                    field = "användar_id",
                                    userId = userId,
                                    onSuccess = {

                                        // STEG 5: Kontrollera resultatet.
                                        onProgress(
                                            "Kontrollerar att alla uppgifter är borttagna..."
                                        )

                                        verifyDeletion(
                                            userId = userId,
                                            onSuccess = {

                                                // STEG 6: Användarprofil.
                                                onProgress(
                                                    "Raderar användarprofil..."
                                                )

                                                db.collection("användare")
                                                    .document(userId)
                                                    .delete()
                                                    .addOnSuccessListener {
                                                        onSuccess()
                                                    }
                                                    .addOnFailureListener {
                                                        onError(it)
                                                    }
                                            },
                                            onError = onError
                                        )
                                    },
                                    onError = onError
                                )
                            },
                            onError = onError
                        )
                    },
                    onError = onError
                )
            },
            onError = onError
        )
    }

    // ==========================================
    // RADERA DOKUMENT I MINDRE OMGÅNGAR
    // ==========================================

    // Raderar högst 100 dokument per omgång.
    // Fortsätter tills inga dokument finns kvar.
    //
    // Använder Source.SERVER för att kontrollera
    // mot databasen och inte bara lokal cache.
    private fun deleteCollectionDocuments(
        collection: String,
        field: String,
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection(collection)
            .whereEqualTo(field, userId)
            .limit(batchSize)
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    onSuccess()
                    return@addOnSuccessListener
                }

                val batch = db.batch()

                snapshot.documents.forEach { document ->
                    batch.delete(document.reference)
                }

                batch.commit()
                    .addOnSuccessListener {

                        // Nästa omgång startas automatiskt.
                        deleteCollectionDocuments(
                            collection = collection,
                            field = field,
                            userId = userId,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                    }
                    .addOnFailureListener { exception ->
                        onError(exception)
                    }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    // ==========================================
    // TA BORT ANVÄNDAREN FRÅN ARBETSPASS
    // ==========================================

    private fun removeUserFromAllShifts(
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Först förmiddagspass.
        removeUserFromShiftType(
            userId = userId,
            bookedField = "förmiddag_bokade",
            aloneField = "förmiddag_ensam",
            onSuccess = {

                // Därefter eftermiddagspass.
                removeUserFromShiftType(
                    userId = userId,
                    bookedField = "eftermiddag_bokade",
                    aloneField = "eftermiddag_ensam",
                    onSuccess = onSuccess,
                    onError = onError
                )
            },
            onError = onError
        )
    }

    // ==========================================
    // UPPDATERA ARBETSPASS MED TRANSAKTIONER
    // ==========================================

    // Hämtar högst 100 arbetspass i taget.
    //
    // Varje arbetspass uppdateras med en egen
    // Firestore-transaktion. På så sätt läser vi
    // aktuella bokningar innan användaren tas bort.
    //
    // Om någon annan ändrar passet samtidigt kan
    // Firestore försöka köra transaktionen igen.
    private fun removeUserFromShiftType(
        userId: String,
        bookedField: String,
        aloneField: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("arbetspass")
            .whereArrayContains(bookedField, userId)
            .limit(batchSize)
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->

                if (snapshot.isEmpty) {
                    onSuccess()
                    return@addOnSuccessListener
                }

                // Uppdatera arbetspassen ett i taget.
                // När omgången är klar hämtas nästa.
                fun updateNextShift(index: Int) {

                    if (index >= snapshot.documents.size) {

                        // Kontrollera om fler arbetspass
                        // behöver uppdateras.
                        removeUserFromShiftType(
                            userId = userId,
                            bookedField = bookedField,
                            aloneField = aloneField,
                            onSuccess = onSuccess,
                            onError = onError
                        )
                        return
                    }

                    val shiftRef =
                        snapshot.documents[index].reference

                    db.runTransaction { transaction ->

                        // Läs arbetspasset inuti transaktionen.
                        val shift = transaction.get(shiftRef)

                        if (shift.exists()) {

                            val bookedUsers =
                                (shift.get(bookedField) as? List<*>)
                                    ?.filterIsInstance<String>()
                                    .orEmpty()

                            // Uppdatera bara om användaren
                            // fortfarande finns på passet.
                            if (userId in bookedUsers) {

                                val updates =
                                    mutableMapOf<String, Any>()

                                // Ta bort användarens ID
                                // utan att påverka andra bokade.
                                updates[bookedField] =
                                    FieldValue.arrayRemove(userId)

                                // Om användaren var ensam bokad
                                // ska ensamarbetsmarkeringen
                                // återställas.
                                if (bookedUsers.size == 1) {
                                    updates[aloneField] = false
                                }

                                transaction.update(
                                    shiftRef,
                                    updates
                                )
                            }
                        }

                        null
                    }.addOnSuccessListener {

                        // Fortsätt med nästa arbetspass
                        // när transaktionen har slutförts.
                        updateNextShift(index + 1)

                    }.addOnFailureListener { exception ->
                        onError(exception)
                    }
                }

                updateNextShift(0)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }

    // ==========================================
    // KONTROLLERA ATT DATA ÄR BORTTAGEN
    // ==========================================

    // Kontrollerar att inga dokument finns kvar
    // som hänvisar till användarens ID.
    private fun verifyDeletion(
        userId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val checks = listOf(
            db.collection("anteckningar")
                .whereEqualTo("författar_id", userId)
                .limit(1)
                .get(Source.SERVER),

            db.collection("evenemang")
                .whereEqualTo("skapad_av", userId)
                .limit(1)
                .get(Source.SERVER),

            db.collection("bokningar")
                .whereEqualTo("användar_id", userId)
                .limit(1)
                .get(Source.SERVER),

            db.collection("arbetspass")
                .whereArrayContains(
                    "förmiddag_bokade",
                    userId
                )
                .limit(1)
                .get(Source.SERVER),

            db.collection("arbetspass")
                .whereArrayContains(
                    "eftermiddag_bokade",
                    userId
                )
                .limit(1)
                .get(Source.SERVER)
        )

        Tasks.whenAllSuccess<QuerySnapshot>(checks)
            .addOnSuccessListener { results ->

                val remainingData = results.any {
                    !it.isEmpty
                }

                if (remainingData) {
                    onError(
                        IllegalStateException(
                            "Det finns fortfarande uppgifter kvar. " +
                                    "Försök igen för att fortsätta raderingen."
                        )
                    )
                } else {
                    onSuccess()
                }
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}



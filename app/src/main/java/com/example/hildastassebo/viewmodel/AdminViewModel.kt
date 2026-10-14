
package com.example.hildastassebo.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.hildastassebo.data.model.Volunteer
import com.example.hildastassebo.data.repository.VolunteerRepository

// =====================================
// ADMIN VIEWMODEL
// =====================================

// Hanterar administratörens behörighet, användarlistan och redigering av användare.
class AdminViewModel : ViewModel() {
    // =====================================
    // FIREBASE
    // =====================================

    // Firebase Authentication används för att identifiera den inloggade användaren.
    private val auth = FirebaseAuth.getInstance()

    // Firestore används för att kontrollera användarens administratörsroll.
    private val db = FirebaseFirestore.getInstance()

    // =====================================
    // ADMINBEHÖRIGHET
    // =====================================

    // Anger om den inloggade användaren är admin.
    // roll = 1 betyder administratör.
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    // =====================================
    // LADDNINGSSTATUS
    // =====================================

    // Används för att visa laddningsindikator medan administratörsrollen kontrolleras.
    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // =====================================
    // VOLONTÄRREPOSITORY
    // =====================================

    // Ansvarar för att hämta och uppdatera användaruppgifter i databasen.
    private val volunteerRepository = VolunteerRepository()

    // =====================================
    // ANVÄNDARLISTA
    // =====================================

    // Innehåller alla användare som hämtas från VolunteerRepository.
    private val _volunteers = MutableStateFlow<List<Volunteer>>(emptyList())
    val volunteers = _volunteers.asStateFlow()

    // =====================================
    // FELHANTERING
    // =====================================

    // Innehåller eventuellt felmeddelande om användarlistan inte kan hämtas.
    private val _volunteersError = MutableStateFlow<String?>(null)
    val volunteersError = _volunteersError.asStateFlow()

    // =====================================
    // INITIERING
    // =====================================

    // Kontrollerar automatiskt administratörsrollen när ViewModel skapas.
    init {
        checkAdminRole()
    }

    // =====================================
    // HÄMTA ANVÄNDARE
    // =====================================

    // Hämtar användarlistan från databasen.
    // Endast administratörer får anropa funktionen.
    fun loadVolunteers() {

        // Avbryt om användaren inte är admin.
        if (!_isAdmin.value) return

        volunteerRepository.getVolunteers(
            // Om hämtningen lyckas:
            onSuccess = { result ->
                _volunteers.value = result
                _volunteersError.value = null
            },
            // Om något går fel:
            onError = { exception ->
                _volunteersError.value =
                    exception.message ?: "Kunde inte hämta volontärer."
            }
        )
    }

    // =====================================
    // REDIGERA ANVÄNDARE
    // =====================================

    // Uppdaterar förnamn, efternamn och roll för en vald användare.
    fun updateUser(
        userId: String,
        firstName: String,
        lastName: String,
        role: Long,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {

        // =====================================
        // KONTROLLERA ADMINBEHÖRIGHET
        // =====================================

        // Endast administratörer får redigera användare.
        if (!_isAdmin.value) {
            onError(Exception("Du saknar administratörsbehörighet."))
            return
        }

        // =====================================
        // SKYDDA DEN EGNA ADMINROLLEN
        // =====================================

        // Förhindrar att en administratör
        // tar bort sin egen administratörsroll.
        if (userId == auth.currentUser?.uid &&
            role != 1L
        ) {
            onError(Exception("Du kan inte ta bort din egen administratörsroll."))
            return
        }

        // =====================================
        // SPARA ÄNDRINGAR I DATABASEN
        // =====================================

        // Skickar uppdateringen till VolunteerRepository.

        volunteerRepository.updateUser(
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            role = role,

            // Om uppdateringen lyckas:
            onSuccess = {

                // Hämtar användarlistan igen så att de nya uppgifterna visas.
                loadVolunteers()

                // Meddelar AdminScreen att uppdateringen lyckades.
                onSuccess()
            },

            // Skickar eventuellt fel tillbaka till AdminScreen.
            onError = onError
        )
    }

    // =====================================
    // KONTROLLERA ADMINISTRATÖRSROLL
    // =====================================

    // Hämtar användarens roll från Firestore.
    // Funktionen är privat eftersom den bara används internt i denna ViewModel.
    private fun checkAdminRole() {
        // Hämtar den inloggade användarens ID.
        val userId = auth.currentUser?.uid

        // =====================================
        // INGEN INLOGGAD ANVÄNDARE
        // =====================================

        // Om ingen är inloggad saknas adminbehörighet.
        if (userId == null) {
            _isAdmin.value = false
            _isLoading.value = false
            return
        }

        // =====================================
        // HÄMTA ANVÄNDARROLL FRÅN FIRESTORE
        // =====================================

        // Läser dokumentet för den inloggade användaren i samlingen "användare".
        db.collection("användare")
            .document(userId)
            .get()

            // =====================================
            // LYCKAD HÄMTNING
            // =====================================
            .addOnSuccessListener { document ->
                // Kontrollera om roll = 1 (admin).
                _isAdmin.value = document.getLong("roll") == 1L
                // Behörighetskontrollen är klar.
                _isLoading.value = false

                // Hämta användarlistan om användaren är administratör.
                if (_isAdmin.value) {
                    loadVolunteers()
                }
            }

            // =====================================
            // MISSLYCKAD HÄMTNING
            // =====================================
            .addOnFailureListener {
                // Om Firestore inte kan läsa rollen ges ingen administratörsbehörighet.
                _isAdmin.value = false
                _isLoading.value = false
            }
    }

}

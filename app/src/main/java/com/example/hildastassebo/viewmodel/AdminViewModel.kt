
package com.example.hildastassebo.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.hildastassebo.data.model.Volunteer
import com.example.hildastassebo.data.repository.VolunteerRepository


class AdminViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin = _isAdmin.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val volunteerRepository = VolunteerRepository()

    private val _volunteers = MutableStateFlow<List<Volunteer>>(emptyList())
    val volunteers = _volunteers.asStateFlow()

    private val _volunteersError = MutableStateFlow<String?>(null)
    val volunteersError = _volunteersError.asStateFlow()


    init {
        checkAdminRole()
    }

    fun loadVolunteers() {
        if (!_isAdmin.value) return

        volunteerRepository.getVolunteers(
            onSuccess = { result ->
                _volunteers.value = result
                _volunteersError.value = null
            },
            onError = { exception ->
                _volunteersError.value =
                    exception.message ?: "Kunde inte hämta volontärer."
            }
        )
    }
    fun updateUser(
        userId: String,
        firstName: String,
        lastName: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        if (!_isAdmin.value) return

        volunteerRepository.updateUser(
            userId = userId,
            firstName = firstName,
            lastName = lastName,
            onSuccess = {
                loadVolunteers()
                onSuccess()
            },
            onError = onError
        )
    }


    private fun checkAdminRole() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            _isAdmin.value = false
            _isLoading.value = false
            return
        }

        db.collection("användare")
            .document(userId)
            .get()

            .addOnSuccessListener { document ->
                _isAdmin.value = document.getLong("roll") == 1L
                _isLoading.value = false

                if (_isAdmin.value) {
                    loadVolunteers()
                }
            }

            .addOnFailureListener {
                _isAdmin.value = false
                _isLoading.value = false
            }
    }

}

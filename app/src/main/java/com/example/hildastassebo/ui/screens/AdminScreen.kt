package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.hildastassebo.viewmodel.AdminViewModel
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable

@Composable
fun AdminScreen(adminViewModel: AdminViewModel = viewModel()) {
        val isAdmin by adminViewModel.isAdmin.collectAsState()
        val isLoading by adminViewModel.isLoading.collectAsState()

        if (isLoading) {
            CircularProgressIndicator()
            return
        }

        if (!isAdmin) {
            Text("Du har inte behörighet att visa denna sida.")
            return
        }
    val volunteers by adminViewModel.volunteers.collectAsState()
    val volunteersError by adminViewModel.volunteersError.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf<String?>(null) }
    var editingUserId by remember { mutableStateOf<String?>(null) }
    var editedFirstName by remember { mutableStateOf("") }
    var editedLastName by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf<String?>(null) }
    val filteredVolunteers = volunteers.filter { volunteer ->
        volunteer.fullName.contains(searchQuery, ignoreCase = true) ||
                volunteer.email.contains(searchQuery, ignoreCase = true)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Administration",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Hantera volontärer",
            style = MaterialTheme.typography.titleLarge
        )

        Button(onClick = { }) {
            Text("Lägg till volontär")
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Sök användare") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text
            )
        )
        if (volunteersError != null) {
            Text(
                text = volunteersError ?: "",
                color = MaterialTheme.colorScheme.error
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredVolunteers, key = { it.id }) { volunteer ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedUserId =
                                if (selectedUserId == volunteer.id) null
                                else volunteer.id
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = volunteer.fullName,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = volunteer.email,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = if (volunteer.role == 1L) "Admin" else "Volontär",
                            style = MaterialTheme.typography.bodySmall
                        )

                        if (selectedUserId == volunteer.id) {
                            HorizontalDivider()

                            Text(
                                text = "Användaruppgifter",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text("Förnamn: ${volunteer.firstName}")
                            Text("Efternamn: ${volunteer.lastName}")
                            Text("E-post: ${volunteer.email}")

                            if (editingUserId == volunteer.id) {
                                OutlinedTextField(
                                    value = editedFirstName,
                                    onValueChange = { editedFirstName = it },
                                    label = { Text("Förnamn") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = editedLastName,
                                    onValueChange = { editedLastName = it },
                                    label = { Text("Efternamn") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (editError != null) {
                                    Text(
                                        text = editError ?: "",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            if (editedFirstName.isBlank() || editedLastName.isBlank()) {
                                                editError = "Fyll i både förnamn och efternamn."
                                            } else {
                                                adminViewModel.updateUser(
                                                    userId = volunteer.id,
                                                    firstName = editedFirstName.trim(),
                                                    lastName = editedLastName.trim(),
                                                    onSuccess = {
                                                        editingUserId = null
                                                        editError = null
                                                    },
                                                    onError = {
                                                        editError = it.message ?: "Kunde inte spara."
                                                    }
                                                )
                                            }
                                        }
                                    ) {
                                        Text("Spara")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            editingUserId = null
                                            editError = null
                                        }
                                    ) {
                                        Text("Avbryt")
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        editingUserId = volunteer.id
                                        editedFirstName = volunteer.firstName
                                        editedLastName = volunteer.lastName
                                        editError = null
                                    }
                                ) {
                                    Text("Redigera användare")
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = "Statistik över arbetade pass",
            style = MaterialTheme.typography.titleLarge
        )
    }
}


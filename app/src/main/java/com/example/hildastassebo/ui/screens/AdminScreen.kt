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
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.ui.text.style.TextAlign


private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Background = Color(0xFFFAFAF7)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)
@Composable
    fun AdminScreen(
        onLogout: () -> Unit,
        onSettingsClick: () -> Unit,
        onStatisticsClick: () -> Unit,
        adminViewModel: AdminViewModel = viewModel()
    ){
        var showLogoutDialog by remember {
            mutableStateOf(false)
        }
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
    var showAddVolunteerDialog by remember { mutableStateOf(false) }
    var newVolunteerFirstName by remember { mutableStateOf("") }
    var newVolunteerLastName by remember { mutableStateOf("") }
    var newVolunteerEmail by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf<String?>(null) }

    var editingUserId by remember { mutableStateOf<String?>(null) }
    var editedFirstName by remember { mutableStateOf("") }
    var editedLastName by remember { mutableStateOf("") }
    var editedRole by remember { mutableStateOf(0L) }

    var editError by remember { mutableStateOf<String?>(null) }
    val currentUser = FirebaseAuth.getInstance().currentUser

    val adminName = currentUser?.displayName
        ?.takeIf { it.isNotBlank() }
        ?: "Administratör"

    val adminEmail = currentUser?.email ?: ""
    val filteredVolunteers = volunteers.filter { volunteer ->
        volunteer.fullName.contains(searchQuery, ignoreCase = true) ||
                volunteer.email.contains(searchQuery, ignoreCase = true)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    )  {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(PaleSage, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PersonOutline,
                contentDescription = null,
                tint = DarkSage,
                modifier = Modifier.size(42.dp)
            )
        }

        Text(
            text = "Min adminsida",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Text(
            text = adminName,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )

        Text(
            text = adminEmail,
            fontSize = 14.sp,
            color = Muted
        )
// INSTÄLLNINGAR OCH LOGGA UT



        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Sök användare") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkSage,
                unfocusedBorderColor = FieldBorder,
                focusedLabelColor = DarkSage
            ),
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
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = FieldBorder
                    )
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
                                Text(
                                    text = "Användarroll",
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = editedRole == 0L,
                                        onClick = { editedRole = 0L }
                                    )

                                    Text(
                                        text = "Volontär",
                                        modifier = Modifier.clickable {
                                            editedRole = 0L
                                        }
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = editedRole == 1L,
                                        onClick = { editedRole = 1L }
                                    )

                                    Text(
                                        text = "Administratör",
                                        modifier = Modifier.clickable {
                                            editedRole = 1L
                                        }
                                    )
                                }

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
                                                    role = editedRole,
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
                                        editedRole = volunteer.role
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
        OutlinedButton(
            onClick = onStatisticsClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Visa statistik över arbetspass",
                color = DarkSage
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, FieldBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // INSTÄLLNINGAR
                TextButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        tint = DarkSage
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = "Inställningar",
                        color = Ink,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        fontSize = 16.sp
                    )
                }

                HorizontalDivider(color = FieldBorder)

                // LOGGA UT
                TextButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Logout,
                        contentDescription = null,
                        tint = Color(0xFFB45353)
                    )

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = "Logga ut",
                        color = Color(0xFFB45353),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Start,
                        fontSize = 16.sp
                    )
                }
            }
        }    // BEKRÄFTA UTLOGGNING
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = {
                    showLogoutDialog = false
                },
                title = {
                    Text("Logga ut")
                },
                text = {
                    Text("Är du säker på att du vill logga ut?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        }
                    ) {
                        Text(
                            text = "Logga ut",
                            color = Color(0xFFB45353)
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                        }
                    ) {
                        Text("Avbryt")
                    }
                }
            )
        }

    }
    if (showAddVolunteerDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddVolunteerDialog = false
            },
            title = {
                Text("Bjud in volontär")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = newVolunteerFirstName,
                        onValueChange = { newVolunteerFirstName = it },
                        label = { Text("Förnamn") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newVolunteerLastName,
                        onValueChange = { newVolunteerLastName = it },
                        label = { Text("Efternamn") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newVolunteerEmail,
                        onValueChange = { newVolunteerEmail = it },
                        label = { Text("E-postadress") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Vi kopplar inbjudningsfunktionen till Firebase senare.
                        showAddVolunteerDialog = false
                    }
                ) {
                    Text("Skicka inbjudan", color = DarkSage)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddVolunteerDialog = false
                    }
                ) {
                    Text("Avbryt")
                }
            }
        )
    }
}


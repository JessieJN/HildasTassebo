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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.hildastassebo.data.repository.ShiftRepository
import com.example.hildastassebo.data.repository.SavedShiftBooking
import androidx.compose.runtime.DisposableEffect
import java.time.LocalDate
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp

// =====================================
// FÄRGER OCH DESIGN
// =====================================
private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Background = Color(0xFFFAFAF7)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)

// =====================================
// ADMINISTRATÖRSSKÄRM
// =====================================
@Composable
    fun AdminScreen(
        onLogout: () -> Unit,
        onSettingsClick: () -> Unit,
        adminViewModel: AdminViewModel = viewModel()
    ){
    // =====================================
    // BEHÖRIGHET OCH INLÄSNING
    // =====================================
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
    // =====================================
    // ANVÄNDARDATA OCH TILLSTÅND
    // =====================================
    val volunteers by adminViewModel.volunteers.collectAsState()
    val volunteersError by adminViewModel.volunteersError.collectAsState()
    var showAddVolunteerDialog by remember { mutableStateOf(false) }
    var newVolunteerFirstName by remember { mutableStateOf("") }
    var newVolunteerLastName by remember { mutableStateOf("") }
    var newVolunteerEmail by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf<String?>(null) }

    // =====================================
    // REDIGERING OCH VAL AV ANVÄNDARE
    // =====================================
    var editingUserId by remember { mutableStateOf<String?>(null) }
    var selectedVolunteerId by remember { mutableStateOf<String?>(null) }
    var volunteerDropdownExpanded by remember { mutableStateOf(false) }
    var volunteerSearchQuery by remember {
        mutableStateOf("")
    }
    var editedFirstName by remember { mutableStateOf("") }
    var editedLastName by remember { mutableStateOf("") }
    var editedRole by remember { mutableStateOf(0L) }
    var editError by remember { mutableStateOf<String?>(null) }

    // =====================================
    // INLOGGAD ADMINISTRATÖR
    // =====================================
    val currentUser = FirebaseAuth.getInstance().currentUser

    // =====================================
    // BOKNINGAR FRÅN FIRESTORE
    // =====================================

    // Repository för arbetspass och bokningar
        val shiftRepository = remember { ShiftRepository() }

    // Lista över alla bokningar
        var bookings by remember {
            mutableStateOf<List<SavedShiftBooking>>(emptyList())
        }

    // Lyssnar på bokningar i realtid
        DisposableEffect(shiftRepository) {
            val listener = shiftRepository.observeBookings(
                onUpdate = { bookings = it },
                onError = { exception ->
                    android.util.Log.e(
                        "AdminScreen",
                        "Kunde inte hämta bokningar",
                        exception
                    )
                }
            )

            onDispose {
                listener.remove()
            }
        }
    // =====================================
    // STATISTIK FÖR BOKADE ARBETSPASS
    // =====================================

    // Inloggad administratör visas som standard
        var statisticsUserId by remember {
            mutableStateOf(currentUser?.uid)
        }

    // Namnet i statistikens dropdown
        var statisticsUserName by remember {
            mutableStateOf("Mina arbetspass")
        }

    // Styr dropdown-menyn
        var statisticsMenuExpanded by remember {
            mutableStateOf(false)
        }

    // Sökning efter volontärer
        var statisticsSearchQuery by remember {
            mutableStateOf("")
        }

    // Alla bokningar för vald användare
        val selectedBookings = bookings.filter {
            it.userId == statisticsUserId
        }

    // Dagens datum enligt svensk tid
        val today = LocalDate.now(ZoneId.of("Europe/Stockholm"))

    // Aktuell vecka: måndag till söndag
        val startOfWeek = today.with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        )

        val endOfWeek = startOfWeek.plusDays(6)

        val weeklyCount = selectedBookings.count {
            !it.date.isBefore(startOfWeek) &&
                    !it.date.isAfter(endOfWeek)
        }

    // Aktuell kalendermånad
        val monthlyCount = selectedBookings.count {
            it.date.year == today.year &&
                    it.date.month == today.month
        }

    // Aktuellt kalenderår
        val yearlyCount = selectedBookings.count {
            it.date.year == today.year
        }

    val adminName = currentUser?.displayName
        ?.takeIf { it.isNotBlank() }
        ?: "Administratör"

    val adminEmail = currentUser?.email ?: ""

    // =====================================
    // SIDANS LAYOUT OCH PROFILHUVUD
    // =====================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
        // =====================================
        // FELMEDDELANDEN VID HÄMTNING
        // =====================================
        if (volunteersError != null) {
            Text(
                text = volunteersError ?: "",
                color = MaterialTheme.colorScheme.error
            )
        }
        // =====================================
        // STATISTIK ÖVER BOKADE ARBETSPASS
        // =====================================
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, FieldBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Bokade arbetspass",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )

                        Spacer(Modifier.height(16.dp))

                        // Välj vems statistik som ska visas
                        Box {
                            OutlinedButton(
                                onClick = { statisticsMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(statisticsUserName)
                            }

                            DropdownMenu(
                                expanded = statisticsMenuExpanded,
                                onDismissRequest = {
                                    statisticsMenuExpanded = false
                                    statisticsSearchQuery = ""
                                },
                                modifier = Modifier.heightIn(max = 350.dp)
                            ) {
                                OutlinedTextField(
                                    value = statisticsSearchQuery,
                                    onValueChange = { statisticsSearchQuery = it },
                                    label = { Text("Sök användare") },
                                    placeholder = { Text("Namn eller e-post") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(280.dp)
                                        .padding(
                                            horizontal = 8.dp,
                                            vertical = 4.dp
                                        )
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("Mina arbetspass") },
                                    onClick = {
                                        statisticsUserId = currentUser?.uid
                                        statisticsUserName = "Mina arbetspass"
                                        statisticsMenuExpanded = false
                                        statisticsSearchQuery = ""
                                    }
                                )

                                volunteers
                                    .filter { volunteer ->
                                        volunteer.id != currentUser?.uid &&
                                                (
                                                        volunteer.fullName.contains(
                                                            statisticsSearchQuery,
                                                            ignoreCase = true
                                                        ) ||
                                                                volunteer.email.contains(
                                                                    statisticsSearchQuery,
                                                                    ignoreCase = true
                                                                )
                                                        )
                                    }
                                    .sortedBy { it.fullName.lowercase() }
                                    .forEach { volunteer ->
                                        DropdownMenuItem(
                                            text = { Text(volunteer.fullName) },
                                            onClick = {
                                                statisticsUserId = volunteer.id
                                                statisticsUserName = volunteer.fullName
                                                statisticsMenuExpanded = false
                                                statisticsSearchQuery = ""
                                            }
                                        )
                                    }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        // Statistik för aktuell vecka, månad och år
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "Denna vecka" to weeklyCount,
                                "Denna månad" to monthlyCount,
                                "Detta år" to yearlyCount
                            ).forEach { (label, count) ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            PaleSage,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .padding(
                                            horizontal = 4.dp,
                                            vertical = 16.dp
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = count.toString(),
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkSage
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        color = DarkSage,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
        // =====================================
        // VÄLJ ANVÄNDARE – DROPDOWN OCH SÖKNING
        // =====================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = PaleSage
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Välj användare",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Text(
                    text = "Sök efter namn eller e-post för att visa och redigera en användare.",
                    fontSize = 14.sp,
                    color = Muted
                )

                Box {
                    OutlinedButton(
                        onClick = { volunteerDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = volunteers
                                .find { it.id == selectedVolunteerId }
                                ?.fullName ?: "Välj användare"
                        )
                    }

                    DropdownMenu(
                        expanded = volunteerDropdownExpanded,
                        onDismissRequest = {
                            volunteerDropdownExpanded = false
                        },
                        modifier = Modifier.heightIn(max = 350.dp)
                    ) {
                        OutlinedTextField(
                            value = volunteerSearchQuery,
                            onValueChange = { volunteerSearchQuery = it },
                            label = { Text("Sök användare") },
                            placeholder = { Text("Namn eller e-post") },
                            singleLine = true,
                            modifier = Modifier
                                .width(280.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        HorizontalDivider()

                        volunteers
                            .filter { volunteer ->
                                volunteer.fullName.contains(
                                    volunteerSearchQuery,
                                    ignoreCase = true
                                ) ||
                                        volunteer.email.contains(
                                            volunteerSearchQuery,
                                            ignoreCase = true
                                        )
                            }
                            .sortedBy { it.fullName.lowercase() }
                            .forEach { volunteer ->
                                DropdownMenuItem(
                                    text = { Text(volunteer.fullName) },
                                    onClick = {
                                        selectedVolunteerId = volunteer.id
                                        volunteerDropdownExpanded = false
                                        volunteerSearchQuery = ""
                                    }
                                )
                            }
                    }
                }
            }
        }
        // =====================================
        // VALD ANVÄNDARE – UPPGIFTER OCH REDIGERINGSKNAPP
        // =====================================
        LazyColumn(
            modifier = Modifier.heightIn(max = 350.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                volunteers.filter { it.id == selectedVolunteerId },
                key = { it.id }
            ) { volunteer ->

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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
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
                            }

                            Icon(
                                imageVector = if (selectedUserId == volunteer.id) {
                                    Icons.Filled.KeyboardArrowUp
                                } else {
                                    Icons.Filled.KeyboardArrowDown
                                },
                                contentDescription = if (selectedUserId == volunteer.id) {
                                    "Dölj användaruppgifter"
                                } else {
                                    "Visa användaruppgifter"
                                },
                                tint = DarkSage
                            )
                        }

                        if (selectedUserId == volunteer.id) {
                            HorizontalDivider()

                            Text(
                                text = "Användaruppgifter",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text("Förnamn: ${volunteer.firstName}")
                            Text("Efternamn: ${volunteer.lastName}")
                            Text("E-post: ${volunteer.email}")

                            Button(
                                onClick = {
                                    editingUserId = volunteer.id
                                    editedFirstName = volunteer.firstName
                                    editedLastName = volunteer.lastName
                                    editedRole = volunteer.role
                                    editError = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkSage,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Redigera användare")
                            }
                        }
                    }
                }
            }
        }

        // =====================================
        // INSTÄLLNINGAR OCH LOGGA UT
        // =====================================
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
        }
        // =====================================
        // BEKRÄFTA UTLOGGNING
        // =====================================
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

    // =====================================
    // POPUP – REDIGERA ANVÄNDARE
    // =====================================
    if (editingUserId != null) {
        AlertDialog(
            onDismissRequest = {
                editingUserId = null
                editError = null
            },
            title = {
                Text("Redigera användare")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editedFirstName,
                        onValueChange = { editedFirstName = it },
                        label = { Text("Förnamn") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkSage,
                            focusedLabelColor = DarkSage,
                            cursorColor = DarkSage,
                            unfocusedBorderColor = FieldBorder
                        )
                    )

                    OutlinedTextField(
                        value = editedLastName,
                        onValueChange = { editedLastName = it },
                        label = { Text("Efternamn") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkSage,
                            focusedLabelColor = DarkSage,
                            cursorColor = DarkSage,
                            unfocusedBorderColor = FieldBorder
                        )
                    )

                    Text(
                        text = "Användarroll",
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = editedRole == 0L,
                            onClick = { editedRole = 0L },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = DarkSage,
                                unselectedColor = Muted
                            )
                        )
                        Text("Volontär")
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = editedRole == 1L,
                            onClick = { editedRole = 1L },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = DarkSage,
                                unselectedColor = Muted
                            )
                        )
                        Text("Administratör")
                    }

                    if (editError != null) {
                        Text(
                            text = editError ?: "",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val userId = editingUserId ?: return@TextButton

                        if (editedFirstName.isBlank() || editedLastName.isBlank()) {
                            editError = "Fyll i både förnamn och efternamn."
                        } else {
                            adminViewModel.updateUser(
                                userId = userId,
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
                    Text("Spara", color = DarkSage)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingUserId = null
                        editError = null
                    }
                ) {
                    Text("Avbryt")
                }
            }
        )
    }

    // =====================================
    // POPUP – BJUD IN VOLONTÄR (ÄNNU EJ KOPPLAD)- Behåll eller inte???
    // =====================================
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


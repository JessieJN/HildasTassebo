package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.hildastassebo.viewmodel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth
import com.example.hildastassebo.data.repository.ShiftRepository
import com.example.hildastassebo.data.repository.SavedShiftBooking
import java.time.LocalDate
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

// =====================================
// FÄRGER
// =====================================
private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Background = Color(0xFFFAFAF7)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)

// =====================================
// STATISTIKSKÄRM
// =====================================
@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    // =====================================
    // VIEWMODEL OCH ANVÄNDARDATA
    // =====================================

    // Hämtar användarlistan från AdminViewModel
    val adminViewModel: AdminViewModel = viewModel()
    val volunteers by adminViewModel.volunteers.collectAsState()
    // Hämtar den inloggade användarens ID
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    // Hämtar bokningar från Firestore i realtid.
    val shiftRepository = remember { ShiftRepository() }

    var bookings by remember {
        mutableStateOf<List<SavedShiftBooking>>(emptyList())
    }

    DisposableEffect(shiftRepository) {
        val listener = shiftRepository.observeBookings(
            onUpdate = { bookings = it },
            onError = { exception ->
                android.util.Log.e(
                    "StatisticsScreen",
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
    // TILLSTÅND FÖR ANVÄNDARVAL
    // =====================================

    // Den valda användarens ID
    // Standard är den inloggade användaren
    var selectedVolunteerId by remember {
        mutableStateOf(currentUserId)
    }


    // Namnet som visas i dropdown-knappen
    var selectedVolunteer by remember { mutableStateOf("Mina arbetspass") }

    // Styr om dropdown-menyn är öppen
    var volunteerMenuExpanded by remember { mutableStateOf(false) }

    // Texten som används för att söka användare
    var volunteerSearchQuery by remember {
        mutableStateOf("")
    }

    // Räknar tidigare bokade arbetspass för vald användare.
    val today = LocalDate.now(ZoneId.of("Europe/Stockholm"))

    val selectedBookings = bookings.filter { booking ->
        booking.userId == selectedVolunteerId
    }
// Aktuell kalendervecka: måndag till söndag
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

    // =====================================
    // SIDANS HUVUDLAYOUT
    // =====================================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    )
    // =====================================
    // TILLBAKA-KNAPP
    // =====================================
    {
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Tillbaka",
                tint = DarkSage
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Tillbaka till min profil",
                color = DarkSage
            )
        }

        Spacer(Modifier.height(16.dp))

        // =====================================
        // STATISTIKIKON
        // =====================================
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(PaleSage, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.BarChart,
                contentDescription = null,
                tint = DarkSage,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // =====================================
        // RUBRIK OCH BESKRIVNING
        // =====================================
        Text(
            text = "Statistik",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Översikt över bokade arbetspass",
            fontSize = 14.sp,
            color = Muted
        )
        Spacer(Modifier.height(30.dp))

        // =====================================
        // KORT FÖR GENOMFÖRDA ARBETSPASS
        // =====================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(1.dp, Color(0xFFD7E2DC))
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                // =====================================
                // RUBRIK FÖR ARBETSPASS
                // =====================================
                Text(
                    text = "Bokade arbetspass",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(Modifier.height(16.dp))
                // =====================================
                // VÄLJ ANVÄNDARE
                // =====================================

                // Dropdown för att välja vems arbetspass
                // som ska visas i statistiken
                Box {
                    OutlinedButton(
                        onClick = { volunteerMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedVolunteer)
                    }

                    // =====================================
                    // DROPDOWN-MENY
                    // =====================================
                    DropdownMenu(
                        expanded = volunteerMenuExpanded,
                        onDismissRequest = {
                            volunteerMenuExpanded = false
                            volunteerSearchQuery = ""
                        },
                        modifier = Modifier.heightIn(max = 350.dp)
                    ) {

                        // =====================================
                        // SÖKFÄLT FÖR ANVÄNDARE
                        // =====================================
                        OutlinedTextField(
                            value = volunteerSearchQuery,
                            onValueChange = { volunteerSearchQuery = it },
                            label = { Text("Sök användare") },
                            placeholder = { Text("Namn eller e-post") },
                            singleLine = true,
                            modifier = Modifier
                                .width(280.dp)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp)
                        )

                        HorizontalDivider()

                        // =====================================
                        // MINA ARBETSPASS
                        // =====================================

                        // Återgår till den inloggade
                        // användarens egen statistik
                        DropdownMenuItem(
                            text = { Text("Mina arbetspass") },
                            onClick = {
                                selectedVolunteer = "Mina arbetspass"
                                selectedVolunteerId = currentUserId
                                volunteerMenuExpanded = false
                                volunteerSearchQuery = ""
                            }
                        )

                        // =====================================
                        // LISTA ÖVER ÖVRIGA ANVÄNDARE
                        // =====================================

                        // Filtrerar användare efter namn eller e-post
                        // och sorterar dem i alfabetisk ordning
                        volunteers
                            .filter { volunteer ->
                                volunteer.id != currentUserId &&
                                        (
                                                volunteer.fullName.contains(
                                                    volunteerSearchQuery,
                                                    ignoreCase = true
                                                ) ||
                                                        volunteer.email.contains(
                                                            volunteerSearchQuery,
                                                            ignoreCase = true
                                                        )
                                                )
                            }
                            .sortedBy { it.fullName.lowercase() }
                            .forEach { volunteer ->
                                DropdownMenuItem(
                                    text = { Text(volunteer.fullName) },
                                    onClick = {
                                        selectedVolunteer = volunteer.fullName
                                        selectedVolunteerId = volunteer.id
                                        volunteerMenuExpanded = false
                                    }
                                )
                            }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // =====================================
                // STATISTIK FÖR OLIKA TIDSPERIODER
                // =====================================

                // Visar tre statistikrutor:
                // veckan, månaden och året.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Denna vecka" to weeklyCount,
                        "Denna månad" to monthlyCount,
                        "Detta år" to yearlyCount
                    ).forEach { (label, count) ->
                        // =====================================
                        // ENSKILD STATISTIKRUTA
                        // =====================================
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

                            // Antal arbetspass
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
    }
}
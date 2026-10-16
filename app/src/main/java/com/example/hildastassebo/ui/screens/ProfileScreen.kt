
package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Settings

import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.google.firebase.auth.FirebaseAuth

import com.example.hildastassebo.data.repository.ShiftRepository
import com.example.hildastassebo.data.repository.SavedShiftBooking
import java.time.LocalDate
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Background = Color(0xFFFAFAF7)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onSettingsClick: () -> Unit
) {

    // Hämta inloggad användare från Firebase
    val user = FirebaseAuth.getInstance().currentUser

    val userName = user?.displayName
        ?.takeIf { it.isNotBlank() }
        ?: "Volontär"

    val email = user?.email ?: ""

    val currentUserId = user?.uid.orEmpty()
    val shiftRepository = remember { ShiftRepository() }

    var myBookings by remember(currentUserId) {
        mutableStateOf(emptyList<SavedShiftBooking>())
    }

    DisposableEffect(currentUserId) {
        if (currentUserId.isBlank()) {
            myBookings = emptyList()
            onDispose { }
        } else {
            val listener = shiftRepository.observeBookings(
                onUpdate = { allBookings ->
                    myBookings = allBookings.filter {
                        it.userId == currentUserId
                    }
                },
                onError = { exception ->
                    android.util.Log.e(
                        "ProfileScreen",
                        "Kunde inte hämta arbetspass",
                        exception
                    )
                }
            )

            onDispose {
                listener.remove()
            }
        }
    }

    val today = LocalDate.now(ZoneId.of("Europe/Stockholm"))

// Aktuell kalendervecka: måndag till söndag
    val startOfWeek = today.with(
        TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
    )

    val endOfWeek = startOfWeek.plusDays(6)

    val shiftsThisWeek = myBookings.count {
        !it.date.isBefore(startOfWeek) &&
                !it.date.isAfter(endOfWeek)
    }

// Aktuell kalendermånad
    val shiftsThisMonth = myBookings.count {
        it.date.year == today.year &&
                it.date.month == today.month
    }

// Aktuellt kalenderår
    val shiftsThisYear = myBookings.count {
        it.date.year == today.year
    }

    // Dialog för att bekräfta utloggning
    var showLogoutDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = 24.dp,
                vertical = 28.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // =====================================
        // PROFILIKON
        // =====================================

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

        Spacer(Modifier.height(16.dp))

        // =====================================
        // RUBRIK
        // =====================================

        Text(
            text = "Min sida",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Spacer(Modifier.height(16.dp))

        // =====================================
        // ANVÄNDARINFORMATION
        // =====================================

        Text(
            text = userName,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )

        if (email.isNotBlank()) {

            Spacer(Modifier.height(4.dp))

            Text(
                text = email,
                fontSize = 14.sp,
                color = Muted,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(30.dp))

        // =====================================
        // STATISTIK ÖVER ARBETSPASS
        // =====================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(
                1.dp,
                FieldBorder
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Mina arbetspass",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Översikt över ditt volontärarbete",
                    fontSize = 13.sp,
                    color = Muted
                )

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    ShiftStatCard(
                        label = "Denna vecka",
                        count = shiftsThisWeek,
                        modifier = Modifier.weight(1f)
                    )

                    ShiftStatCard(
                        label = "Denna månad",
                        count = shiftsThisMonth,
                        modifier = Modifier.weight(1f)
                    )

                    ShiftStatCard(
                        label = "Detta år",
                        count = shiftsThisYear,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // =====================================
        // INSTÄLLNINGAR OCH LOGGA UT
        // =====================================

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(
                1.dp,
                FieldBorder
            )
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

                HorizontalDivider(
                    color = FieldBorder
                )

                // LOGGA UT
                TextButton(
                    onClick = {
                        showLogoutDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Logout,
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

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Hildas Tassebo",
            fontSize = 12.sp,
            color = Muted
        )
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
                Text(
                    "Är du säker på att du vill logga ut?"
                )
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
// STATISTIKKORT
// =====================================

@Composable
private fun ShiftStatCard(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
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
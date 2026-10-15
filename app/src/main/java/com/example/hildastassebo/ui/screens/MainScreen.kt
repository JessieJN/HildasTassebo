
package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

import com.example.hildastassebo.data.model.CalendarEvent
import com.example.hildastassebo.data.repository.EventRepository
import com.example.hildastassebo.ui.navigation.BottomNavigationBar
import com.example.hildastassebo.ui.navigation.SettingsScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

@Composable
fun MainScreen(
    onLogout: () -> Unit
) {

    // Håller reda på vilken flik som är vald.
    var selectedTab by rememberSaveable {
        mutableStateOf("home")
    }

    // Gemensam lista för evenemang i kalendern och på startsidan.
    val events = remember {
        mutableStateMapOf<LocalDate, List<CalendarEvent>>()
    }

    // Hämtar evenemang från Firebase oavsett vilken flik som visas.
    val eventRepository = remember { EventRepository() }

    DisposableEffect(eventRepository) {
        val listener = eventRepository.observeEvents(
            onUpdate = { eventsByDate ->
                events.clear()
                events.putAll(eventsByDate)
            },
            onError = { exception ->
                android.util.Log.e(
                    "MainScreen",
                    "Kunde inte hämta evenemang",
                    exception
                )
            }
        )

        onDispose {
            listener.remove()
        }
    }

    // Håller reda på om inställningssidan ska visas.
    var showSettings by rememberSaveable {
        mutableStateOf(false)
    }

    var showStatistics by remember {
        mutableStateOf(false)
    }

    // Hämtar inloggad användares roll från Firebase.
    var userRole by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId == null) {
            userRole = null
            onDispose { }
        } else {
            val listener = FirebaseFirestore.getInstance()
                .collection("användare")
                .document(userId)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        android.util.Log.e(
                            "HildasFirestore",
                            "Kunde inte hämta användarroll",
                            error
                        )
                        userRole = null
                    } else {
                        userRole = document?.getLong("roll")

                        android.util.Log.d(
                            "HildasFirestore",
                            "Hämtad roll: $userRole, dokument finns: ${document?.exists()}"
                        )
                    }
                }

            onDispose {
                listener.remove()
            }
        }
    }

    Scaffold(
        containerColor = Color.White,

        // Navigationsbaren längst ner.
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,

                onHomeClick = {
                    selectedTab = "home"
                    showSettings = false
                    showStatistics = false
                },

                onCalendarClick = {
                    selectedTab = "calendar"
                    showSettings = false
                    showStatistics = false
                },

                onProfileClick = {
                    selectedTab = "profile"
                    showSettings = false
                    showStatistics = false
                },

                onAboutClick = {
                    selectedTab = "about"
                    showSettings = false
                    showStatistics = false
                }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            // Visa inställningar eller vald flik.
            if (showSettings) {

                SettingsScreen(
                    onBack = {
                        showSettings = false
                    },
                    onAccountDeleted = {
                        showSettings = false
                        onLogout()
                    }
                )

            } else if (showStatistics && userRole == 1L) {

                StatisticsScreen(
                    onBack = {
                        showStatistics = false
                        selectedTab = "profile"
                    }
                )

            } else {

                when (selectedTab) {

                    // HEM
                    "home" -> HomeScreen(
                        userName = FirebaseAuth.getInstance()
                            .currentUser
                            ?.displayName
                            ?.substringBefore(" ")
                            ?: "Volontär",
                        events = events
                    )

                    // KALENDER
                    "calendar" -> CalendarScreen(
                        events = events
                    )

                    // MIN SIDA – olika beroende på användarens roll.
                    "profile" -> {
                        when (userRole) {
                            1L -> AdminScreen(
                                onLogout = onLogout,
                                onSettingsClick = {
                                    showSettings = true
                                },
                                onStatisticsClick = {
                                    showStatistics = true
                                }
                            )

                            0L -> ProfileScreen(
                                onLogout = onLogout,
                                onSettingsClick = {
                                    showSettings = true
                                }
                            )

                            else -> CircularProgressIndicator()
                        }
                    }

                    // OM OSS
                    "about" -> AboutScreen()
                }
            }
        }
    }
}

// ==========================================
// FÖRHANDSVISNING
// ==========================================

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun MainScreenPreview() {

    MainScreen(
        onLogout = {}
    )
}


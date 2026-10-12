
package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

import com.example.hildastassebo.ui.navigation.BottomNavigationBar
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate

@Composable
fun MainScreen(
    onLogout: () -> Unit
) {

    // Håller reda på vilken flik som är vald
    var selectedTab by remember {
        mutableStateOf("home")
    }

    // Gemensam lista för evenemang i kalendern och på startsidan
    val events = remember {
        mutableStateMapOf<LocalDate, List<CalendarEvent>>()
    }

    // Håller reda på om inställningssidan ska visas
    var showSettings by remember {
        mutableStateOf(false)
    }

    Scaffold(
        containerColor = Color.White,

        // Navigationsbaren längst ner
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,

                onHomeClick = {
                    selectedTab = "home"
                    showSettings = false
                },

                onCalendarClick = {
                    selectedTab = "calendar"
                    showSettings = false
                },

                onProfileClick = {
                    selectedTab = "profile"
                    showSettings = false
                },

                onAboutClick = {
                    selectedTab = "about"
                    showSettings = false
                }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            // Visa inställningar eller vald flik
            if (showSettings) {

                SettingsScreen(
                    onBack = {
                        showSettings = false
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

                    // MIN SIDA
                    "profile" -> ProfileScreen(
                        onLogout = onLogout,
                        onSettingsClick = {
                            showSettings = true
                        }
                    )

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

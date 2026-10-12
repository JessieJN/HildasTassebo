
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

@Composable
fun MainScreen(
    onLogout: () -> Unit
) {

    // Håller reda på vilken flik som är vald
    var selectedTab by remember {
        mutableStateOf("home")
    }

    Scaffold(
        containerColor = Color.White,

        // Navigationsbaren längst ner
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,

                onHomeClick = {
                    selectedTab = "home"
                },

                onCalendarClick = {
                    selectedTab = "calendar"
                },

                onProfileClick = {
                    selectedTab = "profile"
                },

                onAboutClick = {
                    selectedTab = "about"
                }
            )
        }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {

            // Visar rätt sida beroende på vald flik
            when (selectedTab) {

                // HEM
                "home" -> HomeScreen(
                    userName = FirebaseAuth.getInstance()
                        .currentUser
                        ?.displayName
                        ?.substringBefore(" ")
                        ?: "Volontär"
                )

                // KALENDER
                "calendar" -> CalendarScreen()

                // MIN SIDA
                "profile" -> ProfileScreen(
                    onLogout = onLogout
                )

                // OM OSS
                "about" -> AboutScreen()
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

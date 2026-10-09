package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.hildastassebo.ui.navigation.BottomNavigationBar

@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf("home") }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onHomeClick = { selectedTab = "home" },
                onCalendarClick = { selectedTab = "calendar" },
                onProfileClick = { selectedTab = "profile" }
            )
        }
    ) { innerPadding ->

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                "home" -> HomeScreen(userName = "Jessie")

                "calendar" -> {
                    // Här lägger vi CalendarScreen senare
                }

                "profile" -> {
                    // Här lägger vi ProfileScreen senare
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    MainScreen()
}
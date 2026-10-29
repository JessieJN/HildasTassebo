
package com.example.hildastassebo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Sage = Color(0xFF91B1A3)
private val DarkSage = Color(0xFF47796C)

@Composable
fun BottomNavigationBar(
    selectedTab: String,
    onHomeClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSupportClick: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White
    ) {

        // HEM
        NavigationBarItem(
            selected = selectedTab == "home",
            onClick = onHomeClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = "Hem"
                )
            },
            label = { Text("Hem") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkSage,
                selectedTextColor = DarkSage,
                indicatorColor = Sage.copy(alpha = 0.2f)
            )
        )

        // KALENDER
        NavigationBarItem(
            selected = selectedTab == "calendar",
            onClick = onCalendarClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = "Kalender"
                )
            },
            label = { Text("Kalender") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkSage,
                selectedTextColor = DarkSage,
                indicatorColor = Sage.copy(alpha = 0.2f)
            )
        )

        // MIN SIDA
        NavigationBarItem(
            selected = selectedTab == "profile",
            onClick = onProfileClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = "Min sida"
                )
            },
            label = { Text("Min sida") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkSage,
                selectedTextColor = DarkSage,
                indicatorColor = Sage.copy(alpha = 0.2f)
            )
        )

        // OM OSS
        NavigationBarItem(
            selected = selectedTab == "about",
            onClick = onAboutClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "Om"
                )
            },
            label = { Text("Om") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkSage,
                selectedTextColor = DarkSage,
                indicatorColor = Sage.copy(alpha = 0.2f)
            )
        )

        // SUPPORT
        NavigationBarItem(
            selected = selectedTab == "support",
            onClick = onSupportClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ContactSupport,
                    contentDescription = "Support"
                )
            },
            label = { Text("Support") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = DarkSage,
                selectedTextColor = DarkSage,
                indicatorColor = Sage.copy(alpha = 0.2f)
            )
        )
    }
}

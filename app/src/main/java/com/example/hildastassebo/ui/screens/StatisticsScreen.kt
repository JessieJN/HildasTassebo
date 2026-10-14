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
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.example.hildastassebo.viewmodel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.automirrored.outlined.ArrowBack


private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Background = Color(0xFFFAFAF7)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)

@Composable
fun StatisticsScreen(onBack: () -> Unit) {
    val adminViewModel: AdminViewModel = viewModel()
    val volunteers by adminViewModel.volunteers.collectAsState()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var selectedVolunteerId by remember {
        mutableStateOf(currentUserId)
    }
    var selectedVolunteer by remember { mutableStateOf("Mina arbetspass") }
    var volunteerMenuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        Text(
            text = "Statistik",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Översikt över genomförda arbetspass",
            fontSize = 14.sp,
            color = Muted
        )
        Spacer(Modifier.height(30.dp))

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
                Text(
                    text = "Genomförda arbetspass",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(Modifier.height(16.dp))

                // Val av volontär
                Box {
                    OutlinedButton(
                        onClick = { volunteerMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedVolunteer)
                    }

                    DropdownMenu(
                        expanded = volunteerMenuExpanded,
                        onDismissRequest = { volunteerMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mina arbetspass") },
                            onClick = {
                                selectedVolunteer = "Mina arbetspass"
                                selectedVolunteerId = currentUserId
                                volunteerMenuExpanded = false
                            }
                        )

                        volunteers
                            .filter { it.id != currentUserId }
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "Senaste veckan",
                        "Senaste månaden",
                        "Senaste året"
                    ).forEach { label ->
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
                                text = "–",
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
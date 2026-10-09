package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hildastassebo.R
import com.example.hildastassebo.PawPrint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Samma färger som i LoginScreen
private val Sage = Color(0xFF91B1A3)
private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)

// Representerar ett bokat arbetspass.
// Detta är tillfällig frontend-data tills Firebase kopplas in.
private data class HomeBookedShift(
    val date: LocalDate,
    val period: String
)

@Composable
fun HomeScreen(
    userName: String = "Volontär",
    onCalendarClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNextShiftClick: () -> Unit = {}
) {

    // Styr om popup-fönstret med användarens pass ska visas.
    var showMyShiftsDialog by remember {
        mutableStateOf(false)
    }

    // Tillfälliga exempelbokningar för att kunna testa designen.
    // Senare ersätts dessa med användarens riktiga bokningar från Firebase.
    val today = LocalDate.now()

    val bookedShifts = remember(today) {
        listOf(
            HomeBookedShift(today.plusDays(3), "Förmiddag"),
            HomeBookedShift(today.plusDays(7), "Eftermiddag"),
            HomeBookedShift(today.plusDays(12), "Förmiddag")
        ).sortedBy { it.date }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {

        // Appens rubrik med tassavtryck
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(top = 24.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "HILDAS TASSEBO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Samma tassavtryck som i LoginScreen, fast mindre
                PawPrint(
                    modifier = Modifier.size(32.dp),
                    color = Sage
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "För en ljusare framtid för hemlösa katter",
                fontSize = 12.sp,
                color = DarkSage
            )
        }

        // Stor kattbild
        Image(
            painter = painterResource(id = R.drawable.home_cat),
            contentDescription = "Katt som vilar i en trädgård",
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Personlig välkomsthälsning
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = PaleSage
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PawPrint(
                        modifier = Modifier.size(44.dp),
                        color = Sage
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Hej $userName!",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Tack för att du hjälper Hildas Tassebo ♡",
                            fontSize = 14.sp,
                            color = Ink
                        )
                    }
                }
            }

            // Nästa arbetspass
            // Öppnar nu ett popup-fönster med användarens bokade pass.
            HomeInfoCard(
                icon = "▦",
                title = "Ditt nästa pass",
                description = "Inget kommande pass",
                onClick = {
                    showMyShiftsDialog = true
                }
            )

            // Evenemang
            HomeInfoCard(
                icon = "campaign",
                title = "Kommande evenemang",
                description = "Inga kommande evenemang"
            )

            // Information
            HomeInfoCard(
                icon = "ⓘ",
                title = "Information",
                description = "Inga nya meddelanden"
            )

            // Dekorativ bild längst ner på startsidan
            Image(
                painter = painterResource(id = R.drawable.bottom_cat_small),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp),
                contentScale = ContentScale.Fit
            )
        }
    }

    // Popup-fönstret visas endast när användaren klickat på
    // kortet "Ditt nästa pass".
    if (showMyShiftsDialog) {
        MyBookedShiftsDialog(
            shifts = bookedShifts,
            onDismiss = {
                showMyShiftsDialog = false
            }
        )
    }
}

// Popup-fönster med användarens kommande arbetspass.
// Fönstret ligger ovanpå HomeScreen och påverkar inte kalendern.
@Composable
private fun MyBookedShiftsDialog(
    shifts: List<HomeBookedShift>,
    onDismiss: () -> Unit
) {

    // Svenska datum, exempelvis "måndag 12 oktober".
    val dateFormatter = remember {
        DateTimeFormatter.ofPattern(
            "EEEE d MMMM",
            Locale.forLanguageTag("sv-SE")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(22.dp),

        title = {
            Column {
                Text(
                    text = "Mina bokade pass",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Dina kommande arbetspass",
                    fontSize = 14.sp,
                    color = Muted
                )
            }
        },

        text = {
            // Begränsad höjd gör att listan går att scrolla
            // om användaren har många bokade pass.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                if (shifts.isEmpty()) {

                    // Visas när användaren inte har några bokningar.
                    Text(
                        text = "Du har inga kommande arbetspass.",
                        fontSize = 14.sp,
                        color = Muted
                    )

                } else {

                    // Visar ett kort för varje bokat pass.
                    shifts.forEachIndexed { index, shift ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            // Datumruta med dag och månad.
                            Column(
                                modifier = Modifier
                                    .size(width = 62.dp, height = 64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PaleSage),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = shift.date.dayOfMonth.toString(),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkSage
                                )

                                Text(
                                    text = shift.date.format(
                                        DateTimeFormatter.ofPattern(
                                            "MMM",
                                            Locale.forLanguageTag("sv-SE")
                                        )
                                    ).uppercase(Locale.forLanguageTag("sv-SE")),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkSage
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = shift.date.format(dateFormatter)
                                        .replaceFirstChar { it.uppercase() },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Ink
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = shift.period,
                                    fontSize = 14.sp,
                                    color = Muted
                                )
                            }
                        }

                        // Skiljelinje mellan passen.
                        if (index < shifts.lastIndex) {
                            HorizontalDivider(
                                color = FieldBorder,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        },

        // Knapp som stänger popup-fönstret.
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Stäng",
                    color = DarkSage,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

// Återanvändbart informationskort på startsidan.
// Utseendet är oförändrat från den ursprungliga HomeScreen.
@Composable
private fun HomeInfoCard(
    icon: String,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            FieldBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (icon == "campaign") {
                Icon(
                    imageVector = Icons.Outlined.Campaign,
                    contentDescription = null,
                    tint = DarkSage,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

            } else {
                Text(
                    text = icon,
                    fontSize = 28.sp,
                    color = DarkSage,
                    modifier = Modifier.width(42.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Muted
                )
            }

            if (onClick != null) {
                Text(
                    text = "›",
                    fontSize = 28.sp,
                    color = DarkSage
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(userName = "Jessie")
}
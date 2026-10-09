package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hildastassebo.R
import com.example.hildastassebo.PawPrint
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material3.Icon

// Samma färger som i LoginScreen
private val Sage = Color(0xFF91B1A3)
private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)

@Composable
fun HomeScreen(
    userName: String = "Volontär",
    onCalendarClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onNextShiftClick: () -> Unit = {}
) {
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
            HomeInfoCard(
                icon = "▦",
                title = "Ditt nästa pass",
                description = "Inget kommande pass",
                onClick = onNextShiftClick
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
}

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
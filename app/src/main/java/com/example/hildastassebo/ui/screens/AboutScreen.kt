
package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hildastassebo.PawPrint

// Färger som matchar resten av appen
private val Sage = Color(0xFF91B1A3)
private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Background = Color(0xFFFAFAF7)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)

@Composable
fun AboutScreen() {

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

        // APPENS LOGOTYP

        PawPrint(
            modifier = Modifier.size(76.dp),
            color = Sage
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "HILDAS TASSEBO",
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink
        )

        Spacer(modifier = Modifier.height(32.dp))

        // SIDANS RUBRIK

        Text(
            text = "Om oss",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Lär känna oss bakom appen",
            fontSize = 14.sp,
            color = Muted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // VILKA ÄR VI?

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(
                width = 1.dp,
                color = FieldBorder
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.School,
                        contentDescription = null,
                        tint = DarkSage,
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Vilka är vi?",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Vi är tre studenter vid Högskolan " +
                        "Kristianstad som under kursen " +
                        "Utveckling i mobila applikationer " +
                        "har utvecklat denna schemaapp " +
                        "för Hildas Tassebo.",
                    fontSize = 15.sp,
                    color = Ink,
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // VARFÖR SKAPADE VI APPEN?

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = PaleSage
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = DarkSage,
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Varför skapade vi appen?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Syftet med appen är att underlätta " +
                        "volontärernas arbete genom att erbjuda " +
                        "en enkel och smidig lösning för " +
                        "schemaläggning.\n\n" +
                        "Genom att samla alla arbetspass " +
                        "på ett och samma ställe vill vi " +
                        "göra det lättare att planera, " +
                        "skapa en tydlig överblick och " +
                        "säkerställa att alla pass är bemannade.",
                    fontSize = 15.sp,
                    color = Ink,
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SÅ ANVÄNDER DU APPEN

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            border = BorderStroke(
                width = 1.dp,
                color = FieldBorder
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {

                Text(
                    text = "Så använder du appen",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Spacer(modifier = Modifier.height(20.dp))

                GuideStep(
                    number = "1",
                    title = "Skapa ett konto",
                    description = "Registrera dig med din " +
                        "e-postadress och ett lösenord " +
                        "för att komma igång."
                )

                Spacer(modifier = Modifier.height(20.dp))

                GuideStep(
                    number = "2",
                    title = "Se schemat",
                    description = "Öppna Kalender för att " +
                        "se kommande arbetspass och " +
                        "vilka dagar som behöver bemannas."
                )

                Spacer(modifier = Modifier.height(20.dp))

                GuideStep(
                    number = "3",
                    title = "Anmäl dig till ett pass",
                    description = "Välj ett ledigt arbetspass " +
                        "som passar dig och anmäl dig " +
                        "direkt i appen."
                )

                Spacer(modifier = Modifier.height(20.dp))

                GuideStep(
                    number = "4",
                    title = "Håll koll på dina pass",
                    description = "Se dina anmälda arbetspass " +
                        "och planera ditt volontärarbete."
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // AVSLUTANDE TEXT

        Icon(
            imageVector = Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = DarkSage,
            modifier = Modifier.size(30.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Vår förhoppning är att appen ska " +
                "bidra till mindre administrativt arbete " +
                "och mer tid till det som verkligen " +
                "betyder något – att ta hand om katterna.",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = DarkSage,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Utvecklad med omtanke för Hildas Tassebo",
            fontSize = 12.sp,
            color = Muted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ÅTERANVÄNDBAR KOMPONENT FÖR GUIDENS STEG

@Composable
private fun GuideStep(
    number: String,
    title: String,
    description: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {

        // GRÖN NUMMERRUTA

        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = PaleSage,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                color = DarkSage,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // RUBRIK OCH BESKRIVNING

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = Muted
            )
        }
    }
}

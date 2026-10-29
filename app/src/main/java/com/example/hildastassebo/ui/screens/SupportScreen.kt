
package com.example.hildastassebo.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContactSupport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

// APPENS FÄRGER
private val Sage = Color(0xFF91B1A3)
private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)

// SUPPORTADRESSER
private val supportRecipients = arrayOf(
    "linnea.nilsson0199@stud.hkr.se",
    "jessiejarl@hotmail.com",
    "gabriella.johansson45@gmail.com"
)

@Composable
fun SupportScreen() {

    val context = LocalContext.current

    // FIREBASE AUTHENTICATION
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val db = remember {
        FirebaseFirestore.getInstance()
    }

    val currentUser = auth.currentUser
    val userId = currentUser?.uid.orEmpty()
    val userEmail = currentUser?.email.orEmpty()

    // HÄMTA ANVÄNDARENS NAMN
    var userName by remember(userId) {
        mutableStateOf(
            currentUser?.displayName
                ?.takeIf { it.isNotBlank() }
                .orEmpty()
        )
    }

    var loadingName by remember(userId) {
        mutableStateOf(
            userName.isBlank() && userId.isNotBlank()
        )
    }

    // HÄMTA NAMN FRÅN FIRESTORE OM DET SAKNAS
    LaunchedEffect(userId) {

        if (userId.isNotBlank() && userName.isBlank()) {

            db.collection("användare")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->

                    val fullName =
                        document.getString("namn")
                            ?: document.getString("name")
                            ?: listOfNotNull(
                                document.getString("förnamn"),
                                document.getString("efternamn")
                            ).joinToString(" ")
                                .takeIf { it.isNotBlank() }

                    userName = fullName.orEmpty()
                    loadingName = false
                }
                .addOnFailureListener {
                    loadingName = false
                }
        }
    }

    // BESKRIVNING AV PROBLEMET
    var problemDescription by remember {
        mutableStateOf("")
    }

    // FELMEDDELANDE
    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    // INFORMATIONSDIALOG
    var showInfoDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(24.dp))

        // SUPPORTIKON
        Icon(
            imageVector = Icons.Outlined.ContactSupport,
            contentDescription = "Support",
            tint = DarkSage,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Support",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Behöver du hjälp? Skicka ett meddelande till oss.",
            fontSize = 15.sp,
            color = DarkSage
        )

        Spacer(modifier = Modifier.height(28.dp))

        // SUPPORTFORMULÄR
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = PaleSage
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Text(
                    text = "Kontakta support",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                // ANVÄNDARENS NAMN
                OutlinedTextField(
                    value = if (loadingName) {
                        "Hämtar ditt namn..."
                    } else {
                        userName.ifBlank { "Namn saknas" }
                    },
                    onValueChange = {},
                    label = {
                        Text("Ditt namn")
                    },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkSage,
                        unfocusedBorderColor = Sage
                    )
                )

                Text(
                    text = "Ditt namn hämtas automatiskt från ditt konto.",
                    fontSize = 12.sp,
                    color = Muted
                )

                // ANVÄNDARENS E-POSTADRESS
                OutlinedTextField(
                    value = userEmail,
                    onValueChange = {},
                    label = {
                        Text("Din e-postadress")
                    },
                    readOnly = true,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkSage,
                        unfocusedBorderColor = Sage
                    )
                )

                Text(
                    text = "Supporten kan svara direkt till mejladressen du skickar från.",
                    fontSize = 12.sp,
                    color = Muted
                )

                // BESKRIVNING AV PROBLEMET
                OutlinedTextField(
                    value = problemDescription,
                    onValueChange = {
                        if (it.length <= 500) {
                            problemDescription = it
                        }
                    },
                    label = {
                        Text("Beskriv ditt problem")
                    },
                    placeholder = {
                        Text(
                            "Beskriv kort vad som har hänt eller " +
                                "vad du behöver hjälp med..."
                        )
                    },
                    minLines = 4,
                    maxLines = 6,
                    supportingText = {
                        Text("${problemDescription.length}/500 tecken")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkSage,
                        unfocusedBorderColor = Sage
                    )
                )

                // SKICKA SUPPORTÄRENDE
                Button(
                    onClick = {

                        errorMessage = null

                        if (currentUser == null) {

                            errorMessage =
                                "Du måste vara inloggad."

                        } else if (userName.isBlank()) {

                            errorMessage =
                                "Kunde inte hämta ditt namn. Försök igen."

                        } else if (problemDescription.isBlank()) {

                            errorMessage =
                                "Du måste beskriva ditt problem."

                        } else {

                            // MEJLETS RUBRIK
                            val subject =
                                "Supportärende - Hildas Tassebo"

                            // MEJLETS INNEHÅLL
                            val message = """
                                Hej Hildas Tassebo Support!

                                Jag behöver hjälp med följande:

                                ${problemDescription.trim()}

                                ------------------------------
                                ANVÄNDARUPPGIFTER
                                ------------------------------

                                Namn: $userName

                                E-post: $userEmail

                                Användar-ID: $userId

                                ------------------------------

                                Med vänliga hälsningar
                                $userName

                            """.trimIndent()

                            // SKAPA MEJLINTENT
                            val emailIntent = Intent(
                                Intent.ACTION_SENDTO
                            ).apply {

                                data = Uri.parse("mailto:")

                                putExtra(
                                    Intent.EXTRA_EMAIL,
                                    supportRecipients
                                )

                                putExtra(
                                    Intent.EXTRA_SUBJECT,
                                    subject
                                )

                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    message
                                )
                            }

                            try {

                                // ÖPPNA ANVÄNDARENS MEJLAPP
                                context.startActivity(
                                    Intent.createChooser(
                                        emailIntent,
                                        "Välj mejlapp"
                                    )
                                )

                                // VISA INFORMATION
                                showInfoDialog = true

                            } catch (
                                e: ActivityNotFoundException
                            ) {

                                errorMessage =
                                    "Ingen mejlapp hittades. " +
                                    "Installera eller konfigurera " +
                                    "en mejlapp och försök igen."

                            } catch (
                                e: Exception
                            ) {

                                errorMessage =
                                    "Kunde inte öppna mejlappen. " +
                                    "Försök igen."
                            }
                        }
                    },
                    enabled =
                        !loadingName &&
                        userName.isNotBlank() &&
                        problemDescription.isNotBlank() &&
                        currentUser != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSage
                    )
                ) {

                    Text(
                        text = "Öppna mejlapp",
                        fontSize = 16.sp
                    )
                }

                // INFORMATION UNDER KNAPPEN
                Text(
                    text = "Din mejlapp öppnas med ett färdigt " +
                        "supportmeddelande. Kontrollera " +
                        "uppgifterna och tryck på Skicka " +
                        "i mejlappen.",
                    fontSize = 12.sp,
                    color = Muted
                )

                // FELMEDDELANDE
                errorMessage?.let { message ->

                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    // INFORMATIONSDIALOG
    if (showInfoDialog) {

        AlertDialog(
            onDismissRequest = {
                showInfoDialog = false
            },
            title = {
                Text("Mejlappen öppnas")
            },
            text = {
                Text(
                    "Ditt supportmeddelande är förberett.\n\n" +
                        "Tryck på Skicka i din mejlapp " +
                        "för att skicka ärendet till supporten.\n\n" +
                        "Supporten kan sedan svara direkt " +
                        "till mejladressen du skickade från."
                )
            },
            confirmButton = {

                TextButton(
                    onClick = {
                        showInfoDialog = false
                    }
                ) {

                    Text(
                        text = "OK",
                        color = DarkSage
                    )
                }
            }
        )
    }
}
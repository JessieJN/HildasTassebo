package com.example.hildastassebo.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private val SageGreen = Color(0xFF47796C)
private val BackgroundColor = Color(0xFFFAFAF7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val user = auth.currentUser

    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
// Visar bekräftelsedialogen för kontoradering
    var showDeleteDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val activeUser = auth.currentUser ?: return@LaunchedEffect

        activeUser.reload()
            .addOnSuccessListener {
                val updatedUser = auth.currentUser
                val updatedEmail = updatedUser?.email
                val userId = updatedUser?.uid

                if (
                    updatedUser?.isEmailVerified == true &&
                    !updatedEmail.isNullOrBlank() &&
                    userId != null
                ) {
                    updatedUser.getIdToken(true)
                        .addOnSuccessListener {
                            db.collection("användare")
                                .document(userId)
                                .update("epost", updatedEmail)
                                .addOnSuccessListener {
                                    message = "E-postadressen har synkroniserats!"
                                }
                                .addOnFailureListener { exception ->
                                    message = "Firestore-fel: ${exception.localizedMessage}"
                                }
                        }
                        .addOnFailureListener { exception ->
                            message = "Token-fel: ${exception.localizedMessage}"
                        }
                } else {
                    message = "E-postadressen är inte verifierad."
                }
            }
            .addOnFailureListener { exception ->
                message = "Reload-fel: ${exception.localizedMessage}"
            }
    }

    val isEmailAccount = user?.providerData?.any {
        it.providerId == "password"
    } == true

    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            TopAppBar(
                title = { Text("Inställningar") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Tillbaka", color = SageGreen)
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Mitt konto",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Inloggad som: ${user?.email ?: "Okänd"}",
                color = Color.Gray
            )

            if (!isEmailAccount) {
                Text(
                    text = "Den här sidan är avsedd för konton " +
                        "som loggar in med e-post och lösenord."
                )
            } else {

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Nuvarande lösenord") },
                    visualTransformation =
                        PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                Text(
                    text = "Ändra e-postadress",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = { newEmail = it },
                    label = { Text("Ny e-postadress") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val activeUser = auth.currentUser
                        val oldEmail = activeUser?.email

                        if (activeUser == null ||
                            oldEmail.isNullOrBlank()
                        ) {
                            message = "Du måste vara inloggad."
                        } else if (currentPassword.isBlank()) {
                            message = "Ange ditt nuvarande lösenord."
                        } else if (newEmail.isBlank()) {
                            message = "Ange en ny e-postadress."
                        } else {
                            isLoading = true
                            message = ""

                            val credential =
                                EmailAuthProvider.getCredential(
                                    oldEmail,
                                    currentPassword
                                )

                            activeUser.reauthenticate(credential)
                                .addOnSuccessListener {
                                    activeUser
                                        .verifyBeforeUpdateEmail(
                                            newEmail.trim()
                                        )
                                        .addOnSuccessListener {
                                            message =
                                                "Ett verifieringsmejl har " +
                                                "skickats till din nya " +
                                                "e-postadress. Följ " +
                                                "instruktionerna i mejlet."
                                            newEmail = ""
                                            isLoading = false
                                        }
                                        .addOnFailureListener {
                                            message =
                                                it.localizedMessage
                                                    ?: "Kunde inte ändra e-post."
                                            isLoading = false
                                        }
                                }
                                .addOnFailureListener {
                                    message =
                                        "Kunde inte verifiera lösenordet."
                                    isLoading = false
                                }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Uppdatera e-postadress")
                }

                HorizontalDivider()

                Text(
                    text = "Ändra lösenord",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nytt lösenord") },
                    visualTransformation =
                        PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Bekräfta nytt lösenord") },
                    visualTransformation =
                        PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val activeUser = auth.currentUser
                        val oldEmail = activeUser?.email

                        when {
                            activeUser == null ||
                                oldEmail.isNullOrBlank() -> {
                                message = "Du måste vara inloggad."
                            }

                            currentPassword.isBlank() -> {
                                message =
                                    "Ange ditt nuvarande lösenord."
                            }

                            newPassword.length < 6 -> {
                                message =
                                    "Lösenordet måste ha minst 6 tecken."
                            }

                            newPassword != confirmPassword -> {
                                message =
                                    "De nya lösenorden matchar inte."
                            }

                            else -> {
                                isLoading = true
                                message = ""

                                val credential =
                                    EmailAuthProvider.getCredential(
                                        oldEmail,
                                        currentPassword
                                    )

                                activeUser
                                    .reauthenticate(credential)
                                    .addOnSuccessListener {
                                        activeUser
                                            .updatePassword(newPassword)
                                            .addOnSuccessListener {
                                                message =
                                                    "Lösenordet har ändrats!"
                                                newPassword = ""
                                                confirmPassword = ""
                                                currentPassword = ""
                                                isLoading = false
                                            }
                                            .addOnFailureListener {
                                                message =
                                                    it.localizedMessage
                                                        ?: "Kunde inte ändra lösenord."
                                                isLoading = false
                                            }
                                    }
                                    .addOnFailureListener {
                                        message =
                                            "Kunde inte verifiera lösenordet."
                                        isLoading = false
                                    }
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SageGreen
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ändra lösenord")
                }
            }
            // =====================================
            // RADERA KONTO
            // =====================================

                        HorizontalDivider()

                        Text(
                            text = "Radera konto",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFB45353)
                        )

                        Text(
                            text = "Om du raderar ditt konto förlorar du åtkomsten till Hildas Tassebo. Åtgärden kan inte ångras.",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        OutlinedButton(
                            onClick = {
                                showDeleteDialog = true
                            },
                            enabled = !isLoading && user != null,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFB45353)
                            )
                        ) {
                            Text("Radera mitt konto")
                        }

            if (isLoading) {
                CircularProgressIndicator(color = SageGreen)
            }

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    color = SageGreen,
                    fontSize = 14.sp
                )
            }
        }

    }

            // =====================================
            // BEKRÄFTA RADERING AV KONTO
            // =====================================

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                    },
                    title = {
                        Text("Radera konto?")
                    },
                    text = {
                        Text(
                            "Är du säker på att du vill radera ditt konto? " +
                                    "Detta går inte att ångra. " +
                                    "Du behöver ange ditt nuvarande lösenord " +
                                    "i inställningarna innan kontot kan raderas."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false

                                val activeUser = auth.currentUser
                                val email = activeUser?.email

                                when {
                                    activeUser == null || email.isNullOrBlank() -> {
                                        message = "Du måste vara inloggad."
                                    }

                                    currentPassword.isBlank() -> {
                                        message = "Ange ditt nuvarande lösenord."
                                    }

                                    !isEmailAccount -> {
                                        message = "Kontot använder en annan inloggningsmetod."
                                    }

                                    else -> {
                                        isLoading = true
                                        message = ""

                                        // Verifiera användarens nuvarande lösenord
                                        val credential = EmailAuthProvider.getCredential(
                                            email,
                                            currentPassword
                                        )

                                        activeUser.reauthenticate(credential)
                                            .addOnSuccessListener {

                                                // Kontrollera att Firestore-profilen finns
                                                val userDocument = db.collection("användare")
                                                    .document(activeUser.uid)

                                                userDocument.get()
                                                    .addOnSuccessListener { document ->

                                                        if (!document.exists()) {
                                                            isLoading = false
                                                            message = "Användarprofilen saknas i Firestore."
                                                        } else {

                                                            // Radera Firestore-profilen
                                                            userDocument.delete()
                                                                .addOnSuccessListener {

                                                                    // Radera Authentication-kontot
                                                                    activeUser.delete()
                                                                        .addOnSuccessListener {
                                                                            isLoading = false
                                                                            auth.signOut()
                                                                            onAccountDeleted()
                                                                        }
                                                                        .addOnFailureListener { exception ->

                                                                            // Försök återställa profilen om Authentication-raderingen misslyckas
                                                                            val previousData = document.data

                                                                            if (previousData != null) {
                                                                                userDocument.set(previousData)
                                                                                    .addOnSuccessListener {
                                                                                        isLoading = false
                                                                                        message =
                                                                                            "Kontot kunde inte raderas. " +
                                                                                                    "Din profil har återställts. Försök igen."
                                                                                    }
                                                                                    .addOnFailureListener {
                                                                                        isLoading = false
                                                                                        message =
                                                                                            "Kontot kunde inte raderas och profilen kunde inte " +
                                                                                                    "återställas. Kontakta administratören."
                                                                                    }
                                                                            } else {
                                                                                isLoading = false
                                                                                message =
                                                                                    "Kontot kunde inte raderas: " +
                                                                                            "${exception.localizedMessage}"
                                                                            }
                                                                        }
                                                                }
                                                                .addOnFailureListener { exception ->
                                                                    isLoading = false
                                                                    message =
                                                                        "Kunde inte radera profilen: " +
                                                                                "${exception.localizedMessage}"
                                                                }
                                                        }
                                                    }
                                                    .addOnFailureListener { exception ->
                                                        isLoading = false
                                                        message =
                                                            "Kunde inte kontrollera profilen: " +
                                                                    "${exception.localizedMessage}"
                                                    }
                                            }
                                            .addOnFailureListener {
                                                isLoading = false
                                                message = "Kunde inte verifiera lösenordet."
                                            }
                                    }
                                }
                            }
                        ) {
                            Text(
                                "Radera konto",
                                color = Color(0xFFB45353)
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                            }
                        ) {
                            Text("Avbryt")
                        }
                    }
                )
            }

}
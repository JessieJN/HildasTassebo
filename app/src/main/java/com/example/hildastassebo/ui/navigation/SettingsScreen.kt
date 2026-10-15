
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
import com.example.hildastassebo.data.repository.AccountDeletionRepository

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
    val deletionRepository = remember { AccountDeletionRepository() }
    val user = auth.currentUser

    var newEmail by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }

    var message by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Visar bekräftelsedialogen för kontoradering.
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Hanterar laddningsrutan och status under kontoraderingen.
    var showDeletionProgress by remember { mutableStateOf(false) }
    var deletionStatus by remember { mutableStateOf("") }
    var deletionError by remember { mutableStateOf("") }

    // =====================================
    // STARTA ELLER ÅTERUPPTA KONTORADERING
    // =====================================

    // Verifierar lösenordet och startar kontoraderingen.
    // Funktionen kan även anropas när användaren
    // väljer att försöka igen efter ett avbrott.
    fun startAccountDeletion() {
        val activeUser = auth.currentUser
        val email = activeUser?.email

        when {
            activeUser == null || email.isNullOrBlank() -> {
                message = "Du måste vara inloggad."
            }

            currentPassword.isBlank() -> {
                message = "Ange ditt nuvarande lösenord."
            }

            activeUser.providerData.none {
                it.providerId == "password"
            } -> {
                message = "Kontot använder en annan inloggningsmetod."
            }

            else -> {
                showDeleteDialog = false
                showDeletionProgress = true
                deletionError = ""
                deletionStatus = "Verifierar lösenord..."
                isLoading = true
                message = ""

                // Kontrollera användarens lösenord innan
                // några uppgifter börjar raderas.
                val credential = EmailAuthProvider.getCredential(
                    email,
                    currentPassword
                )

                activeUser.reauthenticate(credential)
                    .addOnSuccessListener {

                        // Starta rensningen av Firestore.
                        // Repository skickar statusmeddelanden
                        // som visas i laddningsrutan.
                        deletionRepository.deleteUserFirestoreData(
                            onProgress = { status ->
                                deletionStatus = status
                            },

                            onSuccess = {

                                // Alla Firestore-steg är klara.
                                // Radera Firebase Authentication sist.
                                deletionStatus =
                                    "Raderar inloggningskontot..."

                                activeUser.delete()
                                    .addOnSuccessListener {

                                        // Kontot är nu borttaget.
                                        currentPassword = ""
                                        isLoading = false
                                        showDeletionProgress = false

                                        auth.signOut()
                                        onAccountDeleted()
                                    }
                                    .addOnFailureListener { exception ->

                                        // Firestore kan redan vara rensat.
                                        // Användaren kan försöka igen.
                                        deletionError =
                                            "Inloggningskontot kunde inte raderas: " +
                                                    (exception.localizedMessage
                                                        ?: "Okänt fel")

                                        isLoading = false
                                    }
                            },

                            onError = { exception ->

                                // Raderingen avbröts innan
                                // Authentication-kontot togs bort.
                                deletionError =
                                    "Raderingen avbröts: " +
                                            (exception.localizedMessage
                                                ?: "Okänt fel")

                                isLoading = false
                            }
                        )
                    }
                    .addOnFailureListener {

                        deletionError =
                            "Kunde inte verifiera lösenordet. " +
                                    "Kontrollera lösenordet och försök igen."

                        isLoading = false
                    }
            }
        }
    }

    // =====================================
    // SYNKRONISERA E-POSTADRESS
    // =====================================

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
                                .addOnFailureListener { exception ->
                                    message =
                                        "Firestore-fel: ${exception.localizedMessage}"
                                }
                        }
                        .addOnFailureListener { exception ->
                            message =
                                "Token-fel: ${exception.localizedMessage}"
                        }
                } else {
                    message = "E-postadressen är inte verifierad."
                }
            }
            .addOnFailureListener { exception ->
                message =
                    "Reload-fel: ${exception.localizedMessage}"
            }
    }

    val isEmailAccount = user?.providerData?.any {
        it.providerId == "password"
    } == true

    // =====================================
    // INSTÄLLNINGSSIDAN
    // =====================================

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

                // =====================================
                // NUVARANDE LÖSENORD
                // =====================================

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = {
                        currentPassword = it
                    },
                    label = {
                        Text("Nuvarande lösenord")
                    },
                    visualTransformation =
                        PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()

                // =====================================
                // ÄNDRA E-POSTADRESS
                // =====================================

                Text(
                    text = "Ändra e-postadress",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = newEmail,
                    onValueChange = {
                        newEmail = it
                    },
                    label = {
                        Text("Ny e-postadress")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val activeUser = auth.currentUser
                        val oldEmail = activeUser?.email

                        if (
                            activeUser == null ||
                            oldEmail.isNullOrBlank()
                        ) {
                            message = "Du måste vara inloggad."

                        } else if (currentPassword.isBlank()) {
                            message =
                                "Ange ditt nuvarande lösenord."

                        } else if (newEmail.isBlank()) {
                            message =
                                "Ange en ny e-postadress."

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

                // =====================================
                // ÄNDRA LÖSENORD
                // =====================================

                Text(
                    text = "Ändra lösenord",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = {
                        newPassword = it
                    },
                    label = {
                        Text("Nytt lösenord")
                    },
                    visualTransformation =
                        PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                    },
                    label = {
                        Text("Bekräfta nytt lösenord")
                    },
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

                                message =
                                    "Du måste vara inloggad."
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
                CircularProgressIndicator(
                    color = SageGreen
                )
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
                    "Är du säker på att du vill radera ditt konto och " +
                            "tillhörande uppgifter? Detta går inte att ångra. " +
                            "Ange ditt nuvarande lösenord i inställningarna först."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        startAccountDeletion()
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

    // =====================================
    // STATUS UNDER KONTORADERING
    // =====================================

    if (showDeletionProgress) {
        AlertDialog(

            // Dialogen kan inte stängas genom att
            // trycka utanför medan raderingen pågår.
            onDismissRequest = {},

            title = {
                Text("Ditt konto raderas")
            },

            text = {
                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {

                    if (isLoading) {

                        // Visa laddningsindikator.
                        CircularProgressIndicator(
                            color = SageGreen
                        )

                        // Visa det aktuella steget.
                        Text(deletionStatus)

                        // Informera användaren om att
                        // behålla appen öppen.
                        Text(
                            "Behåll appen öppen tills raderingen är klar. " +
                                    "Det kan ta en stund om du har många bokningar.",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )

                    } else {

                        // Visa felmeddelandet om
                        // raderingen har avbrutits.
                        Text(
                            deletionError,
                            color = Color(0xFFB45353)
                        )

                        // Informera om återförsök.
                        Text(
                            "Kontot är ännu inte bekräftat som raderat. " +
                                    "Kontrollera anslutningen och försök igen. " +
                                    "Redan raderade dokument behöver inte raderas på nytt.",
                            fontSize = 14.sp
                        )
                    }
                }
            },

            confirmButton = {

                // Visa knappen först om
                // raderingen har misslyckats.
                if (!isLoading) {
                    TextButton(
                        onClick = {
                            startAccountDeletion()
                        }
                    ) {
                        Text(
                            "Försök igen",
                            color = SageGreen
                        )
                    }
                }
            },

            dismissButton = {

                // Användaren kan stänga felmeddelandet
                // och försöka igen från inställningarna.
                if (!isLoading) {
                    TextButton(
                        onClick = {
                            showDeletionProgress = false

                            message =
                                "Kontoraderingen är inte slutförd. " +
                                        "Du kan försöka igen från inställningarna."
                        }
                    ) {
                        Text("Stäng")
                    }
                }
            }
        )
    }
}

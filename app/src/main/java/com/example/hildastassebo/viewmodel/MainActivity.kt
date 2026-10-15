
package com.example.hildastassebo.viewmodel

import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.example.hildastassebo.ui.screens.MainScreen
import com.example.hildastassebo.ui.theme.HildasTasseboTheme
import com.example.hildastassebo.ui.screens.LoginScreen
import com.example.hildastassebo.ui.screens.AboutScreen
import com.example.hildastassebo.RegisterScreen
import com.example.hildastassebo.data.repository.syncUserEmail
import com.example.hildastassebo.data.repository.AccountDeletionRepository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.statusBarsPadding

private val DeletionGreen = Color(0xFF47796C)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // ==========================================
        // FIRESTORE
        // ==========================================

        val db = FirebaseFirestore.getInstance()

        db.collection("anteckningar")
            .document("LiPEEYwNsRkMq1rH0PuM")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    Log.d(
                        "FIREBASE_TEST",
                        "FOUND DOCUMENT: ${document.data}"
                    )
                } else {
                    Log.d(
                        "FIREBASE_TEST",
                        "Document does NOT exist"
                    )
                }
            }
            .addOnFailureListener { exception ->
                Log.e(
                    "FIREBASE_TEST",
                    "Error reading Firestore",
                    exception
                )
            }

        // ==========================================
        // APPENS GRÄNSSNITT
        // ==========================================

        setContent {

            HildasTasseboTheme {

                val auth = remember {
                    FirebaseAuth.getInstance()
                }

                // Anger om användaren får se appens vanliga innehåll.
                var isLoggedIn by remember {
                    mutableStateOf(false)
                }

                // Visar laddning medan kontot kontrolleras.
                var checkingAuth by remember {
                    mutableStateOf(true)
                }

                // Visar sidan för att återuppta kontoradering.
                var needsDeletionRecovery by remember {
                    mutableStateOf(false)
                }

                // Anger om Firestore-profilen saknas.
                // Det är inte i sig ett bevis på kontoradering.
                var missingProfile by remember {
                    mutableStateOf(false)
                }

                // Fel vid kontroll av kontot.
                var accountCheckError by remember {
                    mutableStateOf("")
                }

                // Används för att göra en ny kontroll.
                var checkVersion by remember {
                    mutableIntStateOf(0)
                }

                // ==========================================
                // KONTROLLERA ANVÄNDARKONTO
                // ==========================================

                LaunchedEffect(checkVersion) {

                    checkingAuth = true
                    isLoggedIn = false
                    needsDeletionRecovery = false
                    missingProfile = false
                    accountCheckError = ""

                    val user = auth.currentUser

                    if (user == null) {
                        checkingAuth = false
                    } else {

                        user.reload()
                            .addOnCompleteListener { task ->

                                if (!task.isSuccessful) {

                                    // Logga inte ut automatiskt vid
                                    // exempelvis ett tillfälligt nätverksfel.
                                    accountCheckError =
                                        "Kunde inte kontrollera kontot. " +
                                                "Kontrollera internetanslutningen."

                                    checkingAuth = false

                                } else if (!user.isEmailVerified) {

                                    // Behåll det tidigare beteendet för
                                    // konton utan verifierad e-post.
                                    auth.signOut()
                                    isLoggedIn = false
                                    checkingAuth = false

                                } else {

                                    // Läs från servern för att undvika
                                    // ett gammalt cachelagrat resultat.
                                    db.collection("användare")
                                        .document(user.uid)
                                        .get(Source.SERVER)
                                        .addOnSuccessListener { document ->

                                            when {

                                                // Kontoradering har påbörjats.
                                                document.exists() &&
                                                        document.getBoolean(
                                                            "kontoradering_pågår"
                                                        ) == true -> {

                                                    needsDeletionRecovery = true
                                                }

                                                // Kontot finns och är aktivt.
                                                document.exists() -> {

                                                    isLoggedIn = true
                                                }

                                                // Profilen saknas, men
                                                // Authentication-kontot finns.
                                                else -> {

                                                    missingProfile = true
                                                    needsDeletionRecovery = true
                                                }
                                            }

                                            checkingAuth = false
                                        }
                                        .addOnFailureListener { exception ->

                                            Log.e(
                                                "AUTH_CHECK",
                                                "Kunde inte kontrollera användarprofilen",
                                                exception
                                            )

                                            // Ingen automatisk utloggning.
                                            // Användaren får försöka igen.
                                            accountCheckError =
                                                "Kunde inte läsa kontouppgifterna. " +
                                                        "Kontrollera internetanslutningen."

                                            checkingAuth = false
                                        }
                                }
                            }
                    }
                }

                // ==========================================
                // SYNKRONISERA E-POSTADRESS
                // ==========================================

                // Synkroniserar användarens e-postadress mellan
                // Firebase Authentication och Firestore.
                // Körs endast när kontot är aktivt.
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner, isLoggedIn) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (
                            event == Lifecycle.Event.ON_RESUME &&
                            isLoggedIn
                        ) {
                            syncUserEmail()
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Bestämmer om registreringssidan visas.
                var showRegister by rememberSaveable {
                    mutableStateOf(false)
                }

                // Visar Om oss utan att användaren behöver logga in.
                var showAbout by rememberSaveable {
                    mutableStateOf(false)
                }

                // ==================================
                // NAVIGERING MELLAN SIDOR
                // ==================================

                when {

                    // Kontroll pågår.
                    checkingAuth -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // Kontrollen misslyckades.
                    accountCheckError.isNotBlank() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .padding(24.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(16.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Kunde inte kontrollera kontot",
                                style =
                                    MaterialTheme.typography.titleLarge
                            )

                            Text(accountCheckError)

                            Button(
                                onClick = {
                                    checkVersion++
                                }
                            ) {
                                Text("Försök igen")
                            }

                            TextButton(
                                onClick = {
                                    auth.signOut()
                                    accountCheckError = ""
                                    isLoggedIn = false
                                    needsDeletionRecovery = false
                                }
                            ) {
                                Text("Logga ut")
                            }
                        }
                    }

                    // Kontoradering behöver återupptas.
                    needsDeletionRecovery -> {

                        AccountDeletionRecoveryScreen(
                            missingProfile = missingProfile,
                            onAccountDeleted = {

                                auth.signOut()
                                needsDeletionRecovery = false
                                isLoggedIn = false
                                showRegister = false
                                showAbout = false
                            },
                            onLogout = {

                                auth.signOut()
                                needsDeletionRecovery = false
                                isLoggedIn = false
                                showRegister = false
                            }
                        )
                    }

                    // Om oss för användare som inte är inloggade.
                    !isLoggedIn && showAbout -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        ) {
                            TextButton(
                                onClick = { showAbout = false }
                            ) {
                                Text("← Tillbaka")
                            }

                            AboutScreen()
                        }
                    }

                    // 1. ANVÄNDAREN ÄR INLOGGAD.
                    isLoggedIn -> {

                        MainScreen(
                            onLogout = {

                                // Logga ut från Firebase.
                                auth.signOut()

                                // Gå tillbaka till inloggningssidan.
                                isLoggedIn = false

                                // Visa inte registreringssidan.
                                showRegister = false
                            }
                        )
                    }

                    // 2. ANVÄNDAREN VILL REGISTRERA SIG.
                    showRegister -> {

                        RegisterScreen(
                            onRegisterSuccess = {

                                // Kontrollera profilen även efter
                                // registrering innan appen öppnas.
                                checkVersion++
                            },
                            onLoginClick = {
                                showRegister = false
                            },
                            onAboutClick = {
                                showAbout = true
                            }
                        )
                    }

                    // 3. ANVÄNDAREN SKA LOGGA IN.
                    else -> {

                        LoginScreen(
                            onLoginSuccess = {

                                // Kontrollera om kontoradering
                                // pågår innan MainScreen visas.
                                // E-postsynkronisering sker
                                // fortfarande för aktiva konton.
                                syncEmailWithFirestore()
                                checkVersion++
                            },
                            onRegisterClick = {
                                showRegister = true
                            },
                            onAboutClick = {
                                showAbout = true
                            }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// ÅTERUPPTA KONTORADERING
// ==========================================

@Composable
private fun AccountDeletionRecoveryScreen(
    missingProfile: Boolean,
    onAccountDeleted: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = remember {
        FirebaseAuth.getInstance()
    }

    val deletionRepository = remember {
        AccountDeletionRepository()
    }

    var password by remember {
        mutableStateOf("")
    }

    var isDeleting by remember {
        mutableStateOf(false)
    }

    var status by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf("")
    }

    // Raderingen kan köras igen efter ett avbrott.
    // Repository hoppar över redan raderade dokument.
    fun resumeDeletion() {

        val user = auth.currentUser
        val email = user?.email

        if (user == null || email.isNullOrBlank()) {
            error = "Du måste vara inloggad."
            return
        }

        if (password.isBlank()) {
            error = "Ange ditt nuvarande lösenord."
            return
        }

        isDeleting = true
        error = ""
        status = "Verifierar lösenord..."

        val credential = EmailAuthProvider.getCredential(
            email,
            password
        )

        user.reauthenticate(credential)
            .addOnSuccessListener {

                status = "Fortsätter kontoraderingen..."

                deletionRepository.deleteUserFirestoreData(
                    onProgress = { progress ->
                        status = progress
                    },
                    onSuccess = {

                        // Authentication raderas sist.
                        status = "Raderar inloggningskontot..."

                        user.delete()
                            .addOnSuccessListener {

                                isDeleting = false
                                password = ""

                                onAccountDeleted()
                            }
                            .addOnFailureListener { exception ->

                                isDeleting = false

                                error =
                                    "Kunde inte radera inloggningskontot: " +
                                            (exception.localizedMessage
                                                ?: "Okänt fel") +
                                            ". Försök igen."
                            }
                    },
                    onError = { exception ->

                        isDeleting = false

                        error =
                            "Raderingen kunde inte slutföras: " +
                                    (exception.localizedMessage
                                        ?: "Okänt fel")
                    }
                )
            }
            .addOnFailureListener {

                isDeleting = false
                error =
                    "Kunde inte verifiera lösenordet. " +
                            "Kontrollera lösenordet och försök igen."
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement =
            Arrangement.spacedBy(18.dp)
    ) {

        Text(
            text = if (missingProfile) {
                "Kontouppgifter saknas"
            } else {
                "Kontoradering pågår"
            },
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = if (missingProfile) {
                "Din användarprofil finns inte längre i databasen, " +
                        "men ditt inloggningskonto finns kvar. " +
                        "Det kan bero på en avbruten kontoradering " +
                        "eller ett annat problem med profilen."
            } else {
                "Du har tidigare påbörjat en kontoradering " +
                        "som ännu inte är slutförd."
            }
        )

        Text(
            "Du kan slutföra raderingen genom att " +
                    "ange ditt lösenord nedan."
        )

        // Användaren måste bekräfta sitt lösenord.
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text("Nuvarande lösenord")
            },
            visualTransformation =
                PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isDeleting,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                resumeDeletion()
            },
            enabled = !isDeleting,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = DeletionGreen
            )
        ) {
            Text("Slutför kontoradering")
        }

        if (isDeleting) {

            CircularProgressIndicator(
                color = DeletionGreen
            )

            Text(status)

            Text(
                "Behåll appen öppen tills raderingen är klar. " +
                        "Om processen avbryts kan du försöka igen " +
                        "nästa gång du öppnar appen."
            )
        }

        if (error.isNotBlank()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error
            )
        }

        // Tillåt utloggning, men inte vanlig navigering
        // till kalendern under pågående kontoradering.
        if (!isDeleting) {
            TextButton(
                onClick = onLogout
            ) {
                Text("Logga ut")
            }
        }
    }
}

// ==========================================
// SYNKRONISERA E-POST MED FIRESTORE
// ==========================================

fun syncEmailWithFirestore() {
    val user = FirebaseAuth.getInstance().currentUser ?: return
    val db = FirebaseFirestore.getInstance()

    user.reload().addOnSuccessListener {
        val updatedEmail = user.email ?: return@addOnSuccessListener

        if (user.isEmailVerified) {
            user.getIdToken(true).addOnSuccessListener {
                db.collection("användare")
                    .document(user.uid)
                    .update("epost", updatedEmail)
                    .addOnSuccessListener {
                        Log.d(
                            "EMAIL_SYNC",
                            "E-post uppdaterad i Firestore!"
                        )
                    }
                    .addOnFailureListener { exception ->
                        Log.e(
                            "EMAIL_SYNC",
                            "Firestore-fel",
                            exception
                        )
                    }
            }.addOnFailureListener { exception ->
                Log.e(
                    "EMAIL_SYNC",
                    "Token-fel",
                    exception
                )
            }
        }
    }.addOnFailureListener { exception ->
        Log.e(
            "EMAIL_SYNC",
            "Kunde inte läsa användaren",
            exception
        )
    }
}

// ==========================================
// TILLFÄLLIG STARTSIDA
// ==========================================

@Composable
fun Greeting(
    name: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = name,
        modifier = modifier
    )
}

// ==========================================
// FÖRHANDSVISNING
// ==========================================

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {

    HildasTasseboTheme {
        Greeting(
            "Välkommen till Hildas Tassebo!"
        )
    }
}


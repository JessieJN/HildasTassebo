
package com.example.hildastassebo

import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

import com.example.hildastassebo.ui.screens.MainScreen
import com.example.hildastassebo.ui.theme.HildasTasseboTheme

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.hildastassebo.data.repository.syncUserEmail

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

                // Firebase Authentication
                val auth = remember {
                    FirebaseAuth.getInstance()
                }

                // Kontrollerar om användaren är inloggad
                var isLoggedIn by remember {
                    mutableStateOf(false)
                }

                var checkingAuth by remember {
                    mutableStateOf(true)
                }

                LaunchedEffect(Unit) {
                    val user = auth.currentUser

                    if (user == null) {
                        checkingAuth = false
                    } else {
                        user.reload().addOnCompleteListener { task ->
                            if (task.isSuccessful && user.isEmailVerified) {
                                isLoggedIn = true
                            } else {
                                auth.signOut()
                                isLoggedIn = false
                            }

                            checkingAuth = false
                        }
                    }
                }

                // Synkroniserar användarens e-postadress mellan Firebase Authentication och Firestore.
                // Kontrollen körs när användaren öppnar eller återgår till appen.
                // Detta behövs eftersom en ny e-postadress inte uppdateras förrän
                // användaren har bekräftat den via verifieringsmejlet.
                // Användaren förblir inloggad under processen.
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner, isLoggedIn) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME && isLoggedIn) {
                            syncUserEmail()
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Bestämmer om registreringssidan visas
                var showRegister by remember {
                    mutableStateOf(false)
                }

                // ==================================
                // NAVIGERING MELLAN SIDOR
                // ==================================

                when {
                        checkingAuth -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    // 1. ANVÄNDAREN ÄR INLOGGAD
                    isLoggedIn -> {

                        MainScreen(
                            onLogout = {

                                // Logga ut från Firebase
                                auth.signOut()

                                // Gå tillbaka till inloggningssidan
                                isLoggedIn = false

                                // Visa inte registreringssidan
                                showRegister = false
                            }
                        )
                    }

                    // 2. ANVÄNDAREN VILL REGISTRERA SIG
                    showRegister -> {

                        RegisterScreen(
                            onRegisterSuccess = {
                                isLoggedIn = true
                            },
                            onLoginClick = {
                                showRegister = false
                            }
                        )
                    }

                    // 3. ANVÄNDAREN SKA LOGGA IN
                    else -> {

                        LoginScreen(
                            onLoginSuccess = {
                                syncEmailWithFirestore()
                                isLoggedIn = true
                            },
                            onRegisterClick = {
                                showRegister = true
                            }
                        )
                    }
                }
            }
        }
    }
}
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
                        Log.d("EMAIL_SYNC", "E-post uppdaterad i Firestore!")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("EMAIL_SYNC", "Firestore-fel", exception)
                    }
            }.addOnFailureListener { exception ->
                Log.e("EMAIL_SYNC", "Token-fel", exception)
            }
        }
    }.addOnFailureListener { exception ->
        Log.e("EMAIL_SYNC", "Kunde inte läsa användaren", exception)
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

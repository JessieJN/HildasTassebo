
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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // ==========================================
        // FIRESTORE - Behåller din kollegas test
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
                    mutableStateOf(
                        auth.currentUser != null
                    )
                }

                // Bestämmer om registreringssidan visas
                var showRegister by remember {
                    mutableStateOf(false)
                }

                // ==================================
                // NAVIGERING MELLAN SIDOR
                // ==================================

                when {

                    // 1. Användaren är inloggad
                    isLoggedIn -> {

                        MainScreen()
                    }

                    // 2. Användaren vill registrera sig
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

                    // 3. Användaren ska logga in
                    else -> {

                        LoginScreen(
                            onLoginSuccess = {
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

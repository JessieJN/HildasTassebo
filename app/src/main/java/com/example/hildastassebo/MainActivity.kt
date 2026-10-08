
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

import com.example.hildastassebo.ui.theme.HildasTasseboTheme

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Behåller din kollegas Firestore-test
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

        // Här startar appens gränssnitt
        setContent {
            HildasTasseboTheme {

                val auth = remember {
                    FirebaseAuth.getInstance()
                }

                var isLoggedIn by remember {
                    mutableStateOf(auth.currentUser != null)
                }

                if (isLoggedIn) {

                    // Tillfällig startsida
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->

                        Greeting(
                            name = "Välkommen till Hildas Tassebo!",
                            modifier = Modifier.padding(innerPadding)
                        )
                    }

                } else {

                    // Er nya inloggningssida
                    LoginScreen(
                        onLoginSuccess = {
                            isLoggedIn = true
                        }
                    )
                }
            }
        }
    }
}

// Tillfällig startsida
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

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HildasTasseboTheme {
        Greeting("Välkommen till Hildas Tassebo!")
    }
}

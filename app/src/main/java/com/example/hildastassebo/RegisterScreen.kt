
package com.example.hildastassebo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

private val Sage = Color(0xFF91B1A3)
private val DarkSage = Color(0xFF47796C)
private val Background = Color(0xFFFAFAF7)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    val auth = remember { FirebaseAuth.getInstance() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Text(
            text = "🐾",
            fontSize = 64.sp
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "HILDAS TASSEBO",
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Ink,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "För en ljusare framtid för hemlösa katter",
            fontSize = 13.sp,
            color = DarkSage,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(40.dp))

        Text(
            text = "Skapa ditt konto",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Bli en del av Hildas Tassebo",
            fontSize = 14.sp,
            color = Muted
        )

        Spacer(Modifier.height(30.dp))

        // Fullständigt namn
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                error = ""
            },
            label = { Text("Fullständigt namn") },
            placeholder = { Text("Förnamn Efternamn") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = registerFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // E-postadress
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                error = ""
            },
            label = { Text("E-postadress") },
            placeholder = { Text("din@email.se") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            shape = RoundedCornerShape(18.dp),
            colors = registerFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Lösenord
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = ""
            },
            label = { Text("Lösenord") },
            placeholder = { Text("Minst 6 tecken") },
            singleLine = true,
            visualTransformation =
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            shape = RoundedCornerShape(18.dp),
            colors = registerFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // Bekräfta lösenord
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                error = ""
            },
            label = { Text("Bekräfta lösenord") },
            placeholder = { Text("Upprepa lösenord") },
            singleLine = true,
            visualTransformation =
                PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            shape = RoundedCornerShape(18.dp),
            colors = registerFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(28.dp))

        // Registreringsknapp
        Button(
            onClick = {
                when {
                    name.isBlank() -> {
                        error = "Ange ditt namn."
                    }
                    email.isBlank() -> {
                        error = "Ange din e-postadress."
                    }
                    password.length < 6 -> {
                        error = "Lösenordet måste ha minst 6 tecken."
                    }
                    password != confirmPassword -> {
                        error = "Lösenorden matchar inte."
                    }
                    else -> {
                        loading = true
                        error = ""

                        auth.createUserWithEmailAndPassword(
                            email.trim(),
                            password
                        ).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val user = auth.currentUser

                                val profile =
                                    UserProfileChangeRequest.Builder()
                                        .setDisplayName(name.trim())
                                        .build()

                                if (user != null) {
                                    user.updateProfile(profile)
                                        .addOnCompleteListener {
                                            loading = false
                                            onRegisterSuccess()
                                        }
                                } else {
                                    loading = false
                                    error = "Kunde inte läsa kontot."
                                }
                            } else {
                                loading = false
                                error = "Kunde inte skapa kontot. " +
                                    "Kontrollera uppgifterna " +
                                    "eller försök igen."
                            }
                        }
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Sage,
                contentColor = Color.White
            )
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Skapa konto",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Har du redan ett konto? ",
                color = Muted,
                fontSize = 14.sp
            )

            Text(
                text = "Logga in",
                color = DarkSage,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    onLoginClick()
                }
            )
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun registerFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = DarkSage,
        unfocusedBorderColor = FieldBorder,
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedLabelColor = DarkSage
    )
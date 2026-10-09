
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
import com.google.firebase.firestore.FirebaseFirestore

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
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var verificationSent by remember { mutableStateOf(false) }

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

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

        PawPrint(
            modifier = Modifier.size(88.dp),
            color = Sage
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

        if (!verificationSent) {

            // REGISTRERING

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

            OutlinedTextField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    error = ""
                },
                label = { Text("Förnamn") },
                placeholder = { Text("Ditt förnamn") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = registerFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    error = ""
                },
                label = { Text("Efternamn") },
                placeholder = { Text("Ditt efternamn") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = registerFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

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

            Button(
                onClick = {
                    when {
                        firstName.isBlank() -> {
                            error = "Ange ditt förnamn."
                        }

                        lastName.isBlank() -> {
                            error = "Ange ditt efternamn."
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

                                if (!task.isSuccessful) {
                                    loading = false
                                    error = task.exception?.localizedMessage
                                        ?: "Kunde inte skapa kontot."
                                } else {
                                    val user = task.result?.user

                                    if (user == null) {
                                        loading = false
                                        error = "Kunde inte läsa kontot."
                                    } else {

                                        val userData = hashMapOf(
                                            "förnamn" to firstName.trim(),
                                            "efternamn" to lastName.trim(),
                                            "epost" to (
                                                user.email ?: email.trim()
                                            ),
                                            "roll" to 0
                                        )

                                        db.collection("användare")
                                            .document(user.uid)
                                            .set(userData)
                                            .addOnSuccessListener {

                                                val profile =
                                                    UserProfileChangeRequest
                                                        .Builder()
                                                        .setDisplayName(
                                                            "${firstName.trim()} " +
                                                            lastName.trim()
                                                        )
                                                        .build()

                                                user.updateProfile(profile)
                                                    .addOnCompleteListener {
                                                        user.sendEmailVerification()
                                                            .addOnCompleteListener { verifyTask ->

                                                                loading = false
                                                                verificationSent = true

                                                                if (verifyTask.isSuccessful) {
                                                                    message =
                                                                        "Vi har skickat ett verifieringsmejl till ${user.email}."
                                                                    error = ""
                                                                } else {
                                                                    error =
                                                                        "Kontot skapades, men mejlet kunde inte skickas."
                                                                }
                                                            }
                                                    }
                                            }
                                            .addOnFailureListener {
                                                loading = false
                                                error =
                                                    "Kontot skapades, men profilen kunde inte sparas."
                                            }
                                    }
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

        } else {

            // E-POSTVERIFIERING

            Text(
                text = "Verifiera din e-post",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Tack för att du registrerade dig!",
                fontSize = 16.sp,
                color = DarkSage,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Vi har skapat ditt konto. " +
                    "Kontrollera din e-post och klicka på " +
                    "verifieringslänken innan du fortsätter.",
                fontSize = 14.sp,
                color = Muted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = auth.currentUser?.email ?: email,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DarkSage,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = DarkSage,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (error.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))

                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            // KONTROLLERA VERIFIERING

            Button(
                onClick = {
                    loading = true
                    error = ""
                    message = ""

                    val user = auth.currentUser

                    if (user == null) {
                        loading = false
                        error = "Ingen användare är inloggad."
                    } else {
                        user.reload()
                            .addOnCompleteListener { task ->
                                loading = false

                                if (!task.isSuccessful) {
                                    error =
                                        "Kunde inte kontrollera verifieringen."
                                } else if (user.isEmailVerified) {
                                    onRegisterSuccess()
                                } else {
                                    error =
                                        "Din e-postadress är inte verifierad ännu."
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
                    Text("Jag har verifierat min e-post")
                }
            }

            Spacer(Modifier.height(16.dp))

            // SKICKA MEJLET IGEN

            OutlinedButton(
                onClick = {
                    loading = true
                    error = ""
                    message = ""

                    val user = auth.currentUser

                    if (user == null) {
                        loading = false
                        error = "Ingen användare är inloggad."
                    } else {
                        user.sendEmailVerification()
                            .addOnCompleteListener { task ->
                                loading = false

                                if (task.isSuccessful) {
                                    message =
                                        "Ett nytt verifieringsmejl har skickats!"
                                } else {
                                    error =
                                        "Kunde inte skicka mejlet. Försök senare."
                                }
                            }
                    }
                },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(
                    text = "Skicka verifieringsmejl igen",
                    color = DarkSage
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

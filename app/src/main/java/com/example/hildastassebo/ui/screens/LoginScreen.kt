package com.example.hildastassebo.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.autofill.contentType
import android.util.Patterns
import com.google.firebase.FirebaseNetworkException
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

// Färgtema från designen
private val Sage = Color(0xFF91B1A3)
private val DarkSage = Color(0xFF47796C)
private val PaleSage = Color(0xFFE7F0EB)
private val Background = Color(0xFFFAFAF7)
private val Ink = Color(0xFF20242C)
private val Muted = Color(0xFF7D8793)
private val FieldBorder = Color(0xFFD7E2DC)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onRegisterClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    // Hanterar dialogrutan för återställning av lösenord
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetLoading by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf("") }

    val auth = remember { FirebaseAuth.getInstance() }

    val passwordFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        // Dekorativ våg längst ner
        BottomWave(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(145.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(Modifier.height(30.dp))

            // Stor tasslogotyp
            PawPrint(
                modifier = Modifier.size(88.dp),
                color = Sage
            )

            Spacer(Modifier.height(20.dp))

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

            Spacer(Modifier.height(58.dp))

            Text(
                text = "Välkommen!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Logga in för att fortsätta hjälpa våra katter",
                fontSize = 14.sp,
                color = Muted,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(38.dp))

            // E-postfält
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    error = ""
                },
                label = { Text("E-postadress") },
                placeholder = {
                    Text("din@email.se", color = Muted)
                },
                leadingIcon = {
                    Text(
                        text = "✉",
                        fontSize = 25.sp,
                        color = DarkSage
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = {
                        passwordFocusRequester.requestFocus()
                    }
                ),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkSage,
                    unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = DarkSage
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .contentType(ContentType.EmailAddress)
            )

            Spacer(Modifier.height(18.dp))

            // Lösenordsfält
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    error = ""
                },
                label = { Text("Lösenord") },
                leadingIcon = {
                    Text(
                        text = "♙",
                        fontSize = 24.sp,
                        color = DarkSage
                    )
                },
                trailingIcon = {
                    Text(
                        text = if (passwordVisible) "Dölj" else "Visa",
                        fontSize = 12.sp,
                        color = DarkSage,
                        modifier = Modifier
                            .clickable {
                                passwordVisible = !passwordVisible
                            }
                            .padding(8.dp)
                    )
                },
                visualTransformation =
                    if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                ),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkSage,
                    unfocusedBorderColor = FieldBorder,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedLabelColor = DarkSage
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 68.dp)
                    .contentType(ContentType.Password)
                    .focusRequester(passwordFocusRequester)
            )

            // Öppnar dialogrutan för att återställa lösenordet
            Text(
                text = "Glömt lösenord?",
                color = DarkSage,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable {
                        resetEmail = email.trim()
                        resetMessage = ""
                        showForgotPasswordDialog = true
                    }
                    .padding(top = 8.dp, bottom = 8.dp)
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

            // Inloggningsknapp
            Button(
                onClick = {
                    loading = true
                    error = ""

                    auth.signInWithEmailAndPassword(
                        email.trim(),
                        password
                    ).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser

                            user?.reload()?.addOnCompleteListener { reloadTask ->
                                if (reloadTask.isSuccessful && user.isEmailVerified) {
                                    onLoginSuccess()
                                } else if (reloadTask.isSuccessful) {
                                    error = "Du måste verifiera din e-postadress innan du kan logga in."
                                    auth.signOut()
                                } else {
                                    error = "Kunde inte kontrollera e-postverifieringen. Försök igen."
                                    auth.signOut()
                                }
                                loading = false
                            } ?: run {
                                error = "Kunde inte hitta användaren."
                                auth.signOut()
                                loading = false
                            }
                        } else {
                            error = when (task.exception) {
                                is FirebaseNetworkException ->
                                    "Ingen internetanslutning. Kontrollera nätverket och försök igen."

                                else ->
                                    "Kunde inte logga in. Kontrollera e-post och lösenord."
                            }

                            loading = false
                        }
                    }
                },
                enabled = !loading &&
                        Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() &&
                        password.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Sage,
                    contentColor = Color.White,
                    disabledContainerColor = Sage.copy(
                        alpha = 0.55f
                    ),
                    disabledContentColor = Color.White
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
                        text = "Logga in",
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
                    text = "Har du inget konto? ",
                    color = Muted,
                    fontSize = 14.sp
                )

                Text(
                    text = "Registrera dig",
                    color = DarkSage,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.clickable {
                        onRegisterClick()
                    }
                )
            }

            Spacer(Modifier.height(115.dp))
        }
    }


    // Dialogruta för användare som har glömt sitt lösenord
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!resetLoading) {
                    showForgotPasswordDialog = false
                }
            },
            title = {
                Text("Återställ lösenord")
            },
            text = {
                Column {
                    Text(
                        "Ange din e-postadress så skickar vi " +
                                "en länk för att återställa lösenordet."
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = {
                            resetEmail = it
                            resetMessage = ""
                        },
                        label = { Text("E-postadress") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .contentType(ContentType.EmailAddress)
                    )

                    if (resetMessage.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = resetMessage,
                            color = DarkSage,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !resetLoading &&
                            Patterns.EMAIL_ADDRESS.matcher(resetEmail.trim()).matches(),
                    onClick = {
                        resetLoading = true
                        resetMessage = ""

                        auth.sendPasswordResetEmail(resetEmail.trim())
                            .addOnCompleteListener { task ->
                                resetLoading = false

                                resetMessage = when {
                                    task.isSuccessful ->
                                        "Om adressen är registrerad kommer " +
                                                "du att få ett mejl med instruktioner."

                                    task.exception is FirebaseNetworkException ->
                                        "Ingen internetanslutning. Kontrollera nätverket och försök igen."

                                    else ->
                                        "Kunde inte behandla begäran just nu. " +
                                                "Försök igen senare."
                                }
                            }
                    }
                ) {
                    if (resetLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Skicka mejl", color = DarkSage)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !resetLoading,
                    onClick = {
                        showForgotPasswordDialog = false
                    }
                ) {
                    Text("Stäng")
                }
            }
        )
    }

}

// Tassymbol ritad direkt i Compose
@Composable
fun PawPrint(
    modifier: Modifier = Modifier,
    color: Color = Sage
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Fyra tådynor
        drawOval(
            color,
            topLeft = Offset(w * 0.12f, h * 0.28f),
            size = Size(
                w * 0.19f, h * 0.27f
            )
        )
        drawOval(
            color,
            topLeft = Offset(w * 0.31f, h * 0.05f),
            size = Size(
                w * 0.19f, h * 0.28f
            )
        )
        drawOval(
            color,
            topLeft = Offset(w * 0.55f, h * 0.05f),
            size = Size(
                w * 0.19f, h * 0.28f
            )
        )
        drawOval(
            color,
            topLeft = Offset(w * 0.76f, h * 0.28f),
            size = Size(
                w * 0.19f, h * 0.27f
            )
        )

        // Stora trampdynan
        val pad = Path().apply {
            moveTo(w * 0.20f, h * 0.78f)
            cubicTo(
                w * 0.20f, h * 0.61f,
                w * 0.38f, h * 0.47f,
                w * 0.53f, h * 0.47f
            )
            cubicTo(
                w * 0.69f, h * 0.47f,
                w * 0.87f, h * 0.65f,
                w * 0.87f, h * 0.80f
            )
            cubicTo(
                w * 0.86f, h * 0.98f,
                w * 0.67f, h * 0.90f,
                w * 0.53f, h * 0.87f
            )
            cubicTo(
                w * 0.39f, h * 0.90f,
                w * 0.21f, h * 0.98f,
                w * 0.20f, h * 0.78f
            )
            close()
        }
        drawPath(pad, color)
    }
}

// Mjuk ljusgrön våg längst ner
@Composable
private fun BottomWave(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val wave = Path().apply {
            moveTo(0f, h * 0.20f)
            cubicTo(
                w * 0.20f, h * 0.15f,
                w * 0.30f, h * 0.72f,
                w * 0.52f, h * 0.56f
            )
            cubicTo(
                w * 0.75f, h * 0.40f,
                w * 0.75f, h * 0.96f,
                w, h * 0.72f
            )
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }

        drawPath(
            path = wave,
            color = PaleSage
        )
    }
}

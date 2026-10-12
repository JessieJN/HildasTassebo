
package com.example.hildastassebo

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.StrokeCap
import com.google.firebase.auth.FirebaseAuth

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

    val auth = remember { FirebaseAuth.getInstance() }

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
                text = "Välkommen tillbaka!",
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
                    keyboardType = KeyboardType.Email
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
                    keyboardType = KeyboardType.Password
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
                        loading = false

                        if (task.isSuccessful) {
                            onLoginSuccess()
                        } else {
                            error = "Kunde inte logga in. " +
                                "Kontrollera e-post och lösenord."
                        }
                    }
                },
                enabled = !loading &&
                    email.isNotBlank() &&
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
            size = androidx.compose.ui.geometry.Size(
                w * 0.19f, h * 0.27f
            )
        )
        drawOval(
            color,
            topLeft = Offset(w * 0.31f, h * 0.05f),
            size = androidx.compose.ui.geometry.Size(
                w * 0.19f, h * 0.28f
            )
        )
        drawOval(
            color,
            topLeft = Offset(w * 0.55f, h * 0.05f),
            size = androidx.compose.ui.geometry.Size(
                w * 0.19f, h * 0.28f
            )
        )
        drawOval(
            color,
            topLeft = Offset(w * 0.76f, h * 0.28f),
            size = androidx.compose.ui.geometry.Size(
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

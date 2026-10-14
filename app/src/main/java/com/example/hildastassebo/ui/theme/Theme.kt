package com.example.hildastassebo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HildasColorScheme = lightColorScheme(
    primary = Color(0xFF47796C),
    onPrimary = Color.White,

    primaryContainer = Color(0xFFE7F0EB),
    onPrimaryContainer = Color(0xFF244E42),

    secondary = Color(0xFF91B1A3),
    onSecondary = Color(0xFF172E27),

    background = Color(0xFFFAFAF7),
    onBackground = Color(0xFF24342E),

    surface = Color(0xFFFAFAF7),
    onSurface = Color(0xFF24342E),

    surfaceVariant = Color(0xFFE7F0EB),
    onSurfaceVariant = Color(0xFF47796C)
)

@Composable
fun HildasTasseboTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = HildasColorScheme,
        typography = Typography,
        content = content
    )
}
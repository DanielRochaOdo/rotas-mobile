package com.odontoart.rotas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OdontoartGreen = Color(0xFF0C6F3D)
private val OdontoartGreenDark = Color(0xFF07552E)
private val OdontoartMint = Color(0xFFDDF3E7)
private val OdontoartCream = Color(0xFFF7F8F5)
private val Ink = Color(0xFF18211B)
private val DarkSurface = Color(0xFF121A15)

private val LightColors = lightColorScheme(
    primary = OdontoartGreen,
    onPrimary = Color.White,
    primaryContainer = OdontoartMint,
    onPrimaryContainer = OdontoartGreenDark,
    secondary = Color(0xFF426653),
    secondaryContainer = Color(0xFFDCE9E1),
    background = OdontoartCream,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDF2EE),
    outlineVariant = Color(0xFFD8E1DA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7DDBA8),
    onPrimary = Color(0xFF00391E),
    primaryContainer = Color(0xFF07552E),
    onPrimaryContainer = Color(0xFFA0F4C3),
    secondary = Color(0xFFA8D0B8),
    background = Color(0xFF0D1410),
    onBackground = Color(0xFFE4EAE5),
    surface = DarkSurface,
    onSurface = Color(0xFFE4EAE5),
    surfaceVariant = Color(0xFF253029),
    outlineVariant = Color(0xFF3B4940),
)

@Composable
fun RotasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

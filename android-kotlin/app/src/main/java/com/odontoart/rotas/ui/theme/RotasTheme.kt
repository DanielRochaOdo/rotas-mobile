package com.odontoart.rotas.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RotasSea = Color(0xFF00C573)
val RotasSeaLight = Color(0xFF3ECF8E)
val RotasInk = Color(0xFF10211A)
val RotasMuted = Color(0xFF3F5A50)
val RotasMutedSoft = Color(0xFF678277)
val RotasCanvas = Color(0xFFF6FAF7)
val RotasCanvasDeep = Color(0xFFF1F6F3)
val RotasSurface = Color(0xFFFFFFFF)
val RotasSand = Color(0xFFF1F6F3)
val RotasMist = Color(0xFFD5E1DB)
val RotasBorder = Color(0xFFC4D3CB)

private val LightColors = lightColorScheme(
    primary = RotasSea,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF7EB),
    onPrimaryContainer = Color(0xFF063C27),
    secondary = RotasSeaLight,
    onSecondary = RotasInk,
    secondaryContainer = Color(0xFFE9F6EF),
    onSecondaryContainer = RotasInk,
    background = RotasCanvas,
    onBackground = RotasInk,
    surface = RotasSurface,
    onSurface = RotasInk,
    surfaceVariant = RotasSand,
    onSurfaceVariant = RotasMuted,
    outline = RotasBorder,
    outlineVariant = RotasMist,
    error = Color(0xFFB91C1C),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColors = darkColorScheme(
    primary = RotasSeaLight,
    onPrimary = Color(0xFF062B1C),
    primaryContainer = Color(0xFF123E2B),
    onPrimaryContainer = Color(0xFFD9FBE9),
    secondary = RotasSea,
    onSecondary = Color(0xFF071A12),
    secondaryContainer = Color(0xFF1F3129),
    onSecondaryContainer = Color(0xFFFAFAFA),
    background = Color(0xFF171717),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF1F1F1F),
    onSurfaceVariant = Color(0xFFB4B4B4),
    outline = Color(0xFF363636),
    outlineVariant = Color(0xFF2E2E2E),
    error = Color(0xFFF87171),
    errorContainer = Color(0xFF451A1A),
    onErrorContainer = Color(0xFFFECACA),
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

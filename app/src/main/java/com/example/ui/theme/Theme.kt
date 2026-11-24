package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DataDexDarkColorScheme = darkColorScheme(
    primary = ScannerRed,
    onPrimary = Color.White,
    primaryContainer = ScannerRedContainer,
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = CyberCyan,
    onSecondary = Color(0xFF002026),
    secondaryContainer = CyberCyanContainer,
    onSecondaryContainer = Color(0xFFB3F7FF),
    tertiary = VoltGold,
    onTertiary = Color(0xFF281D00),
    tertiaryContainer = VoltGoldContainer,
    onTertiaryContainer = Color(0xFFFFECB8),
    background = DexVoid,
    onBackground = TextPrimary,
    surface = DexSurface,
    onSurface = TextPrimary,
    surfaceVariant = DexSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = DexVoid,
    surfaceContainerLow = DexSurface,
    surfaceContainer = DexSurfaceElevated,
    surfaceContainerHigh = DexSurfaceBright,
    surfaceContainerHighest = Color(0xFF2B3B5E),
    outline = DexOutline,
    outlineVariant = Color(0xFF1F2A44),
    error = Color(0xFFFF5C77),
    onError = Color.White
)

private val DataDexLightColorScheme = lightColorScheme(
    primary = Color(0xFFD91E41),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E0),
    onPrimaryContainer = Color(0xFF400010),
    secondary = Color(0xFF007A8C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC2F5FF),
    onSecondaryContainer = Color(0xFF002026),
    tertiary = Color(0xFFB37B00),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFCBD5E1)
)

val DataDexShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DataDexTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DataDexDarkColorScheme else DataDexLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DataDexTypography,
        shapes = DataDexShapes,
        content = content
    )
}

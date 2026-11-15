package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.model.ThemeMode

@Immutable
data class GlassColors(
    val isDark: Boolean,
    val gradientStops: List<Color>,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val orbTertiary: Color,
    val cardFill: Color,
    val cardFillElevated: Color,
    val cardBorderStart: Color,
    val cardBorderEnd: Color,
    val keypadButtonFill: Color,
    val navBarFill: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val success: Color,
    val warning: Color,
    val danger: Color
)

private val DarkGlassColors = GlassColors(
    isDark = true,
    gradientStops = listOf(DeepPurpleBg, MidnightBlueBg, DeepTealBg),
    orbPrimary = OrbPurple,
    orbSecondary = OrbBlue,
    orbTertiary = OrbTeal,
    cardFill = Color.White.copy(alpha = 0.11f),
    cardFillElevated = Color.White.copy(alpha = 0.17f),
    cardBorderStart = Color.White.copy(alpha = 0.34f),
    cardBorderEnd = Color.White.copy(alpha = 0.08f),
    keypadButtonFill = Color.White.copy(alpha = 0.14f),
    navBarFill = Color(0xFF0E1630).copy(alpha = 0.78f),
    textPrimary = DarkOnSurface,
    textSecondary = DarkOnSurfaceMuted,
    accentPrimary = CyanAccent,
    accentSecondary = IndigoAccent,
    success = EmeraldSuccess,
    warning = AmberWarning,
    danger = RoseDanger
)

private val LightGlassColors = GlassColors(
    isDark = false,
    gradientStops = listOf(LightLavenderBg, LightSkyBg, LightMintBg),
    orbPrimary = LightOrbPurple,
    orbSecondary = LightOrbBlue,
    orbTertiary = LightOrbTeal,
    cardFill = Color.White.copy(alpha = 0.62f),
    cardFillElevated = Color.White.copy(alpha = 0.78f),
    cardBorderStart = Color.White.copy(alpha = 0.92f),
    cardBorderEnd = Color(0xFF94A3B8).copy(alpha = 0.25f),
    keypadButtonFill = Color.White.copy(alpha = 0.68f),
    navBarFill = Color.White.copy(alpha = 0.82f),
    textPrimary = LightOnSurface,
    textSecondary = LightOnSurfaceMuted,
    accentPrimary = Color(0xFF0D9488),
    accentSecondary = Color(0xFF4F46E5),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
    danger = Color(0xFFE11D48)
)

val LocalGlassColors = staticCompositionLocalOf { DarkGlassColors }

private val DarkMaterialColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = Color(0xFF042F2E),
    secondary = IndigoAccent,
    onSecondary = Color.White,
    tertiary = SkyAccent,
    background = DeepPurpleBg,
    onBackground = DarkOnSurface,
    surface = Color(0xFF141B38),
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF1D274D),
    onSurfaceVariant = DarkOnSurfaceMuted,
    error = RoseDanger,
    onError = Color.White
)

private val LightMaterialColorScheme = lightColorScheme(
    primary = Color(0xFF0D9488),
    onPrimary = Color.White,
    secondary = Color(0xFF4F46E5),
    onSecondary = Color.White,
    tertiary = Color(0xFF0284C7),
    background = LightLavenderBg,
    onBackground = LightOnSurface,
    surface = Color.White,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = LightOnSurfaceMuted,
    error = Color(0xFFE11D48),
    onError = Color.White
)

val GlassShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun GlassLockTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val glassColors = if (useDark) DarkGlassColors else LightGlassColors
    val colorScheme = if (useDark) DarkMaterialColorScheme else LightMaterialColorScheme

    CompositionLocalProvider(LocalGlassColors provides glassColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GlassLockTypography,
            shapes = GlassShapes,
            content = content
        )
    }
}

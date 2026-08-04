package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    DARK, LIGHT, SYSTEM
}

private val SnapSweepDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = CyanPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = EmeraldKeep,
    onSecondary = Color.Black,
    tertiary = RoseTrash,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder
)

private val SnapSweepLightColorScheme = lightColorScheme(
    primary = CyanPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = CyanPrimary,
    onPrimaryContainer = Color.Black,
    secondary = EmeraldKeep,
    onSecondary = Color.White,
    tertiary = RoseTrash,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

@Composable
fun SnapSweepTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) SnapSweepDarkColorScheme else SnapSweepLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


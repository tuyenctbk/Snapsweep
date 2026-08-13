package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// SnapSweep Midnight Slate & Neon Accent Palette (Dark Mode Base Colors)
val DarkBackgroundPalette = Color(0xFF0B0F19)
val DarkSurfacePalette = Color(0xFF151C2C)
val DarkSurfaceVariantPalette = Color(0xFF1E293B)
val DarkBorderPalette = Color(0xFF334155)

// SnapSweep Bright Clean Palette (Light Mode Base Colors)
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightBorder = Color(0xFFE2E8F0)

val CyanPrimary = Color(0xFF38BDF8)
val CyanPrimaryDark = Color(0xFF0284C7)
val EmeraldKeep = Color(0xFF10B981)
val RoseTrash = Color(0xFFF43F5E)
val AmberWarning = Color(0xFFF59E0B)
val PurpleAccent = Color(0xFFA855F7)

val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkTextMuted = Color(0xFF64748B)

val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF475569)
val LightTextMuted = Color(0xFF64748B)

// Dynamic theme-aware getters for UI components
val DarkBackground: Color
    @Composable get() = MaterialTheme.colorScheme.background

val DarkSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

val DarkSurfaceVariant: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceVariant

val DarkBorder: Color
    @Composable get() = MaterialTheme.colorScheme.outline

val TextPrimary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurface

val TextSecondary: Color
    @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextMuted: Color
    @Composable get() = if (MaterialTheme.colorScheme.background == LightBackground) LightTextMuted else DarkTextMuted

package com.shawkinsrobertson.noguts.ui.theme

import androidx.compose.ui.graphics.Color

// Full brand palette, matching the mascot artwork's navy/teal/cream/red identity. Every
// Material color role used anywhere in the app is defined explicitly in Theme.kt from
// these - an undefined role silently falls back to Material3's default purple baseline,
// which is what produced the inconsistent look before this file existed.

// Dark theme (primary mode - the brand is dark-first)
val DarkBackground = Color(0xFF0D171C)
val DarkOnBackground = Color(0xFFF2EEE3)
val DarkSurface = Color(0xFF142027)
val DarkOnSurface = Color(0xFFF2EEE3)
val DarkSurfaceVariant = Color(0xFF1F2C33)
val DarkOnSurfaceVariant = Color(0xFFB8C2C6)

val DarkPrimary = Color(0xFF4FB894)
val DarkOnPrimary = Color(0xFF063127)
val DarkPrimaryContainer = Color(0xFF1E4A3E)
val DarkOnPrimaryContainer = Color(0xFFBFEEDD)

val DarkSecondary = Color(0xFFC9BBA0)
val DarkOnSecondary = Color(0xFF2B2013)
val DarkSecondaryContainer = Color(0xFF3A2E22)
val DarkOnSecondaryContainer = Color(0xFFEFE2C9)

val DarkTertiary = Color(0xFFD1685C)
val DarkOnTertiary = Color(0xFF3A0E09)
val DarkTertiaryContainer = Color(0xFF5C231C)
val DarkOnTertiaryContainer = Color(0xFFFFDAD3)

val DarkError = Color(0xFFE2685C)
val DarkOnError = Color(0xFF3A0A05)
val DarkErrorContainer = Color(0xFF5C231C)
val DarkOnErrorContainer = Color(0xFFFFDAD3)

val DarkOutline = Color(0xFF5B6167)
val DarkOutlineVariant = Color(0xFF3A4046)

// Light theme
val LightBackground = Color(0xFFFBF8F1)
val LightOnBackground = Color(0xFF191C1A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF191C1A)
val LightSurfaceVariant = Color(0xFFE7E1D2)
val LightOnSurfaceVariant = Color(0xFF4B4A41)

val LightPrimary = Color(0xFF2E7D6B)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFB7DED3)
val LightOnPrimaryContainer = Color(0xFF06201A)

val LightSecondary = Color(0xFF6F5B3E)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFF4E4C6)
val LightOnSecondaryContainer = Color(0xFF251A05)

val LightTertiary = Color(0xFFB3443A)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDAD3)
val LightOnTertiaryContainer = Color(0xFF3A0805)

val LightError = Color(0xFFB3261E)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFF9DEDC)
val LightOnErrorContainer = Color(0xFF410E0B)

val LightOutline = Color(0xFF7C7869)
val LightOutlineVariant = Color(0xFFCDC6B4)

// Absolute gauge bands (plan section 14) - independent of the personal target marker,
// and independent of the primary/tertiary brand colors above.
val GaugeGreen = Color(0xFF3E9C6B)
val GaugeYellow = Color(0xFFE3A526)
val GaugeRed = Color(0xFFD1543E)

val GaugeGreenDark = Color(0xFF5CBE8A)
val GaugeYellowDark = Color(0xFFF2BE55)
val GaugeRedDark = Color(0xFFE47563)

// Fixed brand color for the launch moment (matches @color/splash_background so the
// system splash screen and the in-app branded loading screen read as one continuous
// transition, regardless of the device's light/dark setting).
val SplashBackground = Color(0xFF0D171C)
val SplashOnBackground = Color(0xFFF5EFE0)
val SplashAccent = Color(0xFFB3443A)

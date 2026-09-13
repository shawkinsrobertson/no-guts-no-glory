package com.shawkinsrobertson.noguts.ui.theme

import androidx.compose.ui.graphics.Color

// In-app Material palette seed (independent of the launcher icon's own dark navy card).
val BrandTeal = Color(0xFF2E7D6B)
val BrandTealLight = Color(0xFFB7DED3)
val BrandTealDark = Color(0xFF1A4B40)

val LightBackground = Color(0xFFFBFDFB)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1A1C1B)

val DarkBackground = Color(0xFF10130F)
val DarkSurface = Color(0xFF1A1F1B)
val DarkOnSurface = Color(0xFFE2E3E0)

// Absolute gauge bands (plan section 14) - independent of the personal target marker.
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

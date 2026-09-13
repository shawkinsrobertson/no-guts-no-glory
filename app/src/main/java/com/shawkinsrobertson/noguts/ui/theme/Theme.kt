package com.shawkinsrobertson.noguts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.shawkinsrobertson.noguts.data.datastore.ThemeMode

private val LightColors = lightColorScheme(
    primary = BrandTeal,
    onPrimary = LightSurface,
    primaryContainer = BrandTealLight,
    onPrimaryContainer = BrandTealDark,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface
)

private val DarkColors = darkColorScheme(
    primary = BrandTealLight,
    onPrimary = BrandTealDark,
    primaryContainer = BrandTealDark,
    onPrimaryContainer = BrandTealLight,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface
)

@Composable
fun NoGutsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (useDarkTheme) DarkColors else LightColors
    val gaugeColors = if (useDarkTheme) DarkGaugeColors else LightGaugeColors

    CompositionLocalProvider(LocalGaugeColors provides gaugeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NoGutsTypography,
            content = content
        )
    }
}

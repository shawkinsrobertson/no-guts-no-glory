package com.shawkinsrobertson.noguts.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.shawkinsrobertson.noguts.scoring.LoadTier

/**
 * The absolute gauge band colors, kept separate from the Material color scheme since they
 * carry fixed meaning (green/yellow/red) that must not shift with the rest of the palette.
 */
data class GaugeColors(
    val green: Color,
    val yellow: Color,
    val red: Color
) {
    fun forTier(tier: LoadTier): Color = when (tier) {
        LoadTier.GREEN -> green
        LoadTier.YELLOW -> yellow
        LoadTier.RED -> red
    }
}

val LightGaugeColors = GaugeColors(green = GaugeGreen, yellow = GaugeYellow, red = GaugeRed)
val DarkGaugeColors = GaugeColors(green = GaugeGreenDark, yellow = GaugeYellowDark, red = GaugeRedDark)

val LocalGaugeColors = staticCompositionLocalOf { LightGaugeColors }

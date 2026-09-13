package com.shawkinsrobertson.noguts.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

const val ONBOARDING_ROUTE = "onboarding"

sealed class NoGutsDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : NoGutsDestination("dashboard", "Home", Icons.Filled.Home)
    data object Logbook : NoGutsDestination("logbook", "Logbook", Icons.Filled.MenuBook)
    data object Stats : NoGutsDestination("stats", "Stats", Icons.Filled.BarChart)
    data object Settings : NoGutsDestination("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val bottomNavItems = listOf(Dashboard, Logbook, Stats, Settings)
    }
}

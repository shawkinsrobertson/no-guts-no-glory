package com.shawkinsrobertson.noguts.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

const val ONBOARDING_ROUTE = "onboarding"

sealed class NoGutsDestination(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : NoGutsDestination("dashboard", "Home", Icons.Filled.Home) {
        const val argDate = "date"
        val routeWithArgs = "$route?$argDate={$argDate}"
        fun createRoute(date: java.time.LocalDate) = "$route?$argDate=$date"
    }
    data object Logbook : NoGutsDestination("logbook", "Logbook", Icons.AutoMirrored.Filled.MenuBook)
    data object Stats : NoGutsDestination("stats", "Stats", Icons.Filled.BarChart)
    data object Settings : NoGutsDestination("settings", "Settings", Icons.Filled.Settings)
}

// Deliberately NOT a member of NoGutsDestination's companion object: a companion
// initializer that references sibling nested objects of the same sealed class hits a
// circular class-initialization order bug on the JVM (initializing e.g. Dashboard forces
// its superclass NoGutsDestination to initialize first, which constructs Companion, whose
// list references Dashboard again while it's still mid-initialization - the JVM doesn't
// re-enter that init, it just hands back the not-yet-assigned field, i.e. null). A
// top-level property here lives in a separate compiled class with no such ordering
// dependency, so it's only ever evaluated once the sealed hierarchy is fully loaded.
val bottomNavDestinations: List<NoGutsDestination> = listOf(
    NoGutsDestination.Dashboard,
    NoGutsDestination.Logbook,
    NoGutsDestination.Stats,
    NoGutsDestination.Settings
)

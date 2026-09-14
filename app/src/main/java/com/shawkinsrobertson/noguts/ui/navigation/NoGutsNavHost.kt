package com.shawkinsrobertson.noguts.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import com.shawkinsrobertson.noguts.ui.dashboard.DashboardScreen
import com.shawkinsrobertson.noguts.ui.logbook.LogbookScreen
import com.shawkinsrobertson.noguts.ui.onboarding.OnboardingScreen
import com.shawkinsrobertson.noguts.ui.settings.SettingsScreen
import com.shawkinsrobertson.noguts.ui.stats.StatsScreen

/**
 * The app's single top-level graph: onboarding first, then the four-tab main experience.
 * There's no back-navigation into onboarding once it's complete - it pops itself off the
 * stack on finish.
 */
@Composable
fun NoGutsNavHost(startAtOnboarding: Boolean) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = if (startAtOnboarding) ONBOARDING_ROUTE else NoGutsDestination.Dashboard.route
    ) {
        composable(ONBOARDING_ROUTE) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(NoGutsDestination.Dashboard.route) {
                        popUpTo(ONBOARDING_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        composable(NoGutsDestination.Dashboard.route) { MainScaffold(navController, NoGutsDestination.Dashboard) { DashboardScreen() } }
        composable(NoGutsDestination.Logbook.route) { MainScaffold(navController, NoGutsDestination.Logbook) { LogbookScreen() } }
        composable(NoGutsDestination.Stats.route) { MainScaffold(navController, NoGutsDestination.Stats) { StatsScreen() } }
        composable(NoGutsDestination.Settings.route) { MainScaffold(navController, NoGutsDestination.Settings) { SettingsScreen() } }
    }
}

@Composable
private fun MainScaffold(
    navController: androidx.navigation.NavHostController,
    current: NoGutsDestination,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                bottomNavDestinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (destination.route != current.route) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { androidx.compose.material3.Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.padding(innerPadding)) {
            content()
        }
    }
}

package com.shawkinsrobertson.noguts.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shawkinsrobertson.noguts.ui.dashboard.DashboardScreen
import com.shawkinsrobertson.noguts.ui.logbook.LogbookScreen
import com.shawkinsrobertson.noguts.ui.onboarding.OnboardingScreen
import com.shawkinsrobertson.noguts.ui.settings.SettingsScreen
import com.shawkinsrobertson.noguts.ui.stats.StatsScreen
import java.time.LocalDate

/**
 * The app's single top-level graph.
 * 
 * Refactored to use a single top-level Scaffold. This ensures the bottom navigation bar
 * remains consistent across screen transitions and correctly handles navigation state,
 * including the ability to "reset" to today from a historical log view.
 */
@Composable
fun NoGutsNavHost(startAtOnboarding: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Don't show the bottom bar on the onboarding screen
    val isOnboarding = currentDestination?.hierarchy?.any { it.route == ONBOARDING_ROUTE } == true

    Scaffold(
        bottomBar = {
            if (!isOnboarding) {
                NavigationBar {
                    bottomNavDestinations.forEach { destination ->
                        // Match against base route OR the route with args pattern for the Dashboard
                        val isSelected = currentDestination?.hierarchy?.any { 
                            it.route == destination.route || 
                            (destination == NoGutsDestination.Dashboard && it.route == NoGutsDestination.Dashboard.routeWithArgs)
                        } == true
                        
                        NavigationBarItem(
                            selected = isSelected,
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            onClick = {
                                // If we're on a historical dashboard and click the Home tab, we want to go back to "Today"
                                val isHistoricalDashboard = destination == NoGutsDestination.Dashboard && 
                                        isSelected &&
                                        navBackStackEntry?.arguments?.getString(NoGutsDestination.Dashboard.argDate) != null
                                
                                if (!isSelected || isHistoricalDashboard) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        // If resetting to "Today", we DON'T restore the previous (historical) state
                                        restoreState = !isHistoricalDashboard
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (startAtOnboarding) ONBOARDING_ROUTE else NoGutsDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
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
            
            composable(
                route = NoGutsDestination.Dashboard.routeWithArgs,
                arguments = listOf(navArgument(NoGutsDestination.Dashboard.argDate) {
                    type = NavType.StringType
                    nullable = true
                })
            ) { backStackEntry ->
                val dateString = backStackEntry.arguments?.getString(NoGutsDestination.Dashboard.argDate)
                val date = dateString?.let { LocalDate.parse(it) } ?: LocalDate.now()
                DashboardScreen(
                    logDate = date,
                    onReturnToLogbook = {
                        navController.navigate(NoGutsDestination.Logbook.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            
            composable(NoGutsDestination.Logbook.route) {
                LogbookScreen(onNavigateToDashboard = { date ->
                    navController.navigate(NoGutsDestination.Dashboard.createRoute(date))
                })
            }
            
            composable(NoGutsDestination.Stats.route) {
                StatsScreen()
            }
            
            composable(NoGutsDestination.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

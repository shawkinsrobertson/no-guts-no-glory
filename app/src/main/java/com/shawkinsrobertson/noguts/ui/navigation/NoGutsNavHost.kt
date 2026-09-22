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

    // The historical/edit-a-missed-day screen is the same Dashboard composable/route as
    // "Home", distinguished only by its date argument - so this has to be checked from
    // the actual back stack entry's arguments, not from the destination's route pattern
    // (that's the same string, "dashboard?date={date}", whether or not a date was
    // supplied). Computed once and reused by both the selected-tab highlight below and
    // the "tapping Home from a historical day resets to today" behavior in its onClick.
    val onDashboardRoute = currentDestination?.hierarchy?.any { it.route == NoGutsDestination.Dashboard.routeWithArgs } == true
    val isOnHistoricalDashboard = onDashboardRoute &&
        navBackStackEntry?.arguments?.getString(NoGutsDestination.Dashboard.argDate) != null

    Scaffold(
        bottomBar = {
            if (!isOnboarding) {
                NavigationBar {
                    bottomNavDestinations.forEach { destination ->
                        val isSelected = if (destination == NoGutsDestination.Dashboard) {
                            onDashboardRoute && !isOnHistoricalDashboard
                        } else {
                            currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        }

                        NavigationBarItem(
                            selected = isSelected,
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            onClick = {
                                // If we're on a historical dashboard and click the Home tab, we want to go back to "Today"
                                val isHistoricalDashboard = destination == NoGutsDestination.Dashboard && isOnHistoricalDashboard

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
                    // A plain pop, not a tab-switch navigate(): this screen only ever gets
                    // pushed from Logbook (see onNavigateToDashboard below), so Logbook is
                    // always the entry directly underneath on the back stack. The tab-switch
                    // popUpTo(startDestinationId)/saveState/restoreState pattern used by the
                    // bottom nav doesn't work here because this screen shares its NavGraph
                    // node (and therefore its id) with the Home tab's "today" destination -
                    // popUpTo would be searching for the very node it's already sitting on.
                    onReturnToLogbook = { navController.popBackStack() }
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

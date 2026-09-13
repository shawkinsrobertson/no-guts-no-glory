package com.shawkinsrobertson.noguts.ui.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Implemented fully in the Dashboard + daily logging milestone.
@Composable
fun DashboardScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Dashboard")
    }
}

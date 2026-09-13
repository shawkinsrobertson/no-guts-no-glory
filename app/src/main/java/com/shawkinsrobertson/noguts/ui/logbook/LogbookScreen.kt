package com.shawkinsrobertson.noguts.ui.logbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

// Implemented fully in the Logbook + Stats milestone.
@Composable
fun LogbookScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Logbook")
    }
}

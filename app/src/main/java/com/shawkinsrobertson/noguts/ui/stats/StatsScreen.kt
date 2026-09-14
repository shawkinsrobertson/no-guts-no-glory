package com.shawkinsrobertson.noguts.ui.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shawkinsrobertson.noguts.ui.LocalAppContainer
import com.shawkinsrobertson.noguts.ui.SimpleViewModelFactory
import com.shawkinsrobertson.noguts.ui.components.TrendLineChart
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val MIN_POINTS_FOR_TREND = 3

@Composable
fun StatsScreen() {
    val container = LocalAppContainer.current
    val viewModel: StatsViewModel = viewModel(
        factory = SimpleViewModelFactory { StatsViewModel(container.dailyLogRepository, container.factorRepository) }
    )
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.loading) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TrendCard(uiState) }
        item { MostCommonFactorCard(uiState.mostCommonFactor) }
    }
}

@Composable
private fun TrendCard(uiState: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("72-hour load trend", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            if (uiState.trendPoints.size < MIN_POINTS_FOR_TREND) {
                Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Your trend is still taking shape.", textAlign = TextAlign.Center)
                        Text("Log three days to see your 72-hour pattern.", textAlign = TextAlign.Center)
                    }
                }
            } else {
                TrendLineChart(uiState.trendPoints, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    uiState.trendPoints.forEach { point ->
                        Text(
                            point.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Solid: 72-hour load · Dotted: your target", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MostCommonFactorCard(mostCommon: MostCommonFactor?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Most common this week", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            if (mostCommon == null) {
                Text("Nothing is standing out yet.")
                Text("Keep logging and we'll start looking for patterns.")
            } else {
                Text(mostCommon.name, style = MaterialTheme.typography.headlineMedium)
                Text("Selected on ${mostCommon.selectionCount} of ${mostCommon.loggedDayCount} logged days")
            }
        }
    }
}

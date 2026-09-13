package com.shawkinsrobertson.noguts.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.scoring.InputType
import com.shawkinsrobertson.noguts.scoring.LoadTier
import com.shawkinsrobertson.noguts.scoring.loadTierFor
import com.shawkinsrobertson.noguts.ui.LocalAppContainer
import com.shawkinsrobertson.noguts.ui.SimpleViewModelFactory
import com.shawkinsrobertson.noguts.ui.components.RadialLoadGauge
import com.shawkinsrobertson.noguts.ui.theme.LocalGaugeColors
import java.time.LocalTime

@Composable
fun DashboardScreen() {
    val container = LocalAppContainer.current
    val viewModel: DashboardViewModel = viewModel(
        factory = SimpleViewModelFactory {
            DashboardViewModel(container.dailyLogRepository, container.factorRepository, container.userPreferencesRepository)
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.loading) return

    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { GreetingHeader(uiState.greetingName) }
        item { LoadGaugeCard(uiState) }
        if (uiState.drivingFactors.isNotEmpty()) {
            item { DrivingFactorsCard(uiState.drivingFactors) }
        }
        item {
            if (uiState.isEditingToday) {
                CheckInSection(uiState, viewModel)
            } else {
                SavedTodaySummary(onEdit = viewModel::startEditingToday)
            }
        }
    }
}

@Composable
private fun GreetingHeader(name: String) {
    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Text(
        text = if (name.isBlank()) greeting else "$greeting, $name",
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
private fun LoadGaugeCard(uiState: DashboardUiState) {
    val snapshot = uiState.latestSnapshot
    val gaugeColors = LocalGaugeColors.current
    val rollingPercent = snapshot?.rolling72Percent

    Card {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            val tier = rollingPercent?.let { loadTierFor(it) } ?: LoadTier.GREEN
            RadialLoadGauge(
                percent = rollingPercent,
                tierColor = gaugeColors.forTier(tier),
                targetPercent = snapshot?.targetPercent
            ) {
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    if (rollingPercent != null) {
                        Text("${rollingPercent.toInt()}%", style = MaterialTheme.typography.displayLarge)
                    } else {
                        Text("--", style = MaterialTheme.typography.displayLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (rollingPercent != null) {
                Text("72-hour stomach load", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                snapshot?.targetPercent?.let { target ->
                    Text("Target: ${target.toInt()}%", style = MaterialTheme.typography.bodyMedium)
                    if (rollingPercent > target) {
                        Text("Above your target", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else if (snapshot != null) {
                Text(
                    "Building your baseline",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    "${snapshot.daysInRollingWindow} of 3 days logged",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    "Log your first day to see your load",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun DrivingFactorsCard(drivingFactors: List<Pair<String, Double>>) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("What's driving your load", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            drivingFactors.forEach { (name, percent) ->
                Text("$name — ${percent.toInt()}%", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun CheckInSection(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    Column {
        Text("Today's check-in", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        FactorGrid(uiState.quickFactors, uiState.selections, viewModel)

        if (uiState.moreFactors.isNotEmpty()) {
            TextButton(onClick = viewModel::toggleShowMoreFactors) {
                Text(if (uiState.showMoreFactors) "Fewer factors" else "More factors ▾")
            }
            if (uiState.showMoreFactors) {
                FactorGrid(uiState.moreFactors, uiState.selections, viewModel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Anything worth remembering?", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = uiState.notes,
            onValueChange = viewModel::updateNotes,
            placeholder = { Text("Anything unusual about today?") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = viewModel::saveToday, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(if (uiState.todayAlreadyLogged) "Save changes" else "Log my day")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = viewModel::logNothingNotable, enabled = !uiState.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text("Nothing notable")
        }
    }
}

@Composable
private fun FactorGrid(
    factors: List<FactorEntity>,
    selections: Map<Long, FactorSelectionState>,
    viewModel: DashboardViewModel
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height((((factors.size + 1) / 2) * 80).dp)
    ) {
        items(factors, key = { it.id }) { factor ->
            val state = selections[factor.id] ?: FactorSelectionState.NotSelected
            FactorCard(
                factor = factor,
                selectionState = state,
                onClick = {
                    if (factor.inputType == InputType.BOOLEAN) {
                        viewModel.toggleBooleanFactor(factor.id)
                    } else {
                        viewModel.cycleLevelFactor(factor.id)
                    }
                }
            )
        }
    }
}

@Composable
private fun SavedTodaySummary(onEdit: () -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Today's log saved", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onEdit) {
                Text("Edit today's log")
            }
        }
    }
}

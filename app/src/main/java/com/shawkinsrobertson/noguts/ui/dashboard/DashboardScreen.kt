package com.shawkinsrobertson.noguts.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.shawkinsrobertson.noguts.ui.components.CollapsibleSection
import com.shawkinsrobertson.noguts.ui.components.RadialLoadGauge
import com.shawkinsrobertson.noguts.ui.components.formatPoints
import com.shawkinsrobertson.noguts.ui.theme.LocalGaugeColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    logDate: LocalDate = LocalDate.now(),
    onReturnToLogbook: () -> Unit = {}
) {
    val container = LocalAppContainer.current
    val viewModel: DashboardViewModel = viewModel(
        key = logDate.toString(),
        factory = SimpleViewModelFactory {
            DashboardViewModel(
                container.dailyLogRepository,
                container.factorRepository,
                container.userPreferencesRepository,
                logDate
            )
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.loading) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { GreetingHeader(uiState) }
        item { LoadGaugeCard(uiState) }
        item { TodaysLoadCard(uiState) }
        item {
            if (uiState.isEditingToday) {
                CheckInSection(uiState, viewModel)
            } else {
                SavedTodaySummary(uiState, onEdit = viewModel::startEditingToday, onReturn = onReturnToLogbook)
            }
        }
    }
}

@Composable
private fun GreetingHeader(uiState: DashboardUiState) {
    if (uiState.logDate != LocalDate.now()) {
        Text(
            text = "Logging for ${uiState.logDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))}",
            style = MaterialTheme.typography.headlineMedium
        )
        return
    }

    val hour = LocalTime.now().hour
    val greeting = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Text(
        text = if (uiState.greetingName.isBlank()) greeting else "$greeting, ${uiState.greetingName}",
        style = MaterialTheme.typography.headlineMedium
    )
}

@Composable
private fun LoadGaugeCard(uiState: DashboardUiState) {
    val snapshot = uiState.latestSnapshot
    val gaugeColors = LocalGaugeColors.current
    // The gauge's fill proportion and target tick are still geometry against the
    // normalized percentage (that's what makes a 270-degree dial meaningful) - only the
    // number shown at its center switches to raw points.
    val rollingPercent = snapshot?.rolling72Percent
    val rollingPoints = snapshot?.rolling72Load
    val targetPoints = snapshot?.targetPercent?.let { uiState.maxPossibleDailyLoad * it / 100.0 }

    Card(modifier = Modifier.fillMaxWidth()) {
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
                    if (rollingPoints != null) {
                        Text(rollingPoints.formatPoints(), style = MaterialTheme.typography.displayLarge)
                    } else {
                        Text("--", style = MaterialTheme.typography.displayLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (rollingPoints != null) {
                Text("72-hour stomach load", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                targetPoints?.let { target ->
                    Text("Target: ${target.formatPoints()}", style = MaterialTheme.typography.bodyMedium)
                    if (rollingPoints > target) {
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
private fun TodaysLoadCard(uiState: DashboardUiState) {
    val label = if (uiState.logDate == LocalDate.now()) "Today's load" else "Daily load"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                uiState.todaysLoadPoints?.formatPoints() ?: "Not logged yet",
                style = if (uiState.todaysLoadPoints != null) MaterialTheme.typography.displayLarge else MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun CheckInSection(uiState: DashboardUiState, viewModel: DashboardViewModel) {
    var stressorsExpanded by rememberSaveable { mutableStateOf(true) }
    var recoveryExpanded by rememberSaveable { mutableStateOf(true) }
    var symptomsExpanded by rememberSaveable { mutableStateOf(true) }

    val sectionTitle = if (uiState.logDate == LocalDate.now()) "Today's check-in" else "Check-in"

    Column {
        Text(sectionTitle, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.stressorFactors.isNotEmpty()) {
            CollapsibleSection("Stressors", stressorsExpanded, { stressorsExpanded = !stressorsExpanded }) {
                FactorGrid(uiState.stressorFactors, uiState.selections, viewModel)
            }
        }
        if (uiState.recoveryFactors.isNotEmpty()) {
            CollapsibleSection("Recovery", recoveryExpanded, { recoveryExpanded = !recoveryExpanded }) {
                FactorGrid(uiState.recoveryFactors, uiState.selections, viewModel)
            }
        }
        if (uiState.symptomFactors.isNotEmpty()) {
            CollapsibleSection("Symptoms", symptomsExpanded, { symptomsExpanded = !symptomsExpanded }) {
                FactorGrid(uiState.symptomFactors, uiState.selections, viewModel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Anything worth remembering?", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FactorGrid(
    factors: List<FactorEntity>,
    selections: Map<Long, FactorSelectionState>,
    viewModel: DashboardViewModel
) {
    FlowRow(
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        factors.forEach { factor ->
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
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SavedTodaySummary(uiState: DashboardUiState, onEdit: () -> Unit, onReturn: () -> Unit) {
    val isToday = uiState.logDate == LocalDate.now()
    val label = if (isToday) "Today's log saved" else "Log saved"
    val buttonLabel = if (isToday) "Edit today's log" else "Edit log"
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = onEdit) {
                    Text(buttonLabel)
                }
                
                if (!isToday) {
                    OutlinedButton(onClick = onReturn) {
                        Text("Return to logbook")
                    }
                }
            }
        }
    }
}

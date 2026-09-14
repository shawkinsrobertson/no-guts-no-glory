package com.shawkinsrobertson.noguts.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shawkinsrobertson.noguts.notifications.ReminderScheduler
import com.shawkinsrobertson.noguts.ui.LocalAppContainer
import com.shawkinsrobertson.noguts.ui.SimpleViewModelFactory

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: OnboardingViewModel = viewModel(
        factory = SimpleViewModelFactory {
            OnboardingViewModel(container.factorRepository, container.userPreferencesRepository)
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) {
            ReminderScheduler(context).applyPreferences(
                uiState.reminderEnabled,
                uiState.reminderHour,
                uiState.reminderMinute
            )
            onFinished()
        }
    }

    if (uiState.loading) return

    // safeDrawingPadding keeps this screen clear of the status bar / cutouts / nav bar -
    // it's the only screen in the app not already wrapped in a Scaffold (which handles
    // that for the four main tabs itself).
    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        LinearProgressIndicator(
            progress = { (uiState.step + 1) / ONBOARDING_STEP_COUNT.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.weight(1f)) {
            when (uiState.step) {
                0 -> IntroStep()
                1 -> FactorSelectionStep(uiState, viewModel)
                2 -> WeightStep(uiState, viewModel)
                3 -> TargetAndReminderStep(uiState, viewModel)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (uiState.step > 0) {
                TextButton(onClick = viewModel::previousStep) { Text("Back") }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            Button(
                onClick = {
                    if (uiState.step == ONBOARDING_STEP_COUNT - 1) viewModel.finish() else viewModel.nextStep()
                },
                enabled = !uiState.isSaving
            ) {
                Text(if (uiState.step == ONBOARDING_STEP_COUNT - 1) "Finish" else "Continue")
            }
        }
    }
}

@Composable
private fun IntroStep() {
    Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
        Text("Let's learn what affects your stomach.", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "You'll pick a few things that tend to affect you, get a quick daily check-in, " +
                "and start seeing your own patterns over time. This is a personal tracking " +
                "tool, not a medical device."
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FactorSelectionStep(uiState: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Choose the things you want to track", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "We've pre-selected a few common ones - tap any card to add or remove it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.trackableFactors.forEach { factor ->
                    OnboardingFactorChip(
                        name = factor.name,
                        selected = factor.id in uiState.selectedFactorIds,
                        onClick = { viewModel.toggleFactor(factor.id) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = uiState.customFactorDraftName,
                onValueChange = viewModel::updateCustomFactorDraftName,
                label = { Text("+ Add your own") },
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = viewModel::addCustomFactor) { Text("Add") }
        }
    }
}

/** A fixed-height, two-line-capable selectable card - every card in the grid matches
 * size regardless of label length, which a plain FilterChip (sized to its own text) does not. */
@Composable
private fun OnboardingFactorChip(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun WeightStep(uiState: OnboardingUiState, viewModel: OnboardingViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("How much does this tend to affect your stomach?", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(uiState.selectedFactors, key = { it.id }) { factor ->
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(factor.name)
                        val weight = uiState.weights[factor.id] ?: 5
                        Slider(
                            value = weight.toFloat(),
                            onValueChange = { viewModel.updateWeight(factor.id, it.toInt()) },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                        Text("$weight / 10", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun TargetAndReminderStep(uiState: OnboardingUiState, viewModel: OnboardingViewModel) {
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Your suggested target", style = MaterialTheme.typography.titleLarge)
        Text("${uiState.suggestedTargetPercent.toInt()}%", style = MaterialTheme.typography.displayLarge)
        Text("This is a starting point for your personal tracking system, not a medical threshold.")
        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Button(onClick = viewModel::chooseSuggestedTarget) {
                Text("Use ${uiState.suggestedTargetPercent.toInt()}%")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = { viewModel.chooseCustomTarget(uiState.chosenTargetPercent) }) {
                Text("Set my own target")
            }
        }
        if (uiState.useCustomTarget) {
            Slider(
                value = uiState.chosenTargetPercent.toFloat(),
                onValueChange = { viewModel.chooseCustomTarget(it.toDouble()) },
                valueRange = 10f..90f
            )
            Text("${uiState.chosenTargetPercent.toInt()}%")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Want a daily reminder?", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = uiState.reminderEnabled,
                onCheckedChange = { enabled ->
                    viewModel.setReminderEnabled(enabled)
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Remind me at ${"%02d:%02d".format(uiState.reminderHour, uiState.reminderMinute)}")
        }
    }
}

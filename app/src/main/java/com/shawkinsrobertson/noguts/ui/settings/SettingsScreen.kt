package com.shawkinsrobertson.noguts.ui.settings

import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shawkinsrobertson.noguts.data.datastore.ThemeMode
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.notifications.ReminderScheduler
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.scoring.InputType
import com.shawkinsrobertson.noguts.ui.LocalAppContainer
import com.shawkinsrobertson.noguts.ui.SimpleViewModelFactory

@Composable
fun SettingsScreen() {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel(
        factory = SimpleViewModelFactory {
            SettingsViewModel(container.userPreferencesRepository, container.factorRepository, container.csvExportRepository)
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.pendingShareUris) {
        val uris = uiState.pendingShareUris ?: return@LaunchedEffect
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export your data"))
        viewModel.shareHandled()
    }

    if (uiState.loading) return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ProfileSection(uiState, viewModel) }
        item { FactorsSection(uiState, viewModel) }
        item { TargetSection(uiState, viewModel) }
        item { ReminderSection(uiState, viewModel) }
        item { AppearanceSection(uiState, viewModel) }
        item { DataSection(uiState, viewModel) }
        item { AboutSection() }
        uiState.message?.let { message ->
            item {
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ProfileSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard("Profile") {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::updateName,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FactorsSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    var newFactorName by remember { mutableStateOf("") }

    SectionCard("Your factors") {
        uiState.factors.groupBy { it.category }.forEach { (category, factors) ->
            Text(category.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge)
            factors.forEach { factor -> FactorRow(factor, viewModel) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        OutlinedTextField(
            value = newFactorName,
            onValueChange = { newFactorName = it },
            label = { Text("+ Add your own") },
            modifier = Modifier.fillMaxWidth()
        )
        TextButton(onClick = {
            viewModel.addFactor(newFactorName, FactorCategory.LOAD, InputType.BOOLEAN, weight = 5.0)
            newFactorName = ""
        }) { Text("Add") }
    }
}

@Composable
private fun FactorRow(factor: FactorEntity, viewModel: SettingsViewModel) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(factor.name, modifier = Modifier.weight(1f))
            Switch(checked = factor.active, onCheckedChange = { viewModel.toggleFactorActive(factor) })
            if (!factor.isSystemDefault) {
                IconButton(onClick = { viewModel.deleteFactor(factor) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete ${factor.name}")
                }
            }
        }
        if (factor.active && factor.category != FactorCategory.SYMPTOM) {
            Slider(
                value = factor.weight.toFloat(),
                onValueChange = { viewModel.updateFactorWeight(factor, it.toDouble()) },
                valueRange = 1f..10f,
                steps = 8
            )
        }
    }
}

@Composable
private fun TargetSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard("Your target") {
        Text("${uiState.targetPercent.toInt()}%", style = MaterialTheme.typography.headlineMedium)
        Slider(
            value = uiState.targetPercent.toFloat(),
            onValueChange = { viewModel.updateTargetPercent(it.toDouble()) },
            valueRange = 10f..90f
        )
    }
}

@Composable
private fun ReminderSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    val context = LocalContext.current

    SectionCard("Reminder") {
        Row {
            Text("Daily reminder", modifier = Modifier.weight(1f))
            Switch(
                checked = uiState.reminderEnabled,
                onCheckedChange = { enabled ->
                    viewModel.updateReminder(enabled, uiState.reminderHour, uiState.reminderMinute)
                    ReminderScheduler(context).applyPreferences(enabled, uiState.reminderHour, uiState.reminderMinute)
                }
            )
        }
        if (uiState.reminderEnabled) {
            TextButton(onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        viewModel.updateReminder(true, hour, minute)
                        ReminderScheduler(context).applyPreferences(true, hour, minute)
                    },
                    uiState.reminderHour,
                    uiState.reminderMinute,
                    false
                ).show()
            }) {
                Text("Remind me at ${"%02d:%02d".format(uiState.reminderHour, uiState.reminderMinute)}")
            }
            if (!ReminderScheduler(context).canScheduleExactAlarms()) {
                TextButton(onClick = { context.startActivity(ReminderScheduler(context).exactAlarmSettingsIntent()) }) {
                    Text("Allow exact alarms for a precise reminder time")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard("Appearance") {
        SingleChoiceSegmentedButtonRow {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = uiState.theme == mode,
                    onClick = { viewModel.updateTheme(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size)
                ) {
                    Text(mode.name.lowercase().replaceFirstChar { it.uppercase() })
                }
            }
        }
    }
}

@Composable
private fun DataSection(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    SectionCard("Data") {
        Button(onClick = viewModel::exportCsv, enabled = !uiState.isExporting) {
            Text("Export CSV")
        }
    }
}

@Composable
private fun AboutSection() {
    SectionCard("About") {
        Text(
            "This app is a personal tracking tool. Its load score is not a medical " +
                "measurement or diagnosis.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

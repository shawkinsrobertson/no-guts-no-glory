package com.shawkinsrobertson.noguts.ui.logbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shawkinsrobertson.noguts.data.db.entity.DailyLogStatus
import com.shawkinsrobertson.noguts.scoring.FactorCategory
import com.shawkinsrobertson.noguts.ui.LocalAppContainer
import com.shawkinsrobertson.noguts.ui.SimpleViewModelFactory
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

@Composable
fun LogbookScreen() {
    val container = LocalAppContainer.current
    val viewModel: LogbookViewModel = viewModel(
        factory = SimpleViewModelFactory { LogbookViewModel(container.dailyLogRepository) }
    )
    val entries by viewModel.entries.collectAsState()
    var selected by remember { mutableStateOf<LogbookEntry?>(null) }

    if (entries.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Nothing logged yet.", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(entries, key = { it.loggedDay.date }) { entry ->
            LogbookRow(entry, onClick = { selected = entry })
        }
    }

    selected?.let { entry ->
        DayDetailDialog(entry, onDismiss = { selected = null })
    }
}

@Composable
private fun LogbookRow(entry: LogbookEntry, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(entry.loggedDay.date.format(DATE_FORMATTER), style = MaterialTheme.typography.titleLarge)
            entry.snapshot?.rolling72Percent?.let { percent ->
                Text("72h load: ${percent.toInt()}%", style = MaterialTheme.typography.bodyMedium)
            }
            if (entry.loggedDay.status == DailyLogStatus.LOGGED && entry.loggedDay.factors.isEmpty()) {
                Text("Nothing notable", style = MaterialTheme.typography.bodyMedium)
            } else {
                entry.loggedDay.factors
                    .filter { it.factor.category != FactorCategory.SYMPTOM }
                    .forEach { Text(it.factor.name, style = MaterialTheme.typography.bodyMedium) }
            }
            entry.loggedDay.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(4.dp))
                Text("“$notes”", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun DayDetailDialog(entry: LogbookEntry, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(entry.loggedDay.date.format(DATE_FORMATTER), style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(12.dp))
                entry.snapshot?.let { snapshot ->
                    Text("Today's load: ${snapshot.normalizedDailyPercent.toInt()}%")
                    snapshot.rolling72Percent?.let { Text("72-hour load: ${it.toInt()}%") }
                    Text("Target at the time: ${snapshot.targetPercent.toInt()}%")
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (entry.loggedDay.factors.isEmpty()) {
                    Text("Nothing notable was logged.")
                } else {
                    Text("What was recorded", style = MaterialTheme.typography.titleLarge)
                    entry.loggedDay.factors.forEach { detail ->
                        val levelText = detail.level?.let { " (${it.name.lowercase()})" } ?: ""
                        Text("• ${detail.factor.name}$levelText")
                    }
                }
                entry.loggedDay.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Notes", style = MaterialTheme.typography.titleLarge)
                    Text(notes)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    }
}

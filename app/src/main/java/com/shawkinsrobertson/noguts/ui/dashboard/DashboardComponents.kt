package com.shawkinsrobertson.noguts.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.scoring.InputType

@Composable
fun FactorCard(
    factor: FactorEntity,
    selectionState: FactorSelectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = selectionState !is FactorSelectionState.NotSelected
    Card(
        onClick = onClick,
        modifier = modifier.wrapContentHeight(),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
        border = if (!selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                androidx.compose.foundation.layout.Column {
                    Text(factor.name, style = MaterialTheme.typography.bodyLarge)
                    if (factor.inputType == InputType.LEVEL && selectionState is FactorSelectionState.LevelSelected) {
                        Text(
                            selectionState.level.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

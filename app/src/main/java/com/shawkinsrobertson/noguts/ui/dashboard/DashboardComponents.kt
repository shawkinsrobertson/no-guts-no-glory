package com.shawkinsrobertson.noguts.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shawkinsrobertson.noguts.data.db.entity.FactorEntity
import com.shawkinsrobertson.noguts.scoring.IntensityLevel

private val CARD_HEIGHT = 64.dp

/** How "full" a selection reads visually - selection and intensity are both communicated
 * through this fraction (interpolated container/border color, no checkmark icon), so every
 * card stays exactly [CARD_HEIGHT] regardless of what's selected. */
private fun FactorSelectionState.fillFraction(): Float = when (this) {
    is FactorSelectionState.NotSelected -> 0f
    is FactorSelectionState.BooleanSelected -> 1f
    is FactorSelectionState.LevelSelected -> when (level) {
        IntensityLevel.NONE -> 0f
        IntensityLevel.MILD -> 0.4f
        IntensityLevel.MODERATE -> 0.7f
        IntensityLevel.SEVERE -> 1f
    }
}

/** Color alone doesn't tell a mild/moderate/severe factor which of the three it's on, or
 * make it obvious that tapping through severe is what un-selects it - so a level factor
 * gets this short word alongside its name. Card height stays fixed either way. */
private fun FactorSelectionState.levelLabel(): String? = when (this) {
    is FactorSelectionState.LevelSelected -> when (level) {
        IntensityLevel.NONE -> null
        IntensityLevel.MILD -> "Mild"
        IntensityLevel.MODERATE -> "Moderate"
        IntensityLevel.SEVERE -> "Severe"
    }
    else -> null
}

@Composable
fun FactorCard(
    factor: FactorEntity,
    selectionState: FactorSelectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fillFraction = selectionState.fillFraction()
    val selected = fillFraction > 0f
    val levelLabel = selectionState.levelLabel()

    val containerColor = lerp(
        MaterialTheme.colorScheme.surfaceContainerLow,
        MaterialTheme.colorScheme.primaryContainer,
        fillFraction
    )
    val borderColor = lerp(MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.primary, fillFraction)
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        onClick = onClick,
        modifier = modifier.height(CARD_HEIGHT),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    factor.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (levelLabel != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        levelLabel,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

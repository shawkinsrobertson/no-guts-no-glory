package com.shawkinsrobertson.noguts.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
 * purely through this fraction (interpolated container/border color), never an icon or
 * extra text line, so every card stays exactly [CARD_HEIGHT] regardless of what's selected. */
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

@Composable
fun FactorCard(
    factor: FactorEntity,
    selectionState: FactorSelectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fillFraction = selectionState.fillFraction()
    val selected = fillFraction > 0f

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
            Text(
                factor.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

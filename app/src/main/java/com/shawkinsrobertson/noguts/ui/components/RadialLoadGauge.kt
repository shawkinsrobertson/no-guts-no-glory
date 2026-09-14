package com.shawkinsrobertson.noguts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

private const val START_ANGLE_DEG = 135f
private const val SWEEP_MAX_DEG = 270f

/**
 * A 270-degree radial gauge for the normalized load percentage, with an optional tick
 * mark for the user's personal target (plan section 14: the target is an independent
 * marker from the absolute green/yellow/red bands, drawn here in a neutral color rather
 * than a tier color).
 *
 * [percent] null renders just the empty track, for the "no data yet" / "building your
 * baseline" states where a specific number would be misleading.
 */
@Composable
fun RadialLoadGauge(
    percent: Double?,
    tierColor: Color,
    targetPercent: Double?,
    modifier: Modifier = Modifier,
    centerContent: @Composable () -> Unit
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val markerColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val strokeWidthPx = 20.dp.toPx()
            val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
            val topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)

            drawArc(
                color = trackColor,
                startAngle = START_ANGLE_DEG,
                sweepAngle = SWEEP_MAX_DEG,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                size = arcSize,
                topLeft = topLeft
            )

            if (percent != null) {
                val sweep = (percent.coerceIn(0.0, 100.0) / 100.0 * SWEEP_MAX_DEG).toFloat()
                drawArc(
                    color = tierColor,
                    startAngle = START_ANGLE_DEG,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                    size = arcSize,
                    topLeft = topLeft
                )
            }

            if (targetPercent != null) {
                val angleDeg = START_ANGLE_DEG + (targetPercent.coerceIn(0.0, 100.0) / 100.0 * SWEEP_MAX_DEG).toFloat()
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val center = Offset(size.width / 2, size.height / 2)
                val outerRadius = size.minDimension / 2
                val innerRadius = outerRadius - strokeWidthPx * 1.6f
                val outer = Offset(
                    center.x + outerRadius * cos(angleRad).toFloat(),
                    center.y + outerRadius * sin(angleRad).toFloat()
                )
                val inner = Offset(
                    center.x + innerRadius * cos(angleRad).toFloat(),
                    center.y + innerRadius * sin(angleRad).toFloat()
                )
                drawLine(color = markerColor, start = inner, end = outer, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        centerContent()
    }
}

package com.shawkinsrobertson.noguts.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.shawkinsrobertson.noguts.ui.stats.TrendPoint

/**
 * The 7-day trend from plan section 27: a solid line for the 72-hour rolling load and a
 * dotted line for the personal target at each point in time (each [TrendPoint] carries
 * the target that was actually in effect on that day, so a mid-week target change shows
 * up as a step rather than being rewritten across history).
 *
 * Plotted in raw points rather than the normalized percentage, so unlike a percent scale
 * the y-axis has no fixed ceiling - it auto-scales to whatever this week's highest value
 * (load or target) actually is, with a little headroom so a line never touches the top edge.
 */
@Composable
fun TrendLineChart(points: List<TrendPoint>, modifier: Modifier = Modifier) {
    val loadColor = MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        if (points.size < 2) return@Canvas

        val highestValue = points.maxOf { maxOf(it.rolling72Load, it.targetLoad) }
        val maxY = (highestValue * 1.1).coerceAtLeast(1.0).toFloat()
        val stepX = size.width / (points.size - 1)
        fun yFor(value: Double): Float = size.height - (value.toFloat() / maxY * size.height)

        val loadPath = Path()
        val targetPath = Path()
        points.forEachIndexed { index, point ->
            val x = stepX * index
            val loadY = yFor(point.rolling72Load)
            val targetY = yFor(point.targetLoad)
            if (index == 0) {
                loadPath.moveTo(x, loadY)
                targetPath.moveTo(x, targetY)
            } else {
                loadPath.lineTo(x, loadY)
                targetPath.lineTo(x, targetY)
            }
        }

        drawPath(targetPath, color = targetColor, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))))
        drawPath(loadPath, color = loadColor, style = Stroke(width = 4.dp.toPx()))
    }
}

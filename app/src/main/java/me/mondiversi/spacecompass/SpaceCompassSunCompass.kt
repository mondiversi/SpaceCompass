package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.*

@Composable
internal fun SpaceCompassSunFinderCompass(
    orientation: SpaceCompassSunOrientation?, reliable: Boolean, tint: Color, modifier: Modifier
) {
    val description = if (reliable) SpaceCompassSunCompassLabels.joinToString(", ")
        else stringResource(R.string.sun_finder_compass_accuracy)
    Canvas(modifier.testTag("sun-finder-compass").semantics {
        contentDescription = description
    }) {
        val radius = size.minDimension * 0.43f
        val ink = tint.copy(alpha = if (reliable) 1f else 0.52f)
        drawCircle(Brush.radialGradient(listOf(ink.copy(alpha = 0.02f), ink.copy(alpha = 0.09f)),
            center, radius), radius, center)
        drawCircle(ink.copy(alpha = 0.45f), radius, center, style = Stroke(1.dp.toPx()))
        if (orientation != null) {
            (SpaceCompassSunCompassParallels + SpaceCompassSunCompassMeridians).forEach {
                drawCompassCurve(it, orientation, radius, ink, horizon = false)
            }
            drawCompassCurve(SpaceCompassSunCompassHorizon, orientation, radius, ink, horizon = true)
        }
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            // The compact diagram follows its size, while all numeric data retains normal font scaling.
            textSize = size.minDimension * 0.115f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val occupied = mutableListOf<Rect>()
        val gap = 2.dp.toPx()
        val halfHeight = (paint.descent() - paint.ascent()) / 2
        val reticle = Rect(center.x - 9.dp.toPx(), center.y - 9.dp.toPx(),
            center.x + 9.dp.toPx(), center.y + 9.dp.toPx())
        // Give cardinal points priority when diagonals converge near the sphere's rim.
        if (orientation != null && reliable) listOf(0, 2, 4, 6, 1, 3, 5, 7).forEach { index ->
            val label = SpaceCompassSunCompassLabels[index]
            val p = projectSpaceCompassSunCompass(spaceCompassSunCompassDirection(index * 45.0), orientation)
            // Rear-facing labels are omitted, avoiding overlapping opposite cardinal points.
            if (p.depth >= -0.05) {
                val north = index == 0 && reliable
                paint.color = (if (north) Color(0xFFE46050) else ink).toArgb()
                var point = center + Offset((p.x * radius * 0.82).toFloat(), (p.y * radius * 0.82).toFloat())
                val halfWidth = paint.measureText(label) / 2
                fun bounds() = Rect(point.x - halfWidth - gap, point.y - halfHeight - gap,
                    point.x + halfWidth + gap, point.y + halfHeight + gap)
                // Keep the forward cardinal label clear of the fixed pointing reticle.
                if (bounds().overlaps(reticle)) point = point.copy(y = reticle.top - halfHeight - gap)
                val box = bounds()
                if (occupied.none { it.overlaps(box) }) {
                    occupied.add(box)
                    drawContext.canvas.nativeCanvas.drawText(label, point.x,
                        point.y - (paint.ascent() + paint.descent()) / 2, paint)
                }
            }
        }
        // Fixed reticle = rear-camera pointing axis, not the screen's upper edge.
        drawCircle(ink, 2.dp.toPx(), center, style = Stroke(1.dp.toPx()))
        for (axis in listOf(Offset(1f, 0f), Offset(-1f, 0f), Offset(0f, 1f), Offset(0f, -1f)))
            drawLine(ink, center + axis * 4.dp.toPx(), center + axis * 7.dp.toPx(), 1.dp.toPx(), StrokeCap.Round)
    }
}

private fun DrawScope.drawCompassCurve(
    curve: List<SpaceCompassSunVector>, orientation: SpaceCompassSunOrientation, radius: Float, tint: Color, horizon: Boolean
) {
    val front = Path()
    val back = Path()
    for (index in 1 until curve.size) {
        val a = projectSpaceCompassSunCompass(curve[index - 1], orientation)
        val b = projectSpaceCompassSunCompass(curve[index], orientation)
        val path = if ((a.depth + b.depth) / 2 >= 0) front else back
        path.moveTo(center.x + (a.x * radius).toFloat(), center.y + (a.y * radius).toFloat())
        path.lineTo(center.x + (b.x * radius).toFloat(), center.y + (b.y * radius).toFloat())
    }
    drawPath(back, tint.copy(alpha = tint.alpha * 0.10f), style = Stroke(0.65.dp.toPx()))
    drawPath(front, tint.copy(alpha = tint.alpha * if (horizon) 0.75f else 0.25f),
        style = Stroke((if (horizon) 1.3.dp else 0.65.dp).toPx()))
}

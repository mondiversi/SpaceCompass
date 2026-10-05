package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** A 22 dp canvas compensates for the tube/tripod's inset geometry; stroke stays 1.5 dp. */
@Composable
internal fun SpaceCompassCelestialTelescopeIcon(modifier: Modifier) {
    Canvas(modifier.testTag("celestial-telescope-icon")) {
        fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
        val stroke = 1.5.dp.toPx()
        val tube = Path().apply {
            moveTo(size.width * 0.18f, size.height * 0.36f)
            lineTo(size.width * 0.71f, size.height * 0.12f)
            lineTo(size.width * 0.83f, size.height * 0.37f)
            lineTo(size.width * 0.30f, size.height * 0.61f); close()
        }
        drawPath(tube, Color.White, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(Color.White, point(0.20f, 0.51f), point(0.08f, 0.57f), stroke, StrokeCap.Round)
        drawLine(Color.White, point(0.52f, 0.54f), point(0.52f, 0.72f), stroke, StrokeCap.Round)
        for (foot in listOf(point(0.28f, 0.91f), point(0.52f, 0.91f), point(0.76f, 0.91f)))
            drawLine(Color.White, point(0.52f, 0.70f), foot, stroke, StrokeCap.Round)
    }
}

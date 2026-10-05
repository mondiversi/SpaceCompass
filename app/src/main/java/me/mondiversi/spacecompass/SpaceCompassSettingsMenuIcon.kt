package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** The three settings glyphs match UVIR's existing visual vocabulary. */
@Composable
internal fun SpaceCompassSettingsMenuIcon(route: String, tint: Color) {
    Canvas(Modifier.size(20.dp)) {
        val strokeWidth = maxOf(1.6.dp.toPx(), size.minDimension * 0.08f)
        when (route) {
            "capture" -> {
                val camera = Path().apply {
                    moveTo(size.width * .10f, size.height * .30f)
                    lineTo(size.width * .30f, size.height * .30f)
                    lineTo(size.width * .38f, size.height * .18f)
                    lineTo(size.width * .62f, size.height * .18f)
                    lineTo(size.width * .70f, size.height * .30f)
                    lineTo(size.width * .90f, size.height * .30f)
                    lineTo(size.width * .90f, size.height * .80f)
                    lineTo(size.width * .10f, size.height * .80f)
                    close()
                }
                drawPath(camera, tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                drawCircle(tint, size.width * .16f, Offset(size.width * .50f, size.height * .55f), style = Stroke(strokeWidth))
            }
            "appearance" -> {
                val palette = Path().apply {
                    moveTo(size.width * 0.53f, size.height * 0.12f)
                    cubicTo(size.width * 0.28f, size.height * 0.07f,
                        size.width * 0.10f, size.height * 0.26f,
                        size.width * 0.10f, size.height * 0.50f)
                    cubicTo(size.width * 0.10f, size.height * 0.75f,
                        size.width * 0.31f, size.height * 0.89f,
                        size.width * 0.56f, size.height * 0.88f)
                    cubicTo(size.width * 0.69f, size.height * 0.88f,
                        size.width * 0.72f, size.height * 0.77f,
                        size.width * 0.64f, size.height * 0.69f)
                    cubicTo(size.width * 0.58f, size.height * 0.63f,
                        size.width * 0.61f, size.height * 0.54f,
                        size.width * 0.71f, size.height * 0.54f)
                    cubicTo(size.width * 0.94f, size.height * 0.57f,
                        size.width * 0.94f, size.height * 0.28f,
                        size.width * 0.75f, size.height * 0.17f)
                    cubicTo(size.width * 0.69f, size.height * 0.13f,
                        size.width * 0.61f, size.height * 0.11f,
                        size.width * 0.53f, size.height * 0.12f)
                    close()
                }
                drawPath(palette, tint, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                for ((x, y) in listOf(0.32f to 0.33f, 0.53f to 0.27f,
                    0.74f to 0.37f, 0.27f to 0.55f)) {
                    drawCircle(tint, radius = size.minDimension * 0.055f,
                        center = Offset(size.width * x, size.height * y))
                }
            }
            "language" -> {
                drawCircle(
                    color = tint,
                    radius = size.minDimension * 0.37f,
                    center = Offset(
                        size.width * 0.50f,
                        size.height * 0.50f
                    ),
                    style = Stroke(width = strokeWidth)
                )
                drawOval(
                    color = tint,
                    topLeft = Offset(
                        size.width * 0.34f,
                        size.height * 0.13f
                    ),
                    size = Size(
                        size.width * 0.32f,
                        size.height * 0.74f
                    ),
                    style = Stroke(width = strokeWidth)
                )
                listOf(0.36f, 0.64f).forEach { y ->
                    drawLine(
                        color = tint,
                        start = Offset(
                            size.width * 0.16f,
                            size.height * y
                        ),
                        end = Offset(
                            size.width * 0.84f,
                            size.height * y
                        ),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }
            "units" -> {
                drawLine(
                    color = tint,
                    start = Offset(
                        size.width * 0.20f,
                        size.height * 0.28f
                    ),
                    end = Offset(
                        size.width * 0.20f,
                        size.height * 0.74f
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(
                        size.width * 0.12f,
                        size.height * 0.36f
                    ),
                    end = Offset(
                        size.width * 0.20f,
                        size.height * 0.28f
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = tint,
                    radius = strokeWidth * 0.85f,
                    center = Offset(
                        size.width * 0.43f,
                        size.height * 0.69f
                    )
                )
                drawOval(
                    color = tint,
                    topLeft = Offset(
                        size.width * 0.58f,
                        size.height * 0.27f
                    ),
                    size = Size(
                        size.width * 0.28f,
                        size.height * 0.48f
                    ),
                    style = Stroke(width = strokeWidth)
                )
            }

        }
    }
}

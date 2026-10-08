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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Compact menu glyphs follow the app's existing visual vocabulary. */
@Composable
internal fun SpaceCompassSettingsMenuIcon(
    route: String,
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp),
    iconStrokeWidth: Dp? = null,
    crossed: Boolean = false
) {
    Canvas(modifier) {
        val strokeWidth = iconStrokeWidth?.toPx() ?: maxOf(1.6.dp.toPx(), size.minDimension * 0.08f)
        when (route) {
            "capture" -> {
                drawCircle(tint, size.minDimension * .39f, style = Stroke(strokeWidth))
                repeat(6) { index ->
                    val angle = Math.toRadians(index * 60.0)
                    val next = angle + Math.toRadians(60.0)
                    drawLine(tint, Offset(center.x + kotlin.math.cos(angle).toFloat() * size.width * .38f,
                        center.y + kotlin.math.sin(angle).toFloat() * size.height * .38f),
                        Offset(center.x + kotlin.math.cos(next).toFloat() * size.width * .16f,
                            center.y + kotlin.math.sin(next).toFloat() * size.height * .16f), strokeWidth)
                }
            }
            "observer" -> {
                // A place pin and clock distinguish location/time scenarios from live GPS.
                val pin = Path().apply {
                    moveTo(size.width * .31f, size.height * .67f)
                    cubicTo(size.width * .24f, size.height * .57f,
                        size.width * .10f, size.height * .43f,
                        size.width * .10f, size.height * .31f)
                    cubicTo(size.width * .10f, size.height * .04f,
                        size.width * .52f, size.height * .04f,
                        size.width * .52f, size.height * .31f)
                    cubicTo(size.width * .52f, size.height * .43f,
                        size.width * .38f, size.height * .57f,
                        size.width * .31f, size.height * .67f)
                    close()
                }
                drawPath(pin, tint, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                drawCircle(tint, size.minDimension * .065f,
                    Offset(size.width * .31f, size.height * .30f))
                val clockCenter = Offset(size.width * .70f, size.height * .67f)
                drawCircle(tint, size.minDimension * .23f, clockCenter,
                    style = Stroke(strokeWidth))
                drawLine(tint, clockCenter, Offset(size.width * .70f, size.height * .53f),
                    strokeWidth, StrokeCap.Round)
                drawLine(tint, clockCenter, Offset(size.width * .79f, size.height * .72f),
                    strokeWidth, StrokeCap.Round)
            }
            "info" -> {
                drawCircle(tint, size.minDimension * .39f, style = Stroke(strokeWidth))
                drawCircle(tint, strokeWidth * .7f, Offset(center.x, size.height * .31f))
                drawLine(tint, Offset(center.x, size.height * .45f), Offset(center.x, size.height * .70f),
                    strokeWidth, StrokeCap.Round)
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
        if (crossed) drawLine(tint,
            Offset(size.width * .125f, size.height * .125f),
            Offset(size.width * .875f, size.height * .875f),
            strokeWidth, StrokeCap.Round)
    }
}

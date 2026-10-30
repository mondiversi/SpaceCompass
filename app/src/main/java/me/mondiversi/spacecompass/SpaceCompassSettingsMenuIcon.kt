package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
    val strokeScale = LocalSpaceCompassFloatingStrokeScale.current
    SpaceCompassIconCanvas(tint, modifier) { tint ->
        val strokeWidth = (iconStrokeWidth?.toPx() ?: maxOf(1.6.dp.toPx(), size.minDimension * 0.08f)) * strokeScale
        when (route) {
            "capture" -> {
                val outerRadius = size.minDimension * .39f
                val apertureRadius = size.minDimension * .17f
                val vertices = List(6) { index ->
                    val angle = Math.toRadians(index * 60.0 - 30.0)
                    Offset(center.x + kotlin.math.cos(angle).toFloat() * apertureRadius,
                        center.y + kotlin.math.sin(angle).toFloat() * apertureRadius)
                }
                // Extend each aperture edge to the outer ring; shared vertices leave no loose blade ends.
                val extension = kotlin.math.sqrt(outerRadius * outerRadius - .75f * apertureRadius * apertureRadius) -
                    .5f * apertureRadius
                vertices.forEachIndexed { index, vertex ->
                    val previous = vertices[(index + vertices.lastIndex) % vertices.size]
                    val direction = (vertex - previous) / apertureRadius
                    drawLine(tint, vertex, vertex + direction * extension, strokeWidth, StrokeCap.Round)
                }
                val aperture = Path().apply {
                    moveTo(vertices.first().x, vertices.first().y)
                    vertices.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                drawPath(aperture, tint, style = Stroke(strokeWidth, join = StrokeJoin.Round))
                drawCircle(tint, outerRadius, style = Stroke(strokeWidth))
            }
            "panorama" -> {
                // A central crest and low endpoints suggest an unrolled orbital arc.
                // A lighter dashed horizon separates the above/below portions of the track.
                repeat(5) { index ->
                    val dashStart = size.width * (.06f + index * .196f)
                    drawLine(tint, Offset(dashStart, center.y),
                        Offset(dashStart + size.width * .096f, center.y), strokeWidth * .55f, StrokeCap.Round)
                }
                val orbit = Path().apply {
                    moveTo(size.width * .12f, size.height * .76f)
                    cubicTo(size.width * .29f, size.height * .76f, size.width * .30f, size.height * .22f,
                        center.x, size.height * .22f)
                    cubicTo(size.width * .70f, size.height * .22f, size.width * .71f, size.height * .76f,
                        size.width * .88f, size.height * .76f)
                }
                drawPath(orbit, tint, style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                listOf(Offset(.12f, .76f), Offset(.50f, .22f), Offset(.88f, .76f)).forEach { point ->
                    drawCircle(tint, strokeWidth * 1.25f, Offset(size.width * point.x, size.height * point.y))
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
                // A cog gives General a distinct silhouette from the main menu's horizontal bars.
                val gear = Path().apply {
                    repeat(8) { tooth ->
                        listOf(-22.5 to .32f, -11.25 to .42f, 11.25 to .42f, 22.5 to .32f)
                            .forEachIndexed { corner, (offset, radius) ->
                                val angle = Math.toRadians(tooth * 45.0 + offset)
                                val x = center.x + kotlin.math.cos(angle).toFloat() * size.minDimension * radius
                                val y = center.y + kotlin.math.sin(angle).toFloat() * size.minDimension * radius
                                if (tooth == 0 && corner == 0) moveTo(x, y) else lineTo(x, y)
                            }
                    }
                    close()
                }
                drawPath(gear, tint, style = Stroke(strokeWidth, join = StrokeJoin.Round))
                drawCircle(tint, size.minDimension * .14f, center, style = Stroke(strokeWidth))
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

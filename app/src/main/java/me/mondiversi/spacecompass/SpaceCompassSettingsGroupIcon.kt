package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/** Decorative section glyphs inherit the title color in both themes. */
@Composable
internal fun SpaceCompassSettingsGroupIcon(key: String, tint: Color) {
    if (key == "ambient_music") {
        Icon(painterResource(R.drawable.ic_music), contentDescription = null,
            modifier = Modifier.size(20.dp), tint = tint)
        return
    }
    if (key == "repository") {
        Icon(painterResource(R.drawable.ic_repository), contentDescription = null,
            modifier = Modifier.size(20.dp), tint = tint)
        return
    }
    Canvas(Modifier.size(20.dp)) {
        val strokeWidth = maxOf(1.6.dp.toPx(), size.minDimension * 0.08f)
        val outline = Stroke(strokeWidth, cap = StrokeCap.Round)
        fun point(x: Float, y: Float) = Offset(size.width * x, size.height * y)
        when (key) {
            "credits" -> {
                drawCircle(tint, size.minDimension * .37f, center, style = outline)
                drawArc(tint, 55f, 250f, false, point(.32f, .32f),
                    Size(size.width * .36f, size.height * .36f), style = outline)
            }
            "whats_new" -> {
                for ((x, y, radius) in listOf(Triple(.42f, .42f, .27f), Triple(.73f, .72f, .12f))) {
                    drawLine(tint, point(x, y - radius), point(x, y + radius), strokeWidth, StrokeCap.Round)
                    drawLine(tint, point(x - radius, y), point(x + radius, y), strokeWidth, StrokeCap.Round)
                }
            }
            "theme" -> {
                val radius = size.minDimension * 0.34f
                drawCircle(tint, radius, center, style = Stroke(width = strokeWidth))
                val innerRadius = radius - strokeWidth * 1.5f
                val half = Path().apply {
                    arcTo(Rect(center.x - innerRadius, center.y - innerRadius,
                        center.x + innerRadius, center.y + innerRadius), -90f, 180f, true)
                    close()
                }
                drawPath(half, tint)
            }
            "display", "screensaver" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.09f, size.height * 0.12f),
                    size = Size(size.width * 0.82f, size.height * 0.57f),
                    cornerRadius = CornerRadius(size.minDimension * 0.06f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(tint, Offset(size.width * 0.50f, size.height * 0.69f),
                    Offset(size.width * 0.50f, size.height * 0.87f), strokeWidth, StrokeCap.Round)
                if (key == "screensaver") {
                    drawCircle(tint, size.minDimension * .09f, point(.5f, .40f), style = outline)
                    drawLine(tint, point(.72f, .26f), point(.72f, .40f), strokeWidth, StrokeCap.Round)
                    drawLine(tint, point(.65f, .33f), point(.79f, .33f), strokeWidth, StrokeCap.Round)
                }
                drawLine(tint, Offset(size.width * 0.32f, size.height * 0.87f),
                    Offset(size.width * 0.68f, size.height * 0.87f), strokeWidth, StrokeCap.Round)
            }
            "date_format" -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.20f),
                    size = Size(size.width * 0.76f, size.height * 0.68f),
                    cornerRadius = CornerRadius(size.minDimension * 0.08f),
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    tint,
                    Offset(size.width * 0.12f, size.height * 0.40f),
                    Offset(size.width * 0.88f, size.height * 0.40f),
                    strokeWidth,
                    StrokeCap.Round
                )
                listOf(0.34f, 0.66f).forEach { x ->
                    drawLine(
                        tint,
                        Offset(size.width * x, size.height * 0.10f),
                        Offset(size.width * x, size.height * 0.29f),
                        strokeWidth,
                        StrokeCap.Round
                    )
                }
            }
            "time_format" -> {
                drawCircle(
                    color = tint,
                    radius = size.minDimension * 0.37f,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    tint,
                    center,
                    Offset(size.width * 0.50f, size.height * 0.29f),
                    strokeWidth,
                    StrokeCap.Round
                )
                drawLine(
                    tint,
                    center,
                    Offset(size.width * 0.68f, size.height * 0.60f),
                    strokeWidth,
                    StrokeCap.Round
                )
            }
            "numeric_format" -> {
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

            "speed" -> {
                drawArc(tint, 150f, 240f, false, point(.12f, .14f), Size(size.width * .76f, size.height * .76f), style = outline)
                drawLine(tint, point(.5f, .55f), point(.74f, .31f), strokeWidth, StrokeCap.Round)
                drawCircle(tint, size.minDimension * .06f, point(.5f, .55f))
            }
            "distance" -> {
                drawCircle(tint, size.minDimension * .25f, center, style = outline)
                drawOval(tint, point(.06f, .35f), Size(size.width * .88f, size.height * .3f), style = outline)
            }
            "altitude" -> {
                val mountain = Path().apply {
                    moveTo(size.width * .09f, size.height * .86f)
                    lineTo(size.width * .39f, size.height * .17f)
                    lineTo(size.width * .72f, size.height * .86f)
                    close()
                }
                drawPath(mountain, tint, style = outline)
                drawLine(tint, point(.59f, .60f), point(.72f, .34f), strokeWidth, StrokeCap.Round)
                drawLine(tint, point(.72f, .34f), point(.93f, .86f), strokeWidth, StrokeCap.Round)
                drawLine(tint, point(.72f, .86f), point(.93f, .86f), strokeWidth, StrokeCap.Round)
                drawLine(tint, point(.30f, .38f), point(.39f, .44f), strokeWidth, StrokeCap.Round)
                drawLine(tint, point(.39f, .44f), point(.49f, .38f), strokeWidth, StrokeCap.Round)
            }
            SPACE_COMPASS_DISTANCE_SPEED_UNIT_KEY -> {
                drawRoundRect(tint, point(.12f, .28f), Size(size.width * .76f, size.height * .44f), CornerRadius(2.dp.toPx()), style = outline)
                for (x in listOf(.3f, .5f, .7f)) drawLine(tint, point(x, .28f), point(x, .48f), strokeWidth)
            }
            SPACE_COMPASS_MASS_UNIT_KEY -> {
                drawCircle(tint, size.minDimension * .13f, point(.5f, .23f), style = outline)
                val weight = Path().apply {
                    moveTo(size.width * .28f, size.height * .39f)
                    lineTo(size.width * .72f, size.height * .39f)
                    lineTo(size.width * .85f, size.height * .85f)
                    lineTo(size.width * .15f, size.height * .85f)
                    close()
                }
                drawPath(weight, tint, style = outline)
            }
            SPACE_COMPASS_PRESSURE_UNIT_KEY -> {
                drawCircle(tint, size.minDimension * .36f, center, style = outline)
                for (x in listOf(.27f, .5f, .73f)) {
                    val y = if (x == .5f) .18f else .29f
                    drawLine(tint, point(x, y), point(x, y + .10f), strokeWidth, StrokeCap.Round)
                }
                drawLine(tint, center, point(.68f, .35f), strokeWidth, StrokeCap.Round)
                drawCircle(tint, size.minDimension * .06f, center)
            }
            "temperature" -> {
                drawRoundRect(tint, point(.4f, .1f), Size(size.width * .2f, size.height * .58f), CornerRadius(size.width * .1f), style = outline)
                drawCircle(tint, size.minDimension * .17f, point(.5f, .74f), style = outline)
                drawLine(tint, point(.5f, .34f), point(.5f, .74f), strokeWidth, StrokeCap.Round)
            }
            "coordinates" -> {
                drawCircle(tint, size.minDimension * .28f, center, style = outline)
                drawCircle(tint, size.minDimension * .07f, center)
                drawLine(tint, point(.5f, .05f), point(.5f, .25f), strokeWidth)
                drawLine(tint, point(.5f, .75f), point(.5f, .95f), strokeWidth)
                drawLine(tint, point(.05f, .5f), point(.25f, .5f), strokeWidth)
                drawLine(tint, point(.75f, .5f), point(.95f, .5f), strokeWidth)
            }
        }
    }
}

package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/** Compact decorative event vectors; the adjacent localized name supplies semantics. */
@Composable
internal fun SpaceCompassDailyPathEventIcons(point: SpaceCompassSunPathPoint, isCurrent: Boolean, color: Color,
    trend: SpaceCompassElevationTrend? = null) = Column(Modifier.width(12.dp)) {
    // Current has its original position marker; direction arrows belong to ordinary points.
    val keyPoint = point.events.any { it != SpaceCompassSunPathEvent.HOUR }
    if (isCurrent) {
        Canvas(Modifier.size(12.dp).testTag("daily-path-event-current").clearAndSetSemantics { }) {
            drawCircle(color, size.minDimension * .34f, style = Stroke(1.4.dp.toPx()))
            drawCircle(color, size.minDimension * .12f)
        }
    } else if (!keyPoint) trend?.let {
        val description = stringResource(if (it == SpaceCompassElevationTrend.RISING)
            R.string.celestial_elevation_rising else R.string.celestial_elevation_falling)
        Canvas(Modifier.size(12.dp).testTag("daily-path-trend-${it.name}")
            .clearAndSetSemantics { contentDescription = description }) {
            val top = size.height * .18f; val bottom = size.height * .82f
            val tip = if (it == SpaceCompassElevationTrend.RISING) top else bottom
            val tail = if (it == SpaceCompassElevationTrend.RISING) bottom else top
            val wing = tip + (tail - tip) * .38f
            drawLine(color, Offset(center.x, tail), Offset(center.x, tip), 1.6.dp.toPx(), StrokeCap.Round)
            val arrow = Path().apply {
                moveTo(size.width * .25f, wing); lineTo(center.x, tip); lineTo(size.width * .75f, wing)
            }
            drawPath(arrow, color, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round))
        }
    }
    if (!isCurrent) {
        // Match the caption's representative-event-first order, including coincident events.
        (listOf(point.event) + point.coincidentEvents.sortedBy { it.ordinal }).distinct()
            .filter { it != SpaceCompassSunPathEvent.HOUR }.forEach { event ->
                Canvas(Modifier.size(12.dp).testTag("daily-path-event-${event.name}").clearAndSetSemantics { }) {
                    val d = size.minDimension * .32f
                    val x = center.x; val y = center.y
                    if (event == SpaceCompassSunPathEvent.APPROACH) {
                        drawSpaceCompassApproachGlyph(center, size.minDimension * .30f, color, 1.4.dp.toPx())
                    } else if (event == SpaceCompassSunPathEvent.MINIMUM || event == SpaceCompassSunPathEvent.CULMINATION) {
                        drawLine(color, Offset(x - d, y), Offset(x + d, y), 1.8.dp.toPx(), StrokeCap.Round)
                        if (event == SpaceCompassSunPathEvent.CULMINATION)
                            drawLine(color, Offset(x, y - d), Offset(x, y + d), 1.8.dp.toPx(), StrokeCap.Round)
                    } else {
                        val glyph = Path().apply {
                            if (event == SpaceCompassSunPathEvent.SUNRISE) {
                                moveTo(x, y - d); lineTo(x + d, y + d); lineTo(x - d, y + d)
                            } else {
                                moveTo(x, y + d); lineTo(x + d, y - d); lineTo(x - d, y - d)
                            }
                            close()
                        }
                        drawPath(glyph, color)
                    }
                }
            }
    }
}

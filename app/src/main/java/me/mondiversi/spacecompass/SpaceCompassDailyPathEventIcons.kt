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
import androidx.compose.ui.graphics.StrokeJoin
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
    // Current keeps its position marker; elevation chevrons belong to ordinary points.
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
            val tip = size.height * if (it == SpaceCompassElevationTrend.RISING) .30f else .70f
            val wing = size.height - tip
            val chevron = Path().apply {
                moveTo(size.width * .20f, wing); lineTo(center.x, tip); lineTo(size.width * .80f, wing)
            }
            drawPath(chevron, color, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
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
                            spaceCompassHorizonEventGlyph(event, x, y, d).forEachIndexed { index, (px, py) ->
                                if (index == 0) moveTo(px, py) else lineTo(px, py)
                            }
                            close()
                        }
                        drawPath(glyph, color)
                    }
                }
            }
    }
}

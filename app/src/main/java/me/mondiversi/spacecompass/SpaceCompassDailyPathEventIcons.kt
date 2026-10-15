package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** Compact decorative event vectors; the adjacent localized name supplies semantics. */
@Composable
internal fun SpaceCompassDailyPathEventIcons(point: SpaceCompassSunPathPoint, isCurrent: Boolean, color: Color) {
    if (isCurrent) {
        Canvas(Modifier.size(14.dp).testTag("daily-path-event-current").clearAndSetSemantics { }) {
            drawCircle(color, size.minDimension * .34f, style = Stroke(1.4.dp.toPx()))
            drawCircle(color, size.minDimension * .12f)
        }
    } else {
        // Match the caption's representative-event-first order, including coincident events.
        (listOf(point.event) + point.coincidentEvents.sortedBy { it.ordinal }).distinct()
            .filter { it != SpaceCompassSunPathEvent.HOUR }.forEach { event ->
                Canvas(Modifier.size(14.dp).testTag("daily-path-event-${event.name}").clearAndSetSemantics { }) {
                    val d = size.minDimension * .32f
                    val x = center.x; val y = center.y
                    if (event == SpaceCompassSunPathEvent.MINIMUM || event == SpaceCompassSunPathEvent.CULMINATION) {
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

package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** Match daily-path extrema (+ / −); chevrons distinguish increasing/decreasing mid-height. */
@Composable
internal fun SpaceCompassObservationMomentIcon(kind: SpaceCompassObservationMomentKind, color: Color,
    modifier: Modifier = Modifier) {
    Canvas(modifier.size(12.dp).clearAndSetSemantics { }) {
        val d = size.minDimension * .32f
        if (kind == SpaceCompassObservationMomentKind.MONTHLY) {
            val left = size.width * .16f
            val top = size.height * .25f
            drawRoundRect(color, Offset(left, top), androidx.compose.ui.geometry.Size(size.width * .68f, size.height * .60f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()), style = Stroke(1.3.dp.toPx()))
            drawLine(color, Offset(left, size.height * .43f), Offset(size.width - left, size.height * .43f), 1.2.dp.toPx())
            for (x in listOf(.33f, .67f)) drawLine(color, Offset(size.width * x, size.height * .14f),
                Offset(size.width * x, size.height * .33f), 1.3.dp.toPx(), StrokeCap.Round)
            drawCircle(color, .7.dp.toPx(), Offset(center.x, size.height * .65f))
        } else if (kind == SpaceCompassObservationMomentKind.MAXIMUM || kind == SpaceCompassObservationMomentKind.MINIMUM) {
            drawLine(color, Offset(center.x - d, center.y), Offset(center.x + d, center.y),
                1.8.dp.toPx(), StrokeCap.Round)
            if (kind == SpaceCompassObservationMomentKind.MAXIMUM)
                drawLine(color, Offset(center.x, center.y - d), Offset(center.x, center.y + d),
                    1.8.dp.toPx(), StrokeCap.Round)
        } else {
            val tip = size.height * if (kind == SpaceCompassObservationMomentKind.MID_RISING) .30f else .70f
            val chevron = Path().apply {
                moveTo(size.width * .20f, size.height - tip)
                lineTo(center.x, tip)
                lineTo(size.width * .80f, size.height - tip)
            }
            drawPath(chevron, color, style = Stroke(1.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

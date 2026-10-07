package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Uvir's export-page Save-to-folder icon and familiar three-node Share glyph. */
@Composable
internal fun SpaceCompassPanoramaActionIcon(share: Boolean) {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val stroke = 2.21.dp.toPx()
        fun line(ax: Float, ay: Float, bx: Float, by: Float) = drawLine(tint,
            Offset(size.width * ax, size.height * ay), Offset(size.width * bx, size.height * by),
            stroke, StrokeCap.Round)
        if (share) {
            line(.28f, .50f, .70f, .27f); line(.28f, .50f, .70f, .73f)
            listOf(Offset(.28f, .50f), Offset(.70f, .27f), Offset(.70f, .73f)).forEach {
                drawCircle(tint, size.minDimension * .105f, Offset(it.x * size.width, it.y * size.height))
            }
        } else {
            val folder = Path().apply {
                moveTo(size.width * 3f / 24f, size.height * 6f / 24f)
                lineTo(size.width * 9f / 24f, size.height * 6f / 24f)
                lineTo(size.width * 11f / 24f, size.height * 8.5f / 24f)
                lineTo(size.width * 21f / 24f, size.height * 8.5f / 24f)
                lineTo(size.width * 21f / 24f, size.height * 19f / 24f)
                lineTo(size.width * 3f / 24f, size.height * 19f / 24f)
                close()
            }
            drawPath(folder, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            line(12f / 24f, 10.5f / 24f, 12f / 24f, 16f / 24f)
            line(9.5f / 24f, 13.5f / 24f, 12f / 24f, 16f / 24f)
            line(14.5f / 24f, 13.5f / 24f, 12f / 24f, 16f / 24f)
        }
    }
}

/** Uvir's upward export arrow and tray, including its optical vertical correction. */
@Composable
internal fun SpaceCompassPanoramaExportIcon() {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(24.dp).offset(y = (-1.5).dp)) {
        val stroke = 2.21.dp.toPx()
        fun line(ax: Float, ay: Float, bx: Float, by: Float) = drawLine(tint,
            Offset(size.width * ax, size.height * ay), Offset(size.width * bx, size.height * by),
            stroke, StrokeCap.Round)
        line(.20f, .72f, .20f, .88f); line(.20f, .88f, .80f, .88f); line(.80f, .88f, .80f, .72f)
        line(.50f, .63f, .50f, .25f); line(.50f, .25f, .35f, .39f); line(.50f, .25f, .65f, .39f)
    }
}

@Composable
internal fun SpaceCompassPanoramaGalleryIcon() {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val stroke = 2.dp.toPx()
        drawRoundRect(tint, Offset(size.width * .13f, size.height * .17f),
            androidx.compose.ui.geometry.Size(size.width * .74f, size.height * .66f),
            androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()), style = Stroke(stroke))
        drawCircle(tint, size.minDimension * .06f, Offset(size.width * .68f, size.height * .34f))
        val mountain = Path().apply {
            moveTo(size.width * .21f, size.height * .72f); lineTo(size.width * .42f, size.height * .44f)
            lineTo(size.width * .58f, size.height * .63f); lineTo(size.width * .68f, size.height * .53f)
            lineTo(size.width * .79f, size.height * .72f)
        }
        drawPath(mountain, tint, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

internal enum class SpaceCompassPanoramaControl { BACK, ZOOM_OUT, ZOOM_IN }

@Composable
internal fun SpaceCompassPanoramaControlIcon(control: SpaceCompassPanoramaControl) {
    val tint = LocalContentColor.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(Modifier.size(24.dp)) {
        val stroke = 2.21.dp.toPx()
        fun line(ax: Float, ay: Float, bx: Float, by: Float) = drawLine(tint,
            Offset(size.width * ax, size.height * ay), Offset(size.width * bx, size.height * by),
            stroke, StrokeCap.Round)
        if (control == SpaceCompassPanoramaControl.BACK) {
            // Apply the same optical correction as page chevrons, mirrored in RTL.
            val shift = if (rtl) .0375f else -.0375f
            val tip = (if (rtl) .64f else .36f) + shift
            val wing = (if (rtl) .34f else .66f) + shift
            line(wing, .22f, tip, .50f); line(tip, .50f, wing, .78f)
        } else {
            line(.25f, .50f, .75f, .50f)
            if (control == SpaceCompassPanoramaControl.ZOOM_IN) line(.50f, .25f, .50f, .75f)
        }
    }
}

/** Location disclosure control; a slash distinguishes the omitted-position variant. */
@Composable
internal fun SpaceCompassPanoramaPositionIcon(hidden: Boolean) {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val pin = Path().apply {
            moveTo(size.width * .5f, size.height * .91f)
            cubicTo(size.width * .30f, size.height * .68f, size.width * .19f, size.height * .51f,
                size.width * .19f, size.height * .37f)
            cubicTo(size.width * .19f, size.height * .01f, size.width * .81f, size.height * .01f,
                size.width * .81f, size.height * .37f)
            cubicTo(size.width * .81f, size.height * .51f, size.width * .70f, size.height * .68f,
                size.width * .5f, size.height * .91f)
            close()
        }
        drawPath(pin, tint, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(tint, size.minDimension * .105f, Offset(size.width * .5f, size.height * .37f),
            style = Stroke(1.7.dp.toPx()))
        if (hidden) drawLine(tint, Offset(size.width * .10f, size.height * .12f),
            Offset(size.width * .90f, size.height * .88f), 2.dp.toPx(), StrokeCap.Round)
    }
}


/** Label tag; the diagonal slash alone indicates hidden point labels. */
@Composable
internal fun SpaceCompassPanoramaLabelsIcon(shown: Boolean) {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val tag = Path().apply {
            moveTo(size.width * .16f, size.height * .16f)
            lineTo(size.width * .50f, size.height * .16f)
            lineTo(size.width * .86f, size.height * .52f)
            lineTo(size.width * .52f, size.height * .86f)
            lineTo(size.width * .16f, size.height * .50f)
            close()
        }
        drawPath(tag, tint, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(tint, size.minDimension * .065f, Offset(size.width * .30f, size.height * .30f),
            style = Stroke(1.6.dp.toPx()))
        if (!shown) drawLine(tint, Offset(size.width * .10f, size.height * .12f),
            Offset(size.width * .90f, size.height * .88f), 2.dp.toPx(), StrokeCap.Round)
    }
}


@Composable
internal fun SpaceCompassPanoramaCompassIcon() {
    val tint = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        drawCircle(tint, size.minDimension * .40f, style = Stroke(1.8.dp.toPx()))
        val needle = Path().apply {
            moveTo(size.width * .68f, size.height * .20f)
            lineTo(size.width * .57f, size.height * .57f)
            lineTo(size.width * .20f, size.height * .68f)
            lineTo(size.width * .42f, size.height * .42f)
            close()
        }
        drawPath(needle, tint, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(tint, Offset(size.width*.42f,size.height*.42f), Offset(size.width*.57f,size.height*.57f), 1.8.dp.toPx())
    }
}

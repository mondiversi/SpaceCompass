package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke

internal enum class SpaceCompassOverlayChoiceIcon {
    ALL_LABELS, OBJECT_LABELS, KEY_LABELS, SELECTED_LABELS, NO_LABELS,
    COMPLETE_POSITION, AREA_POSITION, NO_POSITION
}

/** Decorative scope glyphs share the menu metrics; the radio row supplies accessible text. */
@Composable
internal fun SpaceCompassOverlayChoiceGlyph(icon: SpaceCompassOverlayChoiceIcon) {
    SpaceCompassIconCanvas(LocalContentColor.current, Modifier.size(SpaceCompassMenuIconSize)) { ink ->
        val unit = size.minDimension / 24f
        val width = SpaceCompassMenuIconStrokeWidth.toPx()
        val outline = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun point(x: Float, y: Float) = Offset(x * unit, y * unit)
        fun line(ax: Float, ay: Float, bx: Float, by: Float) =
            drawLine(ink, point(ax, ay), point(bx, by), width, StrokeCap.Round)
        fun path(vararg points: Pair<Float, Float>, closed: Boolean = false) {
            val shape = Path().apply {
                moveTo(points.first().first * unit, points.first().second * unit)
                points.drop(1).forEach { lineTo(it.first * unit, it.second * unit) }
                if (closed) close()
            }
            drawPath(shape, ink, style = outline)
        }
        fun circle(x: Float, y: Float, radius: Float) =
            drawCircle(ink, radius * unit, point(x, y), style = outline)
        fun pin() {
            val shape = Path().apply {
                moveTo(12f * unit, 21f * unit)
                cubicTo(8f * unit, 16f * unit, 4.5f * unit, 12f * unit, 4.5f * unit, 8.5f * unit)
                cubicTo(4.5f * unit, -1f * unit, 19.5f * unit, -1f * unit, 19.5f * unit, 8.5f * unit)
                cubicTo(19.5f * unit, 12f * unit, 16f * unit, 16f * unit, 12f * unit, 21f * unit)
                close()
            }
            drawPath(shape, ink, style = outline)
            circle(12f, 8.5f, 2.5f)
        }
        when (icon) {
            SpaceCompassOverlayChoiceIcon.ALL_LABELS -> {
                for (y in listOf(5f, 12f, 19f)) {
                    drawCircle(ink, 1.3f * unit, point(4f, y))
                    line(9f, y, 21f, y)
                }
            }
            SpaceCompassOverlayChoiceIcon.OBJECT_LABELS -> {
                circle(12f, 12f, 7f)
                line(3f, 18f, 21f, 6f)
                drawCircle(ink, 1.5f * unit, point(20f, 19f))
            }
            SpaceCompassOverlayChoiceIcon.KEY_LABELS -> {
                path(12f to 2.5f, 14.5f to 9.5f, 21.5f to 12f, 14.5f to 14.5f,
                    12f to 21.5f, 9.5f to 14.5f, 2.5f to 12f, 9.5f to 9.5f, closed = true)
            }
            SpaceCompassOverlayChoiceIcon.SELECTED_LABELS -> {
                circle(12f, 12f, 9f)
                path(7f to 12f, 10.5f to 15.5f, 17f to 8.5f)
            }
            SpaceCompassOverlayChoiceIcon.NO_LABELS -> {
                path(4f to 4f, 12f to 4f, 21f to 13f, 13f to 21f, 4f to 12f, closed = true)
                circle(8f, 8f, 1.3f)
                line(3f, 21f, 21f, 3f)
            }
            SpaceCompassOverlayChoiceIcon.COMPLETE_POSITION -> pin()
            SpaceCompassOverlayChoiceIcon.AREA_POSITION -> {
                path(3f to 6f, 9f to 3f, 15f to 6f, 21f to 3f, 21f to 18f,
                    15f to 21f, 9f to 18f, 3f to 21f, closed = true)
                line(9f, 3f, 9f, 18f)
                line(15f, 6f, 15f, 21f)
            }
            SpaceCompassOverlayChoiceIcon.NO_POSITION -> {
                pin()
                line(3f, 21f, 21f, 3f)
            }
        }
    }
}

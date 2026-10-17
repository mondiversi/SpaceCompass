package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Actions share a 16 dp width; narrow navigation chevrons leave more room for the name. */
internal enum class SpaceCompassObjectHeaderGlyph {
    CHEVRON, REMOVE, OBSERVATORY, DAILY_PATH;

    val width: Dp get() = if (this == CHEVRON) 9.dp else 16.dp
}

internal val SpaceCompassObjectHeaderGlyphHeight = 16.dp
internal val SpaceCompassObjectHeaderGlyphGap = 24.dp

/** Keep the same visible gap when adjacent glyphs have different widths. */
internal fun spaceCompassObjectHeaderTargetSpacing(first: SpaceCompassObjectHeaderGlyph,
    second: SpaceCompassObjectHeaderGlyph): Dp =
    SpaceCompassObjectHeaderGlyphGap - 48.dp + (first.width + second.width) / 2

/** Header-only geometry: all glyphs occupy 16 dp vertically, with 1.8 dp strokes.
 * Floating sky controls retain their original drawings and dimensions. */
@Composable
internal fun SpaceCompassObjectHeaderIcon(glyph: SpaceCompassObjectHeaderGlyph,
    modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    SpaceCompassIconCanvas(tint, modifier.size(glyph.width, SpaceCompassObjectHeaderGlyphHeight)
        .testTag("object-header-icon-${glyph.name}")) { color ->
        val stroke = 1.8.dp.toPx()
        val inset = stroke / 2
        val left = inset
        val right = size.width - inset
        val top = inset
        val bottom = size.height - inset
        when (glyph) {
            SpaceCompassObjectHeaderGlyph.CHEVRON -> {
                val chevron = Path().apply {
                    moveTo(left, top); lineTo(right, size.height / 2); lineTo(left, bottom)
                }
                drawPath(chevron, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            SpaceCompassObjectHeaderGlyph.REMOVE -> {
                drawLine(color, Offset(left, top), Offset(right, bottom), stroke, StrokeCap.Round)
                drawLine(color, Offset(right, top), Offset(left, bottom), stroke, StrokeCap.Round)
            }
            SpaceCompassObjectHeaderGlyph.OBSERVATORY -> {
                // Preserve the tube and tripod proportions within their actual occupied bounds.
                fun point(x: Float, y: Float) = Offset(left + (x - .08f) / .75f * (right - left),
                    top + (y - .12f) / .79f * (bottom - top))
                val tube = Path().apply {
                    val a = point(.18f, .36f); moveTo(a.x, a.y)
                    for ((x, y) in listOf(.71f to .12f, .83f to .37f, .30f to .61f)) {
                        val p = point(x, y); lineTo(p.x, p.y)
                    }
                    close()
                }
                drawPath(tube, color, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(color, point(.20f, .51f), point(.08f, .57f), stroke, StrokeCap.Round)
                drawLine(color, point(.52f, .54f), point(.52f, .72f), stroke, StrokeCap.Round)
                for (x in listOf(.28f, .52f, .76f))
                    drawLine(color, point(.52f, .70f), point(x, .91f), stroke, StrokeCap.Round)
            }
            SpaceCompassObjectHeaderGlyph.DAILY_PATH -> {
                // For a symmetric cubic the apex is 1/4 endpoint + 3/4 control height.
                val controlY = (top - bottom * .25f) / .75f
                val curve = Path().apply {
                    moveTo(left, bottom)
                    cubicTo(left + (right - left) * .25f, controlY,
                        left + (right - left) * .75f, controlY, right, bottom)
                }
                drawPath(curve, color, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}

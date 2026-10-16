package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer

/** Composite monochrome glyph opacity once, so intersecting strokes do not darken disabled icons. */
@Composable
internal fun SpaceCompassIconCanvas(tint: Color, modifier: Modifier,
    drawIcon: DrawScope.(Color) -> Unit) {
    Canvas(modifier.graphicsLayer(alpha = tint.alpha)) {
        drawIcon(tint.copy(alpha = 1f))
    }
}

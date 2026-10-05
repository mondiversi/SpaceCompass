package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/** Symbolic geocentre reference, not an Earth surface map or a view of its core. */
@Composable
internal fun SpaceCompassEarthCenterSymbol(modifier: Modifier, opacity: Float = 1f) {
    Canvas(modifier) {
        val radius = size.minDimension * 0.32f
        drawCircle(Color(0xFF276EAC).copy(alpha = opacity), radius)
        drawCircle(Color(0xFF70C690).copy(alpha = opacity), radius * 0.32f, center - Offset(radius * .33f, radius * .25f))
        drawCircle(Color(0xFF70C690).copy(alpha = opacity), radius * 0.24f, center + Offset(radius * .32f, radius * .26f))
        drawCircle(Color.White.copy(alpha = opacity), radius * .13f)
        drawCircle(Color.White.copy(alpha = opacity * .8f), radius * .30f, style = Stroke(radius * .045f))
    }
}

package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** Illustrative nucleus and tail, not a photograph, measured shape or current activity. */
@Composable
internal fun SpaceCompassCometSymbol(body: SpaceCompassCelestialBody, modifier: Modifier, opacity: Float = 1f) {
    Canvas(modifier) { drawSpaceCompassCometSymbol(body, opacity) }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpaceCompassCometSymbol(body: SpaceCompassCelestialBody, opacity: Float = 1f) {
    val r = size.minDimension * .15f
    val nucleus = center + Offset(r * .65f, r * .55f)
    val tail = nucleus - Offset(r * 2.8f, r * 2.4f)
    val tint = spaceCompassCelestialPathTint(body)
    val plume = Path().apply {
        moveTo(nucleus.x + r * .4f, nucleus.y - r * .4f)
        quadraticTo(tail.x + r * 1.5f, tail.y - r, tail.x, tail.y)
        quadraticTo(tail.x - r * .4f, tail.y + r * 1.4f, nucleus.x - r * .5f, nucleus.y + r * .4f)
        close()
    }
    drawPath(plume, Brush.linearGradient(listOf(tint.copy(alpha = .55f * opacity), Color.Transparent), nucleus, tail))
    drawCircle(tint.copy(alpha = .15f * opacity), r * 1.35f, nucleus)
    drawOval(Color(0xff9d9b94).copy(alpha = opacity), nucleus - Offset(r, r * .65f),
        androidx.compose.ui.geometry.Size(r * 2f, r * 1.3f))
    drawCircle(Color(0xffd4d2c8).copy(alpha = opacity * .8f), r * .35f, nucleus - Offset(r * .4f, r * .15f))
}

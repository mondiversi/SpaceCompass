package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/** Symbolic illustrations, never presented as a photograph or a measured surface model. */
@Composable
internal fun SpaceCompassDeepSkySymbol(body: SpaceCompassCelestialBody, modifier: Modifier, opacity: Float = 1f) {
    Canvas(modifier) { drawSpaceCompassDeepSkySymbol(body, opacity) }
}

internal fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSpaceCompassDeepSkySymbol(body: SpaceCompassCelestialBody, opacity: Float = 1f) {
    val r = size.minDimension * 0.32f
    val tint = spaceCompassCelestialPathTint(body)
    if (body == SpaceCompassCelestialBody.TRAPPIST_1_E) {
        // A neutral rocky-world symbol, not an observed map or an ocean claim.
        drawCircle(Color(0xff8b8177).copy(alpha = opacity), r * .9f)
        drawCircle(Color(0xffc4b4a0).copy(alpha = opacity * .65f), r * .23f, center - Offset(r * .28f, r * .24f))
        drawCircle(Color(0xff413f3e).copy(alpha = opacity * .55f), r * .16f, center + Offset(r * .28f, r * .20f))
    } else if (body == SpaceCompassCelestialBody.ALPHA_CENTAURI) {
        drawCircle(tint.copy(alpha = opacity * 0.14f), r * 1.45f)
        drawCircle(Color(0xffffedc1).copy(alpha = opacity), r * 0.56f, center - Offset(r*0.48f, 0f))
        drawCircle(Color(0xffffc17b).copy(alpha = opacity), r * 0.40f, center + Offset(r*0.68f, r*0.18f))
    } else if (body == SpaceCompassCelestialBody.PROXIMA_CENTAURI || body == SpaceCompassCelestialBody.RIGEL) {
        drawCircle(tint.copy(alpha = opacity * 0.10f), r * 1.4f)
        drawCircle(tint.copy(alpha = opacity * 0.22f), r * 1.05f)
        drawCircle(tint.copy(alpha = opacity), r * if (body == SpaceCompassCelestialBody.RIGEL) 0.85f else 0.55f)
        drawCircle(Color.White.copy(alpha = opacity * 0.7f), r * 0.20f, center - Offset(r * 0.15f, r * 0.15f))
    } else if (body == SpaceCompassCelestialBody.STEPHENSON_2_18) {
        drawCircle(tint.copy(alpha = opacity * 0.08f), r * 1.5f)
        drawCircle(tint.copy(alpha = opacity * 0.20f), r * 1.2f)
        drawCircle(tint.copy(alpha = opacity), r * 0.95f)
        drawCircle(Color(0xffffbc83).copy(alpha = opacity * 0.55f), r * 0.24f,
            center - Offset(r * 0.32f, r * 0.28f))
    } else if (body == SpaceCompassCelestialBody.RX_J1856 || body == SpaceCompassCelestialBody.PSR_J0437) {
        drawCircle(tint.copy(alpha = opacity * 0.12f), r * 1.3f)
        drawOval(tint.copy(alpha = opacity * 0.75f), center - Offset(r, r * 0.35f),
            Size(r * 2f, r * 0.7f), style = Stroke(r * 0.07f))
        if (body == SpaceCompassCelestialBody.PSR_J0437) {
            val beam = Offset(r * 0.60f, -r * 1.28f)
            drawLine(tint.copy(alpha = opacity * 0.25f), center - beam, center + beam, r * 0.30f)
            drawLine(tint.copy(alpha = opacity), center - beam, center + beam, r * 0.08f)
        }
        drawCircle(tint.copy(alpha = opacity), r * 0.44f)
        drawCircle(Color.White.copy(alpha = opacity), r * 0.25f)
    } else {
        drawCircle(tint.copy(alpha = opacity * 0.13f), r * 1.4f)
        drawOval(tint.copy(alpha = opacity), center - Offset(r*1.32f, r*0.40f), Size(r*2.64f, r*0.8f),
            style = Stroke(r * 0.10f))
        drawCircle(tint.copy(alpha = opacity), r * 0.80f, style = Stroke(r * 0.15f))
        drawCircle(Color(0xff060812).copy(alpha = opacity), r * 0.61f)
    }
}

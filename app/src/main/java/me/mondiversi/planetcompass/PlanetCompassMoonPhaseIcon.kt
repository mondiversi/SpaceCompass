package me.mondiversi.planetcompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** One native vector silhouette; no emoji font dependencies or stacked translucent shapes. */
@Composable
internal fun PlanetCompassMoonPhaseIcon(phase: PlanetCompassMoonPhase, color: Color, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.moon_phase_description, stringResource(phase.kind.nameResource),
        formatPlanetCompassNumber(phase.illuminatedFraction * 100, 1, LocalPlanetCompassNumericFormat.current))
    val silhouette = remember(phase) { planetCompassMoonPhaseSilhouette(phase) }
    Canvas(modifier.size(22.dp).semantics { contentDescription = description }) {
        val radius = (size.minDimension - 3.dp.toPx()) / 2
        val matrix = androidx.compose.ui.graphics.Matrix().apply { translate(center.x, center.y); scale(radius, radius) }
        val scaled = Path().apply { addPath(silhouette); transform(matrix) }
        drawPath(scaled, color)
        drawCircle(color, radius, style = Stroke(1.dp.toPx()))
    }
}

/** Shared continuous illuminated silhouette for the list icon and textured sky miniature. */
internal fun planetCompassMoonPhaseSilhouette(phase: PlanetCompassMoonPhase): Path =
        Path().apply {
            val segments = 64
            for (i in 0..segments) {
                val y = -1.0 + 2.0 * i / segments
                val x = phase.litHorizontalBounds(y).second
                if (i == 0) moveTo(x.toFloat(), y.toFloat()) else lineTo(x.toFloat(), y.toFloat())
            }
            for (i in segments downTo 0) {
                val y = -1.0 + 2.0 * i / segments
                lineTo(phase.litHorizontalBounds(y).first.toFloat(), y.toFloat())
            }
            close()
        }

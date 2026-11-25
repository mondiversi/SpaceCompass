package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Antipodes retain their source event glyph; their unfilled plate preserves the scene beneath it. */
private fun DrawScope.drawSpaceCompassAntipodalEvent(point: SpaceCompassSunPathPoint, center: Offset,
    tint: Color) {
    val r = (if (point.event == SpaceCompassSunPathEvent.HOUR) 4.dp else 6.dp).toPx()
    drawCircle(tint, r, center, style = Stroke(1.5.dp.toPx()))
    val d = 2.5.dp.toPx()
    when (point.event) {
        SpaceCompassSunPathEvent.HOUR -> Unit
        SpaceCompassSunPathEvent.APPROACH -> drawSpaceCompassApproachGlyph(center, 4.dp.toPx(), tint, 1.5.dp.toPx())
        SpaceCompassSunPathEvent.MINIMUM, SpaceCompassSunPathEvent.CULMINATION -> {
            drawLine(tint, center - Offset(d, 0f), center + Offset(d, 0f), 1.8.dp.toPx(), StrokeCap.Round)
            if (point.event == SpaceCompassSunPathEvent.CULMINATION)
                drawLine(tint, center - Offset(0f, d), center + Offset(0f, d), 1.8.dp.toPx(), StrokeCap.Round)
        }
        else -> drawPath(Path().apply {
            spaceCompassHorizonEventGlyph(point.event, center.x, center.y, d).forEachIndexed { i, (x, y) ->
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }, tint)
    }
    drawCircle(tint, r + 3.dp.toPx(), center, style = Stroke(1.8.dp.toPx()))
}

/** Both full-scene and free-viewport layers share source icons, sizes and the frozen lunar phase. */
@Composable
internal fun SpaceCompassAntipodeMarkers(targets: List<SpaceCompassCelestialSceneTarget>,
    state: SpaceCompassSunDailyPathUiState, modifier: Modifier = Modifier, objectOpacity: Float = 1f) {
    val accent = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current.density
    Canvas(modifier.fillMaxSize()) {
        targets.filterNot { it.isCurrent }.forEach { target ->
            drawSpaceCompassAntipodalEvent(target.antipodeOf ?: target.point,
                Offset(target.projection.x.toFloat(), target.projection.y.toFloat()), accent)
        }
    }
    targets.filter { it.isCurrent }.forEach { target -> key(target.body, target.comparison?.id) {
        val source = target.antipodeOf ?: target.point
        val phase = remember(target.body, source.timeMs) {
            if (target.body == SpaceCompassCelestialBody.MOON) calculateSpaceCompassMoonPhase(source.timeMs) else null
        }
        SpaceCompassCelestialLiveMarker(target.body,
            SpaceCompassSunProjection(true, target.projection.x / density, target.projection.y / density, 0.0),
            target.point.position.elevationDegrees < 0, source, state, moonPhase = phase,
            path = target.path, interactive = false, objectOpacity = objectOpacity)
    } }
}

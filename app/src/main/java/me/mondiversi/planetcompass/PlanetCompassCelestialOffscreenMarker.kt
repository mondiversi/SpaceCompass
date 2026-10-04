package me.mondiversi.planetcompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import kotlin.math.min
import kotlin.math.roundToInt

internal const val PLANET_COMPASS_CELESTIAL_OFFSCREEN_DIAMETER_DP = 56f

/** Reuse the upright miniature and live lunar phase; only the coloured pointer rotates. */
@Composable
internal fun PlanetCompassCelestialOffscreenMarker(body: PlanetCompassCelestialBody, projection: PlanetCompassSunProjection,
    belowHorizon: Boolean, pointerColor: Color, menuItems: Int,
    contextExclusions: List<PlanetCompassSunSceneFrame> = emptyList(),
    forcedPlacement: PlanetCompassCelestialOffscreenGeometry? = null,
    diameter: Dp = PLANET_COMPASS_CELESTIAL_OFFSCREEN_DIAMETER_DP.dp,
    moonPhase: PlanetCompassMoonPhase? = null) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val name = stringResource(body.nameResource)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Direction uses the unclamped projection, not a thumbnail moved clear of a menu.
        val bearing = Math.toDegrees(kotlin.math.atan2(projection.y - maxHeight.value / 2,
            projection.x - maxWidth.value / 2)).toFloat()
        Layout(modifier = Modifier.fillMaxSize(), content = {
            Box(Modifier.size(diameter)
                .testTag("celestial-offscreen-marker").semantics { contentDescription = name },
                contentAlignment = Alignment.Center) {
                PlanetCompassCelestialThumbnail(body, Modifier.size((PLANET_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP *
                    (diameter.value / PLANET_COMPASS_CELESTIAL_OFFSCREEN_DIAMETER_DP).coerceAtMost(1f)).dp)
                    .border(0.8.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                    .testTag("celestial-offscreen-preview-${body.name}"), muted = belowHorizon, moonPhase = moonPhase)
                Canvas(Modifier.fillMaxSize().testTag("celestial-offscreen-pointer")) {
                    val scale = (size.minDimension / PLANET_COMPASS_CELESTIAL_OFFSCREEN_DIAMETER_DP.dp.toPx()).coerceAtMost(1f)
                    rotate(bearing, center) {
                        val arrow = Path().apply {
                            moveTo(center.x + 26.dp.toPx() * scale, center.y)
                            lineTo(center.x + 18.dp.toPx() * scale, center.y - 5.dp.toPx() * scale)
                            lineTo(center.x + 18.dp.toPx() * scale, center.y + 5.dp.toPx() * scale); close()
                        }
                        drawPath(arrow, Color.Black.copy(alpha = 0.60f), style = Stroke(2.dp.toPx() * scale))
                        drawPath(arrow, pointerColor)
                    }
                }
            }
        }) { measurables, constraints ->
            val extent = min(diameter.roundToPx(),
                min(constraints.maxWidth, constraints.maxHeight)).coerceAtLeast(0)
            val marker = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0,
                maxWidth = extent, maxHeight = extent))
            val menuWidth = (menuItems * 48 + 8).dp.toPx().toDouble()
            val excluded = contextExclusions + if (menuItems == 0) emptyList() else listOf(PlanetCompassSunSceneFrame(
                if (rtl) 0.0 else constraints.maxWidth - menuWidth, 0.0, menuWidth, 56.dp.toPx().toDouble()))
            val placement = forcedPlacement ?: placePlanetCompassCelestialOffscreenMarker(projection.copy(x = projection.x * density, y = projection.y * density),
                constraints.maxWidth.toDouble(), constraints.maxHeight.toDouble(), extent.toDouble(), excluded)
            layout(constraints.maxWidth, constraints.maxHeight) {
                placement?.let { marker.place((it.center.x - marker.width / 2.0).roundToInt(),
                    (it.center.y - marker.height / 2.0).roundToInt()) }
            }
        }
    }
}

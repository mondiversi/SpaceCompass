package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Comparison paths use the same horizon styles, hit geometry and selection owner as live paths. */
@Composable
internal fun SpaceCompassTemporalComparisonLayer(comparisons: List<SpaceCompassComparisonOverlay>,
    orientation: SpaceCompassSunOrientation?, perspective: SpaceCompassPerspective?,
    state: SpaceCompassSunDailyPathUiState, primary: Color, secondary: Color, background: Color,
    focused: SpaceCompassCelestialSceneTarget?, widthDp: Double, heightDp: Double,
    objectOpacity: Float, drawPaths: Boolean, drawMarkers: Boolean, interactive: Boolean = true,
    exclusions: List<SpaceCompassSunSceneFrame> = emptyList(), activeId: String? = null,
    markerViewport: SpaceCompassSunSceneFrame? = null) {
    val density = LocalDensity.current.density
    comparisons.forEach { (c, overlay) -> key(c.id) {
        val path = overlay.path
        if (drawPaths && path != null) SpaceCompassSunDailyPathLayer(path, orientation, state, primary,
            secondary, background, showActions = false, interactive = false,
            pathTint = spaceCompassComparisonTint(c), perspective = perspective,
            highlightedPoint = focused?.takeIf { it.comparison?.id == c.id && !it.isCurrent }?.point,
            drawApproaches = false)
        val position = overlay.observation?.position
        if (drawMarkers && position != null && orientation != null) {
            val projected = projectSpaceCompassSun(position, orientation, widthDp, heightDp, perspective)
            val radius = SPACE_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP / 2 * density
            val inFreeViewport = markerViewport?.let { frame ->
                projected.x * density - radius >= frame.left && projected.x * density + radius <= frame.left + frame.width &&
                    projected.y * density - radius >= frame.top && projected.y * density + radius <= frame.top + frame.height
            } == true
            if (!inFreeViewport) {
                val fits = projected.visible && projected.x >= 16 && projected.y >= 16 &&
                    projected.x <= widthDp - 16 && projected.y <= heightDp - 16
                val markerPath = path ?: SpaceCompassSunDailyPath(java.time.Instant.ofEpochMilli(c.timeMs)
                    .atZone(c.zone).toLocalDate(), c.zone, listOf(SpaceCompassSunPathPoint(c.timeMs, position)),
                    emptyList(), c.body, comparison = c)
                if (fits) SpaceCompassCelestialLiveMarker(c.body, projected,
                    position.elevationDegrees < 0, SpaceCompassSunPathPoint(c.timeMs, position), state,
                    moonPhase = if (c.body == SpaceCompassCelestialBody.MOON) calculateSpaceCompassMoonPhase(c.timeMs) else null,
                    path = markerPath, interactive = interactive,
                    focused = focused?.let { it.comparison?.id == c.id && it.isCurrent } == true,
                    objectOpacity = objectOpacity)
                else if (c.id == activeId) {
                    val pixelProjection = projected.copy(x = projected.x * density, y = projected.y * density)
                    val projections = mapOf(c.body to pixelProjection)
                    val diameter = fitSpaceCompassCelestialOffscreenDiameter(projections, widthDp * density, heightDp * density,
                        SPACE_COMPASS_CELESTIAL_OFFSCREEN_DIAMETER_DP * density.toDouble(), exclusions)
                    val placement = placeSpaceCompassCelestialOffscreenMarkers(projections, widthDp * density,
                        heightDp * density, diameter, exclusions)[c.body]
                    if (placement != null) SpaceCompassCelestialOffscreenMarker(c.body, projected,
                        position.elevationDegrees < 0, spaceCompassComparisonTint(c), menuItems = 0,
                        forcedPlacement = placement, diameter = (diameter / density).toFloat().dp,
                        moonPhase = if (c.body == SpaceCompassCelestialBody.MOON) calculateSpaceCompassMoonPhase(c.timeMs) else null,
                        objectOpacity = objectOpacity, selected = state.isCurrentSelected(c.body,
                            SpaceCompassSunPathPoint(c.timeMs, position), c.zone, c.id),
                        nameOverride = spaceCompassComparisonDisplayName(c), pathTint = spaceCompassComparisonTint(c))
                }
            }
        }
    } }
}

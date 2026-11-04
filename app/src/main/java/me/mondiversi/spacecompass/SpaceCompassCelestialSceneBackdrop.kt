package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** Draw the sky scene behind every overlay, retaining the original free area's rays and controls. */
@Composable
internal fun SpaceCompassCelestialSceneBackdrop(opticalFrame: SpaceCompassSunSceneFrame?,
    cameraPerspective: SpaceCompassPerspective?, orientation: SpaceCompassSunOrientation?,
    overlays: Map<SpaceCompassCelestialBody, SpaceCompassCelestialOverlay>, body: SpaceCompassCelestialBody?,
    dailyPath: SpaceCompassSunDailyPath?, position: SpaceCompassSunPosition?, timeMs: Long,
    state: SpaceCompassSunDailyPathUiState, primaryText: Color, secondaryText: Color, background: Color,
    latitude: Double?, altitude: Double, showReferences: Boolean, toolbarHeight: Double, modifier: Modifier,
    longitude: Double? = null, objectOpacity: Float = 1f, showReticle: Boolean = true, showAntipodes: Boolean = false) {
    if (opticalFrame == null || orientation == null) return
    val comparisonOverlays = LocalSpaceCompassComparisonOverlays.current
    val density = LocalDensity.current
    val paths = overlays.mapNotNull { (candidate, overlay) -> overlay.path?.let { candidate to it } }.toMap() +
        (dailyPath?.takeIf { body != null && it.comparison == null }?.let { mapOf(requireNotNull(body) to it) } ?: emptyMap())
    val positions = overlays.mapNotNull { (candidate, overlay) -> overlay.observation?.position?.let { candidate to it } }.toMap() +
        (position?.takeIf { body != null && dailyPath?.comparison == null }?.let { mapOf(requireNotNull(body) to it) } ?: emptyMap())
    val current = positions.mapValues { SpaceCompassSunPathPoint(timeMs, it.value) }
    val moonPhase = rememberSpaceCompassMoonMarkerPhase(timeMs.takeIf { SpaceCompassCelestialBody.MOON in positions })
    BoxWithConstraints(modifier.clipToBounds().testTag("celestial-full-scene")) {
        val width = with(density) { maxWidth.toPx().toDouble() }
        val height = with(density) { maxHeight.toPx().toDouble() }
        val perspective = remember(width, height, opticalFrame, cameraPerspective) {
            spaceCompassSceneFramePerspective(width, height, opticalFrame, cameraPerspective)
        }
        val targets = remember(paths, current, comparisonOverlays, orientation, width, height, perspective) {
            projectSpaceCompassCelestialSceneTargets(paths, current, orientation, width, height, perspective) +
                projectSpaceCompassComparisonTargets(comparisonOverlays, orientation, width, height, perspective)
        }
        val focused = if (showReticle) focusedSpaceCompassCelestialSceneTarget(targets, width, height,
            with(density) { SPACE_COMPASS_CELESTIAL_RETICLE_RADIUS_DP.dp.toPx().toDouble() }, perspective) else null
        val projections = remember(positions, orientation, width, height, perspective) {
            positions.mapValues { projectSpaceCompassSun(it.value, orientation, width, height, perspective) }
        }
        val radius = with(density) { (SPACE_COMPASS_CELESTIAL_LIVE_PREVIEW_SIZE_DP / 2).dp.toPx().toDouble() }
        val excluded = projections.values.filter { it.visible }.map {
            SpaceCompassSunSceneFrame(it.x - radius, it.y - radius, radius * 2, radius * 2)
        } + targets.filterNot { it.isCurrent }.map {
            val pointRadius = with(density) { 9.dp.toPx().toDouble() }
            SpaceCompassSunSceneFrame(it.projection.x - pointRadius, it.projection.y - pointRadius,
                pointRadius * 2, pointRadius * 2)
        }
        SpaceCompassSkyReferenceLayer(latitude, timeMs, orientation, perspective, excluded,
            enabled = showReferences, observerAltitude = altitude) {
            SpaceCompassConstellationsLayer(positions.keys, timeMs,
                latitude, longitude, altitude, orientation, perspective)
            SpaceCompassTemporalComparisonLayer(comparisonOverlays, orientation, perspective, state,
                primaryText, secondaryText, background, focused, width / density.density, height / density.density,
                objectOpacity, drawPaths = true, drawMarkers = true, interactive = false, markerViewport = opticalFrame)
            paths.forEach { (candidate, path) -> key(candidate) {
                SpaceCompassSunDailyPathLayer(path, orientation, state, primaryText, secondaryText, background,
                    showActions = false, interactive = false, pathTint = spaceCompassCelestialPathTint(candidate),
                    highlightedPoint = focused?.takeIf { it.body == candidate && it.comparison == null && !it.isCurrent }?.point,
                    perspective = perspective)
            } }
            // The free viewport retains its interactive thumbnails; covered/marginal ones are decorative only.
            projections.forEach { (candidate, projected) -> key(candidate) {
                val onFreeViewport = projected.x - radius >= opticalFrame.left &&
                    projected.x + radius <= opticalFrame.left + opticalFrame.width &&
                    projected.y - radius >= opticalFrame.top - toolbarHeight &&
                    projected.y + radius <= opticalFrame.top + opticalFrame.height
                if (candidate.constellationPattern == null && projected.visible && !onFreeViewport) SpaceCompassCelestialLiveMarker(candidate,
                    projected.copy(x = projected.x / density.density, y = projected.y / density.density),
                    positions.getValue(candidate).elevationDegrees < 0, current[candidate], state,
                    moonPhase = moonPhase, path = paths[candidate], interactive = false, objectOpacity = objectOpacity)
            } }
        }
        val selected = state.selectedPoints
        val antipodes = remember(selected, paths, current, comparisonOverlays, orientation, width, height, perspective, showAntipodes) {
            if (showAntipodes) projectSpaceCompassSelectedOrbitTargets(selected, paths, current, orientation,
                width, height, perspective, antipodes = true, comparisons = comparisonOverlays) else emptyList()
        }
        val accent = MaterialTheme.colorScheme.primary
        Canvas(Modifier.fillMaxSize().testTag("celestial-full-scene-antipodes")) {
            antipodes.forEach { target ->
                drawSpaceCompassAntipode(Offset(target.projection.x.toFloat(), target.projection.y.toFloat()),
                    7.dp.toPx(), accent, 1.8.dp.toPx())
            }
        }
    }
}

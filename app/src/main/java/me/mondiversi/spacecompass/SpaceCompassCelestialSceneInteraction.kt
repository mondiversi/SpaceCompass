package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Catch blank chrome taps without adding an accessibility action; child controls retain priority. */
internal fun Modifier.spaceCompassSceneInputShield(): Modifier = pointerInput(Unit) { detectTapGestures {} }

/** One hit detector and shared pinned captions, independent of which object's details are inspected. */
@Composable
internal fun SpaceCompassCelestialSceneInteraction(targets: List<SpaceCompassCelestialSceneTarget>,
    currentPoints: Map<SpaceCompassCelestialBody, SpaceCompassSunPathPoint>,
    focused: SpaceCompassCelestialSceneTarget?, state: SpaceCompassSunDailyPathUiState, nowMs: Long?,
    orientation: SpaceCompassSunOrientation?, primaryText: Color, secondaryText: Color, backgroundColor: Color,
    excluded: List<SpaceCompassSunSceneFrame>,
    perspective: SpaceCompassPerspective? = null,
    paths: Map<SpaceCompassCelestialBody, SpaceCompassSunDailyPath> = targets.mapNotNull { it.path?.let { p -> it.body to p } }.toMap(),
    pinnedTargets: List<SpaceCompassCelestialSceneTarget>? = null,
    hitExclusions: List<SpaceCompassSunSceneFrame> = emptyList(),
    liveMarkers: @Composable () -> Unit = {}) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val pinned = pinnedTargets ?: projectSpaceCompassSelectedOrbitTargets(state.selectedPoints, paths, currentPoints,
            orientation, with(density) { maxWidth.toPx().toDouble() }, with(density) { maxHeight.toPx().toDouble() }, perspective)
        val historical = pinned.filter { saved -> !saved.isCurrent && targets.none { target ->
            !target.isCurrent && target.body == saved.body && target.path != null && saved.path != null &&
                spaceCompassOrbitPointKeysMatch(
                    spaceCompassOrbitPointKey(target.body, target.point, target.path.zone, target.path.date),
                    spaceCompassOrbitPointKey(saved.body, saved.point, saved.path.zone, saved.path.date))
        } }
        val currentTargets by rememberUpdatedState(targets + historical)
        val blockedRegions by rememberUpdatedState(hitExclusions)
        val zone by rememberUpdatedState(spaceCompassObservationZone())
        val radius = with(LocalDensity.current) { SPACE_COMPASS_SUN_PATH_TOUCH_RADIUS_DP.dp.toPx().toDouble() }
        val accent = MaterialTheme.colorScheme.primary
        Canvas(Modifier.fillMaxSize().pointerInput(radius) {
            detectTapGestures { tap ->
                tappedSpaceCompassCelestialSceneTarget(currentTargets,
                    SpaceCompassSunScenePoint(tap.x.toDouble(), tap.y.toDouble()), radius, blockedRegions)?.let {
                    state.toggleScenePoint(it, zone)
                }
            }
        }) {
            historical.forEach {
                val r = (if (it.point.event == SpaceCompassSunPathEvent.HOUR) 4.dp else 6.dp).toPx()
                val center = Offset(it.projection.x.toFloat(), it.projection.y.toFloat())
                drawCircle(Color.Black.copy(alpha = .45f), r + 1.5.dp.toPx(), center)
                drawCircle(accent, r, center)
                drawCircle(accent, r + 3.dp.toPx(), center, style = Stroke(1.8.dp.toPx()))
            }
        }
        // Local marker targets stay above the scene detector; captions paint and receive taps last.
        liveMarkers()
        if (nowMs != null) {
            SpaceCompassCelestialSceneBadges(pinned, focused, nowMs, primaryText, secondaryText, backgroundColor, excluded)
        }
    }
}

package me.mondiversi.spacecompass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.time.ZoneId

/** One touch detector and one caption for every visible path, independent of the inspected body. */
@Composable
internal fun SpaceCompassCelestialSceneInteraction(targets: List<SpaceCompassCelestialSceneTarget>,
    focused: SpaceCompassCelestialSceneTarget?, state: SpaceCompassSunDailyPathUiState, nowMs: Long?,
    orientation: SpaceCompassSunOrientation?, primaryText: Color, secondaryText: Color, backgroundColor: Color,
    excluded: List<SpaceCompassSunSceneFrame>, onActivateBody: ((SpaceCompassCelestialBody) -> Unit)?, perspective: SpaceCompassPerspective? = null) {
    val currentTargets by rememberUpdatedState(targets)
    val activate by rememberUpdatedState(onActivateBody)
    val radius = with(LocalDensity.current) { SPACE_COMPASS_SUN_PATH_TOUCH_RADIUS_DP.dp.toPx().toDouble() }
    Canvas(Modifier.fillMaxSize().pointerInput(radius) {
        detectTapGestures { tap ->
            tappedSpaceCompassCelestialSceneTarget(currentTargets, SpaceCompassSunScenePoint(tap.x.toDouble(), tap.y.toDouble()), radius)?.let {
                activate?.invoke(it.body)
                if (it.isCurrent) state.selectCurrent(it.body, it.point)
                else it.path?.let { path -> state.select(path, it.point) }
            }
        }
    }) {}
    if (focused != null && nowMs != null) {
        val name = if (focused.isCurrent) stringResource(R.string.celestial_point_current)
            else focused.path?.let { rememberSpaceCompassSunPathLabels(it).name(focused.point) }.orEmpty()
        SpaceCompassCelestialTimeBadge(focused.point.timeMs, nowMs, focused.path?.zone ?: spaceCompassObservationZone(),
            focused.point.position, orientation, primaryText, secondaryText, backgroundColor, name, focused.body, excluded, perspective)
    }
}

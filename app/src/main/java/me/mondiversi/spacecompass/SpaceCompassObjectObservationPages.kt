package me.mondiversi.spacecompass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import java.time.Instant
import kotlin.math.round

/** Object pages return directly to their entry point without changing the scene selection.
 * Main-scene paths/pins are reused; standalone path entries calculate their own observed path. */
@Composable
internal fun SpaceCompassCelestialViewerScreen(body: SpaceCompassCelestialBody, timeMs: Long,
    latitude: Double?, longitude: Double?, altitude: Double, remote: SpaceCompassCelestialRemoteData,
    primaryText: Color, secondaryText: Color, backgroundColor: Color,
    sharedPathState: SpaceCompassSunDailyPathUiState? = null,
    overlay: SpaceCompassCelestialOverlay? = null, startWithPath: Boolean = false,
    exportLocation: SpaceCompassExportLocation? = null, onBack: () -> Unit) {
    if (!startWithPath) {
        SpaceCompassCelestialObservatoryPage(body, timeMs, latitude, longitude, altitude, remote,
            primaryText, secondaryText, backgroundColor, onBack = onBack)
        return
    }
    val ownState = rememberSpaceCompassSunDailyPathUiState()
    val state = sharedPathState ?: ownState
    val zone = spaceCompassObservationZone()
    val date = Instant.ofEpochMilli(timeMs).atZone(zone).toLocalDate()
    val calculated = if (sharedPathState == null) rememberSpaceCompassCelestialOverlays(setOf(body),
        timeMs, date, zone, latitude?.let { round(it * 10_000) / 10_000 },
        longitude?.let { round(it * 10_000) / 10_000 }, round(altitude / 10) * 10, remote)[body] else null
    val available = overlay ?: calculated
    val path = available?.path ?: state.menuPath?.takeIf { it.body == body }
    val current = spaceCompassCurrentPathPoint(body, timeMs, available?.observation?.position)
    SideEffect { state.refreshMenu(path, current) }
    val location = exportLocation ?: latitude?.let { lat -> longitude?.let { lon ->
        SpaceCompassExportLocation(lat, lon, altitude, null)
    } }
    SpaceCompassSunDailyPathPage(state, primaryText, secondaryText, backgroundColor,
        nowMs = timeMs, location = location, onBack = onBack)
}

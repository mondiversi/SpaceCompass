package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.graphics.Color
import java.time.Instant
import kotlin.math.round

/** One navigation level for both object pages; returning never changes the scene selection.
 * Main-scene paths/pins are reused. Catalog-only objects calculate their own observed path. */
@Composable
internal fun SpaceCompassCelestialViewerScreen(body: SpaceCompassCelestialBody, timeMs: Long,
    latitude: Double?, longitude: Double?, altitude: Double, remote: SpaceCompassCelestialRemoteData,
    primaryText: Color, secondaryText: Color, backgroundColor: Color,
    sharedPathState: SpaceCompassSunDailyPathUiState? = null,
    overlay: SpaceCompassCelestialOverlay? = null, startWithPath: Boolean = false,
    exportLocation: SpaceCompassExportLocation? = null, onBack: () -> Unit) {
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
    var pathSelected by rememberSaveable(body) { mutableStateOf(startWithPath) }
    val savedPages = key(body) { rememberSaveableStateHolder() }
    SideEffect { state.refreshMenu(path, current) }
    val pageAction: @Composable (Boolean) -> Unit = { enabled ->
        SpaceCompassObjectPageAction(pathSelected, path != null, enabled) { showPath ->
            if (!showPath) pathSelected = false
            else path?.let {
                if (state.menuPath?.body != body) state.openMenu(it, current)
                else state.refreshMenu(it, current)
                pathSelected = true
            }
        }
    }
    // Retain model rotation/zoom and table scroll when switching, but reset on a new entry.
    savedPages.SaveableStateProvider(pathSelected) {
        if (pathSelected && state.menuPath != null) {
            val location = exportLocation ?: latitude?.let { lat -> longitude?.let { lon ->
                SpaceCompassExportLocation(lat, lon, altitude, null)
            } }
            SpaceCompassSunDailyPathPage(state, primaryText, secondaryText, backgroundColor,
                nowMs = timeMs, location = location, onBack = onBack, pageAction = pageAction)
        } else SpaceCompassCelestialObservatoryPage(body, timeMs, latitude, longitude, altitude, remote,
            primaryText, secondaryText, backgroundColor, pageAction = pageAction, onBack = onBack)
    }
}

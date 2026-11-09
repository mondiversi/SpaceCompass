package me.mondiversi.spacecompass

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.graphics.Color
import java.time.Instant
import kotlin.math.round

@Composable
internal fun spaceCompassObservationCountLabel(label: String, total: Int?, countFirst: Boolean = false): String {
    val format = LocalSpaceCompassNumericFormat.current
    val locale = LocalSpaceCompassDeviceLocale.current
    val number = total?.let { formatSpaceCompassNumber(it.toDouble(), 0, format, grouping = false, systemLocale = locale) } ?: "—"
    return if (countFirst) "$number $label" else "$label ($number)"
}

/** Object pages preserve their entry point, active object and shared path selections. */
@Composable
internal fun SpaceCompassCelestialViewerScreen(body: SpaceCompassCelestialBody, timeMs: Long,
    latitude: Double?, longitude: Double?, altitude: Double, remote: SpaceCompassCelestialRemoteData,
    primaryText: Color, secondaryText: Color, backgroundColor: Color,
    sharedPathState: SpaceCompassSunDailyPathUiState? = null,
    overlay: SpaceCompassCelestialOverlay? = null, startWithPath: Boolean = false,
    exportLocation: SpaceCompassExportLocation? = null,
    dailyObservation: SpaceCompassLocationDailyObservation? = null,
    viewComparisonId: String? = overlay?.path?.comparison?.id, onBack: () -> Unit) {
    var pathVisible by rememberSaveable(body, viewComparisonId, startWithPath) { mutableStateOf(startWithPath) }
    val pages = rememberSaveableStateHolder()
    val ownState = rememberSpaceCompassSunDailyPathUiState()
    val state = sharedPathState ?: ownState
    val dailyTimeMs = dailyObservation?.timeMs ?: timeMs
    val zone = dailyObservation?.zone ?: spaceCompassObservationZone()
    val date = Instant.ofEpochMilli(dailyTimeMs).atZone(zone).toLocalDate()
    val injected = if (dailyObservation != null) dailyObservation.overlay else overlay
    val liveOverlay = spaceCompassLocationDailyOverlay(body, injected)
    val forward24Hours = LocalSpaceCompassPreferences.current?.getBoolean(SPACE_COMPASS_FORWARD_PATH_KEY, false) == true
    val calculated = if (sharedPathState == null || liveOverlay == null) rememberSpaceCompassCelestialOverlays(setOf(body),
        dailyTimeMs, date, zone, latitude?.let { round(it * 10_000) / 10_000 },
        longitude?.let { round(it * 10_000) / 10_000 }, round(altitude / 10) * 10, remote, forward24Hours = forward24Hours)[body] else null
    val standalone = calculated?.let { rememberSpaceCompassForwardOverlays(mapOf(body to it),
        it.path?.forward24Hours == true,dailyTimeMs,latitude,longitude,altitude,remote)[body] }
    val available = liveOverlay ?: standalone
    val path = spaceCompassLocationDailyPath(body, dailyTimeMs, zone, forward24Hours, available?.path, state.menuPath)
    val current = spaceCompassCurrentPathPoint(body, dailyTimeMs, available?.observation?.position)
    if (!pathVisible) {
        pages.SaveableStateProvider(viewComparisonId ?: body.name) {
            SpaceCompassCelestialObservatoryPage(body, timeMs, latitude, longitude, altitude, remote,
                primaryText, secondaryText, backgroundColor, onBack = onBack)
        }
        return
    }
    fun closePath() {
        state.closeMenu()
        if (startWithPath) onBack() else pathVisible = false
    }
    // Direct island navigation can arrive before its Location-based path is ready.
    if (path == null) {
        BackHandler(onBack = ::closePath)
        Surface(Modifier.fillMaxSize().testTag("sun-path-pending-page"),
            color = spaceCompassPageBackground(), contentColor = primaryText) {
            Column(Modifier.fillMaxSize()) {
                SpaceCompassPageToolbar(stringResource(R.string.celestial_daily_path), ::closePath)
                Column(Modifier.fillMaxWidth().weight(1f).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    val loading = remote.loading || (latitude != null && longitude != null && available == null)
                    if (loading) CircularProgressIndicator(Modifier.size(32.dp))
                    Text(if (loading) stringResource(R.string.celestial_loading)
                        else stringResource(R.string.celestial_unavailable, stringResource(body.nameResource)),
                        color = secondaryText)
                }
            }
        }
        return
    }
    SideEffect {
        if (path != null && (state.menuPath?.body != body || state.menuPath?.comparison?.id != path.comparison?.id)) state.openMenu(path, current)
        else state.refreshMenu(path, current)
    }
    val location = exportLocation ?: latitude?.let { lat -> longitude?.let { lon ->
        SpaceCompassExportLocation(lat, lon, altitude, null)
    } }
    CompositionLocalProvider(LocalSpaceCompassObservationZoneOverride provides zone) {
    SpaceCompassSunDailyPathPage(state, primaryText, secondaryText, backgroundColor,
        nowMs = dailyTimeMs, location = location, onBack = ::closePath)
    }
}

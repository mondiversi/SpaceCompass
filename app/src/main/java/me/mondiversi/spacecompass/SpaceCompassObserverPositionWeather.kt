package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/** Read-only weather for the position being edited, using the Location's existing clock and provider. */
@Composable
internal fun SpaceCompassObserverPositionWeather(latitude: Double?, longitude: Double?,
    plan: SpaceCompassObserverPlan?, nowMs: Long) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val screensaver = LocalSpaceCompassScreensaverActive.current
    var resumed by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val listener = LifecycleEventObserver { _, _ ->
            resumed = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(listener)
        onDispose { lifecycle.removeObserver(listener) }
    }
    val active = resumed && !screensaver
    val tile = remember(latitude, longitude) {
        if (latitude != null && longitude != null && latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0)
            spaceCompassSunWeatherTile(latitude, longitude) else null
    }
    // Batch coordinate typing and clear the previous area's estimate immediately when its tile changes.
    var settledTile by remember(tile) { mutableStateOf<SpaceCompassSunWeatherTile?>(null) }
    LaunchedEffect(tile, active) {
        if (active && tile != null) {
            delay(SPACE_COMPASS_OBSERVER_METADATA_DELAY_MS)
            settledTile = tile
        }
    }
    val requestTime = rememberSpaceCompassScenarioRequestTime(plan, nowMs)
    val reading = rememberSpaceCompassSunWeather(settledTile, active, selectedTimeMs = requestTime)
    val displayedTime = plan?.observationTimeMs(nowMs) ?: nowMs
    val current = reading.snapshot?.takeIf { spaceCompassSunWeatherSnapshotUsable(it, displayedTime) }
    val loading = reading.loading || active && tile != null && settledTile == null
    val value = stringResource(spaceCompassWeatherValueResource(current, loading))
    SpaceCompassSettingsDescription(stringResource(R.string.sun_weather_estimate, value),
        Modifier.testTag("observer-position-weather"))
}

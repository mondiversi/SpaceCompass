package me.mondiversi.planetcompass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One live phase for both sky locators, independent of orientation and the inspected object. */
@Composable
internal fun rememberPlanetCompassMoonMarkerPhase(timeMs: Long?): PlanetCompassMoonPhase? {
    val context = LocalContext.current.applicationContext
    // Phase changes slowly; never recalculate it on magnetometer/animation frames.
    val minute = timeMs?.let { Math.floorDiv(it, 60_000L) }
    return produceState<PlanetCompassMoonPhase?>(null, minute) {
        value = if (timeMs == null) null else withContext(Dispatchers.Default) {
            runCatching { calculatePlanetCompassMoonPhase(timeMs) }
                .onFailure { PlanetCompassErrorLog.record(context, "celestial:moon_marker_phase", it) }.getOrNull()
        }
    }.value
}

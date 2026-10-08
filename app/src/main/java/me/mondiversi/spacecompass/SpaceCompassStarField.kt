package me.mondiversi.spacecompass

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val SPACE_COMPASS_STAR_MAP_ASSET = "sky/starmap.webp"

/** A noninteractive fixed astronomical atlas, shared by every observer and date. */
@Composable
internal fun SpaceCompassStarField(timeMs: Long, latitude: Double?, longitude: Double?, altitude: Double,
    orientation: SpaceCompassSunOrientation?, frame: SpaceCompassSunSceneFrame?, solarElevation: Double?,
    weather: SpaceCompassSunWeatherSnapshot?, active: Boolean, modifier: Modifier) {
    val visibility = spaceCompassStarVisibility(solarElevation, weather)
    val opacity by animateFloatAsState(visibility, tween(1_500), label = "stellar twilight")
    val timeBucket = Math.floorDiv(timeMs, 10_000L)
    val basis by produceState<SpaceCompassStarBasis?>(null, timeBucket, latitude, longitude, altitude, active) {
        value = if (!active || latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
            spaceCompassStarBasis(timeBucket * 10_000L, latitude, longitude, altitude)
        }
    }
    val matrix = basis
    if (active && matrix != null && orientation != null && frame != null && frame.width > 0 && frame.height > 0 && opacity > .001f) {
        val drawing = remember(matrix, orientation, frame, opacity) { spaceCompassStarDrawing(matrix, orientation, frame, opacity) }
        AndroidView(factory = { SpaceCompassStarTextureView(it) }, modifier = modifier.testTag("celestial-star-background"),
            onRelease = { it.release() }, update = { it.update(drawing) })
    }
}

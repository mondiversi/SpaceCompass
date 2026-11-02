package me.mondiversi.spacecompass

import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

internal data class SpaceCompassTutorialMeasurements(val sky: Rect, val targets: List<SpaceCompassTutorialTarget>)

/** Any measured member of each central group is enough; individual buttons may be unavailable. */
internal fun spaceCompassTutorialMeasurements(sky: Rect?, targets: List<SpaceCompassTutorialTarget>): SpaceCompassTutorialMeasurements? {
    if (sky == null || sky.width <= 0 || sky.height <= 0) return null
    val controls = targets.filter { it.circular && !it.point && it.bounds.width > 0 && it.bounds.height > 0 }
    val upper = controls.any { it.feature in setOf(SpaceCompassTutorialFeature.FORWARD,
        SpaceCompassTutorialFeature.GUIDES, SpaceCompassTutorialFeature.LOCATION, SpaceCompassTutorialFeature.OBJECTS) }
    val lower = controls.any { it.feature in setOf(SpaceCompassTutorialFeature.AXIS,
        SpaceCompassTutorialFeature.WEATHER, SpaceCompassTutorialFeature.TRANSPARENCY, SpaceCompassTutorialFeature.ZOOM) }
    return if (upper && lower) SpaceCompassTutorialMeasurements(sky, targets) else null
}

/** Missing layout callbacks must never leave the explanations hidden indefinitely. */
internal suspend fun awaitSpaceCompassTutorialMeasurements(measurements: Flow<SpaceCompassTutorialMeasurements?>,
    timeoutMillis: Long = 200): SpaceCompassTutorialMeasurements? = withTimeoutOrNull(timeoutMillis) {
    measurements.filterNotNull().firstOrNull()
}

package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Observer
import kotlin.math.atan2
import kotlin.math.hypot

private val southernCrossPattern by lazy { SpaceCompassConstellationPattern(spaceCompassSouthernCrossStars,
    spaceCompassSouthernCrossEdges) }
internal val SpaceCompassCelestialBody.constellationPattern: SpaceCompassConstellationPattern?
    get() = if (this == SpaceCompassCelestialBody.SOUTHERN_CROSS) southernCrossPattern else largeCatalogEntry?.pattern

internal fun calculateSpaceCompassConstellation(body: SpaceCompassCelestialBody, timeMs: Long,
    latitude: Double, longitude: Double, altitude: Double = 0.0): List<SpaceCompassSunPosition> {
    val observer = Observer(latitude, longitude, altitude)
    return requireNotNull(body.constellationPattern).stars.map {
        calculateSpaceCompassDeepSkyObservation(it.reference, timeMs, observer).position
    }
}
internal fun spaceCompassConstellationCentre(positions: List<SpaceCompassSunPosition>): SpaceCompassSunPosition {
    require(positions.isNotEmpty())
    val vectors = positions.map { spaceCompassSunCompassDirection(it.azimuthDegrees, it.elevationDegrees) }
    val east = vectors.sumOf { it.east }; val north = vectors.sumOf { it.north }; val up = vectors.sumOf { it.up }
    return SpaceCompassSunPosition((Math.toDegrees(atan2(east, north)) + 360) % 360,
        Math.toDegrees(atan2(up, hypot(east, north))))
}

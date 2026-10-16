package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Observer
import kotlin.math.atan2
import kotlin.math.hypot

internal data class SpaceCompassConstellationStar(val name: String, val reference: SpaceCompassDeepSkyReference)

/** SIMBAD ICRS/J2000 with proper motion. These four stars are not a physical bound system. */
internal val spaceCompassSouthernCrossStars = listOf(
    SpaceCompassConstellationStar("Acrux", SpaceCompassDeepSkyReference(12 + 26.0/60 + 35.89522/3600,
        -(63 + 5.0/60 + 56.7343/3600), 1000 / 10.13 * 3.261563777, -35.83, -14.86)),
    SpaceCompassConstellationStar("Mimosa", SpaceCompassDeepSkyReference(12 + 47.0/60 + 43.26877/3600,
        -(59 + 41.0/60 + 19.5792/3600), 1000 / 11.71 * 3.261563777, -42.97, -16.18)),
    SpaceCompassConstellationStar("Gacrux", SpaceCompassDeepSkyReference(12 + 31.0/60 + 9.95961/3600,
        -(57 + 6.0/60 + 47.5684/3600), 1000 / 36.83 * 3.261563777, 28.23, -265.08)),
    SpaceCompassConstellationStar("Imai", SpaceCompassDeepSkyReference(12 + 15.0/60 + 8.7184790676/3600,
        -(58 + 44.0/60 + 56.126676618/3600), 1000 / 7.1681 * 3.261563777, -36.951, -11.296))
)
internal val spaceCompassSouthernCrossEdges = listOf(0 to 2, 1 to 3)

/** Centre of the apparent pattern on the unit sphere, not an invented location in parsecs. */
internal fun spaceCompassSouthernCrossCentre(positions: List<SpaceCompassSunPosition>): SpaceCompassSunPosition {
    require(positions.size == 4)
    val vectors = positions.map { spaceCompassSunCompassDirection(it.azimuthDegrees, it.elevationDegrees) }
    val east = vectors.sumOf { it.east }; val north = vectors.sumOf { it.north }; val up = vectors.sumOf { it.up }
    return SpaceCompassSunPosition((Math.toDegrees(atan2(east, north)) + 360) % 360,
        Math.toDegrees(atan2(up, hypot(east, north))))
}

internal fun calculateSpaceCompassSouthernCross(timeMs: Long, latitude: Double, longitude: Double,
    altitude: Double = 0.0): List<SpaceCompassSunPosition> {
    val observer = Observer(latitude, longitude, altitude)
    return spaceCompassSouthernCrossStars.map { calculateSpaceCompassDeepSkyObservation(it.reference, timeMs, observer).position }
}

internal val spaceCompassSouthernCrossCoordinates: SpaceCompassCelestialCoordinates by lazy {
    val vectors = spaceCompassSouthernCrossStars.map { spaceCompassStellarReferenceVector(it.reference, 946_728_000_000L).unit() }
    requireNotNull(spaceCompassCelestialCoordinatesFromVector(vectors.sumOf { it.x }, vectors.sumOf { it.y },
        vectors.sumOf { it.z }, SpaceCompassCelestialCoordinateFrame.CATALOG_ICRS, 946_728_000_000L)).copy(timeMs = null)
}

package me.mondiversi.spacecompass

import kotlin.math.*

internal enum class SpaceCompassSkyGuidePointKind { NORTH_POLE, SOUTH_POLE, EARTH_CENTER, ZENITH }
internal data class SpaceCompassSkyGuidePoint(val kind: SpaceCompassSkyGuidePointKind,
    val direction: SpaceCompassSunVector, val labelDegrees: Double? = null)

/** Local ENU references. Earth's centre uses WGS84, distinct from the ellipsoid-normal nadir. */
internal fun spaceCompassSkyGuidePoints(latitude: Double, altitudeMeters: Double = 0.0): List<SpaceCompassSkyGuidePoint> {
    require(latitude.isFinite() && latitude in -90.0..90.0 && altitudeMeters.isFinite())
    val pole = spaceCompassNorthCelestialPole(latitude)
    val lat = Math.toRadians(latitude)
    val flattening = 1.0 / 298.257223563
    val e2 = flattening * (2.0 - flattening)
    val radius = 6_378_137.0 / sqrt(1.0 - e2 * sin(lat).pow(2))
    val centreNorth = radius * e2 * sin(lat) * cos(lat)
    val centreUp = -(radius * (1.0 - e2 * sin(lat).pow(2)) + altitudeMeters)
    val length = hypot(centreNorth, centreUp)
    require(length > 0.0 && length.isFinite())
    return listOf(
        SpaceCompassSkyGuidePoint(SpaceCompassSkyGuidePointKind.NORTH_POLE, pole, 90.0),
        SpaceCompassSkyGuidePoint(SpaceCompassSkyGuidePointKind.SOUTH_POLE,
            SpaceCompassSunVector(-pole.east, -pole.north, -pole.up), -90.0),
        SpaceCompassSkyGuidePoint(SpaceCompassSkyGuidePointKind.EARTH_CENTER,
            SpaceCompassSunVector(0.0, centreNorth / length, centreUp / length)),
        SpaceCompassSkyGuidePoint(SpaceCompassSkyGuidePointKind.ZENITH, SpaceCompassSunVector(0.0, 0.0, 1.0), 90.0))
}

/** An exact vertical has no azimuth: one panorama anchor at mid-width, never a fictitious bearing. */
internal fun spaceCompassSkyGuidePointProjection(point: SpaceCompassSkyGuidePoint,
    orientation: SpaceCompassSunOrientation?, width: Double, height: Double,
    perspective: SpaceCompassPerspective? = null, centerAzimuthDegrees: Double = 180.0): SpaceCompassSunScenePoint? {
    val direction = point.direction
    if (orientation == null) {
        val p = spaceCompassPanoramaVectorPoint(direction, width, height, centerAzimuthDegrees) ?: return null
        return if (hypot(direction.east, direction.north) < 1e-10) p.copy(x = width / 2.0) else p
    }
    val position = SpaceCompassSunPosition(Math.toDegrees(atan2(direction.east, direction.north)),
        Math.toDegrees(atan2(direction.up, hypot(direction.east, direction.north))))
    return projectSpaceCompassSun(position, orientation, width, height, perspective)
        .takeIf { it.visible }?.let { SpaceCompassSunScenePoint(it.x, it.y) }
}

package me.mondiversi.spacecompass

import kotlin.math.*

internal enum class SpaceCompassSkyGuidePointKind { NORTH_POLE, SOUTH_POLE, NADIR, ZENITH }
internal data class SpaceCompassSkyGuidePoint(val kind: SpaceCompassSkyGuidePointKind,
    val direction: SpaceCompassSunVector, val labelDegrees: Double? = null)

/** Local ENU references: nadir is the exact opposite of zenith at every latitude and altitude. */
internal fun spaceCompassSkyGuidePoints(latitude: Double, altitudeMeters: Double = 0.0): List<SpaceCompassSkyGuidePoint> {
    require(latitude.isFinite() && latitude in -90.0..90.0 && altitudeMeters.isFinite())
    val pole = spaceCompassNorthCelestialPole(latitude)
    return listOf(
        SpaceCompassSkyGuidePoint(SpaceCompassSkyGuidePointKind.NORTH_POLE, pole, 90.0),
        SpaceCompassSkyGuidePoint(SpaceCompassSkyGuidePointKind.SOUTH_POLE,
            SpaceCompassSunVector(-pole.east, -pole.north, -pole.up), -90.0),
        SpaceCompassSkyGuidePoint(SpaceCompassSkyGuidePointKind.NADIR,
            SpaceCompassSunVector(0.0, 0.0, -1.0), -90.0),
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

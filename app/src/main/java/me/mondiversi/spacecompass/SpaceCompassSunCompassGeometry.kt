package me.mondiversi.spacecompass

import kotlin.math.*

/** Orthographic celestial sphere: +depth faces the same rear-camera axis as the solar reticle. */
internal data class SpaceCompassSunCompassPoint(val x: Double, val y: Double, val depth: Double)

internal fun projectSpaceCompassSunCompass(vector: SpaceCompassSunVector, orientation: SpaceCompassSunOrientation) =
    SpaceCompassSunCompassPoint(vector.dot(orientation.right), -vector.dot(orientation.screenUp),
        vector.dot(orientation.forward))

internal fun spaceCompassSunCompassDirection(azimuth: Double, elevation: Double = 0.0): SpaceCompassSunVector {
    val az = Math.toRadians(azimuth)
    val el = Math.toRadians(elevation)
    return SpaceCompassSunVector(cos(el) * sin(az), cos(el) * cos(az), sin(el))
}

/** A vertical pointing axis has no horizontal bearing; do not manufacture one from small sensor noise. */
internal fun spaceCompassSunPointingHeading(orientation: SpaceCompassSunOrientation?): Double? = orientation?.let {
    it.headingDegrees.takeIf { value -> value.isFinite() && hypot(it.forward.east, it.forward.north) >= 0.0872 }
}

/** Use the strongest shared horizontal axis, so the comparison also works flat, rolled and upside down. */
internal fun spaceCompassSunCompassHeadingDifference(a: SpaceCompassSunOrientation, b: SpaceCompassSunOrientation): Double? {
    val axes = listOf(a.right to b.right, a.screenUp to b.screenUp, a.forward to b.forward)
    val pair = axes.maxByOrNull { (x, y) -> hypot(x.east, x.north) * hypot(y.east, y.north) } ?: return null
    val (x, y) = pair
    if (!listOf(x.east, x.north, y.east, y.north).all { it.isFinite() } ||
        hypot(x.east, x.north) < 0.3 || hypot(y.east, y.north) < 0.3) return null
    val difference = Math.toDegrees(atan2(x.east, x.north) - atan2(y.east, y.north))
    return abs(wrapSpaceCompassSunDegrees(difference + 180) - 180)
}

/** Unit curves are reused across draws; only their projection changes with the device orientation. */
internal val SpaceCompassSunCompassParallels = listOf(-60.0, -30.0, 30.0, 60.0).map { elevation ->
    (0..72).map { spaceCompassSunCompassDirection(it * 5.0, elevation) }
}
internal val SpaceCompassSunCompassMeridians = listOf(0.0, 45.0, 90.0, 135.0).map { azimuth ->
    (0..72).map { spaceCompassSunCompassDirection(azimuth, it * 5.0) }
}
internal val SpaceCompassSunCompassHorizon = (0..72).map { spaceCompassSunCompassDirection(it * 5.0) }

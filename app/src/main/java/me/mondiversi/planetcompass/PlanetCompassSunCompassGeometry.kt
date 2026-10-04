package me.mondiversi.planetcompass

import kotlin.math.*

/** Orthographic celestial sphere: +depth faces the same rear-camera axis as the solar reticle. */
internal data class PlanetCompassSunCompassPoint(val x: Double, val y: Double, val depth: Double)

internal fun projectPlanetCompassSunCompass(vector: PlanetCompassSunVector, orientation: PlanetCompassSunOrientation) =
    PlanetCompassSunCompassPoint(vector.dot(orientation.right), -vector.dot(orientation.screenUp),
        vector.dot(orientation.forward))

internal fun planetCompassSunCompassDirection(azimuth: Double, elevation: Double = 0.0): PlanetCompassSunVector {
    val az = Math.toRadians(azimuth)
    val el = Math.toRadians(elevation)
    return PlanetCompassSunVector(cos(el) * sin(az), cos(el) * cos(az), sin(el))
}

/** A vertical pointing axis has no horizontal bearing; do not manufacture one from small sensor noise. */
internal fun planetCompassSunPointingHeading(orientation: PlanetCompassSunOrientation?): Double? = orientation?.let {
    it.headingDegrees.takeIf { value -> value.isFinite() && hypot(it.forward.east, it.forward.north) >= 0.0872 }
}

/** Use the strongest shared horizontal axis, so the comparison also works flat, rolled and upside down. */
internal fun planetCompassSunCompassHeadingDifference(a: PlanetCompassSunOrientation, b: PlanetCompassSunOrientation): Double? {
    val axes = listOf(a.right to b.right, a.screenUp to b.screenUp, a.forward to b.forward)
    val pair = axes.maxByOrNull { (x, y) -> hypot(x.east, x.north) * hypot(y.east, y.north) } ?: return null
    val (x, y) = pair
    if (!listOf(x.east, x.north, y.east, y.north).all { it.isFinite() } ||
        hypot(x.east, x.north) < 0.3 || hypot(y.east, y.north) < 0.3) return null
    val difference = Math.toDegrees(atan2(x.east, x.north) - atan2(y.east, y.north))
    return abs(wrapPlanetCompassSunDegrees(difference + 180) - 180)
}

/** Unit curves are reused across draws; only their projection changes with the device orientation. */
internal val PlanetCompassSunCompassParallels = listOf(-60.0, -30.0, 30.0, 60.0).map { elevation ->
    (0..72).map { planetCompassSunCompassDirection(it * 5.0, elevation) }
}
internal val PlanetCompassSunCompassMeridians = listOf(0.0, 45.0, 90.0, 135.0).map { azimuth ->
    (0..72).map { planetCompassSunCompassDirection(azimuth, it * 5.0) }
}
internal val PlanetCompassSunCompassHorizon = (0..72).map { planetCompassSunCompassDirection(it * 5.0) }

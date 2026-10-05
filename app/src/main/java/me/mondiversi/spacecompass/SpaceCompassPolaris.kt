package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import kotlin.math.*

internal const val SPACE_COMPASS_LIGHT_YEAR_KM = 9_460_730_472_580.8
internal val SPACE_COMPASS_POLARIS_DISTANCE_KM = 136.90 * (648_000 / PI) * SPACE_COMPASS_AU_KM

/** SIMBAD ICRS/J2000 astrometry; Gaia DR3 Polaris B distance (Evans et al. 2024).
 * This is a star's apparent diurnal track, not an orbit about Earth. No mutable Star1 slot:
 * path, live pointing and other bodies can be evaluated concurrently without sharing state.
 */
internal fun calculateSpaceCompassPolarisObservation(timeMs: Long, observer: Observer): SpaceCompassCelestialObservation {
    val time = spaceCompassAstronomyTime(timeMs)
    val ra = Math.toRadians((2 + 31.0/60 + 49.09456/3600) * 15)
    val dec = Math.toRadians(89 + 15.0/60 + 50.7923/3600)
    val direction = SpaceCompassViewVector(cos(dec)*cos(ra), cos(dec)*sin(ra), sin(dec))
    val east = SpaceCompassViewVector(-sin(ra), cos(ra), 0.0)
    val north = SpaceCompassViewVector(-sin(dec)*cos(ra), -sin(dec)*sin(ra), cos(dec))
    val years = (timeMs - 946_728_000_000L) / (365.25 * 86_400_000)
    val masToRadians = PI / (180 * 3_600_000)
    // pmRA is already multiplied by cos(dec): use the tangent basis, not raw RA addition.
    val stellar = (direction + east*(44.48*masToRadians*years) + north*(-11.85*masToRadians*years)).unit() *
        (SPACE_COMPASS_POLARIS_DISTANCE_KM / SPACE_COMPASS_AU_KM)
    val earth = helioState(Body.Earth, time)
    val site = observer.toVector(time, EquatorEpoch.J2000)
    val ray = stellar - SpaceCompassViewVector(earth.x + site.x, earth.y + site.y, earth.z + site.z)
    val distanceAu = sqrt(ray.dot(ray))
    // First-order annual aberration; residual is far below a phone compass's accuracy.
    val sight = ray.unit()
    val beta = SpaceCompassViewVector(earth.vx, earth.vy, earth.vz) * (1 / 173.144632674240)
    val apparent = (sight + beta - sight*sight.dot(beta)).unit()
    // EQJ -> horizontal includes precession, nutation and sidereal rotation for this UTC instant.
    val horizontal = rotationEqjHor(time, observer).rotate(Vector(apparent.x, apparent.y, apparent.z, time))
    val azimuth = (Math.toDegrees(atan2(-horizontal.y, horizontal.x)) + 360) % 360
    val elevation = Math.toDegrees(atan2(horizontal.z, hypot(horizontal.x, horizontal.y)))
    return SpaceCompassCelestialObservation(SpaceCompassSunPosition(azimuth, elevation), distanceAu * SPACE_COMPASS_AU_KM)
}

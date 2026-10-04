package me.mondiversi.planetcompass

import io.github.cosinekitty.astronomy.*
import kotlin.math.sqrt

/** Actual heliocentric radius, not the observer range shown by the compass. */
internal fun planetCompassCelestialCatalogDistanceAu(body: PlanetCompassCelestialBody, timeMs: Long,
    remote: PlanetCompassCelestialRemoteData = PlanetCompassCelestialRemoteData()): Double? {
    if (body == PlanetCompassCelestialBody.SUN) return 0.0
    val time = planetCompassAstronomyTime(timeMs)
    if (body == PlanetCompassCelestialBody.POLARIS) return 136.90*206264.80624709636
    if (body.usesHorizons) {
        val motion = remote.motions[body]?.takeIf { it.body == body }
        if (motion != null && timeMs in motion.samples.first().timeMs..motion.samples.last().timeMs) {
            val index = motion.samples.indexOfFirst { it.timeMs >= timeMs }.coerceAtLeast(1)
            val a = motion.samples[index-1]; val b = motion.samples[index]
            val f = (timeMs-a.timeMs).toDouble()/(b.timeMs-a.timeMs)
            val x = a.x+(b.x-a.x)*f; val y = a.y+(b.y-a.y)*f; val z = a.z+(b.z-a.z)*f
            return sqrt(x*x+y*y+z*z)
        }
        val geo = remote.ephemerides[body]?.takeIf { it.body == body }?.at(timeMs) ?: return null
        val earth = helioVector(Body.Earth,time)
        return Vector(geo.x+earth.x,geo.y+earth.y,geo.z+earth.z,time).length()
    }
    if (body.isJovianMoon) return planetCompassJovianMoonHelioVector(body,time).length()
    // At 0.1 AU catalogue resolution the ISS shares Earth's heliocentric radius.
    return helioVector(if (body.isEarthSatellite) Body.Earth else requireNotNull(body.engine),time).length()
}

internal const val PLANET_COMPASS_CELESTIAL_CATALOG_LY_THRESHOLD_AU = 10_000.0

/** Catalogue presentation only: keep the underlying heliocentric distance in AU. */
internal fun formatPlanetCompassCelestialCatalogDistance(distance: Double?, numeric: PlanetCompassNumericFormat): String =
    distance?.takeIf { it.isFinite() && it >= 0 }?.let {
        if (it >= PLANET_COMPASS_CELESTIAL_CATALOG_LY_THRESHOLD_AU)
            "${formatPlanetCompassNumber(it / (PLANET_COMPASS_LIGHT_YEAR_KM / PLANET_COMPASS_AU_KM), 1, numeric)} ly"
        else "${formatPlanetCompassNumber(it, if (it < 1000) 1 else 0, numeric)} AU"
    } ?: "—"

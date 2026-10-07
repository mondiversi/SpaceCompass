package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import kotlin.math.sqrt

internal const val SPACE_COMPASS_CELESTIAL_CATALOG_REFRESH_MS = 10 * 60_000L

/** Static distant-object references are available before an asynchronous catalog refresh. */
internal fun spaceCompassCelestialCatalogReferenceDistanceAu(body: SpaceCompassCelestialBody): Double? =
    body.deepSkyReference?.let { it.distanceLy * SPACE_COMPASS_LIGHT_YEAR_KM / SPACE_COMPASS_AU_KM }
        ?: if (body == SpaceCompassCelestialBody.POLARIS) 136.90 * 206264.80624709636 else null

/** Fill only missing distant-object references. Nearby-object summaries keep their km values. */
internal fun spaceCompassCatalogDistancesWithReferences(calculated: Map<SpaceCompassCelestialBody, Double?>,
    bodies: List<SpaceCompassCelestialBody>): Map<SpaceCompassCelestialBody, Double?> = bodies.associateWith { body ->
    calculated[body]?.takeIf { it.isFinite() && it >= 0 } ?: spaceCompassCelestialCatalogReferenceDistanceAu(body)
}

/** Actual heliocentric radius, not the observer range shown by the compass. */
internal fun spaceCompassCelestialCatalogDistanceAu(body: SpaceCompassCelestialBody, timeMs: Long,
    remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData()): Double? {
    if (body == SpaceCompassCelestialBody.SUN) return 0.0
    if (body == SpaceCompassCelestialBody.EARTH_CENTER) return helioVector(Body.Earth, spaceCompassAstronomyTime(timeMs)).length()
    val time = spaceCompassAstronomyTime(timeMs)
    spaceCompassCelestialCatalogReferenceDistanceAu(body)?.let { return it }
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
    if (body.isJovianMoon) return spaceCompassJovianMoonHelioVector(body,time).length()
    // At 0.1 AU catalogue resolution the ISS shares Earth's heliocentric radius.
    return helioVector(if (body.isEarthSatellite) Body.Earth else requireNotNull(body.engine),time).length()
}

internal const val SPACE_COMPASS_CELESTIAL_CATALOG_LY_THRESHOLD_AU = 10_000.0

/** Catalogue presentation only: keep the underlying heliocentric distance in AU. */
internal fun formatSpaceCompassCelestialCatalogDistance(distance: Double?, numeric: SpaceCompassNumericFormat,
    unit: String? = null): String =
    if (unit != null) formatSpaceCompassSelectedDistance(SpaceCompassCelestialBody.SUN, distance?.times(SPACE_COMPASS_AU_KM), numeric, unit)
        ?.replace(" · ", "\n") ?: "—"
    else
    distance?.takeIf { it.isFinite() && it >= 0 }?.let {
        if (it >= SPACE_COMPASS_CELESTIAL_CATALOG_LY_THRESHOLD_AU)
            "${formatSpaceCompassNumber(it / (SPACE_COMPASS_LIGHT_YEAR_KM / SPACE_COMPASS_AU_KM), 1, numeric)} ly"
        else "${formatSpaceCompassNumber(it, if (it < 1000) 1 else 0, numeric)} AU"
    } ?: "—"

internal fun spaceCompassNearbyCatalogDistanceKm(body: SpaceCompassCelestialBody, timeMs: Long,
    remote: SpaceCompassCelestialRemoteData): Double? {
    if (body == SpaceCompassCelestialBody.MOON)
        return geoVector(Body.Moon, spaceCompassAstronomyTime(timeMs), Aberration.Corrected).length() * SPACE_COMPASS_AU_KM
    val orbit = remote.satelliteOrbit(body)?.takeIf { it.usable(timeMs) } ?: return null
    val vector = orbit.teme(timeMs)
    return sqrt(vector[0]*vector[0] + vector[1]*vector[1] + vector[2]*vector[2])
}

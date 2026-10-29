package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.helioVector
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.sqrt

internal suspend fun spaceCompassCatalogObservations(bodies: List<SpaceCompassCelestialBody>, timeMs: Long,
    latitude: Double?, longitude: Double?, altitude: Double, remote: SpaceCompassCelestialRemoteData):
    Map<SpaceCompassCelestialBody, SpaceCompassCelestialObservation?> {
    val frame = if (latitude != null && longitude != null)
        SpaceCompassDeepSkyFrame(timeMs, Observer(latitude, longitude, altitude)) else null
    return buildMap {
        bodies.forEachIndexed { index, body ->
            if (index % 128 == 0) currentCoroutineContext().ensureActive()
            put(body, if (frame == null) null else runCatching {
                val reference = body.deepSkyReference
                if (!body.isFictional && body.constellationPattern == null && reference != null) frame.observe(reference)
                else calculateSpaceCompassCelestialObservation(body,timeMs,latitude!!,longitude!!,altitude,remote)
            }.getOrNull())
        }
    }
}

internal data class SpaceCompassCatalogDistances(val displayed: Map<SpaceCompassCelestialBody,Double?>,
    val solar: Map<SpaceCompassCelestialBody,Double?>, val earth: Map<SpaceCompassCelestialBody,Double?>)

internal suspend fun spaceCompassCatalogDistances(bodies: List<SpaceCompassCelestialBody>, timeMs: Long,
    remote: SpaceCompassCelestialRemoteData): SpaceCompassCatalogDistances {
    val earthVector = helioVector(Body.Earth,spaceCompassAstronomyTime(timeMs)).let { SpaceCompassViewVector(it.x,it.y,it.z) }
    val solar = HashMap<SpaceCompassCelestialBody,Double?>(bodies.size)
    val earth = HashMap<SpaceCompassCelestialBody,Double?>(bodies.size)
    val displayed = HashMap<SpaceCompassCelestialBody,Double?>(bodies.size)
    bodies.forEachIndexed { index, body ->
        if (index % 128 == 0) currentCoroutineContext().ensureActive()
        val reference = body.deepSkyReference.takeUnless { body.isFictional || body.constellationPattern != null }
        val sunRange: Double?
        val earthRange: Double?
        if (reference != null) {
            sunRange = if (reference.hasMeasuredDistance) reference.distanceLy * SPACE_COMPASS_LIGHT_YEAR_KM / SPACE_COMPASS_AU_KM else null
            earthRange = if (reference.hasMeasuredDistance) {
                val ray = spaceCompassStellarReferenceVector(reference,timeMs) - earthVector
                sqrt(ray.dot(ray)) * SPACE_COMPASS_AU_KM
            } else null
        } else {
            sunRange = runCatching { spaceCompassCatalogDisplaySolarDistanceAu(body,timeMs,remote) }.getOrNull()
            earthRange = runCatching { spaceCompassCatalogDisplayEarthDistanceKm(body,timeMs,remote) }.getOrNull()
        }
        solar[body] = sunRange; earth[body] = earthRange
        displayed[body] = if (body.isEarthSatellite || body == SpaceCompassCelestialBody.MOON) earthRange else sunRange
    }
    return SpaceCompassCatalogDistances(displayed,solar,earth)
}

package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.helioVector
import kotlin.math.sqrt

/** Presentation estimates use the named real reference; measured fictional ephemerides stay absent. */
internal fun spaceCompassReferenceDistanceKm(body: SpaceCompassCelestialBody, timeMs: Long,
    remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData(),
    latitude: Double? = null, longitude: Double? = null, altitude: Double = 0.0): Double? {
    val reference = body.fictionalReference ?: return null
    val observer = if (latitude != null && longitude != null) Observer(latitude, longitude, altitude) else null
    val host = reference.hostBody
    val distance = if (host != null) {
        if (observer != null) calculateSpaceCompassCelestialObservation(host, timeMs,
            observer.latitude, observer.longitude, observer.height, remote)?.distanceKm
        else spaceCompassCelestialCatalogEarthDistanceKm(host, timeMs, remote)
    } else {
        val star = reference.stellarReference?.takeIf { it.hasMeasuredDistance && it.distanceLy.isFinite() && it.distanceLy > 0 }
            ?: return null
        if (observer != null) calculateSpaceCompassDeepSkyObservation(star, timeMs, observer).distanceKm
        else {
            val vector = spaceCompassStellarReferenceVector(star, timeMs)
            val earth = helioVector(Body.Earth, spaceCompassAstronomyTime(timeMs))
            val ray = vector - SpaceCompassViewVector(earth.x, earth.y, earth.z)
            sqrt(ray.dot(ray)) * SPACE_COMPASS_AU_KM
        }
    }
    return distance?.takeIf { it.isFinite() && it >= 0 }
}

internal fun spaceCompassCatalogDisplaySolarDistanceAu(body: SpaceCompassCelestialBody, timeMs: Long,
    remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData()): Double? {
    val reference = body.fictionalReference ?: return spaceCompassCelestialCatalogDistanceAu(body, timeMs, remote)
    reference.hostBody?.let { return spaceCompassCelestialCatalogDistanceAu(it, timeMs, remote) }
    return reference.stellarReference?.takeIf { it.hasMeasuredDistance && it.distanceLy.isFinite() && it.distanceLy > 0 }
        ?.let { it.distanceLy * SPACE_COMPASS_LIGHT_YEAR_KM / SPACE_COMPASS_AU_KM }
}

internal fun spaceCompassCatalogDisplayEarthDistanceKm(body: SpaceCompassCelestialBody, timeMs: Long,
    remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData()): Double? =
    if (body.isFictional) spaceCompassReferenceDistanceKm(body, timeMs, remote)
    else spaceCompassCelestialCatalogEarthDistanceKm(body, timeMs, remote)

internal fun spaceCompassDisplayObserverDistanceKm(body: SpaceCompassCelestialBody, measuredKm: Double?, timeMs: Long,
    remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData(), latitude: Double? = null,
    longitude: Double? = null, altitude: Double = 0.0): Double? =
    if (body.isFictional) spaceCompassReferenceDistanceKm(body, timeMs, remote, latitude, longitude, altitude)
    else measuredKm?.takeIf { it.isFinite() && it >= 0 }

/** Idempotent decoration shared by list, Observatory, travel comparison and exports. */
internal fun spaceCompassReferenceDistanceText(body: SpaceCompassCelestialBody, value: String?): String? =
    value?.let { if (body.isFictional && it != "—" && !it.startsWith("≈")) "≈ $it" else it }

internal fun formatSpaceCompassCatalogDisplayDistance(body: SpaceCompassCelestialBody, distanceAu: Double?,
    numeric: SpaceCompassNumericFormat, unit: String): String = spaceCompassReferenceDistanceText(body,
    formatSpaceCompassCelestialCatalogDistance(distanceAu, numeric, unit)) ?: "—"

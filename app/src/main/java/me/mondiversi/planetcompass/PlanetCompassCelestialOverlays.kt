package me.mondiversi.planetcompass

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

internal data class PlanetCompassCelestialOverlay(val body: PlanetCompassCelestialBody,
    val observation: PlanetCompassCelestialObservation?, val distanceKm: Double?, val speedKmSecond: Double?,
    val path: PlanetCompassSunDailyPath?)

/** Stable per-object caches: switching the inspected object never rebuilds the other curves.
 * Position updates and daily paths run off the UI thread, independently of compass frames.
 */
@Composable
internal fun rememberPlanetCompassCelestialOverlays(bodies: Set<PlanetCompassCelestialBody>, timeMs: Long,
    date: LocalDate, zone: ZoneId, latitude: Double?, longitude: Double?, altitude: Double,
    remote: PlanetCompassCelestialRemoteData, pathBodies: Set<PlanetCompassCelestialBody> = bodies): Map<PlanetCompassCelestialBody, PlanetCompassCelestialOverlay> {
    val context = LocalContext.current
    return planetCompassCelestialCatalogOrder.filter { it in bodies }.associateWith { body -> key(body) {
        // ISS moves quickly; ordinary objects do not need another ephemeris calculation every half-second.
        val observationTime = if (body.isEarthSatellite) timeMs else timeMs / 2_000 * 2_000
        val orbit = remote.satelliteOrbit(body)
        val observation by produceState<PlanetCompassCelestialObservation?>(null, observationTime, latitude, longitude,
            altitude, orbit, remote.ephemerides[body]) {
            value = if (latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                runCatching { calculatePlanetCompassCelestialObservation(body, observationTime, latitude, longitude, altitude, remote) }.getOrNull()
            }
        }
        val consulted = body in pathBodies
        val speed by produceState<Double?>(null, consulted, observationTime, orbit, remote.motions[body]) {
            value = if (!consulted) null else withContext(Dispatchers.Default) {
                runCatching { calculatePlanetCompassCelestialSpeed(body, timeMs, remote) }.getOrNull()
            }
        }
        val noonDistance by produceState<Double?>(null, consulted, date, zone, latitude, longitude, altitude, remote.ephemerides[body]) {
            value = if (!consulted || body.usesLiveDistance || latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                runCatching { calculatePlanetCompassCelestialObservation(body, planetCompassCelestialDistanceTime(body, timeMs, zone),
                    latitude, longitude, altitude, remote)?.distanceKm }.getOrNull()
            }
        }
        val passBucket = if (body.isEarthSatellite) timeMs / 60_000 else 0L
        val path by produceState<PlanetCompassSunDailyPath?>(null, consulted, date, zone, latitude, longitude, altitude,
            orbit, remote.ephemerides[body], passBucket) {
            value = if (!consulted || !body.supportsDailyPath || latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                runCatching { calculatePlanetCompassCelestialPath(body, date, zone, timeMs, latitude, longitude, altitude, remote) }
                    .onFailure { PlanetCompassErrorLog.record(context, "celestial:path:${body.name}", it) }.getOrNull()
            }
        }
        PlanetCompassCelestialOverlay(body, observation, if (body.usesLiveDistance) observation?.distanceKm else noonDistance, speed, path)
    } }
}

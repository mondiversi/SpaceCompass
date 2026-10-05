package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

internal data class SpaceCompassCelestialOverlay(val body: SpaceCompassCelestialBody,
    val observation: SpaceCompassCelestialObservation?, val distanceKm: Double?, val speedKmSecond: Double?,
    val path: SpaceCompassSunDailyPath?)

/** Stable per-object caches: switching the inspected object never rebuilds the other curves.
 * Position updates and daily paths run off the UI thread, independently of compass frames.
 */
@Composable
internal fun rememberSpaceCompassCelestialOverlays(bodies: Set<SpaceCompassCelestialBody>, timeMs: Long,
    date: LocalDate, zone: ZoneId, latitude: Double?, longitude: Double?, altitude: Double,
    remote: SpaceCompassCelestialRemoteData, pathBodies: Set<SpaceCompassCelestialBody> = bodies): Map<SpaceCompassCelestialBody, SpaceCompassCelestialOverlay> {
    val context = LocalContext.current
    return spaceCompassCelestialCatalogOrder.filter { it in bodies }.associateWith { body -> key(body) {
        // ISS moves quickly; ordinary objects do not need another ephemeris calculation every half-second.
        val observationTime = if (body.isEarthSatellite) timeMs else timeMs / 2_000 * 2_000
        val orbit = remote.satelliteOrbit(body)
        val observation by produceState<SpaceCompassCelestialObservation?>(null, observationTime, latitude, longitude,
            altitude, orbit, remote.ephemerides[body]) {
            value = if (latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                runCatching { calculateSpaceCompassCelestialObservation(body, observationTime, latitude, longitude, altitude, remote) }.getOrNull()
            }
        }
        val consulted = body in pathBodies
        val speed by produceState<Double?>(null, consulted, observationTime, orbit, remote.motions[body]) {
            value = if (!consulted) null else withContext(Dispatchers.Default) {
                runCatching { calculateSpaceCompassCelestialSpeed(body, timeMs, remote) }.getOrNull()
            }
        }
        val noonDistance by produceState<Double?>(null, consulted, date, zone, latitude, longitude, altitude, remote.ephemerides[body]) {
            value = if (!consulted || body.usesLiveDistance || latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                runCatching { calculateSpaceCompassCelestialObservation(body, spaceCompassCelestialDistanceTime(body, timeMs, zone),
                    latitude, longitude, altitude, remote)?.distanceKm }.getOrNull()
            }
        }
        val passBucket = if (body.isEarthSatellite) timeMs / 60_000 else 0L
        val path by produceState<SpaceCompassSunDailyPath?>(null, consulted, date, zone, latitude, longitude, altitude,
            orbit, remote.ephemerides[body], passBucket) {
            value = if (!consulted || !body.supportsDailyPath || latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                runCatching { calculateSpaceCompassCelestialPath(body, date, zone, timeMs, latitude, longitude, altitude, remote) }
                    .onFailure { SpaceCompassErrorLog.record(context, "celestial:path:${body.name}", it) }.getOrNull()
            }
        }
        SpaceCompassCelestialOverlay(body, observation, if (body.usesLiveDistance) observation?.distanceKm else noonDistance, speed, path)
    } }
}

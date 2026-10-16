package me.mondiversi.spacecompass

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import java.time.LocalDate
import java.time.ZoneId

internal data class SpaceCompassCelestialOverlay(val body: SpaceCompassCelestialBody,
    val observation: SpaceCompassCelestialObservation?, val distanceKm: Double?, val speedKmSecond: Double?,
    val path: SpaceCompassSunDailyPath?, val observedAtMs: Long? = null,
    val currentApproaches: List<SpaceCompassApproachPair> = emptyList())

/** Stable per-object caches: switching the inspected object never rebuilds the other curves.
 * Position updates and daily paths run off the UI thread, independently of compass frames.
 */
@Composable
internal fun rememberSpaceCompassCelestialOverlays(bodies: Set<SpaceCompassCelestialBody>, timeMs: Long,
    date: LocalDate, zone: ZoneId, latitude: Double?, longitude: Double?, altitude: Double,
    remote: SpaceCompassCelestialRemoteData, pathBodies: Set<SpaceCompassCelestialBody> = bodies): Map<SpaceCompassCelestialBody, SpaceCompassCelestialOverlay> {
    val context = LocalContext.current
    return spaceCompassAllCelestialOrder.filter { it in bodies }.associateWith { body -> key(body) {
        // ISS moves quickly; ordinary objects do not need another ephemeris calculation every half-second.
        val observationTime = if (body in pathBodies || body.isEarthSatellite) timeMs else timeMs / 2_000 * 2_000
        val orbit = remote.satelliteOrbit(body)
        val timedObservation by produceState<Pair<Long, SpaceCompassCelestialObservation?>?>(null, observationTime, latitude, longitude,
            altitude, orbit, remote.ephemerides[body]) {
            value = if (latitude == null || longitude == null) null else withContext(Dispatchers.Default) {
                observationTime to runCatching { calculateSpaceCompassCelestialObservation(body, observationTime, latitude, longitude, altitude, remote) }.getOrNull()
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
        val observation = timedObservation?.second
        SpaceCompassCelestialOverlay(body, observation, if (body.usesLiveDistance) observation?.distanceKm else noonDistance,
            speed, path, timedObservation?.first)
    } }
}

/** Pair analysis depends on cached curves/data, never on sensor frames or live marker ticks. */
@Composable
internal fun rememberSpaceCompassApproachOverlays(overlays: Map<SpaceCompassCelestialBody,SpaceCompassCelestialOverlay>,
    latitude: Double?, longitude: Double?, altitude: Double, remote: SpaceCompassCelestialRemoteData,
    timeMs: Long): Map<SpaceCompassCelestialBody,SpaceCompassCelestialOverlay> {
    val paths = overlays.mapNotNull { (body,overlay) -> overlay.path?.let { body to it } }.toMap()
    val bodies = overlays.keys
    val current by produceState<List<SpaceCompassApproachPair>>(emptyList(), timeMs, bodies, latitude, longitude,
        altitude, remote.ephemerides, remote.satellites, remote.iss, remote.starlink) {
        value = if (latitude == null || longitude == null || bodies.size < 2) emptyList() else withContext(Dispatchers.Default) {
            val positions = bodies.mapNotNull { body -> calculateSpaceCompassCelestialObservation(body,timeMs,
                latitude,longitude,altitude,remote)?.position?.let { body to it } }.toMap()
            spaceCompassCurrentApproaches(timeMs,positions)
        }
    }
    val decorated by produceState<Map<SpaceCompassCelestialBody,SpaceCompassSunDailyPath>>(emptyMap(), paths,
        latitude, longitude, altitude, remote.ephemerides, remote.satellites, remote.iss, remote.starlink) {
        value = if (latitude == null || longitude == null || paths.size < 2) paths else withContext(Dispatchers.Default) {
            val job = coroutineContext
            spaceCompassApproachPaths(paths, { body,t ->
                calculateSpaceCompassCelestialObservation(body,t,latitude,longitude,altitude,remote)?.position
            }, { job.ensureActive() })
        }
    }
    // Do not attach a previous day's analysis while new curves are being generated.
    return overlays.mapValues { (body,overlay) -> overlay.copy(currentApproaches = current, path = decorated[body]?.takeIf {
        it.date == overlay.path?.date && it.zone == overlay.path?.zone && it.samples == overlay.path?.samples
    } ?: overlay.path) }
}

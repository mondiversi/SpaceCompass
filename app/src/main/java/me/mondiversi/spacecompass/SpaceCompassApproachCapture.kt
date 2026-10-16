package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId

/** Freeze both bodies and their common time before rendering or exporting a capture. */
internal fun freezeSpaceCompassApproachSnapshot(snapshot: SpaceCompassPanoramaSnapshot,
    checkCancelled: () -> Unit = {}): SpaceCompassPanoramaSnapshot {
    if (!snapshot.observerPositionKnown) return snapshot
    val zone = ZoneId.of(snapshot.zoneId)
    val date = Instant.ofEpochMilli(snapshot.timeMs).atZone(zone).toLocalDate()
    val paths = snapshot.objects.mapNotNull { item ->
        checkCancelled()
        val path = item.path?.takeIf { it.date == date && it.zone == zone &&
            (!item.body.isEarthSatellite || snapshot.timeMs in (it.samples.first().timeMs..it.samples.last().timeMs)) }
            ?: calculateSpaceCompassCelestialPath(item.body,date,zone,snapshot.timeMs,snapshot.latitude,
                snapshot.longitude,snapshot.altitude,snapshot.remote)
        path?.let { item.body to it }
    }.toMap()
    val decorated = spaceCompassApproachPaths(paths,{ body,t ->
        calculateSpaceCompassCelestialObservation(body,t,snapshot.latitude,snapshot.longitude,snapshot.altitude,snapshot.remote)?.position
    },checkCancelled)
    val sun = calculateSpaceCompassSunPosition(snapshot.timeMs,snapshot.latitude,snapshot.longitude,snapshot.altitude)
    val rising = calculateSpaceCompassSunPosition(snapshot.timeMs+60_000,snapshot.latitude,snapshot.longitude,snapshot.altitude)
        .elevationDegrees >= sun.elevationDegrees
    return snapshot.copy(objects = snapshot.objects.map { it.copy(path = decorated[it.body]) },
        phase = spaceCompassSunSkyPhase(sun.elevationDegrees,rising), solarLighting = spaceCompassSolarLighting(sun.elevationDegrees),
        weather = snapshot.weather?.takeIf { spaceCompassSunWeatherSnapshotUsable(it,snapshot.timeMs) })
}

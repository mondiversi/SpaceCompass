package me.mondiversi.spacecompass

import java.time.ZoneId
import java.util.TimeZone

internal data class SpaceCompassPanoramaSelectedPoint(val body: SpaceCompassCelestialBody,
    val point: SpaceCompassSunPathPoint, val path: SpaceCompassSunDailyPath?, val isCurrent: Boolean)

/** Resolve only the shutter snapshot: later UI toggles, refreshes and exports cannot change the pins. */
internal fun resolveSpaceCompassPanoramaSelections(snapshot: SpaceCompassPanoramaSnapshot,
    current: Map<SpaceCompassCelestialBody, SpaceCompassSunPosition>): List<SpaceCompassPanoramaSelectedPoint> {
    val objects = snapshot.objects.associateBy { it.body }
    return snapshot.selectedPoints.mapNotNull { saved ->
        val item = objects[saved.key.body] ?: return@mapNotNull null
        val path = item.path?.takeIf { it.date == saved.key.date && it.zone.id == saved.key.zoneId }
        if (saved.key.isCurrent) {
            val point = spaceCompassCurrentPathPoint(item.body, snapshot.timeMs, current[item.body]) ?: return@mapNotNull null
            if (spaceCompassOrbitPointKey(item.body, point, ZoneId.of(snapshot.zoneId), current = true) != saved.key)
                return@mapNotNull null
            SpaceCompassPanoramaSelectedPoint(item.body, point, path, true)
        } else {
            path ?: return@mapNotNull null
            val refreshed = path.markers.firstOrNull { spaceCompassOrbitPointKeysMatch(saved.key,
                spaceCompassOrbitPointKey(item.body, it, path.zone, path.date)) }
            val point = refreshed ?: saved.point
            if (!point.position.azimuthDegrees.isFinite() || !point.position.elevationDegrees.isFinite() ||
                point.position.elevationDegrees !in -90.0..90.0) return@mapNotNull null
            SpaceCompassPanoramaSelectedPoint(item.body, point, if (refreshed == null) saved.path ?: path else path, false)
        }
    }.distinctBy { Triple(it.body, it.isCurrent, it.point) }
}

internal fun spaceCompassPanoramaPointSelected(selected: List<SpaceCompassPanoramaSelectedPoint>,
    body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint): Boolean =
    selected.any { !it.isCurrent && it.body == body && it.point == point }

/** Selected dots use the same one-row label as ordinary dots, preserving only special event names. */
internal fun formatSpaceCompassPanoramaSelection(snapshot: SpaceCompassPanoramaSnapshot,
    selected: SpaceCompassPanoramaSelectedPoint): String {
    val moment = if (selected.isCurrent) snapshot.currentTime else snapshot.markerTimes[selected.point.timeMs]
    val time = moment ?: formatSpaceCompassPanoramaExportTime(selected.point.timeMs,
        snapshot.displayTimeZone, snapshot.formatting)
    return if (selected.isCurrent) formatSpaceCompassPanoramaPointLabel(time, selected.point.position,
        snapshot.formatting, snapshot.currentPointName,
        formatSpaceCompassPanoramaPointCountdown(selected.point.timeMs, snapshot.timeMs, snapshot.formatting))
    else formatSpaceCompassPanoramaPathPointLabel(time, selected.point, snapshot.formatting,
        snapshot.pointEventNames, snapshot.timeMs)
}

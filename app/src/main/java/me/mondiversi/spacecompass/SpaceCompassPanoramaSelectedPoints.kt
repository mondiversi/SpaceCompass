package me.mondiversi.spacecompass

import java.time.ZoneId
import java.util.TimeZone

internal data class SpaceCompassPanoramaSelectedPoint(val body: SpaceCompassCelestialBody,
    val point: SpaceCompassSunPathPoint, val path: SpaceCompassSunDailyPath?, val isCurrent: Boolean,
    val isAntipode: Boolean = false)

/** Resolve only the shutter snapshot: later UI toggles, refreshes and exports cannot change the pins. */
internal fun resolveSpaceCompassPanoramaSelections(snapshot: SpaceCompassPanoramaSnapshot,
    current: Map<SpaceCompassCelestialBody, SpaceCompassSunPosition>): List<SpaceCompassPanoramaSelectedPoint> {
    val objects = snapshot.objects.associateBy { it.body }
    val originals = snapshot.selectedPoints.mapNotNull { saved ->
        val item = objects[saved.key.body] ?: return@mapNotNull null
        val path = item.path?.takeIf { spaceCompassPathAcceptsSelection(it,saved) }
        if (saved.key.isCurrent) {
            val point = spaceCompassCurrentPathPoint(item.body, snapshot.timeMs, current[item.body]) ?: return@mapNotNull null
            if (spaceCompassOrbitPointKey(item.body, point, ZoneId.of(snapshot.zoneId), current = true) != saved.key)
                return@mapNotNull null
            SpaceCompassPanoramaSelectedPoint(item.body, point, path, true)
        } else {
            path ?: return@mapNotNull null
            val refreshed = path.markers.firstOrNull { spaceCompassOrbitPointKeysMatch(saved.key,
                spaceCompassOrbitPointKey(item.body, it, path.zone, spaceCompassPathSelectionDate(path,it))) }
            val point = refreshed ?: saved.point
            if (!point.position.azimuthDegrees.isFinite() || !point.position.elevationDegrees.isFinite() ||
                point.position.elevationDegrees !in -90.0..90.0) return@mapNotNull null
            SpaceCompassPanoramaSelectedPoint(item.body, point, if (refreshed == null) saved.path ?: path else path, false)
        }
    }.distinctBy { Triple(it.body, it.isCurrent, it.point) }
    return originals + if (snapshot.showAntipodes) originals.map {
        it.copy(point = it.point.antipodalDirection(), isAntipode = true)
    } else emptyList()
}

internal fun spaceCompassPanoramaPointSelected(selected: List<SpaceCompassPanoramaSelectedPoint>,
    body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint): Boolean =
    selected.any { !it.isAntipode && !it.isCurrent && it.body == body && it.point == point }

/** Selected dots use the same one-row label as ordinary dots, preserving only special event names. */
internal fun formatSpaceCompassPanoramaSelection(snapshot: SpaceCompassPanoramaSnapshot,
    selected: SpaceCompassPanoramaSelectedPoint): String {
    val moment = if (selected.isCurrent) snapshot.currentTime else snapshot.markerTimes[selected.point.timeMs]
    val time = moment ?: formatSpaceCompassPanoramaMarkerTime(selected.point.timeMs,snapshot.timeMs,
        snapshot.displayTimeZone, snapshot.formatting)
    if (selected.isAntipode) {
        val event = if (selected.isCurrent) snapshot.currentPointName else
            spaceCompassPanoramaPointEventName(selected.point, snapshot)
        val name = listOf(snapshot.orbitExportVocabulary.antipode, event).filter { it.isNotBlank() }.joinToString(" · ")
        return formatSpaceCompassPanoramaPointLabel(time, selected.point.position, snapshot.formatting, name,
            formatSpaceCompassPanoramaPointCountdown(selected.point.timeMs, snapshot.timeMs, snapshot.formatting))
    }
    return if (selected.isCurrent) formatSpaceCompassPanoramaPointLabel(time, selected.point.position,
        snapshot.formatting, snapshot.currentPointName,
        formatSpaceCompassPanoramaPointCountdown(selected.point.timeMs, snapshot.timeMs, snapshot.formatting))
    else formatSpaceCompassPanoramaPathPointLabel(time, selected.point, snapshot.formatting,
        snapshot.pointEventNames, snapshot.timeMs, snapshot.bodyNames)
}

private fun spaceCompassPanoramaPointEventName(point: SpaceCompassSunPathPoint,
    snapshot: SpaceCompassPanoramaSnapshot): String = point.events.filter { it != SpaceCompassSunPathEvent.HOUR }
    .sortedBy { it.ordinal }.mapNotNull { snapshot.pointEventNames[it] }.joinToString(" · ")

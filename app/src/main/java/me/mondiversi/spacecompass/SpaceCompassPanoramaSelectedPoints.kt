package me.mondiversi.spacecompass

import java.time.ZoneId
import java.util.TimeZone

internal data class SpaceCompassPanoramaSelectedPoint(val body: SpaceCompassCelestialBody,
    val point: SpaceCompassSunPathPoint, val path: SpaceCompassSunDailyPath?, val isCurrent: Boolean,
    val isAntipode: Boolean = false, val comparison: SpaceCompassTemporalComparison? = path?.comparison)

/** Resolve only the shutter snapshot: later UI toggles, refreshes and exports cannot change the pins. */
internal fun resolveSpaceCompassPanoramaSelections(snapshot: SpaceCompassPanoramaSnapshot,
    current: Map<SpaceCompassCelestialBody, SpaceCompassSunPosition>): List<SpaceCompassPanoramaSelectedPoint> {
    val objects = snapshot.objects.associateBy { it.body to it.comparison?.id }
    val originals = snapshot.selectedPoints.mapNotNull { saved ->
        val item = objects[saved.key.body to saved.key.comparisonId] ?: return@mapNotNull null
        val path = item.path?.takeIf { spaceCompassPathAcceptsSelection(it,saved) }
        if (saved.key.isCurrent) {
            val point = spaceCompassCurrentPathPoint(item.body, item.referenceTime(snapshot.timeMs), if (item.comparison == null) current[item.body] else if (snapshot.observerPositionKnown) item.currentPosition(snapshot) else item.currentPoint?.position) ?: return@mapNotNull null
            if (spaceCompassOrbitPointKey(item.body, point, item.comparison?.zone ?: ZoneId.of(snapshot.zoneId), current = true, comparisonId = item.comparison?.id) != saved.key)
                return@mapNotNull null
            SpaceCompassPanoramaSelectedPoint(item.body, point, path, true, comparison = item.comparison)
        } else {
            path ?: return@mapNotNull null
            val refreshed = path.markers.firstOrNull { spaceCompassOrbitPointKeysMatch(saved.key,
                spaceCompassOrbitPointKey(item.body, it, path.zone, spaceCompassPathSelectionDate(path,it), comparisonId = item.comparison?.id)) }
            val point = refreshed ?: saved.point
            if (!point.position.azimuthDegrees.isFinite() || !point.position.elevationDegrees.isFinite() ||
                point.position.elevationDegrees !in -90.0..90.0) return@mapNotNull null
            SpaceCompassPanoramaSelectedPoint(item.body, point, if (refreshed == null) saved.path ?: path else path, false, comparison = item.comparison)
        }
    }.distinctBy { listOf(it.body, it.comparison?.id, it.isCurrent, it.point) }
    return originals + if (snapshot.showAntipodes) originals.map {
        it.copy(point = it.point.antipodalDirection(), isAntipode = true)
    } else emptyList()
}

internal fun spaceCompassPanoramaPointSelected(selected: List<SpaceCompassPanoramaSelectedPoint>,
    body: SpaceCompassCelestialBody, point: SpaceCompassSunPathPoint, comparisonId: String? = null): Boolean =
    selected.any { !it.isAntipode && !it.isCurrent && it.body == body && it.comparison?.id == comparisonId && it.point == point }

/** Selected dots use the same one-row label as ordinary dots, preserving only special event names. */
internal fun formatSpaceCompassPanoramaSelection(snapshot: SpaceCompassPanoramaSnapshot,
    selected: SpaceCompassPanoramaSelectedPoint): String {
    val referenceTime = selected.comparison?.timeMs ?: snapshot.timeMs
    val anchor = selected.isCurrent || selected.comparison?.let { c ->
        c.source == SpaceCompassComparisonSource.HEIGHT && SpaceCompassSunPathEvent.CULMINATION in selected.point.events &&
            kotlin.math.abs(selected.point.timeMs - c.timeMs) <= 120_000L
    } == true
    val currentName = selected.comparison?.let { snapshot.comparisonPointName(it) } ?: snapshot.currentPointName
    val displayZone = selected.comparison?.let { TimeZone.getTimeZone(snapshot.comparisonDisplayZoneId(it)) } ?: snapshot.displayTimeZone
    val moment = if (selected.comparison != null) null else if (selected.isCurrent) snapshot.currentTime else snapshot.markerTimes[selected.point.timeMs]
    val time = moment ?: formatSpaceCompassPanoramaMarkerTime(selected.point.timeMs,referenceTime,
        displayZone, snapshot.formatting)
    if (selected.isAntipode) {
        val event = if (anchor) currentName else
            spaceCompassPanoramaPointEventName(selected.point, snapshot)
        val name = listOf(snapshot.orbitExportVocabulary.antipode, event).filter { it.isNotBlank() }.joinToString(" · ")
        return formatSpaceCompassPanoramaPointLabel(time, selected.point.position, snapshot.formatting, name,
            formatSpaceCompassPanoramaPointCountdown(selected.point.timeMs, referenceTime, snapshot.formatting))
    }
    return if (anchor) formatSpaceCompassPanoramaPointLabel(time, selected.point.position,
        snapshot.formatting, currentName,
        formatSpaceCompassPanoramaPointCountdown(selected.point.timeMs, referenceTime, snapshot.formatting))
    else formatSpaceCompassPanoramaPathPointLabel(time, selected.point, snapshot.formatting,
        snapshot.pointEventNames, referenceTime, snapshot.bodyNames)
}

private fun spaceCompassPanoramaPointEventName(point: SpaceCompassSunPathPoint,
    snapshot: SpaceCompassPanoramaSnapshot): String = point.events.filter { it != SpaceCompassSunPathEvent.HOUR }
    .sortedBy { it.ordinal }.mapNotNull { snapshot.pointEventNames[it] }.joinToString(" · ")

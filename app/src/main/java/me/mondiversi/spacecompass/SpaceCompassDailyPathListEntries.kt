package me.mondiversi.spacecompass

import java.time.Instant

/** A live snapshot is separate from hourly/event markers, even at the same instant. */
internal data class SpaceCompassDailyPathListEntry(
    val point: SpaceCompassSunPathPoint, val markerIndex: Int? = null, val sourcePath: SpaceCompassSunDailyPath? = null
) {
    val isCurrent: Boolean get() = markerIndex == null && sourcePath == null
    val pointId: String get() = if (isCurrent) "current" else if (sourcePath == null) markerIndex.toString()
        else "saved-${point.timeMs}-${point.event.name}"
}

/** Reuse the displayed position; do not regenerate a cached daily curve just to add now. */
internal fun spaceCompassCurrentPathPoint(body: SpaceCompassCelestialBody, timeMs: Long,
    position: SpaceCompassSunPosition?): SpaceCompassSunPathPoint? {
    if (position == null || !position.azimuthDegrees.isFinite() || !position.elevationDegrees.isFinite() ||
        position.elevationDegrees !in -90.0..90.0) return null
    return SpaceCompassSunPathPoint(timeMs, position,
        moonPhase = if (body == SpaceCompassCelestialBody.MOON) calculateSpaceCompassMoonPhase(timeMs) else null)
}

/** Preserve marker IDs/numbers and event rows while inserting one valid, chronological snapshot. */
internal fun spaceCompassDailyPathListEntries(path: SpaceCompassSunDailyPath,
    current: SpaceCompassSunPathPoint?): List<SpaceCompassDailyPathListEntry> {
    val entries = path.markers.mapIndexed { index, point -> SpaceCompassDailyPathListEntry(point, index) }
    val first = path.samples.firstOrNull()?.timeMs ?: return entries
    val last = path.samples.last().timeMs
    val live = current?.takeIf {
        it.timeMs in first..last && it.position.azimuthDegrees.isFinite() &&
            it.position.elevationDegrees.isFinite() && it.position.elevationDegrees in -90.0..90.0 &&
            (path.body.isEarthSatellite || Instant.ofEpochMilli(it.timeMs).atZone(path.zone).toLocalDate() == path.date)
    } ?: return entries
    // Stable sorting puts Current before a coincident hourly/event row without removing that event.
    return (listOf(SpaceCompassDailyPathListEntry(live)) + entries).sortedBy { it.point.timeMs }
}

/** Preserve historical selections when a satellite replaces its rolling marker window. */
internal fun spaceCompassSelectedDailyPathEntries(path: SpaceCompassSunDailyPath,
    current: SpaceCompassSunPathPoint?, selected: List<SpaceCompassSelectedOrbitPoint>): List<SpaceCompassDailyPathListEntry> {
    val entries = spaceCompassDailyPathListEntries(path, current)
    val historical = selected.filter { saved -> !saved.key.isCurrent && saved.key.body == path.body &&
        saved.key.date == path.date && saved.key.zoneId == path.zone.id && saved.path != null &&
        entries.none { !it.isCurrent && spaceCompassOrbitPointKeysMatch(saved.key,
            spaceCompassOrbitPointKey(path.body, it.point, path.zone, path.date)) }
    }.map { SpaceCompassDailyPathListEntry(it.point, sourcePath = it.path) }
    return (entries + historical).sortedBy { it.point.timeMs }
}

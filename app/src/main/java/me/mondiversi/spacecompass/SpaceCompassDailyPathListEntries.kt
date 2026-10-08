package me.mondiversi.spacecompass

import java.time.Instant

/** A live snapshot is separate from hourly/event markers, even at the same instant. */
internal data class SpaceCompassDailyPathListEntry(
    val point: SpaceCompassSunPathPoint, val markerIndex: Int? = null
) {
    val isCurrent: Boolean get() = markerIndex == null
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

package me.mondiversi.spacecompass

internal fun SpaceCompassPanoramaSnapshot.comparisonDisplayZoneId(c: SpaceCompassTemporalComparison): String =
    if (positionHidden) "UTC" else c.zoneId

internal fun SpaceCompassPanoramaObject.displayTimeZone(snapshot: SpaceCompassPanoramaSnapshot): java.util.TimeZone =
    comparison?.let { java.util.TimeZone.getTimeZone(snapshot.comparisonDisplayZoneId(it)) } ?: snapshot.displayTimeZone

/** Each frozen marker inherits its event name and display clock; the live scene keeps Current. */
internal fun SpaceCompassPanoramaSnapshot.forCurrentInstance(c: SpaceCompassTemporalComparison?): SpaceCompassPanoramaSnapshot =
    if (c == null) this else copy(timeMs = c.timeMs, currentTime = null,
        zoneId = comparisonDisplayZoneId(c), displayZoneId = comparisonDisplayZoneId(c), currentPointName = comparisonPointName(c))

internal fun SpaceCompassPanoramaSnapshot.isActive(item: SpaceCompassPanoramaObject): Boolean =
    if (item.comparison != null) item.comparison.id == activeComparisonId
    else activeComparisonId == null && item.body == activeBody

internal fun SpaceCompassPanoramaObject.currentPosition(snapshot: SpaceCompassPanoramaSnapshot): SpaceCompassSunPosition? =
    if (comparison?.hasReference == false) null else
        calculateSpaceCompassCelestialObservation(body, referenceTime(snapshot.timeMs), snapshot.latitude,
            snapshot.longitude, snapshot.altitude, snapshot.remote)?.position

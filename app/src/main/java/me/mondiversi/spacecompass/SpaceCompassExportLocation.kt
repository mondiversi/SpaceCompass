package me.mondiversi.spacecompass

import java.util.TimeZone

/** Raw observer metadata is frozen separately from the orbit geometry. */
internal data class SpaceCompassExportLocation(val latitude: Double, val longitude: Double,
    val altitudeMeters: Double?, val accuracyMeters: Double?)

internal val SpaceCompassPanoramaSnapshot.displayTimeZone: TimeZone
    get() = TimeZone.getTimeZone(displayZoneId ?: zoneId)

/** Keep the calculation zone and selection keys intact; only displayed timestamps change. */
internal fun spaceCompassSnapshotForDisclosure(snapshot: SpaceCompassPanoramaSnapshot,
    position: SpaceCompassPanoramaPosition): SpaceCompassPanoramaSnapshot {
    val zoneId = if (position == SpaceCompassPanoramaPosition.HIDDEN) "UTC" else snapshot.zoneId
    val zone = TimeZone.getTimeZone(zoneId)
    val times = snapshot.markerTimes.keys + snapshot.objects.flatMap { it.path?.markers.orEmpty() }.map { it.timeMs } +
        snapshot.selectedPoints.map { it.point.timeMs }
    return snapshot.copy(displayZoneId=zoneId,
        currentTime=formatSpaceCompassPanoramaExportTime(snapshot.timeMs,zone,snapshot.formatting),
        markerTimes=times.associateWith { formatSpaceCompassPanoramaExportTime(it,zone,snapshot.formatting) })
}

internal fun spaceCompassDailyExportCaption(snapshot: SpaceCompassPanoramaSnapshot,
    profile: SpaceCompassDataExportProfile, location: SpaceCompassExportLocation?, place: SpaceCompassPlaceParts?,
    position: SpaceCompassPanoramaPosition): String {
    val rows = location?.let { spaceCompassPanoramaExportRows(it.latitude,it.longitude,it.altitudeMeters,it.accuracyMeters,
        profile.strings.getString(R.string.sun_finder_altitude,SPACE_COMPASS_SUN_DATA_MARKER),"",profile.formatting) }.orEmpty()
    val data = SpaceCompassPanoramaCaptionData(
        formatSpaceCompassPanoramaExportTimestamp(snapshot.timeMs,TimeZone.getTimeZone(snapshot.zoneId),profile.formatting),
        place?.let(::formatSpaceCompassEstimatedPlace),formatSpaceCompassPanoramaArea(place),rows,null,
        hiddenTimestamp=formatSpaceCompassPanoramaExportTimestamp(snapshot.timeMs,TimeZone.getTimeZone("UTC"),profile.formatting))
    return snapshot.objects.single().name+" · "+spaceCompassPanoramaCaptionForPosition(data,position)
}

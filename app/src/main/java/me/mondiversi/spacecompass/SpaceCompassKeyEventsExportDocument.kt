package me.mondiversi.spacecompass

import java.time.Instant
import java.util.TimeZone

internal data class SpaceCompassKeyEventsSnapshot(val calendar: SpaceCompassObservationCalendar,
    val place: SpaceCompassObservationPlace, val events: List<SpaceCompassKeyEvent>,
    val observationTimeMs: Long, val capturedAtMs: Long)

internal fun captureSpaceCompassKeyEvents(result: SpaceCompassKeyEventsUiState,
    observationTimeMs: Long, capturedAtMs: Long): SpaceCompassKeyEventsSnapshot {
    require(result.complete)
    val request = requireNotNull(result.calculationRequest)
    val events = requireNotNull(result.events).toList()
    val bounds = spaceCompassKeyEventYearBounds(request.calendar)
    require(events.isNotEmpty() && events.all { it.timeMs in bounds })
    require(events.zipWithNext().all { (a,b) -> a.timeMs <= b.timeMs })
    return SpaceCompassKeyEventsSnapshot(request.calendar, request.place, events, observationTimeMs, capturedAtMs)
}

/** Share the reviewed annual CSV/TXT rendering and privacy policy, without elevation-only notes. */
internal fun spaceCompassKeyEventsDocument(snapshot: SpaceCompassKeyEventsSnapshot,
    profile: SpaceCompassDataExportProfile, place: SpaceCompassPlaceParts?,
    disclosure: SpaceCompassPanoramaPosition): SpaceCompassSkyHeightsDocument {
    val strings = profile.strings; val f = profile.formatting
    val zone = TimeZone.getTimeZone(if (disclosure == SpaceCompassPanoramaPosition.HIDDEN) "UTC" else snapshot.calendar.zone.id)
    fun date(time: Long) = formatSpaceCompassDateOnly(time, f.date, f.deviceLocale, zone,
        f.numeric, f.deviceLocale, allowHistorical = true)
    fun clock(time: Long) = formatSpaceCompassTimeOnly(time, f.time, f.locale, zone,
        f.numeric, f.deviceLocale, includeSeconds = true, allowHistorical = true)
    fun offset(time: Long) = formatSpaceCompassObserverUtcOffset(zone.toZoneId().rules.getOffset(Instant.ofEpochMilli(time)))
    fun timestamp(time: Long) = date(time) + " · " + clock(time) + " · " + offset(time)
    val body = snapshot.calendar.body
    val year = formatSpaceCompassNumber(snapshot.calendar.year.toDouble(), 0, f.numeric, grouping = false, systemLocale = f.deviceLocale)
    val title = strings.getString(R.string.key_events_title) + " · " + strings.getString(body.nameResource) + " · " + year
    val observed = timestamp(snapshot.observationTimeMs)
    val metadata = mutableListOf(strings.getString(R.string.capture_table_captured) to timestamp(snapshot.capturedAtMs),
        strings.getString(R.string.capture_table_observed) to observed)
    if (disclosure != SpaceCompassPanoramaPosition.HIDDEN) {
        val fix = snapshot.place
        val caption = SpaceCompassPanoramaCaptionData(observed, place?.let(::formatSpaceCompassEstimatedPlace),
            formatSpaceCompassPanoramaArea(place), spaceCompassPanoramaExportRows(fix.latitude, fix.longitude,
                fix.altitude, null, strings.getString(R.string.sun_finder_altitude, SPACE_COMPASS_SUN_DATA_MARKER), "", f), null)
        metadata += strings.getString(R.string.capture_table_context) to spaceCompassPanoramaCaptionForPosition(caption, disclosure)
    }
    return SpaceCompassSkyHeightsDocument(title, metadata, listOf(strings.getString(R.string.capture_table_events),
        strings.getString(R.string.format_date_time_heading), strings.getString(R.string.format_time_heading),
        strings.getString(R.string.observer_zone), strings.getString(R.string.capture_table_elevation) + " (°)",
        strings.getString(R.string.key_events_value)), snapshot.events.map { event ->
        listOf(strings.getString(event.kind.title), date(event.timeMs), clock(event.timeMs), offset(event.timeMs),
            formatSpaceCompassPanoramaElevation(event.elevation, f), spaceCompassKeyEventValue(event, body, profile))
    }, listOf(strings.getString(if (body == SpaceCompassCelestialBody.SUN) R.string.key_events_horizon_note
        else R.string.key_events_global_note)),
        formatSpaceCompassNumber(1.5,1,f.numeric,grouping=false,systemLocale=f.deviceLocale).contains(','))
}

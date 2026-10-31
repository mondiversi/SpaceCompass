package me.mondiversi.spacecompass

import java.time.Instant
import java.util.TimeZone

internal data class SpaceCompassSkyHeightsSnapshot(val calendar: SpaceCompassObservationCalendar,
    val place: SpaceCompassObservationPlace, val moments: List<SpaceCompassObservationMoment>,
    val observationTimeMs: Long, val capturedAtMs: Long)
internal data class SpaceCompassSkyHeightsDocument(val title: String, val metadata: List<Pair<String, String>>,
    val columns: List<String>, val rows: List<List<String>>, val notes: List<String>, val decimalComma: Boolean)

/** Freeze the accepted calculation inputs, never a newer GPS fix or a partial/stale year. */
internal fun captureSpaceCompassSkyHeights(result: SpaceCompassObservationMomentsUiState,
    observationTimeMs: Long, capturedAtMs: Long): SpaceCompassSkyHeightsSnapshot {
    require(result.complete)
    val request = requireNotNull(result.calculationRequest)
    val moments = requireNotNull(result.moments).toList()
    require(moments.isNotEmpty() && moments.all {
        it.point.position.elevationDegrees.isFinite() &&
            Instant.ofEpochMilli(it.point.timeMs).atZone(request.calendar.zone).year == request.calendar.year
    })
    return SpaceCompassSkyHeightsSnapshot(request.calendar, request.place, moments, observationTimeMs, capturedAtMs)
}

private fun skyHeightsDate(time: Long, zone: TimeZone, format: SpaceCompassPanoramaFormatting) =
    formatSpaceCompassDateOnly(time, format.date, format.deviceLocale, zone,
        format.numeric, format.deviceLocale, allowHistorical = true)
private fun skyHeightsClock(time: Long, zone: TimeZone, format: SpaceCompassPanoramaFormatting) =
    formatSpaceCompassTimeOnly(time, format.time, format.locale, zone,
        format.numeric, format.deviceLocale, includeSeconds = true, allowHistorical = true)
private fun skyHeightsTimestamp(time: Long, zone: TimeZone, format: SpaceCompassPanoramaFormatting): String =
    skyHeightsDate(time, zone, format) + " · " + skyHeightsClock(time, zone, format) + " · " +
        formatSpaceCompassObserverUtcOffset(zone.toZoneId().rules.getOffset(Instant.ofEpochMilli(time)))

internal fun spaceCompassSkyHeightsDocument(snapshot: SpaceCompassSkyHeightsSnapshot,
    profile: SpaceCompassDataExportProfile, place: SpaceCompassPlaceParts?,
    disclosure: SpaceCompassPanoramaPosition): SpaceCompassSkyHeightsDocument {
    val strings = profile.strings
    val format = profile.formatting
    val zone = TimeZone.getTimeZone(if (disclosure == SpaceCompassPanoramaPosition.HIDDEN) "UTC" else snapshot.calendar.zone.id)
    val name = strings.getString(snapshot.calendar.body.nameResource)
    val year = formatSpaceCompassNumber(snapshot.calendar.year.toDouble(), 0, format.numeric,
        grouping = false, systemLocale = format.deviceLocale)
    val title = strings.getString(R.string.observation_heights_title) + " · $name · $year"
    val observed = skyHeightsTimestamp(snapshot.observationTimeMs, zone, format)
    val metadata = mutableListOf(strings.getString(R.string.capture_table_captured) to
        skyHeightsTimestamp(snapshot.capturedAtMs, zone, format),
        strings.getString(R.string.capture_table_observed) to observed)
    if (disclosure != SpaceCompassPanoramaPosition.HIDDEN) {
        val fix = snapshot.place
        val caption = SpaceCompassPanoramaCaptionData(observed, place?.let(::formatSpaceCompassEstimatedPlace),
            formatSpaceCompassPanoramaArea(place), spaceCompassPanoramaExportRows(fix.latitude, fix.longitude,
                fix.altitude, null, strings.getString(R.string.sun_finder_altitude, SPACE_COMPASS_SUN_DATA_MARKER),
                "", format), null)
        metadata += strings.getString(R.string.capture_table_context) to spaceCompassPanoramaCaptionForPosition(caption, disclosure)
    }
    val columns = listOf(strings.getString(R.string.capture_table_events), strings.getString(R.string.format_date_time_heading),
        strings.getString(R.string.format_time_heading), strings.getString(R.string.observer_zone),
        strings.getString(R.string.capture_table_elevation) + " (°)")
    val rows = spaceCompassObservationMomentRows(snapshot.moments).map { entry ->
        val point = requireNotNull(entry.point)
        listOf(spaceCompassObservationMomentTitle(entry, profile), skyHeightsDate(point.timeMs, zone, format),
            skyHeightsClock(point.timeMs, zone, format),
            formatSpaceCompassObserverUtcOffset(zone.toZoneId().rules.getOffset(Instant.ofEpochMilli(point.timeMs))),
            formatSpaceCompassPanoramaElevation(point.position.elevationDegrees, format))
    }
    val notes = listOf(strings.getString(R.string.observation_moments_midpoint_note)) +
        snapshot.calendar.body.fictionalReference?.let {
            listOf(strings.getString(R.string.scifi_reference_note),
                strings.getString(R.string.scifi_reference) + ": ${it.universe} · ${it.referenceName}")
        }.orEmpty()
    return SpaceCompassSkyHeightsDocument(title, metadata, columns, rows, notes,
        formatSpaceCompassNumber(1.5, 1, format.numeric, grouping = false, systemLocale = format.deviceLocale).contains(','))
}

internal fun renderSpaceCompassSkyHeightsCsv(document: SpaceCompassSkyHeightsDocument): String {
    val size = document.columns.size
    fun padded(row: List<String>) = row + List((size - row.size).coerceAtLeast(0)) { "" }
    val grid = listOf(padded(listOf(document.title))) + document.metadata.map { padded(listOf(it.first, it.second)) } +
        listOf(document.columns) + document.rows + document.notes.map { padded(listOf(it)) }
    val separator = if (document.decimalComma) ";" else ","
    return "\uFEFF" + grid.joinToString("\r\n") { row ->
        row.joinToString(separator) { spaceCompassOrbitCsvCell(it) }
    } + "\r\n"
}

internal fun renderSpaceCompassSkyHeightsTxt(document: SpaceCompassSkyHeightsDocument): String = buildString {
    append(document.title).append('\n')
    document.metadata.forEach { (label, value) -> append(label).append(": ").append(value).append('\n') }
    append('\n')
    val widths = document.columns.indices.map { i ->
        maxOf(document.columns[i].length, document.rows.maxOfOrNull { it[i].length } ?: 0)
    }
    (listOf(document.columns) + document.rows).forEachIndexed { index, row ->
        append(row.mapIndexed { i, value -> value.padEnd(widths[i]) }.joinToString(" | ").trimEnd()).append('\n')
        if (index == 0) append(widths.joinToString("-+-") { "─".repeat(it) }).append('\n')
    }
    document.notes.forEach { append('\n').append(it).append('\n') }
}

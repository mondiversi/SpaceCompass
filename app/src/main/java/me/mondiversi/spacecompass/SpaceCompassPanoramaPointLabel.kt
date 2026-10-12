package me.mondiversi.spacecompass

import kotlin.math.abs
import java.util.Locale

/** Both capture modes use the same compact row: optional event, clock, T, azimuth, elevation. */
internal fun formatSpaceCompassPanoramaPointLabel(moment: String, position: SpaceCompassSunPosition,
    formatting: SpaceCompassPanoramaFormatting, pointName: String? = null, countdown: String? = null): String {
    val azimuth = formatSpaceCompassPanoramaElevation(position.azimuthDegrees, formatting)
    val elevation = formatSpaceCompassPanoramaElevation(position.elevationDegrees, formatting)
    val sign = if (position.elevationDegrees > 0 && abs(position.elevationDegrees) >= .05) "+" else ""
    return listOfNotNull(pointName?.takeIf(String::isNotBlank), moment,
        countdown?.takeIf(String::isNotBlank), "$azimuth°", "$sign$elevation°").joinToString(" · ")
}

/** Freeze elapsed/remaining whole minutes to the captured observation, never the export clock. */
internal fun formatSpaceCompassPanoramaPointCountdown(timeMs: Long, captureTimeMs: Long,
    formatting: SpaceCompassPanoramaFormatting): String = formatSpaceCompassPointCountdown(
    timeMs, captureTimeMs, if (formatting.numeric == SpaceCompassNumericFormat.SYSTEM) formatting.deviceLocale else Locale.ROOT)

internal val spaceCompassPanoramaDefaultEventNames = mapOf(
    SpaceCompassSunPathEvent.SUNRISE to "Rise",
    SpaceCompassSunPathEvent.CULMINATION to "Culmination",
    SpaceCompassSunPathEvent.SUNSET to "Set",
    SpaceCompassSunPathEvent.MINIMUM to "Minimum")

/** Hourly dots have no point number; named and coincident events retain their own vocabulary. */
internal fun formatSpaceCompassPanoramaPathPointLabel(moment: String, point: SpaceCompassSunPathPoint,
    formatting: SpaceCompassPanoramaFormatting, eventNames: Map<SpaceCompassSunPathEvent, String>,
    captureTimeMs: Long): String {
    val name = (listOf(point.event) + point.coincidentEvents.sortedBy { it.ordinal }).distinct()
        .filter { it != SpaceCompassSunPathEvent.HOUR }.mapNotNull(eventNames::get).joinToString(" · ")
    return formatSpaceCompassPanoramaPointLabel(moment, point.position, formatting, name,
        formatSpaceCompassPanoramaPointCountdown(point.timeMs, captureTimeMs, formatting))
}

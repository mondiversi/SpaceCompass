package me.mondiversi.spacecompass

import kotlin.math.abs
import java.util.Locale

/** Capture labels put optional event titles above clock/T and azimuth/elevation rows. */
internal fun formatSpaceCompassPanoramaPointLabel(moment: String, position: SpaceCompassSunPosition,
    formatting: SpaceCompassPanoramaFormatting, pointName: String? = null, countdown: String? = null): String {
    val azimuth = formatSpaceCompassPanoramaElevation(position.azimuthDegrees, formatting)
    val elevation = formatSpaceCompassPanoramaElevation(position.elevationDegrees, formatting)
    val sign = if (position.elevationDegrees > 0 && abs(position.elevationDegrees) >= .05) "+" else ""
    val momentLine = listOfNotNull(moment, countdown?.takeIf(String::isNotBlank)).joinToString(" · ")
    return listOfNotNull(pointName?.takeIf(String::isNotBlank), momentLine,
        "$azimuth° · $sign$elevation°").joinToString("\n")
}

/** Freeze elapsed/remaining whole minutes to the captured observation, never the export clock. */
internal fun formatSpaceCompassPanoramaPointCountdown(timeMs: Long, captureTimeMs: Long,
    formatting: SpaceCompassPanoramaFormatting): String = formatSpaceCompassPointCountdown(
    timeMs, captureTimeMs, if (formatting.numeric == SpaceCompassNumericFormat.SYSTEM) formatting.deviceLocale else Locale.ROOT)

internal val spaceCompassPanoramaDefaultEventNames = mapOf(
    SpaceCompassSunPathEvent.SUNRISE to "Rise",
    SpaceCompassSunPathEvent.CULMINATION to "Culmination",
    SpaceCompassSunPathEvent.SUNSET to "Set",
    SpaceCompassSunPathEvent.MINIMUM to "Minimum", SpaceCompassSunPathEvent.APPROACH to "Close approach")

internal fun spaceCompassApproachLabel(point: SpaceCompassSunPathPoint, label: String,
    formatting: SpaceCompassPanoramaFormatting, bodyNames: Map<SpaceCompassCelestialBody,String>): String =
    point.approach?.let { approach ->
        "$label · ${bodyNames[approach.other] ?: approach.other.name.replace('_',' ')} · " +
            formatSpaceCompassPanoramaElevation(approach.separationDegrees,formatting) + "°"
    } ?: label

/** Hourly dots have no point number; named and coincident events retain their own vocabulary. */
internal fun formatSpaceCompassPanoramaPathPointLabel(moment: String, point: SpaceCompassSunPathPoint,
    formatting: SpaceCompassPanoramaFormatting, eventNames: Map<SpaceCompassSunPathEvent, String>,
    captureTimeMs: Long, bodyNames: Map<SpaceCompassCelestialBody,String> = emptyMap()): String {
    val name = (listOf(point.event) + point.coincidentEvents.sortedBy { it.ordinal }).distinct()
        .filter { it != SpaceCompassSunPathEvent.HOUR }.mapNotNull { event -> eventNames[event]?.let {
            if (event == SpaceCompassSunPathEvent.APPROACH) spaceCompassApproachLabel(point,it,formatting,bodyNames) else it
        } }.joinToString(" · ")
    return formatSpaceCompassPanoramaPointLabel(moment, point.position, formatting, name,
        formatSpaceCompassPanoramaPointCountdown(point.timeMs, captureTimeMs, formatting))
}

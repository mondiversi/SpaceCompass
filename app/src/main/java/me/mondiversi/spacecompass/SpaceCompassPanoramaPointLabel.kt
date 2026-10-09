package me.mondiversi.spacecompass

import kotlin.math.abs

/** Shared by photographed and panoramic points; values use the frozen export/preview profile. */
internal fun formatSpaceCompassPanoramaPointLabel(moment: String, elevationDegrees: Double,
    formatting: SpaceCompassPanoramaFormatting, pointName: String? = null): String {
    val elevation = formatSpaceCompassPanoramaElevation(elevationDegrees, formatting)
    val sign = if (elevationDegrees > 0 && abs(elevationDegrees) >= .05) "+" else ""
    return listOfNotNull(pointName?.takeIf(String::isNotBlank), moment, "$sign$elevation°").joinToString(" · ")
}

internal val spaceCompassPanoramaDefaultEventNames = mapOf(
    SpaceCompassSunPathEvent.SUNRISE to "Rise",
    SpaceCompassSunPathEvent.CULMINATION to "Culmination",
    SpaceCompassSunPathEvent.SUNSET to "Set",
    SpaceCompassSunPathEvent.MINIMUM to "Minimum")

/** Name events per marker, since different objects can share a timestamp without sharing an event. */
internal fun formatSpaceCompassPanoramaPathPointLabel(moment: String, point: SpaceCompassSunPathPoint,
    formatting: SpaceCompassPanoramaFormatting, eventNames: Map<SpaceCompassSunPathEvent, String>): String {
    val name = (listOf(point.event) + point.coincidentEvents.sortedBy { it.ordinal }).distinct()
        .filter { it != SpaceCompassSunPathEvent.HOUR }.mapNotNull(eventNames::get).joinToString(" · ")
    return formatSpaceCompassPanoramaPointLabel(moment, point.position.elevationDegrees, formatting, name)
}

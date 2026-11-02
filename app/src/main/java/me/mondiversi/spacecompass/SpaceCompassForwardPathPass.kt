package me.mondiversi.spacecompass

/** Summarize the current/first upcoming pass from the displayed window itself.
 * A pass crossing either endpoint is partial; never import past or >24-hour points.
 */
internal fun spaceCompassForwardSatellitePass(path: SpaceCompassSunDailyPath): List<SpaceCompassSunPathPoint> {
    if (!path.forward24Hours || !path.body.isEarthSatellite || path.samples.size < 2) return emptyList()
    val start=path.samples.first();val end=path.samples.last().timeMs
    val markers=path.markers.filter { it.timeMs > start.timeMs && it.timeMs <= end }.sortedBy { it.timeMs }
    val currentAbove=start.position.elevationDegrees > 0 ||
        start.position.elevationDegrees == 0.0 && path.samples[1].position.elevationDegrees >= 0
    val from=if(currentAbove) start.timeMs else markers.firstOrNull {
        SpaceCompassSunPathEvent.SUNRISE in it.events
    }?.timeMs ?: return emptyList()
    val until=markers.firstOrNull { it.timeMs > from && SpaceCompassSunPathEvent.SUNSET in it.events }?.timeMs ?: end
    val events=markers.filter { it.timeMs in from..until &&
        (SpaceCompassSunPathEvent.SUNRISE in it.events || SpaceCompassSunPathEvent.SUNSET in it.events ||
            SpaceCompassSunPathEvent.CULMINATION in it.events && it.position.elevationDegrees >= 0) }
    return (if(currentAbove) listOf(start) else emptyList())+events
}

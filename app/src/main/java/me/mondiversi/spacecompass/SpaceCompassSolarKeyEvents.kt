package me.mondiversi.spacecompass

import java.time.Instant

/** Same geometric solar model and -0.833° ideal-horizon convention as the daily path.
 * Observer height contributes topocentric parallax, never an invented terrain horizon.
 * https://aa.usno.navy.mil/faq/RST_defs */
internal fun calculateSpaceCompassSolarKeyEvents(calendar: SpaceCompassObservationCalendar,
    place: SpaceCompassObservationPlace, checkCancelled: () -> Unit = {}): List<SpaceCompassKeyEvent> {
    val bounds = spaceCompassKeyEventYearBounds(calendar)
    fun position(time: Long) = calculateSpaceCompassSunPosition(time, place.latitude, place.longitude, place.altitude)
    fun elevation(time: Long) = position(time).elevationDegrees
    // Both endpoints of polar periods crossing New Year are needed for their full duration.
    val margin = 200 * 86_400_000L
    val curve = requireNotNull(spaceCompassEventCurve(bounds.first-margin, bounds.last+1+margin,
        7_200_000L, ::elevation, checkCancelled))
    val crossings = spaceCompassEventCrossings(curve, -.833, ::elevation, checkCancelled)
    val result = mutableListOf<SpaceCompassKeyEvent>()
    fun add(kind: SpaceCompassKeyEventKind, time: Long, duration: Long? = null) {
        if (time in bounds) result += SpaceCompassKeyEvent(kind, time, elevation(time), durationMs = duration)
    }
    val polar = crossings.zipWithNext().filter { (a,b) -> b.timeMs-a.timeMs > 25*3_600_000L }
    for ((a,b) in polar) {
        val duration = b.timeMs-a.timeMs
        add(if (a.rising) SpaceCompassKeyEventKind.POLAR_DAY_START else SpaceCompassKeyEventKind.POLAR_NIGHT_START, a.timeMs, duration)
        add(if (a.rising) SpaceCompassKeyEventKind.POLAR_DAY_END else SpaceCompassKeyEventKind.POLAR_NIGHT_END, b.timeMs, duration)
    }
    val daylight = crossings.zipWithNext().filter { (a,b) -> a.rising && !b.rising &&
        a.timeMs in bounds && b.timeMs-a.timeMs <= 25*3_600_000L }
    fun polarIntersectsYear(day: Boolean) = polar.any { (a,b) ->
        a.rising == day && a.timeMs <= bounds.last && b.timeMs >= bounds.first }
    // A polar season has many equally long/short days: its interval is the meaningful result.
    if (!polarIntersectsYear(true)) daylight.maxByOrNull { (a,b) -> b.timeMs-a.timeMs }?.let { (a,b) ->
        add(SpaceCompassKeyEventKind.LONGEST_DAY, a.timeMs+(b.timeMs-a.timeMs)/2, b.timeMs-a.timeMs) }
    if (!polarIntersectsYear(false)) daylight.minByOrNull { (a,b) -> b.timeMs-a.timeMs }?.let { (a,b) ->
        add(SpaceCompassKeyEventKind.SHORTEST_DAY, a.timeMs+(b.timeMs-a.timeMs)/2, b.timeMs-a.timeMs) }
    for (rising in listOf(true,false)) {
        val candidates = crossings.filter { it.rising == rising && it.timeMs in bounds }
        if (candidates.size < 2) continue
        fun clock(crossing: SpaceCompassSolarCrossing) =
            Instant.ofEpochMilli(crossing.timeMs).atZone(calendar.zone).toLocalTime().toNanoOfDay()
        candidates.minByOrNull(::clock)?.let { add(if (rising) SpaceCompassKeyEventKind.EARLIEST_SUNRISE
            else SpaceCompassKeyEventKind.EARLIEST_SUNSET, it.timeMs) }
        candidates.maxByOrNull(::clock)?.let { add(if (rising) SpaceCompassKeyEventKind.LATEST_SUNRISE
            else SpaceCompassKeyEventKind.LATEST_SUNSET, it.timeMs) }
    }
    return result.sortedBy { it.timeMs }
}

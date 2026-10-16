package me.mondiversi.spacecompass

internal enum class SpaceCompassElevationTrend { RISING, FALLING }

/** Follow elevation, not azimuth or the time relative to now. At a turning point show the outgoing phase. */
internal fun spaceCompassElevationTrend(point: SpaceCompassSunPathPoint,
    samples: List<SpaceCompassSunPathPoint>, events: List<SpaceCompassSunPathPoint> = emptyList()): SpaceCompassElevationTrend? {
    if (!point.position.elevationDegrees.isFinite() || point.position.elevationDegrees !in -90.0..90.0) return null
    if (SpaceCompassSunPathEvent.CULMINATION in point.events) return SpaceCompassElevationTrend.FALLING
    if (SpaceCompassSunPathEvent.MINIMUM in point.events) return SpaceCompassElevationTrend.RISING
    if (SpaceCompassSunPathEvent.SUNRISE in point.events) return SpaceCompassElevationTrend.RISING
    if (SpaceCompassSunPathEvent.SUNSET in point.events) return SpaceCompassElevationTrend.FALLING
    if (samples.size < 2 || point.timeMs !in samples.first().timeMs..samples.last().timeMs) return null
    val found = samples.binarySearchBy(point.timeMs) { it.timeMs }
    val left = if (found >= 0) found.coerceAtMost(samples.lastIndex - 1) else -found - 2
    var a = samples[left]; var b = samples[left + 1]
    // An exact extremum can fall between coarse samples: split there before deciding Current's phase.
    events.filter { it.timeMs in a.timeMs..b.timeMs && it.position.elevationDegrees.isFinite() &&
        it.position.elevationDegrees in -90.0..90.0 &&
        (SpaceCompassSunPathEvent.CULMINATION in it.events || SpaceCompassSunPathEvent.MINIMUM in it.events)
    }.sortedBy { it.timeMs }.forEach {
        if (it.timeMs <= point.timeMs) a = it else if (it.timeMs < b.timeMs) b = it
    }
    if (a.timeMs >= b.timeMs || listOf(a, b).any {
            !it.position.elevationDegrees.isFinite() || it.position.elevationDegrees !in -90.0..90.0
        }) return null
    val change = b.position.elevationDegrees - a.position.elevationDegrees
    return when {
        change > 1e-7 -> SpaceCompassElevationTrend.RISING
        change < -1e-7 -> SpaceCompassElevationTrend.FALLING
        else -> null // Missing or level data must not invent a direction.
    }
}

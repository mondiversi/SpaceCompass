package me.mondiversi.planetcompass

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/** Absolute height extremum in this displayed interval, not an assumed midnight/lower transit.
 * Civil days are half-open (including DST); endExclusive may include an ISS revolution's endpoint.
 * Endpoint candidates matter at exact poles and when a moving body's extremum lies outside the day.
 */
internal fun refinePlanetCompassPathExtremum(
    samples: List<PlanetCompassSunPathPoint>, endExclusive: Long, minimum: Boolean,
    point: (Long, PlanetCompassSunPathEvent) -> PlanetCompassSunPathPoint?
): PlanetCompassSunPathPoint? {
    if (samples.isEmpty() || endExclusive <= samples.first().timeMs) return null
    val event = if (minimum) PlanetCompassSunPathEvent.MINIMUM else PlanetCompassSunPathEvent.CULMINATION
    val limit = endExclusive - 1
    val index = if (minimum) samples.indices.minBy { samples[it].position.elevationDegrees }
        else samples.indices.maxBy { samples[it].position.elevationDegrees }
    // A 25-hour civil day can contain two troughs. Refine every local candidate, not just the
    // lowest coarse sample. A flat trajectory needs only the fallback bracket and endpoints.
    val indices = (1 until samples.lastIndex).filter { i ->
        val previous = samples[i - 1].position.elevationDegrees
        val current = samples[i].position.elevationDegrees
        val next = samples[i + 1].position.elevationDegrees
        if (minimum) (current < previous && current <= next) || (current <= previous && current < next)
        else (current > previous && current >= next) || (current >= previous && current > next)
    }
    val times = mutableListOf(samples.first().timeMs, limit, samples[index].timeMs.coerceAtMost(limit))
    for (candidate in (indices + index).distinct()) {
        var low = samples[max(0, candidate - 1)].timeMs.coerceAtMost(limit)
        var high = samples[min(samples.lastIndex, candidate + 1)].timeMs.coerceAtMost(limit)
        while (high - low > 250L) {
            val third = (high - low) / 3
            val a = low + third; val b = high - third
            val elevationA = point(a, event)?.position?.elevationDegrees ?: return null
            val elevationB = point(b, event)?.position?.elevationDegrees ?: return null
            if (if (minimum) elevationA > elevationB else elevationA < elevationB) low = a else high = b
        }
        times += low + (high - low) / 2
    }
    val candidates = times.distinct().map { point(it, event) ?: return null }
    return if (minimum) candidates.minBy { it.position.elevationDegrees }
        else candidates.maxBy { it.position.elevationDegrees }
}

/** Merge only genuinely coincident named events within two 250 ms refinement steps.
 * Never merge separate instants merely because they look close near a pole, or consume hourly dots.
 */
internal fun mergeCoincidentPlanetCompassPathEvents(markers: List<PlanetCompassSunPathPoint>): List<PlanetCompassSunPathPoint> {
    val groups = mutableListOf<MutableList<PlanetCompassSunPathPoint>>()
    val directionTolerance = 2 * (1 - cos(Math.toRadians(0.002)))
    for (marker in markers.filter { it.event != PlanetCompassSunPathEvent.HOUR }.sortedBy { it.timeMs }) {
        val group = groups.lastOrNull()?.takeIf { candidates -> candidates.all {
            abs(it.timeMs - marker.timeMs) <= 500L &&
                2 * (1 - it.direction.dot(marker.direction).coerceIn(-1.0, 1.0)) <= directionTolerance
        } }
        if (group == null) groups += mutableListOf(marker) else group += marker
    }
    val merged = groups.map { group ->
        val representative = group.firstOrNull { it.event == PlanetCompassSunPathEvent.MINIMUM }
            ?: group.firstOrNull { it.event == PlanetCompassSunPathEvent.CULMINATION } ?: group.first()
        representative.copy(coincidentEvents = group.flatMap { it.events }.toSet() - representative.event)
    }
    return (markers.filter { it.event == PlanetCompassSunPathEvent.HOUR } + merged).sortedBy { it.timeMs }
}

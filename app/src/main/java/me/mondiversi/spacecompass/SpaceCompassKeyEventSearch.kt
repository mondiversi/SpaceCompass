package me.mondiversi.spacecompass

import kotlin.math.abs

internal data class SpaceCompassEventScalar(val timeMs: Long, val value: Double)

/** Bounded numerical search shared by horizon crossings and annual distance extrema.
 * Inserting refined turning points preserves near-tangent crossings shorter than the sample step. */
internal fun spaceCompassEventCurve(start: Long, end: Long, step: Long,
    evaluate: (Long) -> Double?, checkCancelled: () -> Unit): List<SpaceCompassEventScalar>? {
    require(start < end && step > 0)
    val samples = mutableListOf<SpaceCompassEventScalar>()
    var time = start
    while (true) {
        checkCancelled()
        val value = evaluate(time)?.takeIf { it.isFinite() } ?: return null
        samples += SpaceCompassEventScalar(time, value)
        if (time == end) break
        time = minOf(end, time + step)
    }
    val refined = samples.windowed(3).mapNotNull { (a, b, c) ->
        val minimum = b.value <= a.value && b.value <= c.value
        val maximum = b.value >= a.value && b.value >= c.value
        if ((!minimum && !maximum) || maxOf(abs(b.value-a.value), abs(b.value-c.value)) < 1e-10) null
        else refineSpaceCompassEventScalar(a.timeMs, c.timeMs, minimum, evaluate, checkCancelled) ?: return null
    }
    return (samples + refined).sortedBy { it.timeMs }.distinctBy { it.timeMs }
}

private fun refineSpaceCompassEventScalar(start: Long, end: Long, minimum: Boolean,
    evaluate: (Long) -> Double?, checkCancelled: () -> Unit): SpaceCompassEventScalar? {
    var low = start; var high = end
    while (high - low > 500) {
        checkCancelled()
        val third = (high-low)/3
        val a = low+third; val b = high-third
        val x = evaluate(a)?.takeIf { it.isFinite() } ?: return null
        val y = evaluate(b)?.takeIf { it.isFinite() } ?: return null
        if ((x < y) == minimum) high = b else low = a
    }
    val time = low + (high-low)/2
    return evaluate(time)?.takeIf { it.isFinite() }?.let { SpaceCompassEventScalar(time, it) }
}

internal data class SpaceCompassSolarCrossing(val timeMs: Long, val rising: Boolean)

internal fun spaceCompassEventCrossings(curve: List<SpaceCompassEventScalar>, threshold: Double,
    evaluate: (Long) -> Double, checkCancelled: () -> Unit): List<SpaceCompassSolarCrossing> =
    curve.zipWithNext().mapNotNull { (a,b) ->
        val rising = a.value < threshold && b.value >= threshold
        if (!rising && !(a.value >= threshold && b.value < threshold)) return@mapNotNull null
        var low = a.timeMs; var high = b.timeMs
        while (high-low > 250) {
            checkCancelled()
            val middle = low+(high-low)/2
            if ((evaluate(middle) >= threshold) == rising) high = middle else low = middle
        }
        SpaceCompassSolarCrossing(low+(high-low)/2, rising)
    }

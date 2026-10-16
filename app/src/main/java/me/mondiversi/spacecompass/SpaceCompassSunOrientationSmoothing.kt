package me.mondiversi.spacecompass

import kotlin.math.expm1

private const val ORIENTATION_REFERENCE_INTERVAL_NS = 40_000_000L
private const val FILTER_RESET_GAP_NS = 250_000_000L
// Match the previous 0.35 blend per 40 ms without making damping depend on callback rate.
internal const val SPACE_COMPASS_SUN_ORIENTATION_SMOOTHING_NS = 92_854_193.0
// Accelerometer-only fallback: previous 0.2 blend per nominal 60 ms UI sensor interval.
internal const val SPACE_COMPASS_SUN_GRAVITY_SMOOTHING_NS = 268_885_207.0

internal fun spaceCompassSunSmoothingWeight(elapsedNanos: Long,
    timeConstantNanos: Double = SPACE_COMPASS_SUN_ORIENTATION_SMOOTHING_NS): Double =
    if (elapsedNanos <= 0L) 0.0 else -expm1(-elapsedNanos / timeConstantNanos)

/** Filter only measured axes: geographic north and camera capture poses remain sensor-derived. */
internal fun smoothSpaceCompassSunOrientation(previous: SpaceCompassSunOrientation?,
    current: SpaceCompassSunOrientation, elapsedNanos: Long = ORIENTATION_REFERENCE_INTERVAL_NS,
    timeConstantNanos: Double = SPACE_COMPASS_SUN_ORIENTATION_SMOOTHING_NS
): SpaceCompassSunOrientation {
    // Never drag a stale pose or an inverted screen axis through a new valid measurement.
    if (previous == null || elapsedNanos > FILTER_RESET_GAP_NS ||
        previous.forward.dot(current.forward) < -0.8 || previous.right.dot(current.right) < -0.8) return current
    val blend = spaceCompassSunSmoothingWeight(elapsedNanos, timeConstantNanos)
    if (blend == 0.0) return previous
    fun mix(a: SpaceCompassSunVector, b: SpaceCompassSunVector) = SpaceCompassSunVector(
        a.east + (b.east - a.east) * blend,
        a.north + (b.north - a.north) * blend,
        a.up + (b.up - a.up) * blend
    ).normalized()
    val forward = mix(previous.forward, current.forward)
    val rawRight = mix(previous.right, current.right)
    val parallel = rawRight.dot(forward)
    val right = SpaceCompassSunVector(rawRight.east - parallel * forward.east,
        rawRight.north - parallel * forward.north, rawRight.up - parallel * forward.up).normalized()
    val up = SpaceCompassSunVector(right.north * forward.up - right.up * forward.north,
        right.up * forward.east - right.east * forward.up, right.east * forward.north - right.north * forward.east)
    return SpaceCompassSunOrientation(right, up, forward)
}

/** Change geographic reference without discarding the filter's physical motion history. */
internal fun rebaseSpaceCompassSunOrientation(orientation: SpaceCompassSunOrientation,
    declinationDelta: Double): SpaceCompassSunOrientation = SpaceCompassSunOrientation(
        orientation.right.trueNorth(declinationDelta), orientation.screenUp.trueNorth(declinationDelta),
        orientation.forward.trueNorth(declinationDelta))

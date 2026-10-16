package me.mondiversi.spacecompass

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val GYROSCOPE_MAX_AGE_NS = 250_000_000L
private const val ABSOLUTE_CORRECTION_NS = 2_000_000_000.0

/** Predict short physical movements with gyro; measured magnetic/gravity axes remain the anchor. */
internal class SpaceCompassSunMotionStabilizer {
    private data class Rate(val time: Long, val x: Double, val y: Double, val z: Double)
    private val rates = ArrayDeque<Rate>()

    fun reset() = rates.clear()

    fun recordGyroscope(timeNanos: Long, x: Double, y: Double, z: Double, reliable: Boolean) {
        if (!reliable || timeNanos <= 0 || !sqrt(x * x + y * y + z * z).isFinite()) {
            reset()
            return
        }
        val last = rates.lastOrNull()
        if (last != null && timeNanos <= last.time) return
        if (last != null && timeNanos - last.time > GYROSCOPE_MAX_AGE_NS) reset()
        rates.addLast(Rate(timeNanos, x, y, z))
        while (rates.size > 128) rates.removeFirst()
    }

    fun smooth(previous: SpaceCompassSunOrientation?, current: SpaceCompassSunOrientation,
        fromNanos: Long, toNanos: Long, nowNanos: Long, displayRotation: Int = 0
    ): SpaceCompassSunOrientation {
        val elapsed = toNanos - fromNanos
        val latest = rates.lastOrNull()
        if (previous == null || elapsed !in 1..GYROSCOPE_MAX_AGE_NS || latest == null ||
            nowNanos - latest.time !in 0..GYROSCOPE_MAX_AGE_NS || toNanos > nowNanos) {
            return smoothSpaceCompassSunOrientation(previous, current, elapsed)
        }
        val physical = spaceCompassSceneRemapOrientation(previous, displayRotation, 0)
        val predicted = predict(physical, fromNanos, toNanos)
            ?: return smoothSpaceCompassSunOrientation(previous, current, elapsed)
        // No motion threshold: handheld micro-movements must not re-enable noisy magnetic yaw.
        return smoothSpaceCompassSunOrientation(spaceCompassSceneRemapOrientation(predicted, 0, displayRotation),
            current, elapsed, ABSOLUTE_CORRECTION_NS)
    }

    private fun predict(previous: SpaceCompassSunOrientation, fromNanos: Long,
        toNanos: Long): SpaceCompassSunOrientation? {
        var index = rates.indexOfLast { it.time <= fromNanos }
        if (index < 0) return null
        var rate = rates[index]
        if (fromNanos - rate.time > GYROSCOPE_MAX_AGE_NS) return null
        var pose = previous
        var time = fromNanos
        while (++index < rates.size && rates[index].time <= toNanos) {
            val next = rates[index]
            if (next.time - rate.time > GYROSCOPE_MAX_AGE_NS) return null
            pose = advanceSpaceCompassSunOrientation(pose, rate.x, rate.y, rate.z, next.time - time)
            time = next.time
            rate = next
        }
        if (toNanos - rate.time > GYROSCOPE_MAX_AGE_NS) return null
        return advanceSpaceCompassSunOrientation(pose, rate.x, rate.y, rate.z, toNanos - time)
    }
}

/** Android gyro rates use physical device axes, including when the display is rotated. */
internal fun advanceSpaceCompassSunOrientation(pose: SpaceCompassSunOrientation,
    x: Double, y: Double, z: Double, elapsedNanos: Long): SpaceCompassSunOrientation {
    val speed = sqrt(x * x + y * y + z * z)
    if (elapsedNanos <= 0 || speed < 1e-12 || !speed.isFinite()) return pose
    val axis = SpaceCompassSunVector(
        (pose.right.east * x + pose.screenUp.east * y - pose.forward.east * z) / speed,
        (pose.right.north * x + pose.screenUp.north * y - pose.forward.north * z) / speed,
        (pose.right.up * x + pose.screenUp.up * y - pose.forward.up * z) / speed)
    val angle = speed * (elapsedNanos / 1_000_000_000.0)
    val c = cos(angle); val s = sin(angle)
    fun rotate(v: SpaceCompassSunVector): SpaceCompassSunVector {
        val along = axis.dot(v) * (1 - c)
        return SpaceCompassSunVector(
            v.east * c + (axis.north * v.up - axis.up * v.north) * s + axis.east * along,
            v.north * c + (axis.up * v.east - axis.east * v.up) * s + axis.north * along,
            v.up * c + (axis.east * v.north - axis.north * v.east) * s + axis.up * along)
    }
    return SpaceCompassSunOrientation(rotate(pose.right), rotate(pose.screenUp), rotate(pose.forward))
}

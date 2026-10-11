package me.mondiversi.spacecompass

import kotlin.math.abs

internal const val SPACE_COMPASS_SUN_SENSOR_POLL_MS = 250L
private const val restartGraceNs = 2_000_000_000L
private const val initialRetryNs = 5_000_000_000L
private const val maximumRetryNs = 30_000_000_000L

internal fun spaceCompassSunSensorSampleFresh(now: Long, timestamp: Long): Boolean =
    timestamp > 0 && now >= timestamp && now - timestamp <= SPACE_COMPASS_SUN_SENSOR_MAX_AGE_NS

internal fun spaceCompassSunSensorSamplesFresh(now: Long, gravity: Long, magnetic: Long): Boolean =
    spaceCompassSunSensorSampleFresh(now, gravity) && spaceCompassSunSensorSampleFresh(now, magnetic) &&
        abs(gravity - magnetic) <= 200_000_000L

internal data class SpaceCompassSunSensorHealth(val stale: Boolean = false, val restart: Boolean = false)

/** Timed checks remain effective when no callback arrives. Never infer a stall from unchanged values. */
internal class SpaceCompassSunSensorWatchdog {
    private var lastPoll = 0L
    private var missingSince: Long? = null
    private var healthySince: Long? = null
    private var nextAttempt: Long? = null
    private var retryDelay = initialRetryNs

    fun reset() {
        lastPoll = 0L
        missingSince = null
        healthySince = null
        nextAttempt = null
        retryDelay = initialRetryNs
    }

    fun check(now: Long, gravity: Long, magnetic: Long, monitoring: Boolean = true): SpaceCompassSunSensorHealth {
        if (!monitoring || now <= 0) { reset(); return SpaceCompassSunSensorHealth() }
        if (now < lastPoll) reset()
        lastPoll = now
        if (spaceCompassSunSensorSamplesFresh(now, gravity, magnetic)) {
            missingSince = null
            val since = healthySince ?: now.also { healthySince = it }
            // A brief burst must not defeat the retry backoff of an intermittently stalled sensor.
            if (now - since >= restartGraceNs) { nextAttempt = null; retryDelay = initialRetryNs }
            return SpaceCompassSunSensorHealth()
        }
        healthySince = null
        if (spaceCompassSunSensorSampleFresh(now, gravity) && spaceCompassSunSensorSampleFresh(now, magnetic)) {
            // Unsynchronised samples cannot establish an attitude, but both streams are still alive.
            missingSince = null
            return SpaceCompassSunSensorHealth(stale = true)
        }
        val since = missingSince ?: now.also { missingSince = it }
        if (now - since < restartGraceNs || now < (nextAttempt ?: 0L))
            return SpaceCompassSunSensorHealth(stale = true)
        nextAttempt = now + retryDelay
        retryDelay = (retryDelay * 2).coerceAtMost(maximumRetryNs)
        return SpaceCompassSunSensorHealth(stale = true, restart = true)
    }
}

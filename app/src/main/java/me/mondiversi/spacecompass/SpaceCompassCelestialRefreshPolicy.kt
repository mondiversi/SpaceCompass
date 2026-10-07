package me.mondiversi.spacecompass

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.ConnectException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val SPACE_COMPASS_CELESTIAL_REFRESH_MS = 2 * 3_600_000L
internal const val SPACE_COMPASS_CELESTIAL_RETRY_MS = 5 * 60_000L
internal data class SpaceCompassCelestialRequest(val body: SpaceCompassCelestialBody, val motion: Boolean = false)
internal class SpaceCompassCelestialHttpException(val status: Int) : IllegalStateException("Ephemeris HTTP $status")

/** Independent position/velocity backoff. A cancelled request is NOT a failed request. */
internal class SpaceCompassCelestialRefreshPolicy {
    private val retryAfter = mutableMapOf<SpaceCompassCelestialRequest, Long>()
    private val requestMutex = Mutex()
    private val requestedReloads = mutableSetOf<SpaceCompassCelestialRequest>()
    private val transportFailures = mutableSetOf<SpaceCompassCelestialRequest>()
    private var horizonsDay: Long? = null

    /** A different UTC observation day is a different JPL query; satellite HTTP stops remain intact. */
    fun horizonsDateChanged(timeMs: Long): Boolean {
        val day = Math.floorDiv(timeMs, 86_400_000L)
        if (horizonsDay == day) return false
        val changed = horizonsDay != null
        horizonsDay = day
        if (changed) {
            retryAfter.keys.removeAll { it.body.usesHorizons }
            transportFailures.removeAll { it.body.usesHorizons }
        }
        return changed
    }

    /** A genuinely new route may recover transport failures; HTTP rejections stay stopped. */
    fun networkChanged() {
        transportFailures.forEach { retryAfter.remove(it) }
        transportFailures.clear()
    }

    /** A deliberate retry keeps valid caches and provider HTTP stops intact. */
    fun retryRequested(bodies: Set<SpaceCompassCelestialBody>) {
        retryAfter.entries.removeAll { it.key.body in bodies && it.value != Long.MAX_VALUE }
        transportFailures.removeAll { it.body in bodies }
    }

    /** Explicit catalog refresh reloads JPL models once; satellite providers retain their two-hour minimum. */
    fun catalogRefreshRequested(bodies: Set<SpaceCompassCelestialBody>) {
        retryRequested(bodies)
        for (body in bodies.filter { it.usesHorizons }) {
            requestedReloads += SpaceCompassCelestialRequest(body)
            requestedReloads += SpaceCompassCelestialRequest(body, motion = true)
        }
    }

    fun nextAttempt(request: SpaceCompassCelestialRequest, usable: Boolean, cacheTime: Long): Long =
        maxOf(if (usable && cacheTime > 0 && request !in requestedReloads)
            cacheTime + SPACE_COMPASS_CELESTIAL_REFRESH_MS else 0L, retryAfter[request] ?: 0L)

    suspend fun attempt(request: SpaceCompassCelestialRequest, clock: () -> Long,
        fetchAndStore: suspend () -> Unit): Result<Unit> {
        return try {
            // A cancelled blocking HTTP read may take a moment to finish. Do not start another JPL query meanwhile.
            requestMutex.withLock { fetchAndStore() }
            requestedReloads.remove(request)
            retryAfter.remove(request)
            transportFailures.remove(request)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            requestedReloads.remove(request)
            // Stop after any rejected HTTP response, as required by CelesTrak's usage policy.
            // A transport timeout did not obtain an HTTP response or a downloaded data set.
            val transportFailure = error is SocketTimeoutException || error is UnknownHostException || error is ConnectException
            if (transportFailure) transportFailures.add(request) else transportFailures.remove(request)
            retryAfter[request] = if (request.body.isEarthSatellite && error is SpaceCompassCelestialHttpException)
                Long.MAX_VALUE else clock() + if (request.body.isEarthSatellite && !transportFailure)
                    SPACE_COMPASS_CELESTIAL_REFRESH_MS else SPACE_COMPASS_CELESTIAL_RETRY_MS
            Result.failure(error)
        }
    }
}

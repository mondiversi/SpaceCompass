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
    private val transportFailures = mutableSetOf<SpaceCompassCelestialRequest>()

    /** A genuinely new route may recover transport failures; HTTP rejections stay stopped. */
    fun networkChanged() {
        transportFailures.forEach { retryAfter.remove(it) }
        transportFailures.clear()
    }

    fun nextAttempt(request: SpaceCompassCelestialRequest, usable: Boolean, cacheTime: Long): Long =
        maxOf(if (usable && cacheTime > 0) cacheTime + SPACE_COMPASS_CELESTIAL_REFRESH_MS else 0L,
            retryAfter[request] ?: 0L)

    suspend fun attempt(request: SpaceCompassCelestialRequest, clock: () -> Long,
        fetchAndStore: suspend () -> Unit): Result<Unit> {
        return try {
            // A cancelled blocking HTTP read may take a moment to finish. Do not start another JPL query meanwhile.
            requestMutex.withLock { fetchAndStore() }
            retryAfter.remove(request)
            transportFailures.remove(request)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
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

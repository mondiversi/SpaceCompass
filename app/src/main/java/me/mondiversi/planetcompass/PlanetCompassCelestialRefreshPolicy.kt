package me.mondiversi.planetcompass

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal const val PLANET_COMPASS_CELESTIAL_REFRESH_MS = 2 * 3_600_000L
internal const val PLANET_COMPASS_CELESTIAL_RETRY_MS = 5 * 60_000L
internal data class PlanetCompassCelestialRequest(val body: PlanetCompassCelestialBody, val motion: Boolean = false)
internal class PlanetCompassCelestialHttpException(val status: Int) : IllegalStateException("Ephemeris HTTP $status")

/** Independent position/velocity backoff. A cancelled request is NOT a failed request. */
internal class PlanetCompassCelestialRefreshPolicy {
    private val retryAfter = mutableMapOf<PlanetCompassCelestialRequest, Long>()
    private val requestMutex = Mutex()

    fun nextAttempt(request: PlanetCompassCelestialRequest, usable: Boolean, cacheTime: Long): Long =
        maxOf(if (usable && cacheTime > 0) cacheTime + PLANET_COMPASS_CELESTIAL_REFRESH_MS else 0L,
            retryAfter[request] ?: 0L)

    suspend fun attempt(request: PlanetCompassCelestialRequest, clock: () -> Long,
        fetchAndStore: suspend () -> Unit): Result<Unit> {
        return try {
            // A cancelled blocking HTTP read may take a moment to finish. Do not start another JPL query meanwhile.
            requestMutex.withLock { fetchAndStore() }
            retryAfter.remove(request)
            Result.success(Unit)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            // CelesTrak asks clients not to repeat 403/404 requests and to cache for at least two hours.
            retryAfter[request] = if (request.body.isEarthSatellite && error is PlanetCompassCelestialHttpException &&
                error.status in listOf(403, 404)) Long.MAX_VALUE else clock() +
                if (request.body.isEarthSatellite) PLANET_COMPASS_CELESTIAL_REFRESH_MS else PLANET_COMPASS_CELESTIAL_RETRY_MS
            Result.failure(error)
        }
    }
}

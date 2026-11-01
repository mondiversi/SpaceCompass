package me.mondiversi.spacecompass

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException

/** Share transport outages across visible objects, while preserving per-object validation/cache rules. */
internal class SpaceCompassOrbitalSourceHealth {
    private val retryAfter = mutableMapOf<String, Long>()
    private val rejected = mutableMapOf<String, Int>()

    fun networkChanged() { retryAfter.clear() }

    suspend fun fetch(url: String, maximumBytes: Int, clock: () -> Long,
        transport: suspend (String, Int) -> String): String {
        val host = URI(url).host?.lowercase(java.util.Locale.ROOT) ?: error("Missing orbital source host")
        rejected[host]?.let { throw SpaceCompassCelestialHttpException(it) }
        if ((retryAfter[host] ?: 0L) > clock()) throw SocketTimeoutException("Orbital source temporarily unreachable")
        try {
            return transport(url, maximumBytes).also { retryAfter.remove(host) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            when (error) {
                is SocketTimeoutException, is UnknownHostException, is ConnectException ->
                    retryAfter[host] = clock() + SPACE_COMPASS_CELESTIAL_RETRY_MS
                // A host-wide rejection must not be repeated for every satellite in the list.
                is SpaceCompassCelestialHttpException -> if (error.status == 403 || error.status == 429) {
                    if (host == "celestrak.org" || host == "www.celestrak.org" ||
                        host == "www.satcat.com" || host == "db.satnogs.org") rejected[host] = error.status
                    else retryAfter[host] = clock() + SPACE_COMPASS_CELESTIAL_RETRY_MS
                }
            }
            throw error
        }
    }
}

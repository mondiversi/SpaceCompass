package me.mondiversi.spacecompass

import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassOrbitalSourceHealthTest {
    private val a = "https://celestrak.org/NORAD/elements/gp.php?CATNR=25994"
    private val b = "https://celestrak.org/NORAD/elements/gp.php?CATNR=27424"
    private val alternative = "https://db.satnogs.org/api/tle/?norad_cat_id=27424"

    @Test fun oneHostTimeoutDoesNotRepeatForEverySatelliteOrBlockTheAlternative() = runBlocking {
        val health = SpaceCompassOrbitalSourceHealth()
        var now = 1_800_000_000_000L
        var calls = 0
        val fetch: suspend (String, Int) -> String = { url, _ ->
            calls++; if (url.contains("celestrak")) throw SocketTimeoutException() else "alternative"
        }
        assertTrue(runCatching { health.fetch(a, 65536, { now }, fetch) }.isFailure)
        assertTrue(runCatching { health.fetch(b, 65536, { now }, fetch) }.isFailure)
        assertEquals(1, calls)
        assertEquals("alternative", health.fetch(alternative, 65536, { now }, fetch))
        assertEquals(2, calls)
        now += SPACE_COMPASS_CELESTIAL_RETRY_MS
        health.fetch(b, 65536, { now }) { _, _ -> calls++; "recovered" }
        assertEquals(3, calls)
    }

    @Test fun networkChangeClearsTransportBackoffButDoesNotBypassProviderRejections() = runBlocking {
        val health = SpaceCompassOrbitalSourceHealth()
        health.fetch(a, 65536, { 1000L }) { _, _ -> "ok" }
        assertTrue(runCatching { health.fetch(a, 65536, { 1000L }) { _, _ -> throw SocketTimeoutException() } }.isFailure)
        health.networkChanged()
        assertEquals("ok", health.fetch(b, 65536, { 1001L }) { _, _ -> "ok" })
        assertTrue(runCatching { health.fetch(a, 65536, { 1001L }) { _, _ -> throw SpaceCompassCelestialHttpException(429) } }.isFailure)
        health.networkChanged()
        var called = false
        val error = runCatching { health.fetch(b, 65536, { 1002L }) { _, _ -> called = true; "wrong" } }.exceptionOrNull()
        assertTrue(error is SpaceCompassCelestialHttpException)
        assertFalse(called)
    }

    @Test fun missingOneSatelliteDoesNotRejectTheWholeProviderAndCancellationDoesNotBackOff() = runBlocking {
        val health = SpaceCompassOrbitalSourceHealth()
        assertTrue(runCatching { health.fetch(a, 65536, { 1000L }) { _, _ -> throw SpaceCompassCelestialHttpException(404) } }.isFailure)
        assertEquals("other", health.fetch(b, 65536, { 1000L }) { _, _ -> "other" })
        assertTrue(runCatching { health.fetch(a, 65536, { 1000L }) { _, _ -> throw CancellationException() } }.isFailure)
        assertEquals("retry", health.fetch(a, 65536, { 1000L }) { _, _ -> "retry" })
    }
    @Test fun aTemporaryJplRateLimitRecoversInsteadOfStoppingTheWholeSession() = runBlocking {
        val health = SpaceCompassOrbitalSourceHealth()
        val url = "https://ssd.jpl.nasa.gov/api/horizons.api?COMMAND=90377"
        var now = 1000L
        assertTrue(runCatching { health.fetch(url, 65536, { now }) { _, _ -> throw SpaceCompassCelestialHttpException(429) } }.isFailure)
        var called = false
        assertTrue(runCatching { health.fetch(url, 65536, { now }) { _, _ -> called = true; "too soon" } }.isFailure)
        assertFalse(called)
        now += SPACE_COMPASS_CELESTIAL_RETRY_MS
        assertEquals("recovered", health.fetch(url, 65536, { now }) { _, _ -> "recovered" })
    }
}

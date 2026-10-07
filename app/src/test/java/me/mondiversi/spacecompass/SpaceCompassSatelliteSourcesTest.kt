package me.mondiversi.spacecompass

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.time.Instant

class SpaceCompassSatelliteSourcesTest {
    private val now = Instant.parse("2026-10-06T08:00:00Z").toEpochMilli()
    private fun fixture(name: String) = javaClass.classLoader!!
        .getResource("catalog-refresh/$name-2026-10-06.tle")!!.readText().trim()
    private fun page(name: String) = "<html><pre><code>${fixture(name)}</code></pre></html>"

    @Test fun publicIssRecordRestoresCurrentPointingAndSpeedAfterPrimaryTransportFailure() = runBlocking {
        val sources = SpaceCompassIssSources()
        val requests = mutableListOf<String>()
        val fetch: suspend (String, Int) -> String = { url, limit ->
            requests += url
            if (url == SPACE_COMPASS_ISS_TLE_URL) throw SocketTimeoutException()
            assertEquals(SPACE_COMPASS_ISS_SATCAT_URL, url)
            assertEquals(SPACE_COMPASS_SATCAT_MAX_BYTES, limit)
            page("iss")
        }
        val orbit = SpaceCompassIssOrbit.parse(sources.load(now, fetch))
        assertTrue(orbit.usable(now))
        assertEquals(3, orbit.teme(now).size)
        assertTrue(orbit.speedKmPerSecond(now) in 7.0..8.5)
        assertEquals(listOf(SPACE_COMPASS_ISS_TLE_URL, SPACE_COMPASS_ISS_SATCAT_URL), requests)
        requests.clear()
        sources.load(now, fetch)
        assertEquals(listOf(SPACE_COMPASS_ISS_SATCAT_URL), requests)
    }

    @Test fun aPrimaryHttpStopDoesNotStopTheAlternativeOrRepeatTheRejectedRequest() = runBlocking {
        val sources = SpaceCompassIssSources()
        var primary = 0
        repeat(2) {
            val text = sources.load(now) { url, _ ->
                if (url == SPACE_COMPASS_ISS_TLE_URL) { primary++; throw SpaceCompassCelestialHttpException(403) }
                page("iss")
            }
            assertTrue(SpaceCompassIssOrbit.parse(text).usable(now))
        }
        assertEquals(1, primary)
    }

    @Test fun cancellationNeverInitiatesAnotherProviderRequest() = runBlocking {
        var requests = 0
        try {
            SpaceCompassIssSources().load(now) { _, _ -> requests++; throw CancellationException() }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertEquals(1, requests)
    }

    @Test fun extractionRejectsAmbiguityIdentityChangesAndCorruptChecksums() {
        val valid = page("iss")
        for (invalid in listOf("<html>Sign in</html>", valid + valid,
            valid.replace("25544", "25545"), valid.replace("ISS (ZARYA)", "OTHER"),
            valid.replace("98067A", "98067B"), valid.replace("111.6262", "111.6263"))) {
            assertTrue(runCatching { spaceCompassSatcatIssTle(invalid) }.isFailure)
        }
    }

    @Test fun noProviderReturnsAnExpiredOrbitAsIfItWereLive() = runBlocking {
        assertTrue(runCatching { SpaceCompassIssSources().load(now + 5 * 86_400_000L) { url, _ ->
            if (url == SPACE_COMPASS_ISS_TLE_URL) fixture("iss") else page("iss")
        } }.isFailure)
    }

    @Test fun currentStarlinkPublicRecordStillUsesTheCompleteAlphaFiveIdentity() {
        val orbit = SpaceCompassStarlinkOrbit.parse(spaceCompassSatcatStarlinkCsv(page("starlink")))
        assertTrue(orbit.usable(now))
        assertEquals(3, orbit.teme(now).size)
        assertTrue(orbit.speedKmPerSecond(now) in 7.0..8.5)
        assertTrue(kotlin.math.abs(Instant.parse("2026-10-05T22:00:02Z").toEpochMilli() - orbit.epochMs) < 10L)
    }
}

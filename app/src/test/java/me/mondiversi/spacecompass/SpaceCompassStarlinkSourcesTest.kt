package me.mondiversi.spacecompass

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.SocketTimeoutException
import java.time.Instant
import sgp4.TLE

class SpaceCompassStarlinkSourcesTest {
    private val tle = javaClass.classLoader!!.getResource("celestial/starlink-40083-satcat.tle")!!.readText().trim()
    private val page = "<html><pre><code>$tle</code></pre></html>"
    private val now = Instant.parse("2026-10-04T16:00:00Z").toEpochMilli()

    @Test fun publicAlphaFiveTlePreservesIdentityEpochAndIndependentSgp4States() {
        val csv = spaceCompassSatcatStarlinkCsv(page)
        val orbit = SpaceCompassStarlinkOrbit.parse(csv)
        val lines = tle.lines()
        val reference = TLE(lines[1], lines[2])
        assertTrue(csv.contains("STARLINK-40083,2026-225A,"))
        assertTrue(csv.contains(",100855,"))
        assertEquals(Instant.parse("2026-10-04T06:00:02.000160Z").toEpochMilli(), orbit.epochMs)
        for (minutes in listOf(0.0, 60.0, 360.0, 720.0)) {
            val expected = reference.getRV(minutes)
            assertEquals(0, reference.sgp4Error)
            assertArrayEquals(expected[0], orbit.teme(orbit.epochMs+(minutes*60_000).toLong()), 1e-5)
            assertEquals(spaceCompassCelestialVelocityMagnitude(expected[1][0],expected[1][1],expected[1][2]),
                orbit.speedKmPerSecond(orbit.epochMs+(minutes*60_000).toLong()), 1e-8)
        }
    }

    @Test fun extractionRejectsChangedMarkupDuplicatesOtherSatellitesAndCorruptChecksums() {
        for (invalid in listOf("<html>Sign in</html>", page+page, page.replace("A0855","A0856"),
            page.replace("STARLINK-40083","STARLINK-40084"), page.replace("297.0408","297.0409"),
            page.replace("26225A","26225B"), page.replace("<code>","<span>"))) {
            assertTrue(runCatching { spaceCompassSatcatStarlinkCsv(invalid) }.isFailure)
        }
    }

    @Test fun transportFailureFallsBackAndTheWorkingProviderIsPreferredNextTime() = runBlocking {
        val sources = SpaceCompassStarlinkSources()
        val requests = mutableListOf<String>()
        val fetch: suspend (String, Int) -> String = { url, limit ->
            requests += url
            if (url == SPACE_COMPASS_STARLINK_OMM_URL) throw SocketTimeoutException()
            assertEquals(SPACE_COMPASS_SATCAT_MAX_BYTES, limit)
            page
        }
        assertTrue(SpaceCompassStarlinkOrbit.parse(sources.load(now, fetch)).usable(now))
        assertEquals(listOf(SPACE_COMPASS_STARLINK_OMM_URL,SPACE_COMPASS_STARLINK_SATCAT_URL), requests)
        requests.clear()
        sources.load(now, fetch)
        assertEquals(listOf(SPACE_COMPASS_STARLINK_SATCAT_URL), requests)
    }

    @Test fun httpRejectionStopsOnlyThatProviderAndCancellationDoesNotStartFallback() = runBlocking {
        val sources = SpaceCompassStarlinkSources()
        var primaryRequests = 0
        val fetch: suspend (String, Int) -> String = { url, _ ->
            if (url == SPACE_COMPASS_STARLINK_OMM_URL) { primaryRequests++; throw SpaceCompassCelestialHttpException(403) }
            page
        }
        sources.load(now,fetch); sources.load(now,fetch)
        assertEquals(1, primaryRequests)
        var cancelledRequests = 0
        try {
            SpaceCompassStarlinkSources().load(now) { _, _ -> cancelledRequests++; throw CancellationException() }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertEquals(1, cancelledRequests)
    }

    @Test fun fallbackRejectsStaleOrbitsInsteadOfReturningOutdatedPositions() = runBlocking {
        val sources = SpaceCompassStarlinkSources()
        val stale = now + 5*86_400_000L
        assertTrue(runCatching { sources.load(stale) { url, _ ->
            if (url == SPACE_COMPASS_STARLINK_OMM_URL) throw SocketTimeoutException()
            page
        } }.isFailure)
    }
}

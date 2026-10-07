package me.mondiversi.spacecompass

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

/** Public provider responses captured October 6; tests perform no network requests. */
class SpaceCompassLiveCatalogDataTest {
    private val now = Instant.parse("2026-10-06T08:00:00Z").toEpochMilli()
    private fun fixture(component: String) = javaClass.classLoader!!
        .getResource("catalog-refresh/sedna-$component-2026-10-06.txt")!!.readText()

    @Test fun realSednaResponsesProvideDistancePositionAndVelocityWithoutOpeningTheObject() {
        val body = SpaceCompassCelestialBody.SEDNA
        val position = parseSpaceCompassHorizonsEphemeris(body, fixture("position"))
        val motion = parseSpaceCompassHorizonsMotion(body, fixture("motion"))
        assertEquals(73, position.samples.size)
        assertEquals(73, motion.samples.size)
        assertNotNull(position.at(now))
        assertTrue(motion.speedAt(now)!! > 0.0)
        val remote = SpaceCompassCelestialRemoteData(ephemerides = mapOf(body to position), motions = mapOf(body to motion))
        assertTrue(spaceCompassCelestialCatalogDistanceAu(body, now, remote)!! in 82.0..84.0)
        assertNotNull(calculateSpaceCompassCelestialObservation(body, now, 45.0, 9.0, 100.0, remote))
    }

    @Test fun missingDataOutsideTheValidatedWindowCannotBePresentedAsCurrent() {
        val body = SpaceCompassCelestialBody.SEDNA
        val position = parseSpaceCompassHorizonsEphemeris(body, fixture("position"))
        val motion = parseSpaceCompassHorizonsMotion(body, fixture("motion"))
        val outside = position.samples.last().timeMs + 1
        assertNull(position.at(outside))
        assertNull(motion.speedAt(outside))
        assertNull(spaceCompassCelestialCatalogDistanceAu(body, outside,
            SpaceCompassCelestialRemoteData(ephemerides = mapOf(body to position), motions = mapOf(body to motion))))
    }

    @Test fun catalogRefreshReloadsUncheckedJplModelsButKeepsSatelliteIntervalsAndHttpStops() = runBlocking {
        val policy = SpaceCompassCelestialRefreshPolicy()
        val online = spaceCompassCelestialRefreshBodies(setOf(SpaceCompassCelestialBody.SUN), emptySet(), true)
        val requests = online.flatMap { body -> if (body.isEarthSatellite)
            listOf(SpaceCompassCelestialRequest(body)) else listOf(SpaceCompassCelestialRequest(body), SpaceCompassCelestialRequest(body, true)) }
        for (request in requests) policy.attempt(request, { now }) { throw java.net.SocketTimeoutException() }
        val stopped = SpaceCompassCelestialRequest(SpaceCompassCelestialBody.ISS)
        policy.attempt(stopped, { now }) { throw SpaceCompassCelestialHttpException(429) }
        policy.catalogRefreshRequested(online.toSet())
        for (request in requests.filter { it != stopped }) {
            assertEquals(0L, policy.nextAttempt(request, false, 0))
            assertEquals(if (request.body.isEarthSatellite) now + SPACE_COMPASS_CELESTIAL_REFRESH_MS else 0L,
                policy.nextAttempt(request, true, now))
            policy.attempt(request, { now }) {}
            assertEquals(now + SPACE_COMPASS_CELESTIAL_REFRESH_MS, policy.nextAttempt(request, true, now))
        }
        assertEquals(Long.MAX_VALUE, policy.nextAttempt(stopped, false, 0))
    }
}

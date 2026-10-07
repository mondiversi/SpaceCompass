package me.mondiversi.spacecompass

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.URLDecoder
import java.time.Instant

class SpaceCompassCelestialScenarioRefreshTest {
    private val now = Instant.parse("2026-10-06T18:00:00Z").toEpochMilli()
    private val scenario = Instant.parse("2027-04-06T22:00:00Z").toEpochMilli()
    private val jpl = listOf(SpaceCompassCelestialBody.SEDNA, SpaceCompassCelestialBody.HALLEY,
        SpaceCompassCelestialBody.COMET_67P, SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2)

    @Test fun everyJplTargetRequestsTheViewedUtcDateInsteadOfTheDownloadDate() {
        for (body in jpl) for (motion in listOf(false, true)) {
            val requested = spaceCompassCelestialRequestTime(body, now, scenario)
            val query = URLDecoder.decode(spaceCompassHorizonsUrl(body, requested, motion), "UTF-8")
            assertTrue(query.contains("START_TIME='2027-04-05'"))
            assertTrue(query.contains("STOP_TIME='2027-04-08'"))
            assertFalse(query.contains("2026-10"))
            assertFalse(query.contains("SITE_COORD"))
            assertEquals(now, spaceCompassCelestialRequestTime(body, now, null))
        }
    }

    @Test fun satelliteDownloadsAndFreshnessAlwaysUseTheRealClock() {
        for (body in listOf(SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3)) {
            assertEquals(now, spaceCompassCelestialRequestTime(body, now, scenario))
            assertEquals(now, spaceCompassCelestialRequestTime(body, now, now - 365L * 86_400_000))
        }
    }

    @Test fun newObservationDayReleasesOnlyJplBackoffWithoutBypassingFreshCachesOrSatelliteStops() = runBlocking {
        val policy = SpaceCompassCelestialRefreshPolicy()
        assertFalse(policy.horizonsDateChanged(now))
        val requests = jpl.flatMap { listOf(SpaceCompassCelestialRequest(it), SpaceCompassCelestialRequest(it, true)) }
        for (request in requests) policy.attempt(request, { now }) { throw java.net.SocketTimeoutException() }
        val satellite = SpaceCompassCelestialRequest(SpaceCompassCelestialBody.ISS)
        policy.attempt(satellite, { now }) { throw SpaceCompassCelestialHttpException(429) }
        assertFalse(policy.horizonsDateChanged(now + 1_000))
        assertEquals(now + SPACE_COMPASS_CELESTIAL_RETRY_MS, policy.nextAttempt(requests.first(), false, 0))
        assertTrue(policy.horizonsDateChanged(scenario))
        requests.forEach {
            assertEquals(0L, policy.nextAttempt(it, false, now))
            assertEquals(now + SPACE_COMPASS_CELESTIAL_REFRESH_MS, policy.nextAttempt(it, true, now))
        }
        assertEquals(Long.MAX_VALUE, policy.nextAttempt(satellite, false, 0))
    }

    @Test fun changingTheHourWithinAnUnchangedDayDoesNotRepeatFailedQueries() = runBlocking {
        val policy = SpaceCompassCelestialRefreshPolicy()
        policy.horizonsDateChanged(scenario)
        val request = SpaceCompassCelestialRequest(SpaceCompassCelestialBody.SEDNA)
        policy.attempt(request, { now }) { error("Date not supported by provider") }
        assertFalse(policy.horizonsDateChanged(scenario + 60_000))
        assertEquals(now + SPACE_COMPASS_CELESTIAL_RETRY_MS, policy.nextAttempt(request, false, 0))
        assertTrue(policy.horizonsDateChanged(now))
        assertEquals(0L, policy.nextAttempt(request, false, 0))
    }

    @Test fun validFutureJplResponsesFillDistancePositionAndMotionForAllFiveTargets() {
        for (body in jpl) {
            val name = when (body) {
                SpaceCompassCelestialBody.SEDNA -> "sedna"
                SpaceCompassCelestialBody.HALLEY -> "halley"
                SpaceCompassCelestialBody.COMET_67P -> "67p"
                SpaceCompassCelestialBody.VOYAGER_1 -> "voyager-1"
                else -> "voyager-2"
            }
            fun fixture(component: String) = javaClass.classLoader!!.getResource(
                "scenario-refresh/$name-$component.txt")!!.readText()
            val position = parseSpaceCompassHorizonsEphemeris(body, fixture("position"))
            val motion = parseSpaceCompassHorizonsMotion(body, fixture("motion"))
            val remote = SpaceCompassCelestialRemoteData(ephemerides = mapOf(body to position), motions = mapOf(body to motion))
            assertNotNull(spaceCompassCelestialCatalogDistanceAu(body, scenario, remote))
            assertNotNull(calculateSpaceCompassCelestialObservation(body, scenario, 45.0, 9.0, 100.0, remote))
            assertNotNull(calculateSpaceCompassCelestialSpeed(body, scenario, remote))
            assertFalse(spaceCompassCelestialDataOutsideDate(body, scenario, remote))
            assertTrue(spaceCompassCelestialDataOutsideDate(body, now, remote))
        }
    }

    @Test fun aRemoteObjectWithNoModelsIsNotMislabeledAsAConfirmedDateRangeProblem() {
        for (body in jpl + listOf(SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3))
            assertFalse(spaceCompassCelestialDataOutsideDate(body, scenario, SpaceCompassCelestialRemoteData()))
        assertFalse(spaceCompassCelestialDataOutsideDate(SpaceCompassCelestialBody.MOON, scenario, SpaceCompassCelestialRemoteData()))
    }

    @Test fun currentSatelliteElementsRemainUsableNowButDoNotInventPositionsMonthsAhead() {
        fun fixture(name: String) = javaClass.classLoader!!.getResource(
            "catalog-refresh/$name-2026-10-06.tle")!!.readText().trim()
        val remote = SpaceCompassCelestialRemoteData(
            iss = SpaceCompassIssOrbit.parse(fixture("iss")),
            starlink = SpaceCompassStarlinkOrbit.parse(spaceCompassStarlinkTleCsv(fixture("starlink"))))
        for (body in listOf(SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3)) {
            assertFalse(spaceCompassCelestialDataOutsideDate(body, now, remote))
            assertNotNull(spaceCompassNearbyCatalogDistanceKm(body, now, remote))
            assertTrue(spaceCompassCelestialDataOutsideDate(body, scenario, remote))
            assertNull(spaceCompassNearbyCatalogDistanceKm(body, scenario, remote))
        }
    }
}

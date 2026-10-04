package me.mondiversi.planetcompass

import java.time.Instant
import java.time.ZoneId
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sgp4.TLE

class PlanetCompassStarlinkOrbitTest {
    private val fixture = javaClass.classLoader!!.getResource("celestial/starlink-40083.csv")!!.readText()
    private val orbit = PlanetCompassStarlinkOrbit.parse(fixture)
    private val now = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    private val body = PlanetCompassCelestialBody.STARLINK_V3
    private val remote = PlanetCompassCelestialRemoteData(starlink = orbit)

    @Test fun sixDigitIdUsesDirectOmmWithoutTruncatingOrBorrowingIssElements() {
        assertEquals(100855, PLANET_COMPASS_STARLINK_NORAD_ID)
        assertTrue(PLANET_COMPASS_STARLINK_OMM_URL.contains("CATNR=100855&FORMAT=CSV"))
        assertEquals(Instant.parse("2026-10-03T22:00:01.999584Z").toEpochMilli(), orbit.epochMs)
        assertTrue(abs((86_400_000 / 15.94153117).toLong() - orbit.periodMs) <= 1L)
        assertTrue(orbit.usable(now))
        assertNull(calculatePlanetCompassCelestialObservation(body, now, 45.0, 9.0))
        assertNull(calculatePlanetCompassCelestialSpeed(body, now))
        assertNull(remote.satelliteOrbit(PlanetCompassCelestialBody.ISS))
        assertEquals(orbit, remote.satelliteOrbit(body))
    }

    @Test fun ommInitializationMatchesIndependentlyParsedAlphaFiveTle() {
        // Reference TLE rounded to its supported field precision, NOT a runtime conversion/fallback.
        fun checksum(value: String): String {
            assertEquals(68, value.length)
            return value + (value.sumOf { if (it.isDigit()) it.digitToInt() else if (it == '-') 1 else 0 } % 10)
        }
        val tle = TLE(checksum("1 A0855U 26225A   26276.91668981 -.01528498  65710-3 -39405-2 0  999"),
            checksum("2 A0855  30.4879 299.4933 0000307 150.6426  52.3594 15.94153117  203"))
        assertTrue(tle.parseErrors.isNullOrEmpty())
        for (minutes in listOf(-60.0, 0.0, 60.0, 360.0)) {
            val expected = tle.getRV(minutes)
            assertEquals(0, tle.sgp4Error)
            assertArrayEquals(expected[0], orbit.teme(orbit.epochMs + (minutes * 60_000).toLong()), 0.02)
            assertEquals(planetCompassCelestialVelocityMagnitude(expected[1][0],expected[1][1],expected[1][2]),
                orbit.speedKmPerSecond(orbit.epochMs + (minutes * 60_000).toLong()), 0.0001)
        }
    }

    @Test fun parserRejectsWrongIdentityAmbiguousRowsFramesAndNonFiniteElements() {
        for (bad in listOf(fixture.replace("100855", "855"), fixture.replace("STARLINK-40083", "STARLINK-40084"),
            fixture.replace("2026-225A", "2026-225B"), fixture + fixture.lineSequence().last { it.isNotBlank() },
            fixture.replace("15.94153117", "NaN"), fixture.replace("15.94153117", "Infinity"),
            fixture.replace(".0000307", "0.9"), fixture.replace("30.4879", "-30.0"),
            fixture.replace("299.4933", "360.0"), fixture.replace("2026-10-03T22:00:01.999584", "not-an-epoch"),
            fixture.replace("NORAD_CAT_ID", "OTHER_ID"), fixture.replace("MEAN_ANOMALY", "INCLINATION"),
            fixture.replace("STARLINK-40083", "\"STARLINK-40083"))) {
            assertTrue("Malformed OMM accepted", runCatching { PlanetCompassStarlinkOrbit.parse(bad) }.isFailure)
        }
        fun appended(key: String, value: String): String = fixture.trim().lines().let {
            "${it[0]},$key\n${it[1]},$value"
        }
        for ((key,value) in listOf("CENTER_NAME" to "SUN", "REF_FRAME" to "ITRF", "TIME_SYSTEM" to "TAI",
            "MEAN_ELEMENT_THEORY" to "KEPLER"))
            assertTrue(runCatching { PlanetCompassStarlinkOrbit.parse(appended(key,value)) }.isFailure)
        for ((key,value) in listOf("CENTER_NAME" to "EARTH", "REF_FRAME" to "TEME", "TIME_SYSTEM" to "UTC",
            "MEAN_ELEMENT_THEORY" to "SGP4"))
            assertEquals(orbit.epochMs, PlanetCompassStarlinkOrbit.parse(appended(key,value)).epochMs)
        assertEquals(orbit.epochMs, PlanetCompassStarlinkOrbit.parse("\uFEFF" + fixture.replace("STARLINK-40083", "\"STARLINK-40083\"")).epochMs)
    }

    @Test fun utcEpochAndPropagationIgnoreLocalTimezoneAndDaylightSavingTime() {
        val previous = TimeZone.getDefault()
        try {
            for (zone in listOf("Europe/Rome", "America/New_York", "Pacific/Auckland")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                val parsed = PlanetCompassStarlinkOrbit.parse(fixture)
                assertEquals(zone, TimeZone.getDefault().id)
                assertEquals(orbit.epochMs, parsed.epochMs)
                assertArrayEquals(orbit.teme(now), parsed.teme(now), 0.0)
            }
        } finally { TimeZone.setDefault(previous) }
    }

    @Test fun actualRangeAzimuthElevationAndOrbitalSpeedAreLiveAndObserverSpecific() {
        val a = calculatePlanetCompassCelestialObservation(body, now, 45.0, 9.0, 100.0, remote)!!
        val b = calculatePlanetCompassCelestialObservation(body, now + 500, 45.0, 9.0, 100.0, remote)!!
        assertTrue(a.distanceKm in 200.0..14_000.0)
        assertTrue(abs(a.distanceKm - b.distanceKm) > 0.01)
        assertNotEquals(a.position, b.position)
        assertTrue(a.position.azimuthDegrees in 0.0..360.0)
        assertTrue(a.position.elevationDegrees in -90.0..90.0)
        val site = calculatePlanetCompassCelestialObservation(body, now, -45.0, -171.0, 100.0, remote)!!
        assertTrue(abs(a.distanceKm - site.distanceKm) > 100)
        val speed = calculatePlanetCompassCelestialSpeed(body, now, remote)!!
        assertTrue(speed in 7.5..7.9)
        val delta = orbit.teme(now+1000).zip(orbit.teme(now-1000)).map { (a,b) -> (a-b)/2 }
        assertEquals(sqrt(delta.sumOf { it*it }), speed, 0.001)
        assertEquals(R.string.celestial_speed_orbit_earth, planetCompassCelestialSpeedLabel(body))
    }

    @Test fun orbitHas24RegularPointsDenseSmoothSamplesAndBelowHorizonPortion() {
        val zone = ZoneId.of("Europe/Rome")
        val date = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        for (latitude in listOf(0.0,45.0,89.0)) {
            val path = calculatePlanetCompassCelestialPath(body,date,zone,now,latitude,9.0,0.0,remote)!!
            assertEquals(body,path.body)
            assertEquals(orbit.periodMs,path.samples.last().timeMs-path.samples.first().timeMs)
            assertEquals(24,path.markers.count { it.event == PlanetCompassSunPathEvent.HOUR })
            assertTrue(path.samples.size in 500..650)
            assertTrue(path.samples.any { it.position.elevationDegrees < 0 })
            assertTrue(path.markers.any { PlanetCompassSunPathEvent.MINIMUM in it.events })
            assertTrue(now in path.samples.first().timeMs..path.samples.last().timeMs)
            if (latitude == 89.0) assertTrue(path.issPass.isEmpty())
        }
    }

    @Test fun staleOrImplausiblyFutureDataDoesNotProduceFabricatedPositionSpeedOrPath() {
        for (time in listOf(orbit.epochMs + PLANET_COMPASS_ISS_MAX_AGE_MS + 1, orbit.epochMs - PLANET_COMPASS_ISS_FUTURE_EPOCH_ALLOWANCE_MS - 1)) {
            assertFalse(orbit.usable(time))
            assertNull(calculatePlanetCompassCelestialObservation(body,time,45.0,9.0,remote=remote))
            assertNull(calculatePlanetCompassCelestialSpeed(body,time,remote))
            assertNull(calculatePlanetCompassCelestialPath(body,java.time.LocalDate.of(2026,10,4),ZoneId.of("UTC"),time,45.0,9.0,0.0,remote))
        }
    }

    @Test fun catalogAndViewerIdentifyOneSatelliteWithoutInventingDimensionsOrAttitude() {
        assertEquals(body, planetCompassCelestialCatalogOrder[planetCompassCelestialCatalogOrder.indexOf(PlanetCompassCelestialBody.ISS)+1])
        assertTrue(body.isSpacecraft && body.usesLiveDistance && body.supportsDailyPath)
        assertFalse(body.usesHorizons || body.hasPhysicalFace)
        assertNull(body.viewerTexture)
        assertNull(planetCompassCelestialFacts(body).massKg)
        assertNull(planetCompassCelestialFacts(body).dimensionMeters)
        assertTrue(planetCompassCelestialFacts(body).parentIsEarth)
        assertTrue(planetCompassCelestialCraftMesh(body).size > 100)
        assertNotEquals(planetCompassCelestialCraftMesh(PlanetCompassCelestialBody.ISS), planetCompassCelestialCraftMesh(body))
        assertEquals(R.string.celestial_orbit, planetCompassCelestialPathTitle(body))
        assertTrue(planetCompassCelestialCatalogDistanceAu(body,now)!! in 0.98..1.02)
    }

    @Test fun celestrakFailuresRespectTwoHourBackoffAndForbiddenRequestsAreNotRepeated() = runBlocking {
        val request = PlanetCompassCelestialRequest(body)
        val policy = PlanetCompassCelestialRefreshPolicy()
        policy.attempt(request,{now}) { throw IllegalStateException("Network unavailable") }
        assertEquals(now+PLANET_COMPASS_CELESTIAL_REFRESH_MS,policy.nextAttempt(request,false,0))
        policy.attempt(request,{now}) { }
        assertEquals(0L,policy.nextAttempt(request,false,0))
        for (status in listOf(403,404)) {
            policy.attempt(request,{now}) { throw PlanetCompassCelestialHttpException(status) }
            assertEquals(Long.MAX_VALUE,policy.nextAttempt(request,false,0))
        }
    }
}

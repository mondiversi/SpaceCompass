package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test
import sgp4.TLE
import java.time.*
import java.util.TimeZone

class PlanetCompassCelestialModelTest {
    private val now = Instant.parse("2026-10-03T12:00:00Z").toEpochMilli()
    private val tleText = """
        1 25544U 98067A   26276.04623379  .00005750  00000+0  11349-3 0  9994
        2 25544  51.6314 126.3061 0006899 216.4733 143.5786 15.48722558588472
    """.trimIndent()

    @Test fun moonMatchesIndependentJplAirlessTopocentricReference() {
        // NASA/JPL Horizons 301, coord@399, 12.5E 41.9N 0.05km; quantities 4,20.
        val moon = calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.MOON, now, 41.9, 12.5, 50.0)!!
        assertEquals(299.176262, moon.position.azimuthDegrees, 0.04)
        assertEquals(7.222103, moon.position.elevationDegrees, 0.04)
        assertEquals(0.00246801131039 * PLANET_COMPASS_AU_KM, moon.distanceKm, 30.0)
        val later = calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.MOON, now + 60_000, 41.9, 12.5, 50.0)!!
        assertTrue(kotlin.math.abs(later.distanceKm - moon.distanceKm) > 1)
    }
    @Test fun everyLocalObjectHasFiniteTopocentricAnglesAndDistance() {
        PlanetCompassCelestialBody.entries.filter { it.engine != null }.forEach {
            val result = calculatePlanetCompassCelestialObservation(it, now, -33.0, 151.0, 500.0)!!
            assertTrue(result.distanceKm.isFinite() && result.distanceKm > 100_000)
            assertTrue(result.position.azimuthDegrees in 0.0..360.0)
            assertTrue(result.position.elevationDegrees in -90.0..90.0)
        }
    }
    @Test fun representativeDistancesStayDailyExceptLiveMoonIssAndVoyagerIncludingDst() {
        val zone = ZoneId.of("Europe/Rome")
        val morning = Instant.parse("2026-10-25T07:00:00Z").toEpochMilli()
        for (body in PlanetCompassCelestialBody.entries) {
            val a = planetCompassCelestialDistanceTime(body, morning, zone)
            val b = planetCompassCelestialDistanceTime(body, morning + 3_600_000, zone)
            if (body.usesLiveDistance) assertEquals(3_600_000, b - a)
            else assertEquals(a, b)
        }
    }
    @Test fun utcCalculationDoesNotDependOnPhoneTimezoneOrDst() {
        val original = TimeZone.getDefault()
        try {
            val a = calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.MOON, now, 45.0, 9.0)!!
            for (zone in listOf("Europe/Rome", "Pacific/Auckland", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertEquals(a, calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.MOON, now, 45.0, 9.0))
                PlanetCompassIssOrbit.parse(tleText)
                assertEquals(zone, TimeZone.getDefault().id)
            }
        } finally { TimeZone.setDefault(original) }
    }
    @Test fun sgp4MatchesValladoPublishedVerificationVector() {
        val tle = TLE("1 00005U 58002B   00179.78495062  .00000023  00000-0  28098-4 0  4753",
            "2 00005  34.2682 348.7242 1859667 331.7664  19.3264 10.82419157413667")
        val r = tle.getRV(0.0)[0]
        assertArrayEquals(doubleArrayOf(7022.46529266, -1400.08296755, 0.03995155), r, 1e-6)
        assertEquals(0, tle.sgp4Error)
    }
    @Test fun issRejectsChecksumWrongTargetAndStaleElements() {
        val orbit = PlanetCompassIssOrbit.parse(tleText)
        assertTrue(orbit.usable(now))
        assertFalse(orbit.usable(orbit.epochMs + PLANET_COMPASS_ISS_MAX_AGE_MS + 1))
        assertFalse(orbit.usable(orbit.epochMs - 7 * 3_600_000))
        assertThrows(IllegalArgumentException::class.java) { PlanetCompassIssOrbit.parse(tleText.replace("9994", "9995")) }
        assertThrows(IllegalArgumentException::class.java) { PlanetCompassIssOrbit.parse(tleText.replace("25544", "12345")) }
        assertNull(calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.ISS, now, 45.0, 9.0))
        assertNull(calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.SEDNA, now, 45.0, 9.0))
        assertNull(calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.ISS, orbit.epochMs + PLANET_COMPASS_ISS_MAX_AGE_MS + 1,
            45.0, 9.0, remote = PlanetCompassCelestialRemoteData(iss = orbit)))
    }
    @Test fun issDistanceAndPositionUpdateEverySecondAndRangeIsNotGeocentricRadius() {
        val remote = PlanetCompassCelestialRemoteData(iss = PlanetCompassIssOrbit.parse(tleText))
        val a = calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.ISS, now, 45.0, 9.0, remote = remote)!!
        val b = calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.ISS, now + 1000, 45.0, 9.0, remote = remote)!!
        assertTrue(kotlin.math.abs(a.distanceKm - b.distanceKm) > 0.01)
        assertNotEquals(a.position, b.position)
        assertTrue(a.distanceKm in 300.0..14_000.0)
        val differentSite = calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.ISS, now, -45.0, -171.0, remote = remote)!!
        assertTrue(kotlin.math.abs(a.distanceKm - differentSite.distanceKm) > 100)
    }
    @Test fun moonDailyPathUsesLocalDstDayAndIssHas24OrbitMarkers() {
        val date = LocalDate.parse("2026-10-25"); val zone = ZoneId.of("Europe/Rome")
        val autumn = calculatePlanetCompassCelestialPath(PlanetCompassCelestialBody.MOON, date, zone,
            date.atStartOfDay(zone).toInstant().toEpochMilli(), 45.0, 9.0, 0.0, PlanetCompassCelestialRemoteData())!!
        assertEquals(25, autumn.markers.count { it.event == PlanetCompassSunPathEvent.HOUR })
        assertEquals(PlanetCompassCelestialBody.MOON, autumn.body)
        val path = calculatePlanetCompassCelestialPath(PlanetCompassCelestialBody.ISS, LocalDate.parse("2026-10-03"), zone,
            now, 45.0, 9.0, 0.0, PlanetCompassCelestialRemoteData(iss = PlanetCompassIssOrbit.parse(tleText)))!!
        assertEquals(PlanetCompassIssOrbit.parse(tleText).periodMs, path.samples.last().timeMs - path.samples.first().timeMs)
        val hourly = path.markers.filter { it.event == PlanetCompassSunPathEvent.HOUR }
        assertEquals(24, hourly.size)
        assertTrue(hourly.zipWithNext().all { (a,b) -> kotlin.math.abs(
            (b.timeMs - a.timeMs) - PlanetCompassIssOrbit.parse(tleText).periodMs / 24) <= 1 })
        assertTrue(path.issPass.any { it.event == PlanetCompassSunPathEvent.SUNRISE })
        assertTrue(path.issPass.any { it.event == PlanetCompassSunPathEvent.SUNSET })
    }
    @Test fun sednaParserInterpolatesOnlyValidatedJplUtcVectors() {
        val header = "Target body name: 90377 Sedna\nCenter body name: Earth (399)\nOutput units    : AU-D\nReference frame : ICRF\nLT+S CORRECTED\n2026-Oct-03 00:00 UT "
        val start = 2461316.5
        val lines = (0..24).joinToString("\n") { "${start + it / 24.0}, date, 37.7, ${72.0 + it * 0.001}, 12.6," }
        val text = "$header\n\$\$SOE\n$lines\n\$\$EOE"
        val ephemeris = parsePlanetCompassHorizonsEphemeris(PlanetCompassCelestialBody.SEDNA, text)
        val midnight = Instant.parse("2026-10-03T00:00:00Z").toEpochMilli()
        assertEquals(72.0005, ephemeris.at(midnight + 30 * 60_000)!!.y, 1e-8)
        assertNull(ephemeris.at(midnight - 1))
        assertThrows(IllegalArgumentException::class.java) {
            parsePlanetCompassHorizonsEphemeris(PlanetCompassCelestialBody.SEDNA, text.replace("AU-D", "KM-S"))
        }
        val sedna = calculatePlanetCompassCelestialObservation(PlanetCompassCelestialBody.SEDNA, now, 45.0, 9.0,
            remote = PlanetCompassCelestialRemoteData(ephemerides = mapOf(PlanetCompassCelestialBody.SEDNA to ephemeris)))!!
        assertTrue(sedna.distanceKm / PLANET_COMPASS_AU_KM in 80.0..85.0)
        assertFalse(planetCompassHorizonsUrl(PlanetCompassCelestialBody.SEDNA, now).contains("latitude"))
    }
    @Test fun clearAndMainlyClearAreDifferentWeatherKinds() {
        assertEquals(PlanetCompassSunWeatherKind.CLEAR, planetCompassSunWeatherKind(0))
        assertEquals(PlanetCompassSunWeatherKind.MAINLY_CLEAR, planetCompassSunWeatherKind(1))
    }
    @Test fun clearSkyNeverDrawsCloudsAndMainlyClearHasOnlyOneLightCloud() {
        for (cover in listOf(0f, 0.05f, 0.5f, 1f, Float.NaN, Float.POSITIVE_INFINITY, -1f, 2f)) {
            assertEquals(0, planetCompassSunDisplayCloudCount(PlanetCompassSunWeatherSnapshot(PlanetCompassSunWeatherKind.CLEAR, cover, now)))
            assertEquals(0f, planetCompassSunDisplayCloudCover(PlanetCompassSunWeatherSnapshot(PlanetCompassSunWeatherKind.CLEAR, cover, now)), 0f)
            val mainly = PlanetCompassSunWeatherSnapshot(PlanetCompassSunWeatherKind.MAINLY_CLEAR, cover, now)
            assertEquals(1, planetCompassSunDisplayCloudCount(mainly))
            assertTrue(planetCompassSunDisplayCloudCover(mainly) <= 0.18f)
        }
        assertEquals(0, planetCompassSunDisplayCloudCount(null))
    }
}

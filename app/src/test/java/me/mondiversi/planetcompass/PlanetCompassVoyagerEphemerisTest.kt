package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test
import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PlanetCompassVoyagerEphemerisTest {
    private val now = Instant.parse("2026-10-03T12:00:00Z").toEpochMilli()
    private val bodies = listOf(PlanetCompassCelestialBody.VOYAGER_1, PlanetCompassCelestialBody.VOYAGER_2)
    private fun fixture(body: PlanetCompassCelestialBody): String = requireNotNull(javaClass.getResource(
        "/celestial/voyager-${if (body == PlanetCompassCelestialBody.VOYAGER_1) 1 else 2}-jpl-2026-10-03.txt"
    )).readText()
    private fun remote(body: PlanetCompassCelestialBody) = PlanetCompassCelestialRemoteData(
        ephemerides = mapOf(body to parsePlanetCompassHorizonsEphemeris(body, fixture(body))))

    @Test fun probesMatchIndependentJplObserverReferencesIncludingBelowTheHorizon() {
        // Independent Horizons OBSERVER, coord@399, 12.5E/41.9N/0.05km, AIRLESS, quantities 4,20.
        val references = listOf(
            doubleArrayOf(105.820896689, 34.450558936, 172.256055273855),
            doubleArrayOf(139.904789176, -39.090443577, 143.956863053836))
        bodies.zip(references).forEach { (body, ref) ->
            val observation = calculatePlanetCompassCelestialObservation(body, now, 41.9, 12.5, 50.0, remote(body))!!
            assertEquals(ref[0], observation.position.azimuthDegrees, 0.02)
            assertEquals(ref[1], observation.position.elevationDegrees, 0.02)
            assertEquals(ref[2] * PLANET_COMPASS_AU_KM, observation.distanceKm, 50.0)
        }
    }

    @Test fun apparentPointingAndDistanceAreLiveInsteadOfUsingTheDailyNoonValue() {
        bodies.forEach { body ->
            val remote = remote(body)
            val a = calculatePlanetCompassCelestialObservation(body, now, 41.9, 12.5, 50.0, remote)!!
            val b = calculatePlanetCompassCelestialObservation(body, now + 30 * 60_000, 41.9, 12.5, 50.0, remote)!!
            assertNotEquals(a.position, b.position)
            assertTrue(b.distanceKm > a.distanceKm)
            val zone = ZoneId.of("Europe/Rome")
            assertEquals(now, planetCompassCelestialDistanceTime(body, now, zone))
            assertEquals(now + 30 * 60_000, planetCompassCelestialDistanceTime(body, now + 30 * 60_000, zone))
        }
    }

    @Test fun parserRejectsWrongProbeCenterUnitsCorrectionsAndDiscontinuousSamples() {
        val text = fixture(PlanetCompassCelestialBody.VOYAGER_1)
        for (invalid in listOf(
            text.replace("(-31)", "(-32)"), text.replace("Earth (399)", "Sun (10)"),
            text.replace("AU-D", "KM-S"), text.replace("ICRF", "ECLIPTIC"),
            text.replace("LT+S CORRECTED", "GEOMETRIC"),
            text.replace("2461315.541666667", "2461315.551666667"))) {
            assertThrows(IllegalArgumentException::class.java) {
                parsePlanetCompassHorizonsEphemeris(PlanetCompassCelestialBody.VOYAGER_1, invalid)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            parsePlanetCompassHorizonsEphemeris(PlanetCompassCelestialBody.VOYAGER_2, text)
        }
        assertThrows(IllegalArgumentException::class.java) {
            parsePlanetCompassHorizonsEphemeris(PlanetCompassCelestialBody.SEDNA, text)
        }
    }

    @Test fun missingExpiredAndMismatchedDataNeverInventAProbePositionOrDistance() {
        val body = PlanetCompassCelestialBody.VOYAGER_1
        assertNull(calculatePlanetCompassCelestialObservation(body, now, 0.0, 0.0))
        val ephemeris = parsePlanetCompassHorizonsEphemeris(body, fixture(body))
        assertNull(ephemeris.at(ephemeris.samples.first().timeMs - 1))
        assertNull(ephemeris.at(ephemeris.samples.last().timeMs + 1))
        assertNull(calculatePlanetCompassCelestialObservation(body, ephemeris.samples.last().timeMs + 1,
            0.0, 0.0, remote = remote(body)))
        assertNull(calculatePlanetCompassCelestialObservation(body, now, 0.0, 0.0, remote = PlanetCompassCelestialRemoteData(
            ephemerides = mapOf(body to parsePlanetCompassHorizonsEphemeris(PlanetCompassCelestialBody.VOYAGER_2,
                fixture(PlanetCompassCelestialBody.VOYAGER_2))))))
    }

    @Test fun bothProbesHaveOnlyLiveLocatorsAndNoDailyCurveEvenWithValidEphemerides() {
        bodies.forEach { body ->
            assertFalse(body.supportsDailyPath)
            assertTrue(body.usesLiveDistance)
            assertNull(calculatePlanetCompassCelestialPath(body, LocalDate.parse("2026-10-03"),
                ZoneId.of("Europe/Rome"), now, 41.9, 12.5, 50.0, remote(body)))
            assertNotNull(calculatePlanetCompassCelestialObservation(body, now, 41.9, 12.5, 50.0, remote(body)))
            assertNotNull(calculatePlanetCompassCelestialObservation(body, now + 30 * 60_000, 41.9, 12.5, 50.0, remote(body)))
        }
    }

    @Test fun displayedProbeDistancesChangeOverTwoSecondsWithoutInventingDataOutsideCoverage() {
        bodies.forEach { body ->
            val data = remote(body)
            val a = calculatePlanetCompassCelestialObservation(body, now, 41.9, 12.5, 50.0, data)!!
            val b = calculatePlanetCompassCelestialObservation(body, now + 2_000, 41.9, 12.5, 50.0, data)!!
            for (format in PlanetCompassNumericFormat.entries) {
                val first = formatPlanetCompassCelestialDistance(body, a.distanceKm, format)
                val second = formatPlanetCompassCelestialDistance(body, b.distanceKm, format)
                assertNotEquals(first, second)
                assertTrue(first.contains("Mkm") && first.contains("AU"))
                assertFalse(first.contains(" km"))
                assertEquals(1, first.lines().size)
                assertEquals("—", formatPlanetCompassCelestialDistance(body, null, format))
            }
        }
    }

    @Test fun otherObjectsStillSupportTheirDailyPathsAndKeepTheirDistancePolicies() {
        PlanetCompassCelestialBody.entries.filterNot { it.isVoyager }.forEach { assertTrue(it.supportsDailyPath) }
        assertNotNull(calculatePlanetCompassCelestialPath(PlanetCompassCelestialBody.SUN, LocalDate.parse("2026-10-03"),
            ZoneId.of("Europe/Rome"), now, 41.9, 12.5, 50.0, PlanetCompassCelestialRemoteData()))
    }

    @Test fun everyBodyDisplaysOnlyOneKilometerUnitAlongsideAstronomicalUnits() {
        for (body in PlanetCompassCelestialBody.entries) for (numeric in PlanetCompassNumericFormat.entries) {
            val text = formatPlanetCompassCelestialDistance(body, 123_456_789.0, numeric)
            val near = body == PlanetCompassCelestialBody.MOON || body.isEarthSatellite
            assertEquals(near, text.contains(" km"))
            val star = body == PlanetCompassCelestialBody.POLARIS
            assertEquals(!near && !star, text.contains(" Mkm"))
            assertEquals(star, text.contains(" ly"))
            assertEquals(if (star) 0 else 1, Regex("(?:Mkm| km)").findAll(text).count())
            assertTrue(text.endsWith(" AU"))
        }
    }

    @Test fun requestsUseDistinctMissionIdsAndNeverTransmitObserverPosition() {
        bodies.forEach { body ->
            val decoded = URLDecoder.decode(planetCompassHorizonsUrl(body, now), "UTF-8")
            assertTrue(decoded.contains("COMMAND='${if (body == PlanetCompassCelestialBody.VOYAGER_1) -31 else -32}'"))
            assertTrue(decoded.contains("CENTER='500@399'"))
            assertTrue(decoded.contains("VEC_CORR='LT+S'"))
            assertTrue(decoded.contains("TIME_TYPE='UT'"))
            assertFalse(decoded.contains("SITE_COORD"))
            assertFalse(decoded.contains("latitude"))
        }
        assertEquals(18, PlanetCompassCelestialBody.entries.size)
    }
}

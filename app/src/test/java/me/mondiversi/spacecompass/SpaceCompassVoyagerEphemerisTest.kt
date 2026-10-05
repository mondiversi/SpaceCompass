package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SpaceCompassVoyagerEphemerisTest {
    private val now = Instant.parse("2026-10-03T12:00:00Z").toEpochMilli()
    private val bodies = listOf(SpaceCompassCelestialBody.VOYAGER_1, SpaceCompassCelestialBody.VOYAGER_2)
    private fun fixture(body: SpaceCompassCelestialBody): String = requireNotNull(javaClass.getResource(
        "/celestial/voyager-${if (body == SpaceCompassCelestialBody.VOYAGER_1) 1 else 2}-jpl-2026-10-03.txt"
    )).readText()
    private fun remote(body: SpaceCompassCelestialBody) = SpaceCompassCelestialRemoteData(
        ephemerides = mapOf(body to parseSpaceCompassHorizonsEphemeris(body, fixture(body))))

    @Test fun probesMatchIndependentJplObserverReferencesIncludingBelowTheHorizon() {
        // Independent Horizons OBSERVER, coord@399, 12.5E/41.9N/0.05km, AIRLESS, quantities 4,20.
        val references = listOf(
            doubleArrayOf(105.820896689, 34.450558936, 172.256055273855),
            doubleArrayOf(139.904789176, -39.090443577, 143.956863053836))
        bodies.zip(references).forEach { (body, ref) ->
            val observation = calculateSpaceCompassCelestialObservation(body, now, 41.9, 12.5, 50.0, remote(body))!!
            assertEquals(ref[0], observation.position.azimuthDegrees, 0.02)
            assertEquals(ref[1], observation.position.elevationDegrees, 0.02)
            assertEquals(ref[2] * SPACE_COMPASS_AU_KM, observation.distanceKm, 50.0)
        }
    }

    @Test fun apparentPointingAndDistanceAreLiveInsteadOfUsingTheDailyNoonValue() {
        bodies.forEach { body ->
            val remote = remote(body)
            val a = calculateSpaceCompassCelestialObservation(body, now, 41.9, 12.5, 50.0, remote)!!
            val b = calculateSpaceCompassCelestialObservation(body, now + 30 * 60_000, 41.9, 12.5, 50.0, remote)!!
            assertNotEquals(a.position, b.position)
            assertTrue(b.distanceKm > a.distanceKm)
            val zone = ZoneId.of("Europe/Rome")
            assertEquals(now, spaceCompassCelestialDistanceTime(body, now, zone))
            assertEquals(now + 30 * 60_000, spaceCompassCelestialDistanceTime(body, now + 30 * 60_000, zone))
        }
    }

    @Test fun parserRejectsWrongProbeCenterUnitsCorrectionsAndDiscontinuousSamples() {
        val text = fixture(SpaceCompassCelestialBody.VOYAGER_1)
        for (invalid in listOf(
            text.replace("(-31)", "(-32)"), text.replace("Earth (399)", "Sun (10)"),
            text.replace("AU-D", "KM-S"), text.replace("ICRF", "ECLIPTIC"),
            text.replace("LT+S CORRECTED", "GEOMETRIC"),
            text.replace("2461315.541666667", "2461315.551666667"))) {
            assertThrows(IllegalArgumentException::class.java) {
                parseSpaceCompassHorizonsEphemeris(SpaceCompassCelestialBody.VOYAGER_1, invalid)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseSpaceCompassHorizonsEphemeris(SpaceCompassCelestialBody.VOYAGER_2, text)
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseSpaceCompassHorizonsEphemeris(SpaceCompassCelestialBody.SEDNA, text)
        }
    }

    @Test fun missingExpiredAndMismatchedDataNeverInventAProbePositionOrDistance() {
        val body = SpaceCompassCelestialBody.VOYAGER_1
        assertNull(calculateSpaceCompassCelestialObservation(body, now, 0.0, 0.0))
        val ephemeris = parseSpaceCompassHorizonsEphemeris(body, fixture(body))
        assertNull(ephemeris.at(ephemeris.samples.first().timeMs - 1))
        assertNull(ephemeris.at(ephemeris.samples.last().timeMs + 1))
        assertNull(calculateSpaceCompassCelestialObservation(body, ephemeris.samples.last().timeMs + 1,
            0.0, 0.0, remote = remote(body)))
        assertNull(calculateSpaceCompassCelestialObservation(body, now, 0.0, 0.0, remote = SpaceCompassCelestialRemoteData(
            ephemerides = mapOf(body to parseSpaceCompassHorizonsEphemeris(SpaceCompassCelestialBody.VOYAGER_2,
                fixture(SpaceCompassCelestialBody.VOYAGER_2))))))
    }

    @Test fun bothProbesHaveDailySkyTracksFromEarthRotationWithCompleteEphemerides() {
        bodies.forEach { body ->
            assertTrue(body.supportsDailyPath)
            assertTrue(body.usesLiveDistance)
            val date = LocalDate.parse("2026-10-03")
            val zone = ZoneId.of("UTC")
            val path = calculateSpaceCompassCelestialPath(body, date, zone, now,
                41.9, 12.5, 50.0, remote(body))!!
            assertEquals(24, path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
            assertTrue(path.issPass.isEmpty())
            assertTrue(path.samples.maxOf { it.position.elevationDegrees } -
                path.samples.minOf { it.position.elevationDegrees } > 30)
            for (point in path.samples) {
                val expected = calculateSpaceCompassCelestialObservation(body, point.timeMs,
                    41.9, 12.5, 50.0, remote(body))!!.position
                assertEquals(expected, point.position)
            }
            assertNull(calculateSpaceCompassCelestialPath(body, date, zone, now,
                41.9, 12.5, 50.0, SpaceCompassCelestialRemoteData()))
            // Do not extrapolate a whole day beyond the available JPL sample interval.
            assertNull(calculateSpaceCompassCelestialPath(body, date.minusDays(2), zone, now,
                41.9, 12.5, 50.0, remote(body)))
        }
    }
    @Test fun displayedProbeDistancesChangeOverTwoSecondsWithoutInventingDataOutsideCoverage() {
        bodies.forEach { body ->
            val data = remote(body)
            val a = calculateSpaceCompassCelestialObservation(body, now, 41.9, 12.5, 50.0, data)!!
            val b = calculateSpaceCompassCelestialObservation(body, now + 2_000, 41.9, 12.5, 50.0, data)!!
            for (format in SpaceCompassNumericFormat.entries) {
                val first = formatSpaceCompassCelestialDistance(body, a.distanceKm, format)
                val second = formatSpaceCompassCelestialDistance(body, b.distanceKm, format)
                assertNotEquals(first, second)
                assertTrue(first.contains("Mkm") && first.contains("AU"))
                assertFalse(first.contains(" km"))
                assertEquals(1, first.lines().size)
                assertEquals("—", formatSpaceCompassCelestialDistance(body, null, format))
            }
        }
    }

    @Test fun otherObjectsStillSupportTheirDailyPathsAndKeepTheirDistancePolicies() {
        SpaceCompassCelestialBody.entries.forEach { assertEquals(it != SpaceCompassCelestialBody.EARTH_CENTER, it.supportsDailyPath) }
        assertNotNull(calculateSpaceCompassCelestialPath(SpaceCompassCelestialBody.SUN, LocalDate.parse("2026-10-03"),
            ZoneId.of("Europe/Rome"), now, 41.9, 12.5, 50.0, SpaceCompassCelestialRemoteData()))
    }

    @Test fun everyBodyDisplaysOnlyOneKilometerUnitAlongsideAstronomicalUnits() {
        for (body in SpaceCompassCelestialBody.entries) for (numeric in SpaceCompassNumericFormat.entries) {
            val text = formatSpaceCompassCelestialDistance(body, 123_456_789.0, numeric)
            val near = body == SpaceCompassCelestialBody.MOON || body.isEarthSatellite || body == SpaceCompassCelestialBody.EARTH_CENTER
            assertEquals(near, text.contains(" km"))
            val star = body == SpaceCompassCelestialBody.POLARIS
            assertEquals(!near && !star, text.contains(" Mkm"))
            assertEquals(star, text.contains(" ly"))
            assertEquals(if (star) 0 else 1, Regex("(?:Mkm| km)").findAll(text).count())
            assertEquals(body != SpaceCompassCelestialBody.EARTH_CENTER, text.endsWith(" AU"))
        }
    }

    @Test fun requestsUseDistinctMissionIdsAndNeverTransmitObserverPosition() {
        bodies.forEach { body ->
            val decoded = URLDecoder.decode(spaceCompassHorizonsUrl(body, now), "UTF-8")
            assertTrue(decoded.contains("COMMAND='${if (body == SpaceCompassCelestialBody.VOYAGER_1) -31 else -32}'"))
            assertTrue(decoded.contains("CENTER='500@399'"))
            assertTrue(decoded.contains("VEC_CORR='LT+S'"))
            assertTrue(decoded.contains("TIME_TYPE='UT'"))
            assertFalse(decoded.contains("SITE_COORD"))
            assertFalse(decoded.contains("latitude"))
        }
        assertEquals(29, SpaceCompassCelestialBody.entries.size)
    }
}

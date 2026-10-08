package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import org.junit.Assert.*
import org.junit.Test
import sgp4.TLE
import java.net.URLDecoder
import java.time.Instant
import java.util.TimeZone
import kotlin.math.abs

class SpaceCompassCelestialVelocityTest {
    private val now = Instant.parse("2026-10-03T12:00:00Z").toEpochMilli()
    private val orbit = SpaceCompassIssOrbit.parse("1 25544U 98067A   26276.04623379  .00005750  00000+0  11349-3 0  9994\n" +
        "2 25544  51.6314 126.3061 0006899 216.4733 143.5786 15.48722558588472")
    private val targets = mapOf(SpaceCompassCelestialBody.SEDNA to "sedna",
        SpaceCompassCelestialBody.VOYAGER_1 to "voyager-1", SpaceCompassCelestialBody.VOYAGER_2 to "voyager-2")
    private fun fixture(body: SpaceCompassCelestialBody) = requireNotNull(javaClass.getResource(
        "/celestial/${targets.getValue(body)}-heliocentric-motion-jpl-2026-10-03.txt")).readText()
    private fun remote(body: SpaceCompassCelestialBody) = SpaceCompassCelestialRemoteData(
        motions = mapOf(body to parseSpaceCompassHorizonsMotion(body, fixture(body))))

    @Test fun localOrbitalSpeedsMatchIndependentGeometricJplStates() {
        // Horizons: Earth/Mars relative to Sun; Moon relative to Earth. FRAME/ICRF, NONE,
        // VEC_TABLE=2, AU-D, at 2026-10-03 12:00 UTC. These are orbital, not range-rate speeds.
        for ((body, reference) in mapOf(SpaceCompassCelestialBody.SUN to 29.765415655721444,
            SpaceCompassCelestialBody.MOON to 1.058034277336237, SpaceCompassCelestialBody.MARS to 23.54009617752453)) {
            assertEquals(body.name, reference, calculateSpaceCompassCelestialSpeed(body, now)!!, 0.01)
        }
    }
    @Test fun localBodiesUseTheirOrbitalPrimaryAndSunSelectionShowsEarthsSpeed() {
        for (body in SpaceCompassCelestialBody.entries.filter { it.engine != null }) {
            val speed = calculateSpaceCompassCelestialSpeed(body, now)!!
            assertTrue("${body.name}: $speed", speed.isFinite() && speed in 0.1..140.0)
            fun position(ms: Long): Vector {
                val t = spaceCompassAstronomyTime(ms)
                return when (body) {
                    SpaceCompassCelestialBody.SUN -> helioVector(Body.Earth, t)
                    SpaceCompassCelestialBody.MOON -> geoMoon(t)
                    else -> helioVector(requireNotNull(body.engine), t)
                }
            }
            val before = position(now - 10_000)
            val delta = position(now + 10_000).withTime(before.t) - before
            val reference = delta.length() * SPACE_COMPASS_AU_KM / 20.0
            assertEquals(body.name, reference, speed, 0.01)
        }
        assertTrue(calculateSpaceCompassCelestialSpeed(SpaceCompassCelestialBody.SUN, now)!! > 29.0)
    }
    @Test fun issUsesSgp4InertialVelocityAndIsNotTheRadialRateOrEarthFixedMotion() {
        val remote = SpaceCompassCelestialRemoteData(iss = orbit)
        val speed = calculateSpaceCompassCelestialSpeed(SpaceCompassCelestialBody.ISS, now, remote)!!
        assertTrue(speed in 7.5..7.8)
        val displacement = orbit.teme(now + 1000).zip(orbit.teme(now - 1000)).map { (a, b) -> (a - b) / 2.0 }
        assertEquals(spaceCompassCelestialVelocityMagnitude(displacement[0], displacement[1], displacement[2]), speed, 0.001)
        val r = orbit.teme(now)
        val radial = r.zip(displacement).sumOf { (p, v) -> p * v } / kotlin.math.sqrt(r.sumOf { it * it })
        assertTrue(abs(radial) < speed / 10)
        assertNull(calculateSpaceCompassCelestialSpeed(SpaceCompassCelestialBody.ISS, orbit.epochMs + SPACE_COMPASS_ISS_MAX_AGE_MS + 1, remote))
        assertNull(calculateSpaceCompassCelestialSpeed(SpaceCompassCelestialBody.ISS, now))
    }
    @Test fun sgp4VelocityAlsoMatchesValladosPublishedVerificationVector() {
        val tle = TLE("1 00005U 58002B   00179.78495062  .00000023  00000-0  28098-4 0  4753",
            "2 00005  34.2682 348.7242 1859667 331.7664  19.3264 10.82419157413667")
        assertArrayEquals(doubleArrayOf(1.893841015, 6.405893759, 4.534807250), tle.getRV(0.0)[1], 1e-8)
        assertEquals(0, tle.sgp4Error)
    }
    @Test fun remoteSpeedsMatchJplSunCenteredOrbitalOrOutwardComponents() {
        // 500@10, NONE: Sedna |v|; probes dr/dt = dot(r,v)/|r|, not geocentric motion.
        val references = mapOf(SpaceCompassCelestialBody.VOYAGER_1 to 16.8816849181763,
            SpaceCompassCelestialBody.VOYAGER_2 to 15.045948057988776, SpaceCompassCelestialBody.SEDNA to 4.447081853174975)
        references.forEach { (body, expected) ->
            assertEquals(expected, calculateSpaceCompassCelestialSpeed(body, now, remote(body))!!, 1e-8)
            assertNotEquals(calculateSpaceCompassCelestialSpeed(body, now, remote(body)),
                calculateSpaceCompassCelestialSpeed(body, now + 30 * 60_000, remote(body)))
        }
    }
    @Test fun missingExpiredOrWrongTargetVelocityNeverInventsASpeed() {
        targets.keys.forEach { body ->
            assertNull(calculateSpaceCompassCelestialSpeed(body, now))
            val motion = remote(body).motions.getValue(body)
            assertNull(motion.speedAt(motion.samples.first().timeMs - 1))
            assertNull(motion.speedAt(motion.samples.last().timeMs + 1))
        }
        assertNull(calculateSpaceCompassCelestialSpeed(SpaceCompassCelestialBody.VOYAGER_1, now, SpaceCompassCelestialRemoteData(
            motions = mapOf(SpaceCompassCelestialBody.VOYAGER_1 to remote(SpaceCompassCelestialBody.VOYAGER_2).motions.getValue(SpaceCompassCelestialBody.VOYAGER_2)))))
        // Cached apparent pointing is independent of the optional velocity request.
        val text = requireNotNull(javaClass.getResource("/celestial/voyager-1-jpl-2026-10-03.txt")).readText()
        val pointingOnly = SpaceCompassCelestialRemoteData(ephemerides = mapOf(SpaceCompassCelestialBody.VOYAGER_1 to
            parseSpaceCompassHorizonsEphemeris(SpaceCompassCelestialBody.VOYAGER_1, text)))
        assertNotNull(calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.VOYAGER_1, now, 45.0, 9.0, remote = pointingOnly))
        assertNull(calculateSpaceCompassCelestialSpeed(SpaceCompassCelestialBody.VOYAGER_1, now, pointingOnly))
    }
    @Test fun parserRejectsApparentVectorsWrongIdentityUnitsFrameTimesAndInvalidVelocities() {
        val body = SpaceCompassCelestialBody.VOYAGER_1
        val text = fixture(body)
        val firstVelocity = text.substringAfter("\$\$SOE").lineSequence().first { it.contains(',') }.split(',')[5].trim()
        for (invalid in listOf(text.replace("(-31)", "(-32)"), text.replace("Sun (10)", "Earth (399)"),
            text.replace("AU-D", "KM-S"), text.replace("ICRF", "ECLIPTIC"), text.replace("BODY CENTER", "OBSERVATORY"),
            text.replace("GEOMETRIC cartesian states", "LT+S CORRECTED cartesian states"),
            text.replace(" UT ", " TDB "), text.replace("2461315.541666667", "2461315.551666667"),
            text.replace(firstVelocity, "NaN"), text.replace(firstVelocity, "1.0E+99"))) {
            assertThrows(IllegalArgumentException::class.java) { parseSpaceCompassHorizonsMotion(body, invalid) }
        }
        val apparent = requireNotNull(javaClass.getResource("/celestial/voyager-1-jpl-2026-10-03.txt")).readText()
        assertThrows(IllegalArgumentException::class.java) { parseSpaceCompassHorizonsMotion(body, apparent) }
        assertThrows(IllegalArgumentException::class.java) { parseSpaceCompassHorizonsEphemeris(body, text) }
        // Legacy Earth-centered motion caches must never be accepted under the new reference.
        val legacy = requireNotNull(javaClass.getResource("/celestial/voyager-1-motion-jpl-2026-10-03.txt")).readText()
        assertThrows(IllegalArgumentException::class.java) { parseSpaceCompassHorizonsMotion(body, legacy) }
    }
    @Test fun interpolationUsesVelocityComponentsBeforeTheMagnitudeAndConvertsAuPerDay() {
        val motion = SpaceCompassHorizonsMotion(SpaceCompassCelestialBody.SEDNA, listOf(
            SpaceCompassHorizonsStateSample(now, 80.0, 0.0, 0.0, 0.01, 0.0, 0.0),
            SpaceCompassHorizonsStateSample(now + 3_600_000, 80.0, 0.0, 0.0, -0.01, 0.0, 0.0)))
        assertEquals(17.314568368055555, motion.speedAt(now)!!, 1e-9)
        assertEquals(0.0, motion.speedAt(now + 30 * 60_000)!!, 1e-9)
        assertEquals(5.0, spaceCompassCelestialVelocityMagnitude(3.0, 4.0, 0.0), 1e-9)
    }
    @Test fun utcSpeedIsIndependentOfTimezoneAndDst() {
        val original = TimeZone.getDefault()
        try {
            val expected = calculateSpaceCompassCelestialSpeed(SpaceCompassCelestialBody.MOON, now)
            for (zone in listOf("Europe/Rome", "Pacific/Auckland", "America/New_York")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertEquals(expected, calculateSpaceCompassCelestialSpeed(SpaceCompassCelestialBody.MOON, now))
            }
        } finally { TimeZone.setDefault(original) }
    }
    @Test fun geometricRequestsAreSunCenteredButPointingStaysEarthCenteredAndNeitherSendsGps() {
        for (body in targets.keys) {
            val url = URLDecoder.decode(spaceCompassHorizonsUrl(body, now, geometricMotion = true), "UTF-8")
            assertTrue(url.contains("CENTER='500@10'"))
            assertTrue(url.contains("VEC_CORR='NONE'"))
            assertTrue(url.contains("VEC_TABLE='2'"))
            assertTrue(url.contains("OUT_UNITS='AU-D'"))
            assertTrue(url.contains("TIME_TYPE='UT'"))
            assertFalse(url.contains("SITE_COORD"))
            assertTrue(URLDecoder.decode(spaceCompassHorizonsUrl(body, now), "UTF-8").contains("CENTER='500@399'"))
        }
    }

    @Test fun probeOutwardSpeedUsesRadialProjectionAndKeepsItsSign() {
        fun sample(t: Long, vx: Double) = SpaceCompassHorizonsStateSample(t, 100.0, 0.0, 0.0, vx, 0.02, 0.0)
        val probe = SpaceCompassHorizonsMotion(SpaceCompassCelestialBody.VOYAGER_1,
            listOf(sample(now, 0.01), sample(now + 3_600_000, -0.01)))
        assertEquals(0.01 * SPACE_COMPASS_AU_DAY_TO_KM_SECOND, probe.speedAt(now)!!, 1e-9)
        assertEquals(0.0, probe.speedAt(now + 1_800_000)!!, 1e-9)
        assertEquals(-0.01 * SPACE_COMPASS_AU_DAY_TO_KM_SECOND, probe.speedAt(now + 3_600_000)!!, 1e-9)
        assertTrue(SpaceCompassHorizonsMotion(SpaceCompassCelestialBody.SEDNA, probe.samples).speedAt(now)!! > probe.speedAt(now)!!)
    }

    @Test fun speedLabelsIdentifyAllFivePhysicalReferences() {
        assertEquals(R.string.celestial_speed_earth_orbit, spaceCompassCelestialSpeedLabel(SpaceCompassCelestialBody.SUN))
        assertEquals(R.string.celestial_speed_orbit_saturn, spaceCompassCelestialSpeedLabel(SpaceCompassCelestialBody.TITAN))
        listOf(SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3).forEach {
            assertEquals(R.string.celestial_speed_orbit_earth, spaceCompassCelestialSpeedLabel(it))
        }
        SpaceCompassCelestialBody.entries.filter { it.isVoyager }.forEach {
            assertEquals(R.string.celestial_speed_outward_sun, spaceCompassCelestialSpeedLabel(it))
        }
        SpaceCompassCelestialBody.entries.filter { it != SpaceCompassCelestialBody.EARTH_CENTER && it != SpaceCompassCelestialBody.SUN && it != SpaceCompassCelestialBody.MOON &&
            !it.isEarthSatellite && !it.isExtrasolar && !it.isVoyager && !it.isJovianMoon && it != SpaceCompassCelestialBody.TITAN }.forEach {
            assertEquals(R.string.celestial_speed_orbit_sun, spaceCompassCelestialSpeedLabel(it))
        }
        SpaceCompassCelestialBody.entries.filter { it.isJovianMoon }.forEach {
            assertEquals(R.string.celestial_speed_orbit_jupiter, spaceCompassCelestialSpeedLabel(it))
        }
        assertEquals(R.string.celestial_speed_unavailable, spaceCompassCelestialSpeedLabel(SpaceCompassCelestialBody.POLARIS))
        assertNull(calculateSpaceCompassCelestialSpeed(SpaceCompassCelestialBody.POLARIS, now))
    }
}

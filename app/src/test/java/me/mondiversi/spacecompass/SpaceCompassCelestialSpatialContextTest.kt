package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.geoVector
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Locale
import kotlin.math.PI
import kotlin.math.pow

class SpaceCompassCelestialSpatialContextTest {
    private val now = Instant.parse("2026-10-06T08:00:00Z").toEpochMilli()
    private val numeric = SpaceCompassNumericFormat.AMERICAN
    private val frame = SpaceCompassCelestialCoordinateFrame.GEOCENTRIC_EQJ
    private fun calculate(body: SpaceCompassCelestialBody, time: Long = now,
        remote: SpaceCompassCelestialRemoteData = SpaceCompassCelestialRemoteData()) =
        calculateSpaceCompassCelestialCoordinates(body, time, remote)

    @Test fun axesAndQuadrantsPreserveEquatorialConvention() {
        fun position(x: Double, y: Double, z: Double) = spaceCompassCelestialCoordinatesFromVector(x, y, z, frame, now)!!
        assertEquals(0.0, position(1.0, 0.0, 0.0).rightAscensionHours, 1e-12)
        assertEquals(6.0, position(0.0, 1.0, 0.0).rightAscensionHours, 1e-12)
        assertEquals(12.0, position(-1.0, 0.0, 0.0).rightAscensionHours, 1e-12)
        assertEquals(18.0, position(0.0, -1.0, 0.0).rightAscensionHours, 1e-12)
        assertEquals(90.0, position(0.0, 0.0, 1.0).declinationDegrees, 1e-12)
        assertEquals(-90.0, position(0.0, 0.0, -1.0).declinationDegrees, 1e-12)
        assertEquals(-45.0, position(1.0, 0.0, -1.0).declinationDegrees, 1e-12)
    }

    @Test fun noInvalidVectorBecomesAPlausibleDirection() {
        for (bad in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertNull(spaceCompassCelestialCoordinatesFromVector(bad, 1.0, 1.0, frame, now))
            assertNull(spaceCompassCelestialCoordinatesFromVector(1.0, bad, 1.0, frame, now))
            assertNull(spaceCompassCelestialCoordinatesFromVector(1.0, 1.0, bad, frame, now))
        }
        assertNull(spaceCompassCelestialCoordinatesFromVector(0.0, 0.0, 0.0, frame, now))
    }

    @Test fun solarCoordinatesMatchEngineWithoutAnyGpsFix() {
        val vector = geoVector(Body.Sun, spaceCompassAstronomyTime(now), Aberration.Corrected)
        val result = calculate(SpaceCompassCelestialBody.SUN)!!
        assertEquals(spaceCompassCelestialCoordinatesFromVector(vector.x, vector.y, vector.z, frame, now), result)
        assertTrue(result.rightAscensionHours in 0.0..24.0)
        assertTrue(result.declinationDegrees in -90.0..90.0)
        assertNotEquals(result.rightAscensionHours, calculate(SpaceCompassCelestialBody.SUN, now + 86400000)!!.rightAscensionHours)
        assertNull(calculate(SpaceCompassCelestialBody.EARTH_CENTER))
    }

    @Test fun jovianMoonsKeepEngineJ2000Reference() {
        for (body in listOf(SpaceCompassCelestialBody.IO, SpaceCompassCelestialBody.EUROPA)) {
            val result = calculate(body)!!
            assertEquals(frame, result.frame)
            val vector = spaceCompassJovianMoonGeoVector(body, spaceCompassAstronomyTime(now), Aberration.Corrected)
            assertEquals(spaceCompassCelestialCoordinatesFromVector(vector.x, vector.y, vector.z, frame, now), result)
        }
    }

    @Test fun distantObjectsRetainCatalogueEpochAndHostDirections() {
        for (body in SpaceCompassCelestialBody.entries.filter { it.deepSkyReference != null }) {
            val result = calculate(body)!!
            assertEquals(body.deepSkyReference!!.raHours, result.rightAscensionHours, 0.0)
            assertEquals(body.deepSkyReference!!.decDegrees, result.declinationDegrees, 0.0)
            assertEquals(SpaceCompassCelestialCoordinateFrame.CATALOG_ICRS, result.frame)
            assertNull(result.timeMs)
            assertEquals(result, calculate(body, now + 100 * 365 * 86400000L))
        }
        assertEquals(SPACE_COMPASS_POLARIS_RA_HOURS, calculate(SpaceCompassCelestialBody.POLARIS)!!.rightAscensionHours, 0.0)
    }

    @Test fun remoteCoordinatesRequireMatchingCoveredEphemerides() {
        val body = SpaceCompassCelestialBody.SEDNA
        val ephemeris = SpaceCompassHorizonsEphemeris(body, listOf(
            SpaceCompassHorizonsSample(now - 1000, 1.0, 0.0, 0.0),
            SpaceCompassHorizonsSample(now + 1000, 0.0, 1.0, 0.0)))
        val remote = SpaceCompassCelestialRemoteData(ephemerides = mapOf(body to ephemeris))
        val result = calculate(body, now, remote)!!
        assertEquals(3.0, result.rightAscensionHours, 1e-12)
        assertEquals(SpaceCompassCelestialCoordinateFrame.GEOCENTRIC_ICRF, result.frame)
        assertEquals(now, result.timeMs)
        assertNull(calculate(body, now + 1001, remote))
        assertNull(calculate(body, now - 1001, remote))
        assertNull(calculate(body))
        assertNull(calculate(SpaceCompassCelestialBody.TITAN, now,
            remote.copy(ephemerides = mapOf(SpaceCompassCelestialBody.TITAN to ephemeris))))
    }

    @Test fun satellitesKeepTemeAndRejectExpiredOrbits() {
        val tle = javaClass.classLoader!!.getResource("catalog-refresh/iss-2026-10-06.tle")!!.readText().trim()
        val orbit = SpaceCompassIssOrbit.parse(tle)
        val remote = SpaceCompassCelestialRemoteData(iss = orbit)
        val result = calculate(SpaceCompassCelestialBody.ISS, now, remote)!!
        val vector = orbit.teme(now)
        assertEquals(spaceCompassCelestialCoordinatesFromVector(vector[0], vector[1], vector[2],
            SpaceCompassCelestialCoordinateFrame.GEOCENTRIC_TEME, now), result)
        assertNull(calculate(SpaceCompassCelestialBody.ISS, now + 365 * 86400000L, remote))
        assertNull(calculate(SpaceCompassCelestialBody.STARLINK_V3, now, remote))
    }

    @Test fun rightAscensionWrapsRoundingAndUsesHoursNotClockTime() {
        assertEquals("0h 0m 0.00s", formatSpaceCompassRightAscension(23.999999999, numeric, true))
        assertEquals("23h 0m 0.00s", formatSpaceCompassRightAscension(-1.0, numeric, true))
        assertEquals("0.00000 h", formatSpaceCompassRightAscension(23.999999999, numeric, false))
        assertEquals("6.00000 h", formatSpaceCompassRightAscension(30.0, numeric, false))
        assertEquals("1h 1m 1.20s", formatSpaceCompassRightAscension(1 + 1.0/60 + 1.2/3600, numeric, true))
        assertEquals("1,25000 h", formatSpaceCompassRightAscension(1.25, SpaceCompassNumericFormat.EUROPEAN, false))
        assertEquals("—", formatSpaceCompassRightAscension(Double.NaN, numeric, true))
    }

    @Test fun declinationCarriesSecondsAndNeverBecomesGpsLatitude() {
        assertEquals("+13° 0′ 0.0″", formatSpaceCompassDeclination(12.999999999, numeric, true))
        assertEquals("−90° 0′ 0.0″", formatSpaceCompassDeclination(-90.0, numeric, true))
        assertEquals("−12.50000°", formatSpaceCompassDeclination(-12.5, numeric, false))
        for (bad in listOf(-91.0, 91.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertEquals("—", formatSpaceCompassDeclination(bad, numeric, true))
        val arabic = formatSpaceCompassDeclination(12.5, SpaceCompassNumericFormat.SYSTEM, true, Locale.forLanguageTag("ar-EG"))
        assertTrue(arabic.contains("١٢"))
        assertFalse(arabic.contains("12"))
    }

    @Test fun membershipsDistinguishGalaxyAndUnresolvedSystem() {
        assertEquals(R.string.celestial_spatial_solar_system, spaceCompassCelestialMembership(SpaceCompassCelestialBody.VOYAGER_1).systemResource)
        assertEquals(spaceCompassCelestialMembership(SpaceCompassCelestialBody.ALPHA_CENTAURI),
            spaceCompassCelestialMembership(SpaceCompassCelestialBody.PROXIMA_CENTAURI))
        assertEquals("TRAPPIST-1", spaceCompassCelestialMembership(SpaceCompassCelestialBody.TRAPPIST_1_E).systemDesignation)
        assertEquals(SpaceCompassCelestialGalaxy.ANDROMEDA, spaceCompassCelestialMembership(SpaceCompassCelestialBody.ANDROMEDA_CORE).galaxy)
        assertEquals(SpaceCompassCelestialGalaxy.TON_618_HOST, spaceCompassCelestialMembership(SpaceCompassCelestialBody.TON_618).galaxy)
        assertNull(spaceCompassCelestialMembership(SpaceCompassCelestialBody.STEPHENSON_2_18).systemResource)
        assertNull(spaceCompassCelestialMembership(SpaceCompassCelestialBody.STEPHENSON_2_18).systemDesignation)
    }

    @Test fun volumeIsAQualifiedSphereEstimateAndConvertsCubically() {
        val body = SpaceCompassCelestialBody.MOON
        val facts = SpaceCompassCelestialFacts(diameterKm = 2.0)
        assertEquals(4 * PI / 3, spaceCompassCelestialSphericalVolumeKm3(body, facts)!!, 1e-12)
        assertEquals("≈ 1 mi³", formatSpaceCompassCelestialVolume(1.609344.pow(3), numeric, true))
        assertEquals("≈ 1 km³", formatSpaceCompassCelestialVolume(1.0, numeric, false))
        for (excluded in listOf(SpaceCompassCelestialBody.TON_618, SpaceCompassCelestialBody.COMET_67P,
            SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.ORION_NEBULA, SpaceCompassCelestialBody.LV_426))
            assertNull(spaceCompassCelestialSphericalVolumeKm3(excluded, facts))
        for (bad in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertNull(spaceCompassCelestialSphericalVolumeKm3(body, facts.copy(diameterKm = bad)))
        assertNull(formatSpaceCompassCelestialVolume(null, numeric, false))
    }
}

package me.mondiversi.spacecompass

import io.github.cosinekitty.astronomy.*
import java.time.Instant
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogEarthDistanceTest {
    private val now = Instant.parse("2026-10-16T12:00:00Z").toEpochMilli()
    private val sun = SpaceCompassCelestialBody.SUN
    private val iss = SpaceCompassCelestialBody.ISS
    private val starlink = SpaceCompassCelestialBody.STARLINK_V3

    @Test fun earthSortUsesDisplayedRangesInsteadOfSolarDistanceOrDefaultOrder() {
        val bodies = listOf(iss, starlink, sun)
        val solar = mapOf(iss to 1.0, starlink to 1.0, sun to 0.0)
        val earth = mapOf(iss to 6798.0, starlink to 6722.0, sun to SPACE_COMPASS_AU_KM)
        assertEquals(listOf(starlink, iss, sun), spaceCompassSortCatalog(bodies,
            SpaceCompassCatalogSort.EARTH_DISTANCE_ASC, emptyMap(), solar, Locale.ROOT, earth))
        assertEquals(listOf(sun, iss, starlink), spaceCompassSortCatalog(bodies,
            SpaceCompassCatalogSort.EARTH_DISTANCE_DESC, emptyMap(), solar, Locale.ROOT, earth))
        assertEquals(listOf(sun, iss, starlink), spaceCompassSortCatalog(bodies,
            SpaceCompassCatalogSort.DISTANCE_ASC, emptyMap(), solar, Locale.ROOT, earth))
        assertEquals(bodies, spaceCompassSortCatalog(bodies,
            SpaceCompassCatalogSort.CATALOG, emptyMap(), solar, Locale.ROOT, earth))
    }

    @Test fun missingInvalidAndEqualEarthRangesKeepStableOrderInBothDirections() {
        val moon = SpaceCompassCelestialBody.MOON
        val mars = SpaceCompassCelestialBody.MARS
        val venus = SpaceCompassCelestialBody.VENUS
        val input = listOf(mars, starlink, moon, venus, iss, sun)
        for (bad in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            val earth = mapOf(iss to 6800.0, starlink to 6800.0, sun to SPACE_COMPASS_AU_KM,
                mars to bad, venus to bad, moon to bad)
            for (sort in listOf(SpaceCompassCatalogSort.EARTH_DISTANCE_ASC, SpaceCompassCatalogSort.EARTH_DISTANCE_DESC)) {
                val known = if (sort.descending) listOf(sun, iss, starlink) else listOf(iss, starlink, sun)
                assertEquals(known + listOf(venus, moon, mars), spaceCompassSortCatalog(input,
                    sort, emptyMap(), emptyMap(), Locale.ROOT, earth))
            }
        }
    }

    @Test fun engineAndJovianMoonRangesUseEarthCentredVectorsWithoutGps() {
        val time = spaceCompassAstronomyTime(now)
        for (body in listOf(sun, SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.MERCURY,
            SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.JUPITER)) {
            assertEquals(geoVector(body.engine!!, time, Aberration.Corrected).length() * SPACE_COMPASS_AU_KM,
                spaceCompassCelestialCatalogEarthDistanceKm(body, now)!!, 1e-6)
        }
        assertTrue(spaceCompassCelestialCatalogEarthDistanceKm(sun, now)!! > 140_000_000)
        assertEquals(0.0, spaceCompassCelestialCatalogDistanceAu(sun, now)!!, 0.0)
        for (body in listOf(SpaceCompassCelestialBody.IO, SpaceCompassCelestialBody.EUROPA))
            assertEquals(spaceCompassJovianMoonGeoVector(body, time, Aberration.Corrected).length() * SPACE_COMPASS_AU_KM,
                spaceCompassCelestialCatalogEarthDistanceKm(body, now)!!, 1e-6)
    }

    @Test fun horizonsRangesUseOnlyValidatedEarthCentredStatesWithinTheirWindow() {
        val body = SpaceCompassCelestialBody.JWST
        val ephemeris = SpaceCompassHorizonsEphemeris(body, listOf(
            SpaceCompassHorizonsSample(now, 0.003, 0.004, 0.0),
            SpaceCompassHorizonsSample(now + 3600000, 0.003, 0.004, 0.0)))
        val remote = SpaceCompassCelestialRemoteData(ephemerides = mapOf(body to ephemeris))
        assertEquals(0.005 * SPACE_COMPASS_AU_KM,
            spaceCompassCelestialCatalogEarthDistanceKm(body, now + 1800000, remote)!!, 1e-6)
        assertNull(spaceCompassCelestialCatalogEarthDistanceKm(body, now - 1, remote))
        assertNull(spaceCompassCelestialCatalogEarthDistanceKm(body, now + 3600001, remote))
        assertNull(spaceCompassCelestialCatalogEarthDistanceKm(body, now))
        assertNull(spaceCompassCelestialCatalogEarthDistanceKm(SpaceCompassCelestialBody.NEW_HORIZONS, now,
            remote.copy(ephemerides = mapOf(SpaceCompassCelestialBody.NEW_HORIZONS to ephemeris))))
        val motion = SpaceCompassHorizonsMotion(body, listOf(
            SpaceCompassHorizonsStateSample(now, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            SpaceCompassHorizonsStateSample(now + 3600000, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0)))
        assertNull(spaceCompassCelestialCatalogEarthDistanceKm(body, now,
            SpaceCompassCelestialRemoteData(motions = mapOf(body to motion))))
    }

    @Test fun satellitesUseOrbitalRadiusAndRejectMissingOrExpiredData() {
        val text = javaClass.classLoader!!.getResource("catalog-refresh/iss-2026-10-06.tle")!!.readText()
        val orbit = SpaceCompassIssOrbit.parse(text)
        val time = orbit.epochMs + 60000
        val remote = SpaceCompassCelestialRemoteData(iss = orbit)
        val position = orbit.teme(time)
        val radius = sqrt(position.sumOf { it * it })
        assertEquals(radius, spaceCompassCelestialCatalogEarthDistanceKm(iss, time, remote)!!, 1e-6)
        assertTrue(radius > 6400)
        assertTrue(abs(radius - orbit.observe(time, 45.0, 9.0, 100.0).distanceKm) > 1.0)
        assertNull(spaceCompassCelestialCatalogEarthDistanceKm(iss, time + 365 * 86400000L, remote))
        assertNull(spaceCompassCelestialCatalogEarthDistanceKm(starlink, time, remote))
    }

    @Test fun distantReferenceRangesRetainProperMotionParallaxAndLightYears() {
        for (body in listOf(SpaceCompassCelestialBody.PROXIMA_CENTAURI, SpaceCompassCelestialBody.PROXIMA_B,
            SpaceCompassCelestialBody.POLARIS, SpaceCompassCelestialBody.M87)) {
            val range = spaceCompassCelestialCatalogEarthDistanceKm(body, now)!!
            // A surface observer differs from the geocentre by at most one Earth radius.
            val observed = calculateSpaceCompassCelestialObservation(body, now, 45.0, 9.0, 100.0)!!.distanceKm
            assertTrue("$body: $range / $observed", abs(range - observed) < 7000.0)
            assertTrue(formatSpaceCompassCelestialCatalogEarthDistance(body, range,
                SpaceCompassNumericFormat.INTERNATIONAL, "mkm").endsWith(" ly"))
        }
    }

    @Test fun earthColumnRespectsSelectedUnitsAndPreservesUnknownValues() {
        val numeric = SpaceCompassNumericFormat.INTERNATIONAL
        assertEquals(formatSpaceCompassPhysicalLength(6722000.0, 2, numeric, false, large = true),
            formatSpaceCompassCelestialCatalogEarthDistance(starlink, 6722.0, numeric, "mkm"))
        assertEquals(formatSpaceCompassPhysicalLength(6722000.0, 2, numeric, true, large = true),
            formatSpaceCompassCelestialCatalogEarthDistance(starlink, 6722.0, numeric, "mmi", true))
        for (invalid in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertEquals("—", formatSpaceCompassCelestialCatalogEarthDistance(iss, invalid, numeric, "mkm"))
        assertEquals(SpaceCompassCatalogSortField.EARTH_DISTANCE, SpaceCompassCatalogSort.EARTH_DISTANCE_ASC.valueField)
        assertEquals(R.string.catalog_distance_from_earth, SpaceCompassCatalogSort.EARTH_DISTANCE_ASC.valueField.label)
    }
}

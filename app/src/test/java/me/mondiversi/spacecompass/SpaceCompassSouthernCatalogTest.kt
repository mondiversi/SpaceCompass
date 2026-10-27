package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSouthernCatalogTest {
    private val now = Instant.parse("2026-10-16T12:30:00Z").toEpochMilli()
    private val cross = SpaceCompassCelestialBody.SOUTHERN_CROSS
    private fun fixture(name: String) = javaClass.classLoader!!.getResource("catalog-south/$name")!!.readText()
    private fun remote(body: SpaceCompassCelestialBody): SpaceCompassCelestialRemoteData {
        val name = body.name.lowercase(Locale.ROOT)
        return SpaceCompassCelestialRemoteData(
            ephemerides = mapOf(body to parseSpaceCompassHorizonsEphemeris(body, fixture("$name-pointing.txt"))),
            motions = mapOf(body to parseSpaceCompassHorizonsMotion(body, fixture("$name-motion.txt"))))
    }

    @Test fun additionsAreUniqueAndRetainTheCorrectCategoriesAndDefaults() {
        val bodies = listOf(SpaceCompassCelestialBody.CERES, SpaceCompassCelestialBody.ENCELADUS,
            SpaceCompassCelestialBody.VEGA, SpaceCompassCelestialBody.DENEB, SpaceCompassCelestialBody.ALTAIR, cross)
        assertEquals(1009, spaceCompassCelestialCatalogOrder.size)
        for (body in bodies) {
            assertEquals(1, spaceCompassCelestialCatalogOrder.count { it == body })
            assertNotNull(body.scientificDesignation)
            assertNotNull(body.scientificTypeResource)
            assertTrue(body.supportsDailyPath)
        }
        assertEquals(SpaceCompassCatalogType.DWARF_PLANET, bodies[0].catalogType)
        assertEquals(SpaceCompassCatalogType.NATURAL_SATELLITE, bodies[1].catalogType)
        assertEquals(SpaceCompassCatalogType.CONSTELLATION, cross.catalogType)
        assertEquals(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON), SpaceCompassCelestialSelection().selected)
    }

    @Test fun movingBodiesMatchIndependentJplTopocentricPointingAndRange() {
        val expected = mapOf(
            SpaceCompassCelestialBody.CERES to listOf(300.193835, 3.483363, 2.40991349262214),
            SpaceCompassCelestialBody.ENCELADUS to listOf(39.388144, -35.896650, 8.45778507903469))
        for ((body, value) in expected) {
            val actual = calculateSpaceCompassCelestialObservation(body, now, 45.0, 9.0, 100.0, remote(body))!!
            assertEquals(body.name, value[0], actual.position.azimuthDegrees, .02)
            assertEquals(body.name, value[1], actual.position.elevationDegrees, .02)
            assertEquals(body.name, value[2] * SPACE_COMPASS_AU_KM, actual.distanceKm, 10.0)
        }
    }

    @Test fun remotePathsRefreshAndNeverFabricateMissingOrOutOfWindowPositions() {
        for (body in listOf(SpaceCompassCelestialBody.CERES, SpaceCompassCelestialBody.ENCELADUS)) {
            val data = remote(body)
            val path = calculateSpaceCompassCelestialPath(body, LocalDate.parse("2026-10-16"), ZoneId.of("UTC"),
                now, 45.0, 9.0, 100.0, data)!!
            assertEquals(24, path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
            assertTrue(path.samples.maxOf { it.position.elevationDegrees } - path.samples.minOf { it.position.elevationDegrees } > 20)
            assertTrue(body in spaceCompassCelestialRefreshBodies(emptySet(), emptySet(), true))
            assertTrue(spaceCompassCelestialCatalogEarthDistanceKm(body, now, data)!! > 0)
            assertNull(calculateSpaceCompassCelestialObservation(body, now, 45.0, 9.0))
            val outside = data.ephemerides.getValue(body).samples.last().timeMs + 1
            assertNull(calculateSpaceCompassCelestialObservation(body, outside, 45.0, 9.0, 100.0, data))
            assertNull(spaceCompassCelestialCatalogEarthDistanceKm(body, outside, data))
        }
    }

    @Test fun enceladusSpeedIsRelativeToSaturnAndItsFaceUsesTheIauAxis() {
        val body = SpaceCompassCelestialBody.ENCELADUS
        val data = remote(body)
        assertTrue(calculateSpaceCompassCelestialSpeed(body, now, data)!! in 12.0..13.5)
        assertEquals(R.string.celestial_speed_orbit_saturn, spaceCompassCelestialSpeedLabel(body))
        val geometry = calculateSpaceCompassCelestialViewGeometry(body, now, -35.0, 151.0, 0.0, data)!!
        assertTrue(geometry.illuminatedFraction!! in 0.0..1.0)
        assertTrue(geometry.matrix().all { it.isFinite() })
        assertNull(calculateSpaceCompassCelestialViewGeometry(body, now, -35.0, 151.0))
        val axis = spaceCompassEnceladusAxis(spaceCompassAstronomyTime(946_728_000_000L))
        assertEquals(40.66 / 15, axis.ra, .000001)
        assertEquals(83.52, axis.dec, .000001)
        assertEquals(0.0, body.textureLongitudeOffset, 0.0)
    }

    @Test fun brightStarsWorkOfflineWithProperMotionAndReferenceDistances() {
        for (body in listOf(SpaceCompassCelestialBody.VEGA, SpaceCompassCelestialBody.DENEB, SpaceCompassCelestialBody.ALTAIR)) {
            assertFalse(body.usesHorizons)
            assertTrue(body.deepSkyReference!!.distanceLy > 0)
            assertTrue(spaceCompassCelestialFacts(body).massSolar!! > 0)
            assertTrue(body.usesDeepSkySymbol)
            val a = calculateSpaceCompassCelestialObservation(body, now, 45.0, 9.0)!!
            val b = calculateSpaceCompassCelestialObservation(body, now + 6 * 3600000, 45.0, 9.0)!!
            assertNotEquals(a.position, b.position)
            assertTrue(a.distanceKm > SPACE_COMPASS_LIGHT_YEAR_KM)
            assertTrue(formatSpaceCompassSelectedDistance(body, a.distanceKm, SpaceCompassNumericFormat.INTERNATIONAL, "mkm")!!.endsWith(" ly"))
        }
    }

    @Test fun southernCrossTracksFourRealStarsAndHasNoInventedSingleDistanceOrMass() {
        assertEquals(listOf("Acrux", "Mimosa", "Gacrux", "Imai"), spaceCompassSouthernCrossStars.map { it.name })
        assertEquals(4, spaceCompassSouthernCrossStars.map { it.reference.distanceLy }.distinct().size)
        for (hour in 0..23) {
            val time = now + hour * 3600000L
            val south = calculateSpaceCompassSouthernCross(time, -35.0, 151.0)
            val north = calculateSpaceCompassSouthernCross(time, 45.0, 9.0)
            assertTrue(south.all { it.elevationDegrees > 0 })
            assertTrue(north.all { it.elevationDegrees < 0 })
        }
        val observation = calculateSpaceCompassCelestialObservation(cross, now, -35.0, 151.0)!!
        assertTrue(observation.distanceKm.isNaN())
        assertEquals(spaceCompassSouthernCrossCentre(calculateSpaceCompassSouthernCross(now, -35.0, 151.0)), observation.position)
        assertNull(spaceCompassCelestialCatalogDistanceAu(cross, now))
        assertNull(spaceCompassCelestialCatalogEarthDistanceKm(cross, now))
        assertNull(spaceCompassCelestialFacts(cross).massKg)
        assertNull(spaceCompassCelestialFacts(cross).diameterKm)
        assertNull(calculateSpaceCompassCelestialSpeed(cross, now))
        assertNull(formatSpaceCompassSelectedDistance(cross, observation.distanceKm, SpaceCompassNumericFormat.INTERNATIONAL, "mkm"))
        assertTrue(spaceCompassSouthernCrossCoordinates.declinationDegrees in -62.0..-58.0)
        assertNotEquals(-90.0, spaceCompassSouthernCrossCoordinates.declinationDegrees, 1.0)
    }

    @Test fun constellationFilterAndDailyTrackUseThePatternCentreAndKeepMissingSortKeysLast() {
        assertEquals(listOf(cross), spaceCompassFilterCatalog(setOf(SpaceCompassCatalogType.CONSTELLATION),
            SpaceCompassCatalogVisibility.ALL, emptyMap()).filter { it.largeCatalogEntry == null })
        val path = calculateSpaceCompassCelestialPath(cross, LocalDate.parse("2026-10-16"), ZoneId.of("UTC"),
            now, -35.0, 151.0, 0.0, SpaceCompassCelestialRemoteData())!!
        assertEquals(24, path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
        assertTrue(path.samples.all { it.position.elevationDegrees > 0 })
        for (sort in listOf(SpaceCompassCatalogSort.EARTH_DISTANCE_ASC, SpaceCompassCatalogSort.EARTH_DISTANCE_DESC))
            assertEquals(listOf(SpaceCompassCelestialBody.SUN, cross), spaceCompassSortCatalog(
                listOf(cross, SpaceCompassCelestialBody.SUN), sort, emptyMap(), emptyMap(), Locale.ROOT,
                mapOf(SpaceCompassCelestialBody.SUN to SPACE_COMPASS_AU_KM)))
    }
}

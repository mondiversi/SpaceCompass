package me.mondiversi.spacecompass

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Frozen public JPL topocentric responses at a synthetic site, not private device GPS. */
class SpaceCompassExpandedCatalogTest {
    private val now = Instant.parse("2026-10-16T12:30:00Z").toEpochMilli()
    private val newBodies = listOf(SpaceCompassCelestialBody.NEW_HORIZONS, SpaceCompassCelestialBody.JWST,
        SpaceCompassCelestialBody.PARKER_SOLAR_PROBE, SpaceCompassCelestialBody.APOPHIS,
        SpaceCompassCelestialBody.ATLAS_3I, SpaceCompassCelestialBody.EUROPA_CLIPPER,
        SpaceCompassCelestialBody.JUICE, SpaceCompassCelestialBody.HUBBLE,
        SpaceCompassCelestialBody.TIANGONG, SpaceCompassCelestialBody.M87, SpaceCompassCelestialBody.PROXIMA_B)
    private fun fixture(name: String) = javaClass.classLoader!!.getResource("catalog-142/$name")!!.readText()
    private fun remote(body: SpaceCompassCelestialBody): SpaceCompassCelestialRemoteData {
        val name = body.name.lowercase(java.util.Locale.ROOT)
        return SpaceCompassCelestialRemoteData(
            ephemerides = mapOf(body to parseSpaceCompassHorizonsEphemeris(body, fixture("$name-pointing.txt"))),
            motions = mapOf(body to parseSpaceCompassHorizonsMotion(body, fixture("$name-motion.txt"))))
    }

    @Test fun elevenUniqueObjectsUseExistingTrackingAndCategoryBoundaries() {
        assertEquals(11, newBodies.distinct().size)
        for (body in newBodies) {
            assertEquals(1, spaceCompassCelestialCatalogOrder.count { it == body })
            assertTrue(body.supportsDailyPath)
            assertNotNull(body.scientificTypeResource)
            assertNotNull(body.scientificDesignation)
            assertNotNull(spaceCompassCelestialPathTint(body))
        }
        assertEquals(SpaceCompassCatalogType.ASTEROID, SpaceCompassCelestialBody.APOPHIS.catalogType)
        assertTrue(SpaceCompassCelestialBody.ATLAS_3I.isComet)
        assertFalse(SpaceCompassCelestialBody.JWST.isEarthSatellite)
        assertTrue(SpaceCompassCelestialBody.HUBBLE.isEarthSatellite)
        assertTrue(SpaceCompassCelestialBody.TIANGONG.isEarthSatellite)
    }

    @Test fun sevenMovingTargetsMatchIndependentJplTopocentricAzimuthAltitudeAndRange() {
        fun compare(body: SpaceCompassCelestialBody, az: Double, alt: Double, au: Double) {
            val observation = requireNotNull(calculateSpaceCompassCelestialObservation(body,now,45.0,9.0,100.0,remote(body)))
            assertEquals(body.name+" azimuth",az,observation.position.azimuthDegrees,.02)
            assertEquals(body.name+" elevation",alt,observation.position.elevationDegrees,.02)
            assertEquals(body.name+" distance",au*SPACE_COMPASS_AU_KM,observation.distanceKm,10.0)
        }
        compare(SpaceCompassCelestialBody.NEW_HORIZONS, 119.763360, 0.397564, 65.9126815990316)
        compare(SpaceCompassCelestialBody.JWST, 351.140183, -23.615565, 0.00972382924063)
        compare(SpaceCompassCelestialBody.PARKER_SOLAR_PROBE, 156.615195, 17.209457, 0.88155029807669)
        compare(SpaceCompassCelestialBody.APOPHIS, 168.762976, 27.163919, 1.48013261170303)
        compare(SpaceCompassCelestialBody.ATLAS_3I, 299.776656, -1.785776, 12.2094434086372)
        compare(SpaceCompassCelestialBody.EUROPA_CLIPPER, 149.010918, 10.977166, 0.35328093523867)
        compare(SpaceCompassCelestialBody.JUICE, 245.634243, 28.920405, 0.11628107226364)
    }

    @Test fun remoteObjectsHaveTrueDailySkyPathsAndNeverInventMissingOrExpiredPositions() {
        for (body in newBodies.filter { it.usesHorizons }) {
            val data = remote(body)
            assertTrue(calculateSpaceCompassCelestialSpeed(body, now, data)!! > 0)
            assertTrue(spaceCompassCelestialCatalogDistanceAu(body, now, data)!! > 0)
            val path = requireNotNull(calculateSpaceCompassCelestialPath(body, LocalDate.parse("2026-10-16"),
                ZoneId.of("UTC"),now,45.0,9.0,100.0,data))
            assertEquals(24,path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
            assertTrue(path.samples.maxOf { it.position.elevationDegrees } - path.samples.minOf { it.position.elevationDegrees } > 20)
            assertNull(calculateSpaceCompassCelestialObservation(body,now,45.0,9.0))
            val outside=data.ephemerides.getValue(body).samples.last().timeMs+1
            assertNull(calculateSpaceCompassCelestialObservation(body,outside,45.0,9.0,100.0,data))
            assertNull(calculateSpaceCompassCelestialSpeed(body,outside,data))
            assertTrue(spaceCompassCelestialRefreshBodies(emptySet(),emptySet(),true).contains(body))
        }
    }

    @Test fun wrongHorizonsIdentityOrCentreCannotPoisonAnObjectCache() {
        val body=SpaceCompassCelestialBody.APOPHIS
        assertThrows(IllegalArgumentException::class.java) {
            parseSpaceCompassHorizonsEphemeris(body,fixture("atlas_3i-pointing.txt")) }
        assertThrows(IllegalArgumentException::class.java) {
            parseSpaceCompassHorizonsEphemeris(body,fixture("apophis-motion.txt")) }
    }

    @Test fun apophisCloseEarthFlybyRetainsAccurateLocalPointing() {
        val time=Instant.parse("2029-04-13T21:30:00Z").toEpochMilli()
        val body=SpaceCompassCelestialBody.APOPHIS
        val data=SpaceCompassCelestialRemoteData(ephemerides=mapOf(body to
            parseSpaceCompassHorizonsEphemeris(body,fixture("apophis-close-pointing.txt"))))
        val expected=fixture("apophis-close-observer.txt").substringAfter("\$\$SOE")
            .substringBefore("\$\$EOE").trim().split(',').map { it.trim() }
        val result=requireNotNull(calculateSpaceCompassCelestialObservation(body,time,45.0,9.0,100.0,data))
        assertEquals(expected[3].toDouble(),result.position.azimuthDegrees,.02)
        assertEquals(expected[4].toDouble(),result.position.elevationDegrees,.02)
        assertEquals(expected[5].toDouble()*SPACE_COMPASS_AU_KM,result.distanceKm,10.0)
    }

    @Test fun earthSatellitesUseCheckedFreshSgp4ElementsForOneRevolutionAndLocalParallax() {
        for (body in listOf(SpaceCompassCelestialBody.HUBBLE,SpaceCompassCelestialBody.TIANGONG)) {
            val text=fixture(body.name.lowercase()+".tle")
            val orbit=SpaceCompassCatalogSatelliteOrbit.parse(body,text)
            val data=SpaceCompassCelestialRemoteData(satellites=mapOf(body to orbit))
            assertTrue(orbit.usable(now))
            assertTrue(orbit.periodMs in 80*60_000L..110*60_000L)
            assertTrue(calculateSpaceCompassCelestialSpeed(body,now,data)!! in 6.0..9.0)
            val a=requireNotNull(calculateSpaceCompassCelestialObservation(body,now,45.0,9.0,100.0,data))
            val b=requireNotNull(calculateSpaceCompassCelestialObservation(body,now,-30.0,130.0,100.0,data))
            assertTrue(kotlin.math.abs(a.position.elevationDegrees-b.position.elevationDegrees) > 1)
            assertNotNull(calculateSpaceCompassSatellitePath(body,LocalDate.parse("2026-10-16"),ZoneId.of("UTC"),now,45.0,9.0,100.0,orbit))
            assertNull(calculateSpaceCompassCelestialObservation(body,orbit.epochMs+SPACE_COMPASS_ISS_MAX_AGE_MS+1,45.0,9.0,100.0,data))
            assertThrows(IllegalArgumentException::class.java) {
                SpaceCompassCatalogSatelliteOrbit.parse(if (body==SpaceCompassCelestialBody.HUBBLE)
                    SpaceCompassCelestialBody.TIANGONG else SpaceCompassCelestialBody.HUBBLE,text) }
            assertThrows(IllegalArgumentException::class.java) { SpaceCompassCatalogSatelliteOrbit.parse(body,text.replace("U ", "X ")) }
        }
    }

    @Test fun satelliteFallbackUsesTheSameVerifiedIdentity() = runBlocking {
        for (body in listOf(SpaceCompassCelestialBody.HUBBLE,SpaceCompassCelestialBody.TIANGONG)) {
            val tle=fixture(body.name.lowercase()+".tle")
            val urls=mutableListOf<String>()
            val source=SpaceCompassCatalogSatelliteSources(body)
            val result=source.load(now) { url,_ ->
                urls+=url
                if (url.contains("celestrak")) throw SpaceCompassCelestialHttpException(503)
                "<pre><code>$tle</code></pre>"
            }
            assertEquals(tle.trim(),result.trim())
            assertEquals(2,urls.size)
            assertTrue(urls.last().endsWith("/"+body.catalogSatellite!!.norad))
        }
    }

    @Test fun distantDirectionsUseCatalogReferencesAndDoNotPretendToResolveTheExoplanet() {
        assertEquals(SpaceCompassCelestialBody.PROXIMA_CENTAURI.deepSkyReference,SpaceCompassCelestialBody.PROXIMA_B.deepSkyReference)
        for (body in listOf(SpaceCompassCelestialBody.M87,SpaceCompassCelestialBody.PROXIMA_B)) {
            val a=requireNotNull(calculateSpaceCompassCelestialObservation(body,now,45.0,9.0))
            val b=requireNotNull(calculateSpaceCompassCelestialObservation(body,now+6*3_600_000L,45.0,9.0))
            assertTrue(kotlin.math.abs(a.position.elevationDegrees-b.position.elevationDegrees)>5)
            assertTrue(a.distanceKm>SPACE_COMPASS_LIGHT_YEAR_KM)
            assertNull(calculateSpaceCompassCelestialSpeed(body,now))
            assertNull(spaceCompassAtmosphericPressure(body))
        }
        assertEquals(SpaceCompassCelestialGalaxy.M87,spaceCompassCelestialMembership(SpaceCompassCelestialBody.M87).galaxy)
        assertEquals(6.5e9,spaceCompassCelestialFacts(SpaceCompassCelestialBody.M87).massSolar!!,0.0)
        assertNull(spaceCompassCelestialFacts(SpaceCompassCelestialBody.PROXIMA_B).diameterKm)
        assertTrue(spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.PROXIMA_B).isEmpty())
    }
}

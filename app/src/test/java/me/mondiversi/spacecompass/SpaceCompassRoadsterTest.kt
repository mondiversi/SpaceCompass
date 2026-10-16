package me.mondiversi.spacecompass

import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassRoadsterTest {
    private val body = SpaceCompassCelestialBody.TESLA_ROADSTER
    private val now = Instant.parse("2026-10-16T12:00:00Z").toEpochMilli()
    private fun fixture(kind: String) = javaClass.getResource("/roadster/roadster-$kind.txt")!!.readText()
    private fun remote() = SpaceCompassCelestialRemoteData(
        ephemerides = mapOf(body to parseSpaceCompassHorizonsEphemeris(body, fixture("pointing"))),
        motions = mapOf(body to parseSpaceCompassHorizonsMotion(body, fixture("motion"))))

    @Test fun cataloguePreservesDefaultsAndRestoresTheNewStableIdentifier() {
        val defaults = SpaceCompassCelestialSelection()
        assertEquals(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON), defaults.selected)
        assertEquals(body, SpaceCompassCelestialBody.entries.last())
        val restored = restoreSpaceCompassCelestialSelection(setOf("SUN", "TESLA_ROADSTER"), "TESLA_ROADSTER")
        assertEquals(body, restored.active)
        assertEquals(1, spaceCompassCelestialCatalogOrder.count { it == body })
        assertEquals(spaceCompassCelestialCatalogOrder.indexOf(SpaceCompassCelestialBody.MOON) + 1,
            spaceCompassCelestialCatalogOrder.indexOf(body))
        assertEquals(spaceCompassCelestialCatalogOrder.indexOf(body) + 1,
            spaceCompassCelestialCatalogOrder.indexOf(SpaceCompassCelestialBody.MARS))
    }

    @Test fun roadsterIsAnArtificialSolarOrbiterRatherThanAnEarthSatelliteOrProbe() {
        assertTrue(body.isSpacecraft && body.usesHorizons && body.usesLiveDistance && body.supportsDailyPath)
        assertFalse(body.isEarthSatellite || body.isVoyager || body.isExtrasolar || body.hasPhysicalFace)
        assertEquals(SpaceCompassCatalogType.ARTIFICIAL_SATELLITE, body.catalogType)
        assertTrue(body in spaceCompassFilterCatalog(setOf(body.catalogType), SpaceCompassCatalogVisibility.ALL, emptyMap()))
        assertEquals(R.string.celestial_speed_orbit_sun, spaceCompassCelestialSpeedLabel(body))
        assertEquals(SpaceCompassCelestialBody.SUN, spaceCompassCelestialFacts(body).parent)
    }

    @Test fun searchFindsTheCarByNameStarmanAndInternationalDesignation() {
        for (query in listOf("Tesla Roadster", "Starman", "SpaceX Roadster", "2018-017A", "43205"))
            assertEquals(listOf(body), spaceCompassSearchCatalog(spaceCompassCelestialCatalogOrder, emptyMap(), query))
        assertEquals("2018-017A · Starman", body.scientificDesignation)
        assertEquals(R.string.celestial_roadster_type, body.scientificTypeResource)
    }

    @Test fun realJplStatesSupplyDistinctSolarAndObserverDistancesAndCoordinates() {
        val data = remote()
        assertEquals(73, data.ephemerides.getValue(body).samples.size)
        assertEquals(73, data.motions.getValue(body).samples.size)
        val solarAu = requireNotNull(spaceCompassCelestialCatalogDistanceAu(body, now, data))
        val observation = requireNotNull(calculateSpaceCompassCelestialObservation(body, now, 45.0, 9.0, 100.0, data))
        assertTrue(solarAu in 0.9..1.8)
        assertTrue(observation.distanceKm / SPACE_COMPASS_AU_KM in 0.01..3.0)
        assertNotEquals(solarAu, observation.distanceKm / SPACE_COMPASS_AU_KM, 0.001)
        assertTrue(observation.position.azimuthDegrees in 0.0..360.0)
        assertTrue(observation.position.elevationDegrees in -90.0..90.0)
        assertEquals(SpaceCompassCelestialCoordinateFrame.GEOCENTRIC_ICRF,
            calculateSpaceCompassCelestialCoordinates(body, now, data)!!.frame)
    }

    @Test fun orbitalSpeedUsesTheFullSolarVelocityInsteadOfVoyagersRadialRate() {
        val data = remote()
        val sample = data.motions.getValue(body).samples.first { it.timeMs == now }
        val expected = sqrt(sample.vx*sample.vx + sample.vy*sample.vy + sample.vz*sample.vz) * SPACE_COMPASS_AU_DAY_TO_KM_SECOND
        val radial = (sample.x*sample.vx + sample.y*sample.vy + sample.z*sample.vz) /
            sqrt(sample.x*sample.x + sample.y*sample.y + sample.z*sample.z) * SPACE_COMPASS_AU_DAY_TO_KM_SECOND
        assertEquals(expected, calculateSpaceCompassCelestialSpeed(body, now, data)!!, 1e-9)
        assertTrue(expected in 15.0..40.0)
        assertNotEquals(expected, radial, 1.0)
    }

    @Test fun dailySkyTrackHasHourlyPointsAndChronologicalKeyEvents() {
        val path = requireNotNull(calculateSpaceCompassCelestialPath(body, LocalDate.parse("2026-10-16"),
            ZoneId.of("Europe/Rome"), now, 45.0, 9.0, 100.0, remote()))
        assertEquals(body, path.body)
        assertEquals(24, path.markers.count { it.event == SpaceCompassSunPathEvent.HOUR })
        for (event in listOf(SpaceCompassSunPathEvent.CULMINATION, SpaceCompassSunPathEvent.MINIMUM,
            SpaceCompassSunPathEvent.SUNRISE, SpaceCompassSunPathEvent.SUNSET))
            assertTrue(path.markers.any { event in it.events })
        assertTrue(path.samples.zipWithNext().all { (a,b) -> b.timeMs > a.timeMs })
    }

    @Test fun automaticAndManualCatalogWarmupIncludeAnUncheckedRoadster() {
        assertTrue(body in spaceCompassCelestialRefreshBodies(emptySet(), emptySet(), true))
        assertFalse(body in spaceCompassCelestialRefreshBodies(emptySet(), emptySet(), false))
        assertEquals(listOf(SpaceCompassCelestialRequest(body, true), SpaceCompassCelestialRequest(body)),
            spaceCompassCelestialRefreshRequests(body, emptySet(), emptySet(), true, false, false))
    }

    @Test fun requestsFollowTheScenarioDateAndNeverSendObserverCoordinates() {
        val future = Instant.parse("2030-05-12T18:00:00Z").toEpochMilli()
        for (motion in listOf(false, true)) {
            val url = URLDecoder.decode(spaceCompassHorizonsUrl(body, spaceCompassCelestialRequestTime(body, now, future), motion), "UTF-8")
            assertTrue(url.contains("COMMAND='-143205'"))
            assertTrue(url.contains("START_TIME='2030-05-11'"))
            assertTrue(url.contains(if (motion) "CENTER='500@10'" else "CENTER='500@399'"))
            assertTrue(url.contains(if (motion) "VEC_CORR='NONE'" else "VEC_CORR='LT+S'"))
            assertFalse(url.contains("SITE_COORD"))
        }
    }

    @Test fun missingAndOutOfCoverageDataNeverInventPositionsOrDistances() {
        assertNull(calculateSpaceCompassCelestialObservation(body, now, 45.0, 9.0))
        assertNull(spaceCompassCelestialCatalogDistanceAu(body, now))
        assertNull(calculateSpaceCompassCelestialSpeed(body, now))
        val data = remote()
        for (time in listOf(data.ephemerides.getValue(body).samples.first().timeMs - 1,
            data.ephemerides.getValue(body).samples.last().timeMs + 1)) {
            assertNull(calculateSpaceCompassCelestialObservation(body, time, 45.0, 9.0, remote = data))
            assertNull(spaceCompassCelestialCatalogDistanceAu(body, time, data))
            assertNull(calculateSpaceCompassCelestialSpeed(body, time, data))
        }
    }

    @Test fun parserRejectsAnotherObjectAndSwappedPointingAndMotionFrames() {
        assertThrows(IllegalArgumentException::class.java) {
            parseSpaceCompassHorizonsEphemeris(body, fixture("pointing").replace("SpaceX Roadster (spacecraft) (-143205)", "Voyager 1 (spacecraft) (-31)"))
        }
        assertThrows(IllegalArgumentException::class.java) { parseSpaceCompassHorizonsEphemeris(body, fixture("motion")) }
        assertThrows(IllegalArgumentException::class.java) { parseSpaceCompassHorizonsMotion(body, fixture("pointing")) }
    }

    @Test fun referenceFactsDoNotInventSurfacePressureTemperatureOrCurrentAttitude() {
        val facts = spaceCompassCelestialFacts(body)
        assertEquals(1250.0, facts.massKg!!, 0.0)
        assertTrue(facts.massEstimated)
        assertNull(facts.diameterKm); assertNull(facts.gravity); assertNull(facts.rotationHours)
        assertTrue(spaceCompassCelestialTemperatures(body).isEmpty())
        assertNull(spaceCompassCelestialCoreTemperature(body))
        assertNull(spaceCompassAtmosphericPressure(body))
        assertNull(calculateSpaceCompassCelestialViewGeometry(body, now, 45.0, 9.0))
    }

    @Test fun authenticPhotographUsesTheVerifiedExternalPackAndNoSphericalProjection() {
        assertTrue(body.hasCatalogPhotograph)
        assertEquals("tesla_roadster.webp", body.viewerTexture)
        val image = requireNotNull(SpaceCompassCelestialTexturePolicy.texture(body.viewerTexture))
        assertEquals(157614, image.bytes)
        assertEquals("54c1a76dcb92b1512d9dcb1cd273af4771f7f347ff3abdd124383df934de19fe", image.sha256)
        assertEquals(2048, image.width); assertEquals(1024, image.height)
        assertTrue(image.url.endsWith("/${SpaceCompassCelestialTexturePolicy.PACK}/tesla_roadster.webp"))
        assertEquals("SpaceX · CC0 1.0", spaceCompassCatalogImageCredit(body.viewerTexture))
    }
}

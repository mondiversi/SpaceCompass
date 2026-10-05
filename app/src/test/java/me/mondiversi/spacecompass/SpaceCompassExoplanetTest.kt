package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SpaceCompassExoplanetTest {
    private val body = SpaceCompassCelestialBody.TRAPPIST_1_E
    private val now = Instant.parse("2026-10-05T12:00:00Z").toEpochMilli()
    @Test fun unresolvedPlanetHasOfflinePointingDailyPathAndLightYearDistances() {
        assertEquals(SpaceCompassCatalogType.EXOPLANET, body.catalogType)
        val reference = body.deepSkyReference!!
        assertTrue(reference.distanceLy in 40.0..41.0)
        for (latitude in listOf(-70.0, 0.0, 45.0, 89.0)) {
            val observation = calculateSpaceCompassCelestialObservation(body, now, latitude, 12.5)!!
            assertTrue(observation.position.azimuthDegrees in 0.0..360.0)
            assertTrue(observation.position.elevationDegrees in -90.0..90.0)
            assertEquals(reference.distanceLy, observation.distanceKm / SPACE_COMPASS_LIGHT_YEAR_KM, .0001)
            for (unit in listOf("default", "mkm", "mmi"))
                assertTrue(formatSpaceCompassSelectedDistance(body, observation.distanceKm, SpaceCompassNumericFormat.EUROPEAN, unit)!!.endsWith(" ly"))
        }
        assertNotNull(calculateSpaceCompassCelestialPath(body, LocalDate.parse("2026-10-05"), ZoneId.of("Europe/Rome"), now, 45.0, 12.5, 0.0, SpaceCompassCelestialRemoteData()))
        assertNull(calculateSpaceCompassCelestialSpeed(body, now))
        assertNull(body.viewerTexture)
        assertFalse(body.hasPhysicalFace)
        assertTrue(spaceCompassCelestialTemperatures(body).isEmpty())
        assertEquals(listOf(body), spaceCompassFilterCatalog(setOf(SpaceCompassCatalogType.EXOPLANET), SpaceCompassCatalogVisibility.ALL, emptyMap()))
    }
    @Test fun planetaryMassIsNotPresentedInSolarUnitsAndUnmeasuredRotationRemainsAbsent() {
        val facts = spaceCompassCelestialFacts(body)
        assertEquals(.692 * 5.9722e24, facts.massKg!!, 1e10)
        assertEquals(6.101013, facts.revolutionDays!!, 0.0)
        assertEquals("TRAPPIST-1", facts.parentName)
        assertTrue(facts.diameterKm!! in 11700.0..11800.0)
        assertTrue(facts.gravity!! in 7.9..8.1)
        assertFalse(spaceCompassUsesSolarMass(facts))
        assertTrue(formatSpaceCompassCelestialMass(body, facts, SpaceCompassNumericFormat.EUROPEAN).contains("kg"))
        assertNull(facts.rotationHours)
        assertTrue(SpaceCompassCelestialBody.PROXIMA_CENTAURI.deepSkyReference!!.distanceLy < body.deepSkyReference!!.distanceLy)
    }
}

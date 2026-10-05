package me.mondiversi.spacecompass

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassEarthCenterTest {
    private val body = SpaceCompassCelestialBody.EARTH_CENTER
    private val now = Instant.parse("2026-10-05T12:00:00Z").toEpochMilli()
    @Test fun centrePointsUndergroundAndRangeFollowsEarthShapeAndObserverAltitude() {
        val equator = calculateSpaceCompassCelestialObservation(body, now, 0.0, 0.0)!!
        val pole = calculateSpaceCompassCelestialObservation(body, now, 90.0, 0.0)!!
        assertEquals(6378.137, equator.distanceKm, .01)
        assertEquals(6356.752, pole.distanceKm, .01)
        assertEquals(1.0, calculateSpaceCompassCelestialObservation(body, now, 0.0, 0.0, 1000.0)!!.distanceKm - equator.distanceKm, 1e-6)
        for (latitude in listOf(-90.0, -45.0, 0.0, 45.0, 90.0)) {
            val first = calculateSpaceCompassCelestialObservation(body, now, latitude, 23.0)!!
            val later = calculateSpaceCompassCelestialObservation(body, now + 21600000, latitude, 23.0)!!
            assertTrue(first.position.azimuthDegrees in 0.0..360.0)
            assertTrue(first.position.elevationDegrees < -89.7)
            assertEquals(first.distanceKm, later.distanceKm, 1e-6)
            assertEquals(first.position.elevationDegrees, later.position.elevationDegrees, 1e-6)
        }
    }
    @Test fun noInventedDailyOrbitCoreTemperatureOrHeliocentricSpeedAndOnlyNormalRangeUnits() {
        assertFalse(body.supportsDailyPath)
        assertNull(calculateSpaceCompassCelestialSpeed(body, now))
        assertTrue(spaceCompassCelestialTemperatures(body).isEmpty())
        for (numeric in SpaceCompassNumericFormat.entries) {
            val metric = formatSpaceCompassSelectedDistance(body, 6378.137, numeric, "mkm", false)!!
            val imperial = formatSpaceCompassSelectedDistance(body, 6378.137, numeric, "mmi", true)!!
            assertTrue(metric.endsWith(" km")); assertTrue(imperial.endsWith(" mi"))
            assertFalse(metric.contains("AU") || metric.contains("Mkm"))
            assertFalse(imperial.contains("AU") || imperial.contains("Mmi"))
        }
        assertEquals(SpaceCompassCelestialFacts(), spaceCompassCelestialFacts(body))
    }
}

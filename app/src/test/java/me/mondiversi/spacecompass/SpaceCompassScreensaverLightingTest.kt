package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.abs

class SpaceCompassScreensaverLightingTest {
    @Test fun daylightAtDawnIsIndependentOfThePhoneClockAndTimeZone() {
        for (elevation in listOf(0.0, 3.0, 6.0, 15.0)) {
            val light = spaceCompassScreensaverLightHour(elevation, 0.0)
            assertTrue("Dawn must not be replaced by midnight", light > 6.0)
            for (clock in listOf(7.25, 7.5, 7.75, 12.0, 23.0))
                assertEquals(light, spaceCompassScreensaverLightHour(elevation, clock), 0.0)
        }
    }

    @Test fun theGlobeBrightensProgressivelyFromAstronomicalTwilightToDaylight() {
        val samples = (-180..80).map { spaceCompassScreensaverLightHour(it / 10.0, 12.0) }
        assertEquals(0.0, samples.first(), 0.0)
        assertEquals(12.0, samples.last(), 0.0)
        samples.zipWithNext().forEach { (a, b) -> assertTrue(b >= a) }
        assertTrue(spaceCompassScreensaverLightHour(-12.0, 12.0) <
            spaceCompassScreensaverLightHour(-2.0, 12.0))
    }

    @Test fun noAbruptIlluminationChangesAtTwilightAndDaylightBoundaries() {
        for (height in listOf(-18.0, -6.0, -.833, 2.0, 8.0))
            assertTrue(abs(spaceCompassScreensaverLightHour(height - .0001, 8.0) -
                spaceCompassScreensaverLightHour(height + .0001, 8.0)) < .001)
    }

    @Test fun missingOrInvalidSolarFixRetainsAValidLocalClock() {
        for (height in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
            assertEquals(7.75, spaceCompassScreensaverLightHour(height, 7.75), 0.0)
            assertEquals(23.75, spaceCompassScreensaverLightHour(height, -.25), 0.0)
            assertEquals(0.0, spaceCompassScreensaverLightHour(height, 24.0), 0.0)
            assertEquals(12.0, spaceCompassScreensaverLightHour(height, Double.NaN), 0.0)
        }
    }

    @Test fun simulatedDayAndNightOverrideTheActualDeviceClock() {
        assertEquals(0.0, spaceCompassScreensaverLightHour(-25.0, 12.0), 0.0)
        assertEquals(12.0, spaceCompassScreensaverLightHour(25.0, 0.0), 0.0)
    }

    @Test fun polarDayAndWinterTwilightFollowTheSameSolarCoordinatesAsTheMainSky() {
        val summer = calculateSpaceCompassSunPosition(Instant.parse("2026-06-21T23:00:00Z").toEpochMilli(), 78.0, 15.0)
        val winter = calculateSpaceCompassSunPosition(Instant.parse("2026-12-21T11:00:00Z").toEpochMilli(), 78.0, 15.0)
        assertTrue(summer.elevationDegrees > 8.0)
        assertTrue(winter.elevationDegrees < -6.0)
        assertEquals(12.0, spaceCompassScreensaverLightHour(summer.elevationDegrees, 0.0), 0.0)
        assertTrue(spaceCompassScreensaverLightHour(winter.elevationDegrees, 12.0) < 5.5)
    }
}

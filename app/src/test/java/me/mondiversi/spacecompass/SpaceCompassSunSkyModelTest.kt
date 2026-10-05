package me.mondiversi.spacecompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSunSkyModelTest {
    @Test fun sixSkyPhasesFollowSolarHeightAndDirectionNotTheTimezone() {
        assertEquals(SpaceCompassSunSkyPhase.NIGHT, spaceCompassSunSkyPhase(-20.0, false))
        assertEquals(SpaceCompassSunSkyPhase.NIGHT, spaceCompassSunSkyPhase(-13.0, true))
        assertEquals(SpaceCompassSunSkyPhase.DAWN, spaceCompassSunSkyPhase(-9.0, true))
        assertEquals(SpaceCompassSunSkyPhase.DAWN, spaceCompassSunSkyPhase(3.0, true))
        assertEquals(SpaceCompassSunSkyPhase.MORNING, spaceCompassSunSkyPhase(40.0, true))
        assertEquals(SpaceCompassSunSkyPhase.AFTERNOON, spaceCompassSunSkyPhase(40.0, false))
        assertEquals(SpaceCompassSunSkyPhase.SUNSET, spaceCompassSunSkyPhase(3.0, false))
        assertEquals(SpaceCompassSunSkyPhase.EVENING, spaceCompassSunSkyPhase(-10.0, false))
    }

    @Test fun unknownLocationDoesNotInventSunriseOrAfternoon() {
        assertEquals(SpaceCompassSunSkyPhase.NIGHT, spaceCompassSunSkyPhase(null, true))
        assertEquals(SpaceCompassSunSkyPhase.NIGHT, spaceCompassSunSkyPhase(Double.NaN, true))
    }

    @Test fun polarDayAndNightDoNotUseFixedClockHours() {
        assertEquals(SpaceCompassSunSkyPhase.MORNING, spaceCompassSunSkyPhase(12.0, true))
        assertEquals(SpaceCompassSunSkyPhase.AFTERNOON, spaceCompassSunSkyPhase(12.0, false))
        assertEquals(SpaceCompassSunSkyPhase.NIGHT, spaceCompassSunSkyPhase(-24.0, true))
    }

    @Test fun wmoCodesCoverPrecipitationFogSnowAndThunderstorms() {
        assertEquals(SpaceCompassSunWeatherKind.CLEAR, spaceCompassSunWeatherKind(0))
        assertEquals(SpaceCompassSunWeatherKind.PARTLY_CLOUDY, spaceCompassSunWeatherKind(2))
        assertEquals(SpaceCompassSunWeatherKind.CLOUDY, spaceCompassSunWeatherKind(3))
        for (code in listOf(45, 48)) assertEquals(SpaceCompassSunWeatherKind.FOG, spaceCompassSunWeatherKind(code))
        for (code in listOf(51, 53, 55, 56, 57)) assertEquals(SpaceCompassSunWeatherKind.DRIZZLE, spaceCompassSunWeatherKind(code))
        for (code in listOf(61, 63, 65, 66, 67, 80, 81, 82)) assertEquals(SpaceCompassSunWeatherKind.RAIN, spaceCompassSunWeatherKind(code))
        for (code in listOf(71, 73, 75, 77, 85, 86)) assertEquals(SpaceCompassSunWeatherKind.SNOW, spaceCompassSunWeatherKind(code))
        for (code in listOf(95, 96, 97, 99)) assertEquals(SpaceCompassSunWeatherKind.STORM, spaceCompassSunWeatherKind(code))
        assertNull(spaceCompassSunWeatherKind(-1)); assertNull(spaceCompassSunWeatherKind(100))
    }

    @Test fun requestsRoundLocationAndNeverIncludeAltitudeOrPhoneIdentity() {
        val tile = spaceCompassSunWeatherTile(45.123456789, 9.987654321)
        assertEquals(45.12, tile.latitude, 1e-9)
        assertEquals(9.98, tile.longitude, 1e-9)
        assertTrue(tile.url().startsWith("https://api.open-meteo.com/"))
        assertTrue(tile.url().contains("latitude=45.12&longitude=9.98"))
        assertFalse(tile.url().contains("123456")); assertFalse(tile.url().contains("altitude"))
        assertFalse(tile.url().contains("sensor")); assertFalse(tile.url().contains("device"))
    }

    @Test fun urlFormattingIsIndependentOfDecimalComma() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALIAN)
            assertTrue(spaceCompassSunWeatherTile(-33.123456, -70.123456).url().contains("latitude=-33.12&longitude=-70.12"))
        } finally { Locale.setDefault(original) }
    }

    @Test fun gridHandlesPolesDateLineAndRejectsInvalidLocations() {
        assertEquals(90.0, spaceCompassSunWeatherTile(90.0, 180.0).latitude, 0.0)
        assertEquals(-180.0, spaceCompassSunWeatherTile(-90.0, -180.0).longitude, 0.0)
        for ((lat, lon) in listOf(Double.NaN to 0.0, 91.0 to 0.0, 0.0 to 181.0)) {
            assertThrows(IllegalArgumentException::class.java) { spaceCompassSunWeatherTile(lat, lon) }
        }
    }

    @Test fun weatherValidityRejectsStaleFutureAndMissingTimes() {
        val now = 1_800_000_000_000L
        assertTrue(spaceCompassSunWeatherTimeUsable(now, now))
        assertTrue(spaceCompassSunWeatherTimeUsable(now - 15 * 60_000, now))
        assertFalse(spaceCompassSunWeatherTimeUsable(now - 91 * 60_000, now))
        assertFalse(spaceCompassSunWeatherTimeUsable(now + 31 * 60_000, now))
        assertFalse(spaceCompassSunWeatherTimeUsable(0, now))
        assertEquals(15 * 60_000L, SPACE_COMPASS_SUN_WEATHER_REFRESH_MS)
        assertEquals(5 * 60_000L, SPACE_COMPASS_SUN_WEATHER_RETRY_MS)
    }

    @Test fun compassHasEightMarksClockwiseAndNoDuplicatedNorth() {
        assertEquals(listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"), SpaceCompassSunCompassLabels)
        assertEquals(8, SpaceCompassSunCompassLabels.toSet().size)
    }
}

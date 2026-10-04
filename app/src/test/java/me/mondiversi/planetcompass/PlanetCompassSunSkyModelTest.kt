package me.mondiversi.planetcompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassSunSkyModelTest {
    @Test fun sixSkyPhasesFollowSolarHeightAndDirectionNotTheTimezone() {
        assertEquals(PlanetCompassSunSkyPhase.NIGHT, planetCompassSunSkyPhase(-20.0, false))
        assertEquals(PlanetCompassSunSkyPhase.NIGHT, planetCompassSunSkyPhase(-13.0, true))
        assertEquals(PlanetCompassSunSkyPhase.DAWN, planetCompassSunSkyPhase(-9.0, true))
        assertEquals(PlanetCompassSunSkyPhase.DAWN, planetCompassSunSkyPhase(3.0, true))
        assertEquals(PlanetCompassSunSkyPhase.MORNING, planetCompassSunSkyPhase(40.0, true))
        assertEquals(PlanetCompassSunSkyPhase.AFTERNOON, planetCompassSunSkyPhase(40.0, false))
        assertEquals(PlanetCompassSunSkyPhase.SUNSET, planetCompassSunSkyPhase(3.0, false))
        assertEquals(PlanetCompassSunSkyPhase.EVENING, planetCompassSunSkyPhase(-10.0, false))
    }

    @Test fun unknownLocationDoesNotInventSunriseOrAfternoon() {
        assertEquals(PlanetCompassSunSkyPhase.NIGHT, planetCompassSunSkyPhase(null, true))
        assertEquals(PlanetCompassSunSkyPhase.NIGHT, planetCompassSunSkyPhase(Double.NaN, true))
    }

    @Test fun polarDayAndNightDoNotUseFixedClockHours() {
        assertEquals(PlanetCompassSunSkyPhase.MORNING, planetCompassSunSkyPhase(12.0, true))
        assertEquals(PlanetCompassSunSkyPhase.AFTERNOON, planetCompassSunSkyPhase(12.0, false))
        assertEquals(PlanetCompassSunSkyPhase.NIGHT, planetCompassSunSkyPhase(-24.0, true))
    }

    @Test fun wmoCodesCoverPrecipitationFogSnowAndThunderstorms() {
        assertEquals(PlanetCompassSunWeatherKind.CLEAR, planetCompassSunWeatherKind(0))
        assertEquals(PlanetCompassSunWeatherKind.PARTLY_CLOUDY, planetCompassSunWeatherKind(2))
        assertEquals(PlanetCompassSunWeatherKind.CLOUDY, planetCompassSunWeatherKind(3))
        for (code in listOf(45, 48)) assertEquals(PlanetCompassSunWeatherKind.FOG, planetCompassSunWeatherKind(code))
        for (code in listOf(51, 53, 55, 56, 57)) assertEquals(PlanetCompassSunWeatherKind.DRIZZLE, planetCompassSunWeatherKind(code))
        for (code in listOf(61, 63, 65, 66, 67, 80, 81, 82)) assertEquals(PlanetCompassSunWeatherKind.RAIN, planetCompassSunWeatherKind(code))
        for (code in listOf(71, 73, 75, 77, 85, 86)) assertEquals(PlanetCompassSunWeatherKind.SNOW, planetCompassSunWeatherKind(code))
        for (code in listOf(95, 96, 97, 99)) assertEquals(PlanetCompassSunWeatherKind.STORM, planetCompassSunWeatherKind(code))
        assertNull(planetCompassSunWeatherKind(-1)); assertNull(planetCompassSunWeatherKind(100))
    }

    @Test fun requestsRoundLocationAndNeverIncludeAltitudeOrPhoneIdentity() {
        val tile = planetCompassSunWeatherTile(45.123456789, 9.987654321)
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
            assertTrue(planetCompassSunWeatherTile(-33.123456, -70.123456).url().contains("latitude=-33.12&longitude=-70.12"))
        } finally { Locale.setDefault(original) }
    }

    @Test fun gridHandlesPolesDateLineAndRejectsInvalidLocations() {
        assertEquals(90.0, planetCompassSunWeatherTile(90.0, 180.0).latitude, 0.0)
        assertEquals(-180.0, planetCompassSunWeatherTile(-90.0, -180.0).longitude, 0.0)
        for ((lat, lon) in listOf(Double.NaN to 0.0, 91.0 to 0.0, 0.0 to 181.0)) {
            assertThrows(IllegalArgumentException::class.java) { planetCompassSunWeatherTile(lat, lon) }
        }
    }

    @Test fun weatherValidityRejectsStaleFutureAndMissingTimes() {
        val now = 1_800_000_000_000L
        assertTrue(planetCompassSunWeatherTimeUsable(now, now))
        assertTrue(planetCompassSunWeatherTimeUsable(now - 15 * 60_000, now))
        assertFalse(planetCompassSunWeatherTimeUsable(now - 91 * 60_000, now))
        assertFalse(planetCompassSunWeatherTimeUsable(now + 31 * 60_000, now))
        assertFalse(planetCompassSunWeatherTimeUsable(0, now))
        assertEquals(15 * 60_000L, PLANET_COMPASS_SUN_WEATHER_REFRESH_MS)
        assertEquals(5 * 60_000L, PLANET_COMPASS_SUN_WEATHER_RETRY_MS)
    }

    @Test fun compassHasEightMarksClockwiseAndNoDuplicatedNorth() {
        assertEquals(listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW"), PlanetCompassSunCompassLabels)
        assertEquals(8, PlanetCompassSunCompassLabels.toSet().size)
    }
}

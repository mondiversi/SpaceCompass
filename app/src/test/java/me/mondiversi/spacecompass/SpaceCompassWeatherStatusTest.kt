package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassWeatherStatusTest {
    private val unavailable = SpaceCompassSunWeatherReading(failed = true)
    @Test fun failedEnabledWeatherOffersAnActionInsteadOfPretendingToLoad() {
        assertTrue(spaceCompassWeatherFailureVisible(unavailable, true, false, false))
        assertFalse(spaceCompassWeatherFailureVisible(unavailable.copy(loading = true), true, false, false))
        assertFalse(spaceCompassWeatherFailureVisible(SpaceCompassSunWeatherReading(), true, false, false))
    }
    @Test fun inactiveWeatherAndTheCameraDoNotHideCompassWarningsWithIrrelevantErrors() {
        assertFalse(spaceCompassWeatherFailureVisible(unavailable, false, false, false))
        assertFalse(spaceCompassWeatherFailureVisible(unavailable, true, true, false))
    }
    @Test fun locationAndOrbitalRetriesKeepTheirPriority() {
        assertFalse(spaceCompassWeatherFailureVisible(unavailable, true, false, true))
    }

    @Test fun positionDetailsDistinguishAnActualOrQueuedRequestFromMissingWeather() {
        assertEquals(R.string.pc_loading, spaceCompassWeatherValueResource(null, loading = true))
        assertEquals(R.string.sun_weather_unavailable, spaceCompassWeatherValueResource(null, loading = false))
    }

    @Test fun availableWeatherKeepsItsDescriptionWhileARefreshRuns() {
        val expected = mapOf(
            SpaceCompassSunWeatherKind.CLEAR to R.string.sun_weather_clear,
            SpaceCompassSunWeatherKind.MAINLY_CLEAR to R.string.celestial_weather_mainly_clear,
            SpaceCompassSunWeatherKind.PARTLY_CLOUDY to R.string.sun_weather_partial,
            SpaceCompassSunWeatherKind.CLOUDY to R.string.sun_weather_cloudy,
            SpaceCompassSunWeatherKind.FOG to R.string.sun_weather_fog,
            SpaceCompassSunWeatherKind.DRIZZLE to R.string.sun_weather_drizzle,
            SpaceCompassSunWeatherKind.RAIN to R.string.sun_weather_rain,
            SpaceCompassSunWeatherKind.SNOW to R.string.sun_weather_snow,
            SpaceCompassSunWeatherKind.STORM to R.string.sun_weather_storm)
        for ((kind, resource) in expected) for (loading in listOf(false, true)) {
            val snapshot = SpaceCompassSunWeatherSnapshot(kind, 0f, 1L)
            assertEquals(resource, spaceCompassWeatherValueResource(snapshot, loading))
        }
    }
}

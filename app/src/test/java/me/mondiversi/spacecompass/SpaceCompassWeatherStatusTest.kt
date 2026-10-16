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
}

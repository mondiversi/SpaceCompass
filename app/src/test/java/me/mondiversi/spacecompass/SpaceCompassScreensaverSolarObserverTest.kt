package me.mondiversi.spacecompass

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScreensaverSolarObserverTest {
    private val noon = Instant.parse("2026-10-11T10:00:00Z").toEpochMilli()
    private val midnight = Instant.parse("2026-10-11T22:00:00Z").toEpochMilli()

    @Test fun lastKnownPositionStillChangesFromDayToNightWithoutNewGpsReadings() {
        val observer = SpaceCompassScreensaverSolarObserver(45.31, 8.98, 70.0)
        assertTrue(observer.elevation(noon) > 20)
        assertTrue(observer.elevation(midnight) < -20)
    }

    @Test fun aFixedScenarioDateKeepsItsChosenLightingWithoutLiveEphemerisWork() {
        val observer = SpaceCompassScreensaverSolarObserver(45.31, 8.98, 70.0, noon)
        assertEquals(observer.elevation(noon), observer.elevation(midnight), .000001)
    }
}

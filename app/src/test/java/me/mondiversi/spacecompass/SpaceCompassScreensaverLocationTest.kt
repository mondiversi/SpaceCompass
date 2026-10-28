package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SpaceCompassScreensaverLocationTest {
    @Test fun noInventedPositionBeforeTheFirstKnownFix() {
        assertNull(spaceCompassScreensaverLocation(null, null))
        assertNull(spaceCompassScreensaverLocation(SpaceCompassScreensaverSolarObserver(Double.NaN, 0.0), null))
    }

    @Test fun pausedOrInvalidReadingsRetainTheLastUsablePosition() {
        val previous = SpaceCompassScreensaverLocation(45.31, 8.98)
        for (observer in listOf(null, SpaceCompassScreensaverSolarObserver(91.0, 0.0),
            SpaceCompassScreensaverSolarObserver(0.0, -181.0),
            SpaceCompassScreensaverSolarObserver(Double.POSITIVE_INFINITY, 0.0),
            SpaceCompassScreensaverSolarObserver(0.0, Double.NaN)))
            assertSame(previous, spaceCompassScreensaverLocation(observer, previous))
    }

    @Test fun aFreshRealGpsFixReplacesThePreviousFix() {
        val previous = SpaceCompassScreensaverLocation(45.31, 8.98)
        val device = SpaceCompassScreensaverSolarObserver(28.5, -14.0)
        assertEquals(SpaceCompassScreensaverLocation(28.5, -14.0),
            spaceCompassScreensaverLocation(device, previous))
        assertEquals(previous, spaceCompassScreensaverLocation(
            SpaceCompassScreensaverSolarObserver(45.31, 8.98),
            spaceCompassScreensaverLocation(device, previous)))
    }

    @Test fun serializationIsNumericEvenWithADecimalCommaLocale() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALIAN)
            assertEquals("[45.31,8.98]", SpaceCompassScreensaverLocation(45.31, 8.98).javascript)
            for (latitude in listOf(-90.0, 0.0, 90.0)) for (longitude in listOf(-180.0, 0.0, 180.0))
                assertNotNull(spaceCompassScreensaverLocation(SpaceCompassScreensaverSolarObserver(latitude, longitude), null))
        } finally { Locale.setDefault(original) }
    }
}

package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScreensaverBatteryTest {
    @Test fun emptyAndFullBatteriesRemainDistinctFromMissingData() {
        assertEquals(0, spaceCompassScreensaverBatteryPercent(0, 100))
        assertEquals(100, spaceCompassScreensaverBatteryPercent(100, 100))
        assertNull(spaceCompassScreensaverBatteryPercent(-1, 100))
    }
    @Test fun devicesWithDifferentReportingScalesHaveTheSamePercentage() {
        assertEquals(50, spaceCompassScreensaverBatteryPercent(50, 100))
        assertEquals(50, spaceCompassScreensaverBatteryPercent(100, 200))
        assertEquals(25, spaceCompassScreensaverBatteryPercent(1, 4))
        assertEquals(67, spaceCompassScreensaverBatteryPercent(2, 3))
    }
    @Test fun invalidAndUnavailableReadingsDoNotFabricateACharge() {
        for (scale in listOf(-1, 0)) assertNull(spaceCompassScreensaverBatteryPercent(0, scale))
        assertNull(spaceCompassScreensaverBatteryPercent(-1, -1))
        assertNull(spaceCompassScreensaverBatteryPercent(101, 100))
    }
    @Test fun largeValidBatteryScalesCannotOverflowTheConversion() {
        assertEquals(100, spaceCompassScreensaverBatteryPercent(Int.MAX_VALUE, Int.MAX_VALUE))
        assertEquals(50, spaceCompassScreensaverBatteryPercent(Int.MAX_VALUE / 2, Int.MAX_VALUE))
    }
}

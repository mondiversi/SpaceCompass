package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSkyFluidityTest {
    @Test fun missingOrInvalidChoiceUsesBalancedFluidity() {
        for (value in listOf(null, "", "unknown", "HIGH"))
            assertEquals(SpaceCompassSkyFluidity.BALANCED, SpaceCompassSkyFluidity.fromStored(value))
        assertEquals(33_333, SpaceCompassSkyFluidity.BALANCED.sensorPeriodUs)
        assertEquals(30_000_000L, SpaceCompassSkyFluidity.BALANCED.minimumUpdateIntervalNs)
    }

    @Test fun savedModesRestoreThroughTheSharedPresentationSettings() {
        val saved = mutableMapOf<String, String>()
        for (mode in SpaceCompassSkyFluidity.entries) {
            saved[SPACE_COMPASS_SKY_FLUIDITY_KEY] = mode.stored
            val restored = spaceCompassReadPresentationSettings(saved::get)
            assertEquals(mode, SpaceCompassSkyFluidity.fromStored(restored[SPACE_COMPASS_SKY_FLUIDITY_KEY]))
        }
    }

    @Test fun lowerModesReduceSamplingAndCoalesceInterleavedEvents() {
        val modes = SpaceCompassSkyFluidity.entries
        assertEquals(modes.size, modes.map { it.stored }.toSet().size)
        assertTrue(modes.zipWithNext().all { (a, b) ->
            a.sensorPeriodUs > b.sensorPeriodUs && a.minimumUpdateIntervalNs > b.minimumUpdateIntervalNs })
        for (mode in modes) {
            assertTrue(mode.sensorPeriodUs > 0)
            assertTrue(mode.minimumUpdateIntervalNs in mode.sensorPeriodUs * 800L..mode.sensorPeriodUs * 1000L)
        }
    }
}

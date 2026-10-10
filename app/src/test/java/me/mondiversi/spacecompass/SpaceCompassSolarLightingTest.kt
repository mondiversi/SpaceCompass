package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class SpaceCompassSolarLightingTest {
    private fun colors(h: Double?, phase: SpaceCompassSunSkyPhase = SpaceCompassSunSkyPhase.SUNSET): List<Int> {
        val lighting = spaceCompassSolarLighting(h)
        val sky = spaceCompassSunSkyPalette(phase, lighting)
        val ground = spaceCompassSunGroundPalette(phase, lighting)
        val weather = spaceCompassWeatherPalette(phase, false, lighting)
        return listOf(sky.topArgb, sky.horizonArgb, ground.farArgb, ground.nearArgb, ground.hazeArgb,
            weather.overcastTopArgb, weather.overcastBottomArgb, weather.cloudTopArgb,
            weather.cloudBottomArgb, weather.fogTopArgb, weather.fogBottomArgb, weather.rainArgb, weather.snowArgb)
    }
    private fun brightness(c: Int) = ((c ushr 16) and 255) * .2126 + ((c ushr 8) and 255) * .7152 + (c and 255) * .0722

    @Test fun missingAndInvalidSunSamplesUseNightWithoutInventingAFirstGlow() {
        for (height in listOf(null, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, -90.0))
            assertEquals(colors(-18.0), colors(height))
    }

    @Test fun nauticalDawnIsFaintAndSixDegreesAboveTheHorizonIsMostlyDaylight() {
        val early = colors(-12.0)
        val horizon = colors(0.0)
        for (index in listOf(1, 3, 4, 7, 8))
            assertTrue("Early dawn surface $index must remain subdued", brightness(early[index]) < brightness(horizon[index]) * .6)
        val above = spaceCompassSolarLighting(6.0)
        assertEquals(SpaceCompassSolarLightBand.DAY, above.to)
        assertTrue(above.mix > .7f)
    }

    @Test fun everySurfaceRemainsContinuousAtOldAndNewPhaseBoundaries() {
        for (boundary in listOf(-18.0, -12.0, -6.0, -.833, 0.0, 2.0, 6.0, 8.0)) {
            val before = colors(boundary - .0001)
            val after = colors(boundary + .0001)
            for (i in before.indices) for (shift in 0..24 step 8)
                assertTrue("Boundary $boundary surface $i channel $shift",
                    abs(((before[i] ushr shift) and 255) - ((after[i] ushr shift) and 255)) <= 1)
        }
    }

    @Test fun skyGroundAndAtmosphereDimProgressivelyAsTheSunDescends() {
        val samples = listOf(20.0, 8.0, 6.0, 2.0, -.833, -2.0, -6.0, -9.0, -12.0, -18.0, -25.0).map { colors(it) }
        samples.zipWithNext().forEach { (brighter, darker) ->
            for (i in brighter.indices) assertTrue("Surface $i must not brighten at dusk",
                brightness(brighter[i]) + 1 >= brightness(darker[i]))
        }
    }

    @Test fun theSameSunHeightHasTheSameColorsAcrossNoonAndDiscretePhaseChanges() {
        for (height in listOf(-15.0, -6.0, -1.0, 0.0, 3.0, 6.0, 10.0, 60.0))
            for (phase in SpaceCompassSunSkyPhase.entries) assertEquals(colors(height), colors(height, phase))
    }

    @Test fun continuousLightingPreservesPrecipitationOpacityAndDarkStormClouds() {
        for (height in listOf(-25.0, -12.0, -4.0, 0.0, 4.0, 10.0)) {
            val lighting = spaceCompassSolarLighting(height)
            val weather = spaceCompassWeatherPalette(SpaceCompassSunSkyPhase.SUNSET, false, lighting)
            val storm = spaceCompassWeatherPalette(SpaceCompassSunSkyPhase.SUNSET, true, lighting)
            assertEquals(0x4D, weather.rainArgb ushr 24)
            assertEquals(0xA6, weather.snowArgb ushr 24)
            assertTrue(brightness(storm.cloudTopArgb) < brightness(weather.cloudTopArgb))
            assertTrue(brightness(storm.cloudBottomArgb) < brightness(weather.cloudBottomArgb))
            assertEquals(weather.fogTopArgb, storm.fogTopArgb)
        }
    }
}

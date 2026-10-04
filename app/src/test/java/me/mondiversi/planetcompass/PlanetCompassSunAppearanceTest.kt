package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test

class PlanetCompassSunAppearanceTest {
    private fun channel(argb: Int, shift: Int) = (argb ushr shift) and 0xFF

    @Test fun lowSunIsWarmAndHighSunGraduallyBecomesBrightCream() {
        var green = 0; var blue = 0
        for (height in 0..90) {
            val appearance = planetCompassSunAppearance(height.toDouble(), true)
            assertFalse(appearance.belowHorizon)
            assertEquals(255, channel(appearance.edgeArgb, 16))
            assertTrue(channel(appearance.edgeArgb, 8) >= green)
            assertTrue(channel(appearance.edgeArgb, 0) >= blue)
            green = channel(appearance.edgeArgb, 8); blue = channel(appearance.edgeArgb, 0)
        }
        assertTrue(green > 240 && blue > 220)
    }

    @Test fun sunriseAndSunsetHaveDistinctWarmHorizonPalettesButTheSameHighSun() {
        assertNotEquals(planetCompassSunAppearance(0.0, true).edgeArgb, planetCompassSunAppearance(0.0, false).edgeArgb)
        assertEquals(planetCompassSunAppearance(40.0, true), planetCompassSunAppearance(40.0, false))
    }

    @Test fun auraStartsGraduallyAboveSixDegreesAndIsBoundedAtHighElevation() {
        assertEquals(0f, planetCompassSunAppearance(6.0, true).glowStrength, 0f)
        assertTrue(planetCompassSunAppearance(7.0, true).glowStrength < 0.01f)
        var previous = 0f
        for (height in 0..90) {
            val appearance = planetCompassSunAppearance(height.toDouble(), true)
            assertTrue(appearance.glowStrength >= previous && appearance.glowStrength in 0f..0.48f)
            assertTrue(appearance.glowRadiusDp in 30f..64f)
            previous = appearance.glowStrength
        }
        assertEquals(0.48f, planetCompassSunAppearance(60.0, false).glowStrength, 1e-6f)
    }

    @Test fun belowHorizonIsARecognizableVioletLocatorWithoutDaylightAura() {
        for (height in listOf(-0.001, -15.0, -90.0)) {
            val appearance = planetCompassSunAppearance(height, true)
            assertTrue(appearance.belowHorizon)
            assertEquals(0f, appearance.glowStrength, 0f)
            assertTrue(channel(appearance.edgeArgb, 0) > channel(appearance.edgeArgb, 16))
            assertNotEquals(planetCompassSunAppearance(10.0, true).edgeArgb, appearance.edgeArgb)
        }
    }

    @Test fun daytimeColourAndAuraHaveNoAbruptJumpsAtPaletteBoundaries() {
        for (height in listOf(6.0, 20.0, 40.0, 55.0)) {
            val before = planetCompassSunAppearance(height - 0.0001, true)
            val after = planetCompassSunAppearance(height + 0.0001, true)
            for (shift in listOf(16, 8, 0)) {
                assertTrue(kotlin.math.abs(channel(before.edgeArgb, shift) - channel(after.edgeArgb, shift)) <= 1)
            }
            assertEquals(before.glowStrength, after.glowStrength, 1e-5f)
        }
        assertThrows(IllegalArgumentException::class.java) { planetCompassSunAppearance(Double.NaN, true) }
        assertEquals(planetCompassSunAppearance(90.0, false), planetCompassSunAppearance(100.0, false))
    }
}

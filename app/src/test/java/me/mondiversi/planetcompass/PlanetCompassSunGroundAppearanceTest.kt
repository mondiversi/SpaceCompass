package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test

class PlanetCompassSunGroundAppearanceTest {
    private fun brightness(argb: Int): Double =
        ((argb ushr 16) and 255) * 0.2126 + ((argb ushr 8) and 255) * 0.7152 + (argb and 255) * 0.0722

    @Test fun everySolarPhaseHasItsOwnOpaqueGroundAndHorizonPalette() {
        val palettes = PlanetCompassSunSkyPhase.entries.map(::planetCompassSunGroundPalette)
        assertEquals(6, palettes.distinct().size)
        for (palette in palettes) for (color in listOf(palette.farArgb, palette.nearArgb, palette.hazeArgb))
            assertEquals(255, color ushr 24)
    }

    @Test fun nightAndEveningDarkenTheWholeGroundIncludingTheHorizonHaze() {
        val night = planetCompassSunGroundPalette(PlanetCompassSunSkyPhase.NIGHT)
        val evening = planetCompassSunGroundPalette(PlanetCompassSunSkyPhase.EVENING)
        for (phase in listOf(PlanetCompassSunSkyPhase.DAWN, PlanetCompassSunSkyPhase.MORNING,
            PlanetCompassSunSkyPhase.AFTERNOON, PlanetCompassSunSkyPhase.SUNSET)) {
            val day = planetCompassSunGroundPalette(phase)
            for ((dark, dusk, light) in listOf(Triple(night.farArgb, evening.farArgb, day.farArgb),
                Triple(night.nearArgb, evening.nearArgb, day.nearArgb),
                Triple(night.hazeArgb, evening.hazeArgb, day.hazeArgb))) {
                assertTrue(brightness(dark) < brightness(dusk))
                assertTrue(brightness(dusk) < brightness(light))
            }
        }
    }

    @Test fun sunriseAndSunsetTintTheHazeWarmerThanDaylight() {
        for (phase in listOf(PlanetCompassSunSkyPhase.DAWN, PlanetCompassSunSkyPhase.SUNSET)) {
            val haze = planetCompassSunGroundPalette(phase).hazeArgb
            assertTrue(((haze ushr 16) and 255) > ((haze ushr 8) and 255))
            assertTrue(((haze ushr 8) and 255) > (haze and 255))
        }
        assertTrue(brightness(planetCompassSunGroundPalette(PlanetCompassSunSkyPhase.MORNING).nearArgb) >
            brightness(planetCompassSunGroundPalette(PlanetCompassSunSkyPhase.AFTERNOON).nearArgb))
    }
}

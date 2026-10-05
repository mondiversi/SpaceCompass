package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSunGroundAppearanceTest {
    private fun brightness(argb: Int): Double =
        ((argb ushr 16) and 255) * 0.2126 + ((argb ushr 8) and 255) * 0.7152 + (argb and 255) * 0.0722

    @Test fun everySolarPhaseHasItsOwnOpaqueGroundAndHorizonPalette() {
        val palettes = SpaceCompassSunSkyPhase.entries.map(::spaceCompassSunGroundPalette)
        assertEquals(6, palettes.distinct().size)
        for (palette in palettes) for (color in listOf(palette.farArgb, palette.nearArgb, palette.hazeArgb))
            assertEquals(255, color ushr 24)
    }

    @Test fun nightAndEveningDarkenTheWholeGroundIncludingTheHorizonHaze() {
        val night = spaceCompassSunGroundPalette(SpaceCompassSunSkyPhase.NIGHT)
        val evening = spaceCompassSunGroundPalette(SpaceCompassSunSkyPhase.EVENING)
        for (phase in listOf(SpaceCompassSunSkyPhase.DAWN, SpaceCompassSunSkyPhase.MORNING,
            SpaceCompassSunSkyPhase.AFTERNOON, SpaceCompassSunSkyPhase.SUNSET)) {
            val day = spaceCompassSunGroundPalette(phase)
            for ((dark, dusk, light) in listOf(Triple(night.farArgb, evening.farArgb, day.farArgb),
                Triple(night.nearArgb, evening.nearArgb, day.nearArgb),
                Triple(night.hazeArgb, evening.hazeArgb, day.hazeArgb))) {
                assertTrue(brightness(dark) < brightness(dusk))
                assertTrue(brightness(dusk) < brightness(light))
            }
        }
    }

    @Test fun sunriseAndSunsetTintTheHazeWarmerThanDaylight() {
        for (phase in listOf(SpaceCompassSunSkyPhase.DAWN, SpaceCompassSunSkyPhase.SUNSET)) {
            val haze = spaceCompassSunGroundPalette(phase).hazeArgb
            assertTrue(((haze ushr 16) and 255) > ((haze ushr 8) and 255))
            assertTrue(((haze ushr 8) and 255) > (haze and 255))
        }
        assertTrue(brightness(spaceCompassSunGroundPalette(SpaceCompassSunSkyPhase.MORNING).nearArgb) >
            brightness(spaceCompassSunGroundPalette(SpaceCompassSunSkyPhase.AFTERNOON).nearArgb))
    }
}

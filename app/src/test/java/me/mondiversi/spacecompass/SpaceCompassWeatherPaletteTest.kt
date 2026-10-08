package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassWeatherPaletteTest {
    private fun brightness(argb: Int): Double =
        ((argb ushr 16) and 255) * .2126 + ((argb ushr 8) and 255) * .7152 + (argb and 255) * .0722
    private fun surfaces(p: SpaceCompassWeatherPalette) = listOf(p.overcastTopArgb, p.overcastBottomArgb,
        p.cloudTopArgb, p.cloudBottomArgb, p.fogTopArgb, p.fogBottomArgb, p.rainArgb, p.snowArgb)

    @Test fun everyAtmosphericEffectDimsFromDayToSunsetEveningAndNight() {
        for (storm in listOf(false, true)) {
            val sunset = surfaces(spaceCompassWeatherPalette(SpaceCompassSunSkyPhase.SUNSET, storm))
            val evening = surfaces(spaceCompassWeatherPalette(SpaceCompassSunSkyPhase.EVENING, storm))
            val night = surfaces(spaceCompassWeatherPalette(SpaceCompassSunSkyPhase.NIGHT, storm))
            for (day in listOf(SpaceCompassSunSkyPhase.MORNING, SpaceCompassSunSkyPhase.AFTERNOON)) {
                val daylight = surfaces(spaceCompassWeatherPalette(day, storm))
                for (i in daylight.indices) {
                    assertTrue("$day surface $i remains bright at sunset", brightness(daylight[i]) > brightness(sunset[i]))
                    assertTrue("Surface $i remains bright in evening", brightness(sunset[i]) > brightness(evening[i]))
                    assertTrue("Surface $i remains bright at night", brightness(evening[i]) > brightness(night[i]))
                }
            }
        }
    }

    @Test fun sunriseAndSunsetWarmCloudsAndFogNearTheHorizon() {
        for (phase in listOf(SpaceCompassSunSkyPhase.DAWN, SpaceCompassSunSkyPhase.SUNSET)) {
            val p = spaceCompassWeatherPalette(phase)
            for (c in listOf(p.overcastBottomArgb, p.cloudBottomArgb, p.fogBottomArgb))
                assertTrue("$phase horizon should be warm", ((c ushr 16) and 255) > (c and 255))
            assertTrue(brightness(p.cloudBottomArgb) > brightness(p.cloudTopArgb))
            assertTrue(brightness(p.fogBottomArgb) > brightness(p.fogTopArgb))
        }
    }

    @Test fun stormsStayDarkerThanOtherCloudsInEverySolarPhase() {
        for (phase in SpaceCompassSunSkyPhase.entries) {
            val clear = spaceCompassWeatherPalette(phase)
            val storm = spaceCompassWeatherPalette(phase, storm = true)
            assertTrue(brightness(storm.cloudTopArgb) < brightness(clear.cloudTopArgb))
            assertTrue(brightness(storm.cloudBottomArgb) < brightness(clear.cloudBottomArgb))
        }
    }

    @Test fun solarLightingNeverChangesPrecipitationOpacityOrCoverage() {
        for (phase in SpaceCompassSunSkyPhase.entries) {
            val p = spaceCompassWeatherPalette(phase)
            assertEquals(0x4D, p.rainArgb ushr 24)
            assertEquals(0xA6, p.snowArgb ushr 24)
            surfaces(p).take(6).forEach { assertEquals(255, it ushr 24) }
        }
    }

    @Test fun allSixSolarPhasesHaveTheirOwnAtmosphericLighting() {
        val palettes = SpaceCompassSunSkyPhase.entries.map { spaceCompassWeatherPalette(it) }
        assertEquals(6, palettes.map { it.cloudTopArgb }.distinct().size)
        assertEquals(6, palettes.map { it.fogTopArgb }.distinct().size)
        assertEquals(6, palettes.map { it.rainArgb }.distinct().size)
        assertEquals(6, palettes.map { it.snowArgb }.distinct().size)
    }
}

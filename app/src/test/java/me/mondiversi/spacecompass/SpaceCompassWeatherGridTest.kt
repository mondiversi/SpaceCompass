package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassWeatherGridTest {
    private fun weather(kind: SpaceCompassSunWeatherKind)=SpaceCompassSunWeatherSnapshot(kind,1f,0L)

    @Test fun onlyTheThreeRequestedConditionsChangeTheDaytimeSky() {
        for(phase in SpaceCompassSunSkyPhase.entries) for(kind in SpaceCompassSunWeatherKind.entries) {
            val wanted=phase in setOf(SpaceCompassSunSkyPhase.MORNING,SpaceCompassSunSkyPhase.AFTERNOON) &&
                kind in setOf(SpaceCompassSunWeatherKind.FOG,SpaceCompassSunWeatherKind.CLOUDY,SpaceCompassSunWeatherKind.SNOW)
            assertEquals(wanted,spaceCompassWeatherGridBlack(phase,weather(kind)))
            assertFalse(spaceCompassWeatherGridBlack(phase,weather(kind),enabled=false))
        }
        assertFalse(spaceCompassWeatherGridBlack(SpaceCompassSunSkyPhase.MORNING,null))
    }

    @Test fun wholeGridUsesOneInkAndKeepsTheChosenOpacity() {
        for(percent in 0..100 step 10) {
            val black=spaceCompassAngularGridPalette(true,percent)
            val white=spaceCompassGridPalette(percent)
            assertEquals(white.lineArgb ushr 24,black.lineArgb ushr 24)
            assertEquals(0,black.lineArgb and 0xffffff)
            assertEquals(black.lineArgb,black.majorArgb)
            assertEquals(black.lineArgb,black.liveLineArgb)
            assertEquals(white,spaceCompassAngularGridPalette(false,percent))
        }
    }

    @Test fun frozenCaptureUsesItsOwnWeatherTimeAndOpacity() {
        val base=SpaceCompassPanoramaSnapshot(0L,0.0,0.0,0.0,SpaceCompassSunSkyPhase.MORNING,
            weather(SpaceCompassSunWeatherKind.FOG),emptyList(),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList(),gridOpacityPercent=30)
        assertEquals(spaceCompassAngularGridPalette(true,30),spaceCompassWeatherGridPalette(base))
        assertEquals(spaceCompassGridPalette(30),spaceCompassWeatherGridPalette(base,enabled=false))
        assertEquals(spaceCompassGridPalette(30),spaceCompassWeatherGridPalette(base.copy(weatherEffectsEnabled=false)))
        assertEquals(spaceCompassGridPalette(30),spaceCompassWeatherGridPalette(base.copy(phase=SpaceCompassSunSkyPhase.NIGHT)))
    }
}

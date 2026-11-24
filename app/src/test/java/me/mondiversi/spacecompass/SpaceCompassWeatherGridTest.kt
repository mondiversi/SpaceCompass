package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassWeatherGridTest {
    private fun snapshot(phase: SpaceCompassSunSkyPhase,weather: SpaceCompassSunWeatherSnapshot?=null,
        weatherEnabled: Boolean=true,opacity: Int=SPACE_COMPASS_GRID_OPACITY_DEFAULT)=SpaceCompassPanoramaSnapshot(
        0L,0.0,0.0,0.0,phase,weather,emptyList(),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList(),
        gridOpacityPercent=opacity,weatherEffectsEnabled=weatherEnabled)

    @Test fun morningAndAfternoonAreBlackForEveryWeatherIncludingMissingOrHiddenForecasts() {
        val weather=SpaceCompassSunWeatherKind.entries.map { SpaceCompassSunWeatherSnapshot(it,1f,0L) }+null
        for(phase in SpaceCompassSunSkyPhase.entries) for(condition in weather) for(enabled in listOf(false,true)) {
            val wanted=phase==SpaceCompassSunSkyPhase.MORNING || phase==SpaceCompassSunSkyPhase.AFTERNOON
            assertEquals(wanted,spaceCompassSkyGridBlack(phase))
            assertEquals(spaceCompassAngularGridPalette(wanted),spaceCompassSkyGridPalette(snapshot(phase,condition,enabled)))
        }
    }

    @Test fun oneInkCoversTheCompleteGridAndKeepsTheChosenOpacity() {
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

    @Test fun captureFreezesItsSolarPhaseAndOpacityWhileCameraAndGlobeRemainIndependent() {
        val capture=snapshot(SpaceCompassSunSkyPhase.MORNING,opacity=30)
        assertEquals(spaceCompassAngularGridPalette(true,30),spaceCompassSkyGridPalette(capture))
        assertEquals(spaceCompassGridPalette(30),spaceCompassSkyGridPalette(capture,enabled=false))
        assertEquals(spaceCompassAngularGridPalette(true,30),spaceCompassSkyGridPalette(capture.copy(weatherEffectsEnabled=false)))
        for(phase in listOf(SpaceCompassSunSkyPhase.DAWN,SpaceCompassSunSkyPhase.SUNSET,SpaceCompassSunSkyPhase.EVENING,SpaceCompassSunSkyPhase.NIGHT))
            assertEquals(spaceCompassGridPalette(30),spaceCompassSkyGridPalette(capture.copy(phase=phase)))
    }
}

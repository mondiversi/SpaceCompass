package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SpaceCompassGridLightingTest {
    private fun snapshot()=SpaceCompassPanoramaSnapshot(Instant.parse("2026-03-20T12:00:00Z").toEpochMilli(),
        0.0,0.0,0.0,SpaceCompassSunSkyPhase.NIGHT,null,emptyList(),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList())

    @Test fun defaultGridIsWhiteInLiveAndImages() {
        val ink=spaceCompassGridPalette()
        assertEquals(0x80ffffff.toInt(),ink.lineArgb)
        assertEquals(ink.lineArgb,ink.majorArgb)
        assertEquals(ink.lineArgb,ink.liveLineArgb)
        assertEquals(0x80000000.toInt(),ink.cameraOutlineArgb)
    }
    @Test fun solarBandsNeverRecolourOrBlendTheGrid() {
        val source=snapshot();val expected=spaceCompassGridPalette()
        for(hour in 0..23) for(month in listOf(3,6,12)) {
            val time=java.time.LocalDate.of(2026,month,21).atTime(hour,0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
            assertEquals(expected,spaceCompassGridPalette(source.copy(timeMs=time)))
        }
    }
    @Test fun fiftyPercentDefaultAndAllElevenStopsAreRetained() {
        assertEquals(50,SPACE_COMPASS_GRID_OPACITY_DEFAULT)
        for(percent in 0..100 step 10) {
            assertEquals(percent,spaceCompassGridOpacity(percent))
            val ink=spaceCompassGridPalette(percent)
            assertEquals(ink.lineArgb ushr 24,ink.cameraOutlineArgb ushr 24)
            assertEquals(0,ink.cameraOutlineArgb and 0xffffff)
        }
        assertEquals(0,spaceCompassGridOpacity(-20));assertEquals(100,spaceCompassGridOpacity(150))
        assertEquals(40,spaceCompassGridOpacity(44));assertEquals(50,spaceCompassGridOpacity(45))
    }
    @Test fun halfSliderScalesWhiteStrokesWithoutRecolouring() {
        val ink=spaceCompassGridPalette(50)
        for(core in listOf(ink.lineArgb,ink.majorArgb,ink.liveLineArgb)) {
            assertEquals(128,core ushr 24)
            assertEquals(0xffffff,core and 0xffffff)
        }
        assertEquals(0xffffffff.toInt(),spaceCompassGridPalette(100).lineArgb)
    }
    @Test fun zeroOpacityHidesTheGrid() {
        val ink=spaceCompassGridPalette(0)
        for(value in listOf(ink.lineArgb,ink.majorArgb,ink.liveLineArgb,ink.cameraOutlineArgb))
            assertEquals(0,value ushr 24)
    }
    @Test fun backgroundBlendingAndPhaseLabelsCannotOverrideWhiteInk() {
        val source=snapshot();val expected=spaceCompassGridPalette(source)
        for(phase in SpaceCompassSunSkyPhase.entries) for(band in SpaceCompassSolarLightBand.entries)
            for(mix in listOf(0f,.5f,1f)) assertEquals(expected,spaceCompassGridPalette(source.copy(phase=phase,
                solarLighting=SpaceCompassSolarLighting(band,SpaceCompassSolarLightBand.DAY,mix))))
    }
    @Test fun ObserverPositionAndUnavailableDataDoNotChangeThePalette() {
        val source=snapshot();val expected=spaceCompassGridPalette()
        for(view in listOf(source,source.copy(longitude=180.0),source.copy(altitude=20_000.0),
            source.copy(observerPositionKnown=false),source.copy(latitude=100.0),
            source.copy(longitude=Double.NaN),source.copy(altitude=Double.NaN)))
            assertEquals(expected,spaceCompassGridPalette(view))
    }
    @Test fun applicationThemeAndObjectSelectionDoNotChangeWhiteInk() {
        val source=snapshot();val expected=spaceCompassGridPalette(source)
        for(dark in listOf(false,true)) for(body in listOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON,SpaceCompassCelestialBody.ISS)) {
            val view=source.copy(globe=SpaceCompassGlobeCapture(SpaceCompassGlobePose(),"",dark=dark),
                objects=listOf(SpaceCompassPanoramaObject(body,"Object",null)))
            assertEquals(expected,spaceCompassGridPalette(view))
        }
    }
    @Test fun frozenTimeTimezoneAndDisclosureKeepTheSamePalette() {
        val source=snapshot();val expected=spaceCompassGridPalette(source)
        for(hour in listOf("00","05","12","18"))
            assertEquals(expected,spaceCompassGridPalette(source.copy(timeMs=Instant.parse("2026-03-20T"+hour+":00:00Z").toEpochMilli())))
        for(position in SpaceCompassPanoramaPosition.entries)
            assertEquals(expected,spaceCompassGridPalette(spaceCompassSnapshotForDisclosure(source,position)))
        assertEquals(expected,spaceCompassGridPalette(source.copy(zoneId="Pacific/Honolulu")))
    }
    @Test fun frozenCaptureKeepsItsOwnOpacityInsteadOfCurrentLiveChoice() {
        val source=snapshot().copy(gridOpacityPercent=30)
        val frozen=spaceCompassGridPalette(source)
        assertEquals(spaceCompassGridPalette(30),frozen)
        assertNotEquals(spaceCompassGridPalette(80),frozen)
        assertEquals(frozen,spaceCompassGridPalette(source.copy(positionHidden=true)))
    }
    @Test fun webRgbaMatchesNativeWhiteForEveryOpacityStop() {
        for(percent in 0..100 step 10) {
            val ink=spaceCompassGridPalette(percent)
            val rgba=spaceCompassGridCss(ink.liveLineArgb)
            assertEquals("ffffff",rgba.substring(1,7))
            assertEquals(ink.lineArgb,ink.liveLineArgb)
        }
        assertEquals("#ffffff80",spaceCompassGridCss(spaceCompassGridPalette().liveLineArgb))
        assertEquals("#ffffffcc",spaceCompassGridCss(spaceCompassGridPalette(80).liveLineArgb))
        assertEquals("#ffffff80",spaceCompassGridCss(spaceCompassGridPalette(50).liveLineArgb))
    }
}

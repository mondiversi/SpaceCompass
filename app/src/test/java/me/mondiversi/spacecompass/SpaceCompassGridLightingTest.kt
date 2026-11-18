package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SpaceCompassGridLightingTest {
    private fun snapshot()=SpaceCompassPanoramaSnapshot(Instant.parse("2026-03-20T12:00:00Z").toEpochMilli(),
        0.0,0.0,0.0,SpaceCompassSunSkyPhase.NIGHT,null,emptyList(),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList())

    @Test fun dawnTwilightAndDayUseTheSameBrightInkInLiveAndImages() {
        for(elevation in listOf(-17.999999,-12.0,-6.0,-0.833,0.0,6.0,8.0,20.0,90.0)) {
            val ink=spaceCompassGridPalette(elevation)
            assertEquals(0xccffffff.toInt(),ink.lineArgb)
            assertEquals(ink.lineArgb,ink.majorArgb)
            assertEquals(ink.lineArgb,ink.liveLineArgb)
            assertEquals(0xffffffff.toInt(),spaceCompassGridPalette(elevation,100).lineArgb)
            assertTrue((ink.lineArgb and 255)>0xbd)
        }
    }
    @Test fun onlyNightRetainsPreviousGreyWithAnImmediateSwitch() {
        for(elevation in listOf(null,Double.NaN,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,
            -90.0,-30.0,-18.000001,-18.0)) {
            val ink=spaceCompassGridPalette(elevation)
            assertEquals(0xccbdbdbd.toInt(),ink.lineArgb)
            assertEquals(ink.lineArgb,ink.majorArgb)
            assertEquals(ink.lineArgb,ink.liveLineArgb)
        }
        for(h in listOf(-90.0,-30.0,-18.000001,-18.0,-17.999999,-12.0,-6.0,0.0,6.0,8.0,45.0,90.0)) {
            val lighting=spaceCompassSolarLighting(h)
            assertEquals(lighting.from!=SpaceCompassSolarLightBand.NIGHT || lighting.to!=SpaceCompassSolarLightBand.NIGHT,
                spaceCompassGridUsesDayInk(h))
        }
        assertEquals(spaceCompassGridPalette(-90.0),spaceCompassGridPalette(-18.0))
        assertEquals(spaceCompassGridPalette(90.0),spaceCompassGridPalette(-17.999999))
    }
    @Test fun eightyPercentDefaultAndAllElevenStopsAreRetained() {
        assertEquals(80,SPACE_COMPASS_GRID_OPACITY_DEFAULT)
        for(percent in 0..100 step 10) assertEquals(percent,spaceCompassGridOpacity(percent))
        assertEquals(0,spaceCompassGridOpacity(-20));assertEquals(100,spaceCompassGridOpacity(150))
        assertEquals(40,spaceCompassGridOpacity(44));assertEquals(50,spaceCompassGridOpacity(45))
    }
    @Test fun halfOpacityScalesStrokesAndOutlinesWithoutRecolouring() {
        for(elevation in listOf(-20.0,-12.0,0.0,20.0)) {
            val ink=spaceCompassGridPalette(elevation,50)
            for(core in listOf(ink.lineArgb,ink.majorArgb,ink.liveLineArgb)) {
                assertEquals(128,core ushr 24)
                assertEquals(if(elevation> -18.0) 0xffffff else 0xbdbdbd,core and 0xffffff)
            }
            assertEquals(32,ink.shadowArgb ushr 24);assertEquals(45,ink.liveShadowArgb ushr 24)
        }
    }
    @Test fun zeroOpacityHidesCoreAndOutlinesTogether() {
        for(elevation in listOf(-20.0,-12.0,0.0,20.0)) {
            val ink=spaceCompassGridPalette(elevation,0)
            for(value in listOf(ink.lineArgb,ink.majorArgb,ink.liveLineArgb,ink.shadowArgb,ink.liveShadowArgb))
                assertEquals(0,value ushr 24)
        }
    }
    @Test fun backgroundBlendingAndPhaseLabelsCannotOverrideFrozenSun() {
        for(hour in listOf("00","12")) {
            val source=snapshot().copy(timeMs=Instant.parse("2026-03-20T"+hour+":00:00Z").toEpochMilli())
            val expected=spaceCompassGridPalette(source)
            for(phase in SpaceCompassSunSkyPhase.entries) for(band in SpaceCompassSolarLightBand.entries)
                for(mix in listOf(0f,.5f,1f)) assertEquals(expected,spaceCompassGridPalette(source.copy(phase=phase,
                    solarLighting=SpaceCompassSolarLighting(band,SpaceCompassSolarLightBand.DAY,mix))))
        }
    }
    @Test fun activeObserverDeterminesSunAndUnavailableDataKeepsNightGrey() {
        val source=snapshot()
        assertEquals(spaceCompassGridPalette(90.0),spaceCompassGridPalette(source))
        assertEquals(spaceCompassGridPalette(-90.0),spaceCompassGridPalette(source.copy(longitude=180.0)))
        assertEquals(spaceCompassGridPalette(source),spaceCompassGridPalette(source.copy(altitude=20_000.0)))
        for(view in listOf(source.copy(observerPositionKnown=false),source.copy(latitude=100.0),
            source.copy(longitude=Double.NaN),source.copy(altitude=Double.NaN)))
            assertEquals(spaceCompassGridPalette(),spaceCompassGridPalette(view))
    }
    @Test fun applicationThemeAndObjectSelectionDoNotChangeGrey() {
        val source=snapshot();val expected=spaceCompassGridPalette(source)
        for(dark in listOf(false,true)) for(body in listOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON,SpaceCompassCelestialBody.ISS)) {
            val view=source.copy(globe=SpaceCompassGlobeCapture(SpaceCompassGlobePose(),"",dark=dark),
                objects=listOf(SpaceCompassPanoramaObject(body,"Object",null)))
            assertEquals(expected,spaceCompassGridPalette(view))
        }
    }
    @Test fun frozenTimeChangesSunButTimezoneAndDisclosureDoNot() {
        val source=snapshot();val day=spaceCompassGridPalette(source)
        val night=spaceCompassGridPalette(source.copy(timeMs=Instant.parse("2026-03-20T00:00:00Z").toEpochMilli()))
        assertNotEquals(day,night)
        assertEquals(spaceCompassGridPalette(-90.0),night)
        for(time in listOf("2026-03-20T05:30:00Z","2026-03-20T18:30:00Z")) {
            val twilight=source.copy(timeMs=Instant.parse(time).toEpochMilli())
            val elevation=calculateSpaceCompassSunPosition(twilight.timeMs,twilight.latitude,twilight.longitude,twilight.altitude).elevationDegrees
            assertTrue(elevation> -18.0 && elevation<0.0)
            assertEquals(day,spaceCompassGridPalette(twilight))
        }
        for(position in SpaceCompassPanoramaPosition.entries)
            assertEquals(day,spaceCompassGridPalette(spaceCompassSnapshotForDisclosure(source,position)))
        assertEquals(day,spaceCompassGridPalette(source.copy(zoneId="Pacific/Honolulu")))
    }
    @Test fun frozenCaptureKeepsItsOwnOpacityInsteadOfCurrentLiveChoice() {
        val source=snapshot().copy(gridOpacityPercent=30)
        val frozen=spaceCompassGridPalette(source)
        assertEquals(spaceCompassGridPalette(90.0,30),frozen)
        assertNotEquals(spaceCompassGridPalette(90.0,80),frozen)
        assertEquals(frozen,spaceCompassGridPalette(source.copy(positionHidden=true)))
    }
    @Test fun webRgbaMatchesNativeGreysForEveryOpacityStop() {
        for(percent in 0..100 step 10) for(elevation in listOf(-20.0,-12.0,0.0,20.0)) {
            val rgba=spaceCompassGridCss(spaceCompassGridPalette(elevation,percent).liveLineArgb)
            assertEquals(if(elevation> -18.0) "ffffff" else "bdbdbd",rgba.substring(1,7))
        }
        assertEquals("#bdbdbdcc",spaceCompassGridCss(spaceCompassGridPalette().liveLineArgb))
        assertEquals("#ffffffcc",spaceCompassGridCss(spaceCompassGridPalette(20.0).liveLineArgb))
        assertEquals("#ffffff80",spaceCompassGridCss(spaceCompassGridPalette(20.0,50).liveLineArgb))
        assertEquals("#00000047",spaceCompassGridCss(spaceCompassGridPalette().liveShadowArgb))
    }
}

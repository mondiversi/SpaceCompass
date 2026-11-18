package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SpaceCompassGridLightingTest {
    private fun snapshot(instant: String="2026-03-20T12:00:00Z",longitude: Double=0.0)=
        SpaceCompassPanoramaSnapshot(Instant.parse(instant).toEpochMilli(),0.0,longitude,0.0,
            SpaceCompassSunSkyPhase.NIGHT,null,emptyList(),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList())

    @Test fun nightUsesOneFixedGreyBelowMinusSixDegrees() {
        for(h in listOf(-90.0,-18.0,-6.0001)) {
            val p=spaceCompassGridPalette(h,100)
            assertEquals(SpaceCompassAngularGridAppearance.lineArgb,p.lineArgb)
            assertEquals(SpaceCompassAngularGridAppearance.majorArgb,p.majorArgb)
            assertEquals(SpaceCompassAngularGridAppearance.liveLineArgb,p.liveLineArgb)
            assertEquals(SpaceCompassAngularGridAppearance.shadowArgb,p.shadowArgb)
            assertEquals(SpaceCompassAngularGridAppearance.liveShadowArgb,p.liveShadowArgb)
            assertFalse(spaceCompassGridUsesDayInk(h))
        }
    }
    @Test fun civilDawnDayAndDuskUseOneLighterDayGrey() {
        for(h in listOf(-6.0,-5.0,0.0,8.0,20.0,90.0)) {
            val p=spaceCompassGridPalette(h)
            assertEquals(0xcc707070.toInt(),p.lineArgb);assertEquals(0xcc707070.toInt(),p.majorArgb)
            assertEquals(0xcc707070.toInt(),p.liveLineArgb)
            assertTrue(spaceCompassGridUsesDayInk(h))
        }
    }
    @Test fun theSwitchHasNoIntermediateGreyAndTheDayToneRemainsLighterThanTheFirstTrial() {
        val day=spaceCompassGridPalette(90.0)
        assertFalse(spaceCompassGridUsesDayInk(-6.000001))
        assertTrue(spaceCompassGridUsesDayInk(-6.0))
        for(i in 0..28) assertEquals(day,spaceCompassGridPalette(-6.0+i*.5))
        assertEquals(spaceCompassGridPalette(-90.0),spaceCompassGridPalette(-6.000001))
        val grey=day.liveLineArgb and 255
        assertTrue(grey>0x48);assertTrue(grey<0xff)
        assertEquals("#707070cc",spaceCompassGridCss(day.liveLineArgb))
    }
    @Test fun opacityAndShadowStayUnchangedAtEverySolarHeight() {
        val night=spaceCompassGridPalette(-90.0)
        for(h in listOf(-6.0,-2.0,0.0,2.0,5.0,8.0)) {
            val p=spaceCompassGridPalette(h)
            assertEquals(night.lineArgb ushr 24,p.lineArgb ushr 24)
            assertEquals(night.liveLineArgb ushr 24,p.liveLineArgb ushr 24)
            assertEquals(night.majorArgb ushr 24,p.majorArgb ushr 24)
            assertEquals(night.shadowArgb,p.shadowArgb);assertEquals(night.liveShadowArgb,p.liveShadowArgb)
        }
    }
    @Test fun invalidOrUnavailableSolarDataKeepsTheExistingNightGrid() {
        for(h in listOf(null,Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY))
            assertEquals(spaceCompassGridPalette(-90.0),spaceCompassGridPalette(h))
        assertEquals(spaceCompassGridPalette(-90.0),spaceCompassGridPalette(snapshot().copy(observerPositionKnown=false)))
        assertEquals(spaceCompassGridPalette(-90.0),spaceCompassGridPalette(snapshot().copy(latitude=100.0)))
    }
    @Test fun capturesUseTheirScenarioInstantAndPlaceRatherThanDeviceThemeOrSelectedObject() {
        val day=snapshot();val night=snapshot("2026-03-20T00:00:00Z")
        assertEquals(spaceCompassGridPalette(90.0),spaceCompassGridPalette(day))
        assertEquals(spaceCompassGridPalette(-90.0),spaceCompassGridPalette(night))
        assertEquals(spaceCompassGridPalette(night),spaceCompassGridPalette(day.copy(longitude=180.0)))
        for(body in listOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON,SpaceCompassCelestialBody.ISS))
            assertEquals(spaceCompassGridPalette(day),spaceCompassGridPalette(day.copy(objects=listOf(SpaceCompassPanoramaObject(body,"Object",null)))))
    }
    @Test fun appLightDarkCaptureThemeAndFallbackPhaseNeverOverrideScenarioSunlight() {
        val original=snapshot()
        for(dark in listOf(false,true)) {
            val view=original.copy(globe=SpaceCompassGlobeCapture(SpaceCompassGlobePose(),"",dark=dark))
            assertEquals(spaceCompassGridPalette(original),spaceCompassGridPalette(view))
        }
        for(phase in SpaceCompassSunSkyPhase.entries)
            assertEquals(spaceCompassGridPalette(original),spaceCompassGridPalette(original.copy(phase=phase)))
    }
    @Test fun frozenImageDisclosureAndTimezoneDoNotAlterItsGridLighting() {
        val original=snapshot()
        for(position in SpaceCompassPanoramaPosition.entries)
            assertEquals(spaceCompassGridPalette(original),spaceCompassGridPalette(spaceCompassSnapshotForDisclosure(original,position)))
        assertEquals(spaceCompassGridPalette(original),spaceCompassGridPalette(original.copy(zoneId="Pacific/Honolulu")))
    }
    @Test fun opacityHasElevenStopsAndDefaultsToEightyPercent() {
        assertEquals(80, SPACE_COMPASS_GRID_OPACITY_DEFAULT)
        for(percent in 0..100 step 10) assertEquals(percent, spaceCompassGridOpacity(percent))
        assertEquals(0, spaceCompassGridOpacity(-20)); assertEquals(100, spaceCompassGridOpacity(150))
        assertEquals(40, spaceCompassGridOpacity(44)); assertEquals(50, spaceCompassGridOpacity(45))
        for(h in listOf(-18.0, -6.0, 20.0)) {
            assertEquals(204, spaceCompassGridPalette(h).liveLineArgb ushr 24)
            assertEquals(255, spaceCompassGridPalette(h,100).liveLineArgb ushr 24)
        }
        assertEquals("#70707080",spaceCompassGridCss(spaceCompassGridPalette(20.0,50).liveLineArgb))
    }
    @Test fun zeroOpacityHidesBothStrokesAndCapturesFreezeTheirOwnOpacity() {
        val zero=spaceCompassGridPalette(20.0,0)
        for(ink in listOf(zero.lineArgb,zero.majorArgb,zero.liveLineArgb,zero.shadowArgb,zero.liveShadowArgb)) assertEquals(0,ink ushr 24)
        val source=snapshot().copy(gridOpacityPercent=30)
        assertEquals(spaceCompassGridPalette(90.0,30),spaceCompassGridPalette(source))
        for(position in SpaceCompassPanoramaPosition.entries)
            assertEquals(spaceCompassGridPalette(source),spaceCompassGridPalette(spaceCompassSnapshotForDisclosure(source,position)))
    }
    @Test fun webCanvasUsesTheSameLiveRgbaInkAsNativeViews() {
        assertEquals("#9c9c9ccc",spaceCompassGridCss(spaceCompassGridPalette(-6.0001).liveLineArgb))
        assertEquals("#707070cc",spaceCompassGridCss(spaceCompassGridPalette(8.0).liveLineArgb))
        assertEquals("#00000047",spaceCompassGridCss(spaceCompassGridPalette(8.0).liveShadowArgb))
    }
}

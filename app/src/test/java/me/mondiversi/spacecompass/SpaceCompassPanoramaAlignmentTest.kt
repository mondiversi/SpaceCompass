package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPanoramaAlignmentTest {
    @Test fun verticalCentreSplitsTheMeasuredControlGapEqually() {
        for((w,h) in listOf(400.0 to 550.0,400.0 to 700.0,900.0 to 400.0)) {
            val upper=74.0;val lower=h-63.0
            val band=spaceCompassGlobeMapClearBand(w,h,upper,lower,10.0)
            assertEquals((upper+lower)/2,band.top+band.height/2,1e-10)
            assertEquals(band.top-upper,lower-band.top-band.height,1e-10)
        }
    }
    @Test fun landscapeMapKeepsFullWidthAndCentresOnTheUncoveredColumnIncludingRtl() {
        for(left in listOf(0.0,405.0)) {
            val pointing=SpaceCompassSunSceneFrame(left,0.0,495.0,400.0)
            val optical=spaceCompassGlobeMapClearBand(495.0,400.0,70.0,330.0,10.0).copy(left=left)
            val viewport=spaceCompassGlobeMapViewport(pointing,900.0,400.0,10.0,optical=optical)
            assertEquals(900.0,viewport.width,0.0)
            assertEquals(left+247.5,viewport.left+viewport.width/2,1e-10)
            assertEquals(200.0,viewport.top+viewport.height/2,1e-10)
        }
    }
    @Test fun compactPortraitFooterAndExpandedPanelsStillBoundTheProjection() {
        for(height in listOf(550.0,710.0)) {
            val pointing=SpaceCompassSunSceneFrame(10.0,0.0,380.0,height)
            val optical=spaceCompassGlobeMapClearBand(380.0,height,70.0,height-60.0,10.0).copy(left=10.0)
            val viewport=spaceCompassGlobeMapViewport(pointing,400.0,800.0,10.0,optical=optical)
            assertEquals(200.0,viewport.left+viewport.width/2,1e-10)
            assertEquals((70+height-60)/2,viewport.top+viewport.height/2,1e-10)
            assertTrue(viewport.top+viewport.height<height)
        }
    }
    @Test fun portraitCentreIgnoresThePointingHostsUnevenLayoutPadding() {
        val pointing=SpaceCompassSunSceneFrame(7.0,0.0,390.0,550.0)
        val optical=SpaceCompassSunSceneFrame(7.0,80.0,390.0,390.0)
        val viewport=spaceCompassGlobeMapViewport(pointing,400.0,800.0,10.0,optical=optical)
        assertEquals(200.0,viewport.left+viewport.width/2,1e-10)
        assertEquals(275.0,viewport.top+viewport.height/2,1e-10)
    }
    @Test fun entryRecentresBothTranslationsAndKeepsEveryZoomValue() {
        for(zoom in listOf(.5,1.0,1.25,2.0,8.0)) {
            val pose=spaceCompassGlobeMapCenteredPose(SpaceCompassGlobePose(.4,.6,zoom,.5))
            assertEquals(0.0,pose.longitude,0.0);assertEquals(0.0,pose.latitude,0.0)
            assertEquals(0.0,pose.roll,0.0);assertEquals(zoom,pose.zoom,0.0)
        }
    }
    @Test fun hidingControlsUsesTheEntirePointingAreaWithoutAStaleInset() {
        val full=spaceCompassGlobeMapClearBand(400.0,800.0,0.0,800.0,10.0)
        assertEquals(400.0,full.top+full.height/2,1e-10)
        assertEquals(780.0,full.height,1e-10)
    }
    @Test fun smallControlGapsNeverCreateAnInvalidViewport() {
        val band=spaceCompassGlobeMapClearBand(400.0,80.0,40.0,41.0,10.0)
        assertEquals(1.0,band.height,0.0);assertEquals(40.5,band.top+band.height/2,0.0)
    }
}

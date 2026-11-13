package me.mondiversi.spacecompass

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassGlobeMapGeometryTest {
    @Test fun flatZoomExtendsPastTheSphericalLimitWithoutChangingIt() {
        val input = SpaceCompassGlobePose(0.2, 0.0, 20.0, 1.0)
        assertEquals(8.0, spaceCompassGlobeMapPose(input).zoom, 0.0)
        assertEquals(3.0, input.normalized().zoom, 0.0)
        assertEquals(0.0, spaceCompassGlobeMapPose(input).roll, 0.0)
    }
    @Test fun mapsShorterThanTheViewportStayVerticallyCentered() {
        for ((w,h) in listOf(400.0 to 800.0, 800.0 to 400.0, 480.0 to 400.0))
            for (latitude in listOf(-PI/2, 0.0, PI/2))
                assertEquals(0.0, spaceCompassGlobeMapPose(SpaceCompassGlobePose(latitude=latitude),w,h).latitude, 1e-10)
    }
    @Test fun maximumZoomCoversTheVisibleHeightAndNeverPansPastItsEdges() {
        for ((w,h) in listOf(400.0 to 800.0, 900.0 to 400.0, 480.0 to 400.0)) {
            val mapHeight = w*8/2
            assertTrue(mapHeight >= h)
            for (latitude in listOf(-PI/2, 0.0, PI/2)) {
                val pose = spaceCompassGlobeMapPose(SpaceCompassGlobePose(latitude=latitude,zoom=8.0),w,h)
                val top = (h-mapHeight)/2+pose.latitude/PI*mapHeight
                assertTrue(top <= 1e-8);assertTrue(top+mapHeight >= h-1e-8)
            }
        }
    }
    @Test fun mapPanningUsesTheRenderedScaleAndStopsAtThePoles() {
        val start = SpaceCompassGlobePose(zoom=8.0)
        val moved = spaceCompassGlobeMapDrag(start,40.0,50.0,400.0,800.0)
        assertEquals(40.0/3200*PI*2,moved.longitude,1e-10)
        assertEquals(-50.0/1600*PI,moved.latitude,1e-10)
        val limited=spaceCompassGlobeMapDrag(start,0.0,1e9,400.0,800.0)
        assertEquals(-PI/4,limited.latitude,1e-10)
        assertEquals(0.0,spaceCompassGlobeMapPose(limited.copy(zoom=1.0),400.0,800.0).latitude,0.0)
    }
    @Test fun landscapeMapsUseTheFullSceneWithExpandedCompactOrMirroredPanels() {
        for (pointing in listOf(
            SpaceCompassSunSceneFrame(10.0,0.0,475.0,390.0),
            SpaceCompassSunSceneFrame(415.0,0.0,475.0,390.0),
            SpaceCompassSunSceneFrame(10.0,0.0,835.0,390.0))) {
            assertEquals(SpaceCompassSunSceneFrame(0.0,0.0,900.0,400.0),
                spaceCompassGlobeMapViewport(pointing,900.0,400.0,10.0))
        }
    }
    @Test fun landscapePanUsesWidthFitInsteadOfTheOldHeightLimit() {
        for (zoom in listOf(1.0,2.0,8.0)) {
            val moved=spaceCompassGlobeMapDrag(SpaceCompassGlobePose(zoom=zoom),30.0,10.0,1000.0,400.0)
            assertEquals(30.0/(1000*zoom)*PI*2,moved.longitude,1e-10)
            assertEquals(-10.0/(500*zoom)*PI,moved.latitude,1e-10)
            val atPole=spaceCompassGlobeMapDrag(moved,0.0,1e9,1000.0,400.0)
            val mapHeight=500*zoom
            assertEquals(-(mapHeight-400)/2/mapHeight*PI,atPole.latitude,1e-10)
        }
    }
    @Test fun portraitPanelsAndCompactFooterRemainRealBoundaries() {
        val pointing=SpaceCompassSunSceneFrame(10.0,0.0,380.0,550.0)
        assertEquals(SpaceCompassSunSceneFrame(0.0,0.0,400.0,550.0),
            spaceCompassGlobeMapViewport(pointing,400.0,800.0,10.0))
        assertEquals(490.0,spaceCompassGlobeMapViewport(pointing,400.0,800.0,10.0,500.0).height,0.0)
        val clean=SpaceCompassSunSceneFrame(0.0,0.0,400.0,800.0)
        assertEquals(clean,spaceCompassGlobeMapViewport(clean,400.0,800.0,10.0))
    }
}

package me.mondiversi.spacecompass

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassGlobeCompactLayoutTest {
    private fun ring(): SpaceCompassGlobeModel {
        val curve=(0..180).map { index -> val a=index*PI/90
            SpaceCompassGlobeVector(1.55*sin(a),0.0,1.55*cos(a)) to false }
        val item=SpaceCompassPanoramaObject(SpaceCompassCelestialBody.SUN,"Sun",null)
        return SpaceCompassGlobeModel(listOf(SpaceCompassGlobeTrack(item,curve,emptyList())),1.55)
    }
    @Test fun rectangularFitUsesLandscapeHeightWithoutShrinkingToAnUnnecessarySphere() {
        val model=ring();val frame=SpaceCompassSunSceneFrame(0.0,65.0,700.0,220.0)
        val pose=SpaceCompassGlobePose(0.0,.15,1.0,.1)
        val bounds=spaceCompassGlobeFitBounds(model,pose)
        val radius=spaceCompassGlobeFitRadius(frame,bounds,1.0)
        assertTrue(radius>spaceCompassGlobeFitRadius(frame,model.extent,1.0)*1.4)
        for(longitude in 0..36) {
            val rotation=pose.copy(longitude=longitude*PI/18)
            assertEquals(bounds,spaceCompassGlobeFitBounds(model,rotation))
            for((v,_) in model.tracks.single().curve) {
                val point=spaceCompassGlobeView(v,rotation)
                assertTrue(abs(point.x*radius)<=frame.width/2-24+1e-8)
                assertTrue(abs(point.y*radius)<=frame.height/2-24+1e-8)
            }
        }
        assertTrue(radius<=min(frame.width,frame.height)/2-24)
        assertEquals(radius*2,spaceCompassGlobeFitRadius(frame,bounds,2.0),1e-10)
    }
    @Test fun compactStripRemainsAViewportBoundaryEvenWithoutFloatingControls() {
        val full=SpaceCompassSunSceneFrame(0.0,60.0,800.0,280.0)
        val free=spaceCompassGlobeViewportAboveFooter(full,300.0,10.0)
        assertEquals(290.0,free.top+free.height,1e-10)
        assertEquals(175.0,free.top+free.height/2,1e-10)
        assertEquals(full,spaceCompassGlobeViewportAboveFooter(full,400.0,10.0))
    }
}

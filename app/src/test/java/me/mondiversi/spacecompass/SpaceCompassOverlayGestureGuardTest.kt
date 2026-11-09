package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassOverlayGestureGuardTest {
    @Test fun nearMissesAreIgnoredAtEveryEdgeButDistantSkyStaysInteractive() {
        val frame=SpaceCompassSunSceneFrame(100.0,200.0,48.0,48.0)
        for(point in listOf(99.0 to 224.0,149.0 to 224.0,124.0 to 199.0,124.0 to 249.0,
            88.0 to 188.0,160.0 to 260.0))
            assertFalse(spaceCompassOverlayGestureAllowed(SpaceCompassSunScenePoint(point.first,point.second),listOf(frame),12.0))
        assertTrue(spaceCompassOverlayGestureAllowed(SpaceCompassSunScenePoint(87.0,224.0),listOf(frame),12.0))
        assertTrue(spaceCompassOverlayGestureAllowed(SpaceCompassSunScenePoint(124.0,261.0),listOf(frame),12.0))
    }
    @Test fun protectionScalesWithDensityAndDisappearsWhenControlsAreHidden() {
        val frame=SpaceCompassSunSceneFrame(300.0,600.0,144.0,144.0)
        val point=SpaceCompassSunScenePoint(270.0,672.0)
        assertFalse(spaceCompassOverlayGestureAllowed(point,listOf(frame),12.0*3))
        assertTrue(spaceCompassOverlayGestureAllowed(point,emptyList(),12.0*3))
        assertFalse(spaceCompassOverlayGestureAllowed(SpaceCompassSunScenePoint(Double.NaN,0.0),emptyList(),12.0))
    }
}

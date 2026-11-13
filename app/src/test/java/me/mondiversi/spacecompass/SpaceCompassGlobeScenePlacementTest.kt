package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassGlobeScenePlacementTest {
    @Test fun expandingTheCanvasPreservesOpticalCentreAndFrozenPixelRadius() {
        for(pointing in listOf(SpaceCompassSunSceneFrame(10.0,0.0,340.0,500.0),
            SpaceCompassSunSceneFrame(10.0,0.0,700.0,360.0),
            SpaceCompassSunSceneFrame(140.0,20.0,500.0,320.0))) {
            val viewport=SpaceCompassSunSceneFrame(0.0,40.0,pointing.width,pointing.height-80.0)
            val layout=SpaceCompassGlobeScenePlacement(pointing,viewport,emptyList(),10.0,true)
            val scene=layout.sceneViewport()
            assertEquals(pointing.left+viewport.width/2,scene.left+scene.width/2,1e-9)
            val y=scene.top+scene.height/2
            val global=SpaceCompassGlobeFraming(900.0,700.0,y/700.0,210.0/900.0)
            val frozen=global.inPointingFrame(pointing)
            assertEquals(y,frozen.centerY*frozen.height+pointing.top,1e-9)
            assertEquals(210.0,frozen.radiusByWidth*frozen.width,1e-9)
            assertEquals(global.guideTextSizeByHeight,frozen.guideTextSizeByHeight,0.0)
        }
    }

    @Test fun controlsRemainCoveredInSceneCoordinatesWhenDrawingExtendsPastTheirHost() {
        val pointing=SpaceCompassSunSceneFrame(10.0,20.0,700.0,320.0)
        val control=SpaceCompassSunSceneFrame(640.0,10.0,48.0,48.0)
        val layout=SpaceCompassGlobeScenePlacement(pointing,pointing.copy(left=0.0,top=0.0),listOf(control),10.0,true)
        val covered=layout.sceneExclusions(900.0,360.0)
        assertTrue(covered.contains(SpaceCompassSunSceneFrame(650.0,30.0,48.0,48.0)))
        assertTrue(covered.contains(SpaceCompassSunSceneFrame(710.0,0.0,190.0,360.0)))
        assertTrue(covered.contains(SpaceCompassSunSceneFrame(10.0,340.0,700.0,20.0)))
    }
}

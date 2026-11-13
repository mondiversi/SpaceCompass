package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassLiveGridNumbersTest {
    @Test fun crossingsUseTheActualCurveAnglesAtTheVisibleFrame() {
        val grid=SpaceCompassLiveGridNumbers(SpaceCompassSunSceneFrame(10.0,20.0,100.0,80.0))
        grid.beginCurve(SpaceCompassCameraGridAxis.AZIMUTH,120.0)
        grid.moveTo(60.0,0.0);grid.lineTo(60.0,120.0)
        assertEquals(listOf(SpaceCompassCameraGridEdge.TOP,SpaceCompassCameraGridEdge.BOTTOM),grid.ticks.map { it.edge })
        assertEquals(listOf(20.0,100.0),grid.ticks.map { it.point.y })
        assertTrue(grid.ticks.all { it.degrees==120.0&&it.axis==SpaceCompassCameraGridAxis.AZIMUTH })
        grid.beginCurve(SpaceCompassCameraGridAxis.ELEVATION,-30.0)
        grid.moveTo(0.0,60.0);grid.lineTo(150.0,60.0)
        assertEquals(listOf(10.0,110.0),grid.ticks.takeLast(2).map { it.point.x })
        assertTrue(grid.ticks.takeLast(2).all { it.degrees == -30.0 })
    }
    @Test fun hiddenDisconnectedAndDuplicateSegmentsCreateNoFalseTicks() {
        val grid=SpaceCompassLiveGridNumbers(SpaceCompassSunSceneFrame(0.0,0.0,100.0,100.0))
        grid.beginCurve(SpaceCompassCameraGridAxis.ELEVATION,10.0)
        grid.moveTo(-20.0,-20.0);grid.lineTo(-10.0,-10.0)
        assertTrue(grid.ticks.isEmpty())
        grid.moveTo(-10.0,40.0);grid.lineTo(0.0,40.0);grid.lineTo(10.0,40.0)
        assertEquals(1,grid.ticks.size)
        grid.moveTo(80.0,40.0);grid.lineTo(90.0,40.0)
        assertEquals(1,grid.ticks.size)
    }
}

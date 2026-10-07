package me.mondiversi.spacecompass

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCameraAngularGridTest {
    private val width = 800.0
    private val height = 600.0
    private val lens = SpaceCompassPerspective(1.0, 1.2, .47, .54)
    private val frame = SpaceCompassSunSceneFrame(35.0, 70.0, 730.0, 485.0)
    private fun orientation(azimuth: Double, elevation: Double, roll: Double = 0.0): SpaceCompassSunOrientation {
        val a = Math.toRadians(azimuth); val e = Math.toRadians(elevation); val r = Math.toRadians(roll)
        val right = SpaceCompassSunVector(cos(a), -sin(a), 0.0)
        val up = SpaceCompassSunVector(-sin(a)*sin(e), -cos(a)*sin(e), cos(e))
        fun mix(x: SpaceCompassSunVector, y: SpaceCompassSunVector, c: Double, s: Double) =
            SpaceCompassSunVector(x.east*c+y.east*s, x.north*c+y.north*s, x.up*c+y.up*s)
        return SpaceCompassSunOrientation(mix(right,up,cos(r),sin(r)), mix(up,right,cos(r),-sin(r)),
            SpaceCompassSunVector(sin(a)*cos(e),cos(a)*cos(e),sin(e)))
    }

    @Test fun pixelRayRoundTripsThroughTheActualOffCenterLensAtAllScreenRotations() {
        for (roll in listOf(0.0,90.0,180.0,270.0)) {
            val camera = orientation(123.0,25.0,roll)
            for (p in listOf(100.0 to 100.0, 650.0 to 470.0, 376.0 to 324.0)) {
                val position = spaceCompassCameraPixelPosition(p.first,p.second,width,height,camera,lens)
                val projected = projectSpaceCompassSun(position,camera,width,height,lens)
                assertTrue(projected.visible)
                assertEquals(p.first,projected.x,1e-8); assertEquals(p.second,projected.y,1e-8)
            }
        }
    }

    @Test fun gridContainsOnlyCurvesInsideThePhotographicFrame() {
        val grid = spaceCompassCameraAngularGrid(orientation(355.0,15.0,27.0),lens,width,height,frame)
        assertTrue(grid.lines.isNotEmpty()); assertTrue(grid.ticks.isNotEmpty())
        for (line in grid.lines) for (s in line.segments) for (p in listOf(s.start,s.end)) {
            assertTrue(p.x in (frame.left-1e-6)..(frame.left+frame.width+1e-6))
            assertTrue(p.y in (frame.top-1e-6)..(frame.top+frame.height+1e-6))
        }
    }

    @Test fun azimuthTicksAroundNorthWrapCorrectlyWithoutOppositeBearings() {
        val grid = spaceCompassCameraAngularGrid(orientation(355.0,0.0),lens,width,height,frame)
        val angles = grid.ticks.filter { it.axis == SpaceCompassCameraGridAxis.AZIMUTH }.map { it.degrees }
        assertTrue(angles.isNotEmpty()); assertTrue(0.0 in angles)
        assertTrue(angles.all { it >= 330 || it <= 20 })
        assertFalse(180.0 in angles)
    }

    @Test fun tickValuesMatchTheCapturedRayIncludingTiltAndRoll() {
        for (camera in listOf(orientation(0.0,0.0),orientation(125.0,20.0,32.0),orientation(270.0,-15.0,90.0))) {
            val grid = spaceCompassCameraAngularGrid(camera,lens,width,height,frame)
            for (tick in grid.ticks) {
                val position = spaceCompassCameraPixelPosition(tick.point.x,tick.point.y,width,height,camera,lens)
                if (tick.axis == SpaceCompassCameraGridAxis.AZIMUTH) {
                    val difference = abs(wrapSpaceCompassSunDegrees(position.azimuthDegrees-tick.degrees+180)-180)
                    assertEquals(0.0,difference,1e-5)
                    assertTrue(tick.edge in setOf(SpaceCompassCameraGridEdge.TOP,SpaceCompassCameraGridEdge.BOTTOM))
                } else {
                    assertEquals(tick.degrees,position.elevationDegrees,.05)
                    assertEquals(SpaceCompassCameraGridEdge.LEFT,tick.edge)
                }
            }
        }
    }

    @Test fun horizonCurvesRemainTrueZeroElevationEvenWithSlantedCamera() {
        val camera = orientation(45.0,0.0,35.0)
        val horizon = spaceCompassCameraAngularGrid(camera,lens,width,height,frame).lines
            .single { it.axis == SpaceCompassCameraGridAxis.ELEVATION && it.degrees == 0.0 }
        assertTrue(horizon.segments.any { abs(it.end.y-it.start.y) > .1 })
        for (s in horizon.segments) for (p in listOf(s.start,s.end))
            assertEquals(0.0,spaceCompassCameraPixelPosition(p.x,p.y,width,height,camera,lens).elevationDegrees,1e-8)
    }

    @Test fun lookingAboveTheHorizonCannotCreateFalseCardinalPointsInThePhoto() {
        val camera = orientation(180.0,80.0)
        for (az in 0 until 360 step 45)
            assertFalse(projectSpaceCompassSun(SpaceCompassSunPosition(az.toDouble(),0.0),camera,width,height,lens).visible)
        assertTrue(spaceCompassCameraAngularGrid(camera,lens,width,height,frame).lines.isNotEmpty())
    }

    @Test fun clippingRejectsOutsideSegmentsAndRetainsTheExactVisiblePart() {
        val rectangle = SpaceCompassSunSceneFrame(10.0,20.0,100.0,80.0)
        val crossing = SpaceCompassSunPathSegment(SpaceCompassSunScenePoint(-10.0,60.0),SpaceCompassSunScenePoint(200.0,60.0),false)
        val clipped = spaceCompassCameraGridClip(crossing,rectangle)!!
        assertEquals(10.0,clipped.start.x,1e-9); assertEquals(110.0,clipped.end.x,1e-9)
        assertNull(spaceCompassCameraGridClip(crossing.copy(start=crossing.start.copy(y=0.0),end=crossing.end.copy(y=0.0)),rectangle))
    }
}

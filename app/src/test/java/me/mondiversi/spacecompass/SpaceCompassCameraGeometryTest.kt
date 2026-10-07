package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassCameraGeometryTest {
    private val north = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, 1.0, 0.0))
    private val lens = SpaceCompassCameraLens(0.0, 0.0, 4000.0, 3000.0, 3000.0, 3000.0, 2000.0, 1500.0, 90)
    @Test fun portraitCameraFocalMatchesAspectCropAndNotVirtualFieldOfView() {
        val geometry = spaceCompassCameraGeometry(lens, 1280.0, 960.0, 0, 400.0, 600.0,
            SpaceCompassSunSceneFrame(0.0, 0.0, 400.0, 600.0))!!
        assertEquals(450.0, geometry.perspective.focalX(400.0), 1e-8)
        assertEquals(450.0, geometry.perspective.focalY(600.0), 1e-8)
        val projected = projectSpaceCompassSun(SpaceCompassSunPosition(15.0, 0.0), north, 400.0, 600.0, geometry.perspective)
        assertEquals(200 + 450 * tan(Math.toRadians(15.0)), projected.x, 1e-8)
        assertNotEquals(spaceCompassSunProjectionFocalLength(600.0), 450.0, 1e-8)
    }
    @Test fun rearPreviewKeepsAxisOnOffsetReticleWithoutStretchingOrMirroring() {
        for (sensor in listOf(0, 90, 180, 270)) for (display in listOf(0, 90, 180, 270)) {
            val frame = SpaceCompassSunSceneFrame(10.0, 50.0, 380.0, 400.0)
            val geometry = spaceCompassCameraGeometry(lens.copy(sensorRotation = sensor), 1280.0, 960.0, display,
                400.0, 900.0, frame)!!
            val corners = geometry.destination
            val centerX = corners.sumOf { it.x } / 4
            val centerY = corners.sumOf { it.y } / 4
            assertEquals(200.0, centerX, 1e-8)
            assertEquals(250.0, centerY, 1e-8)
            val a = corners[0]; val b = corners[1]; val c = corners[2]
            assertTrue((b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x) > 0)
            assertEquals(1280.0 / 960.0, hypot(b.x-a.x,b.y-a.y)/hypot(c.x-b.x,c.y-b.y), 1e-8)
            assertTrue(corners.minOf { it.x } <= 0 && corners.maxOf { it.x } >= 400)
            assertTrue(corners.minOf { it.y } <= 0 && corners.maxOf { it.y } >= 900)
        }
    }
    @Test fun streamAspectCropAndReturnedSensorCropAreIncluded() {
        val frame = SpaceCompassSunSceneFrame(0.0, 0.0, 600.0, 400.0)
        val full = spaceCompassCameraGeometry(lens.copy(sensorRotation=0), 1920.0, 1080.0, 0, 600.0, 400.0, frame)!!
        val zoom = spaceCompassCameraGeometry(lens.copy(cropLeft=1000.0, cropTop=750.0, cropWidth=2000.0, cropHeight=1500.0,
            sensorRotation=0), 1920.0, 1080.0, 0, 600.0, 400.0, frame)!!
        assertEquals(2 * full.perspective.horizontal, zoom.perspective.horizontal, 1e-8)
        assertEquals(2 * full.perspective.vertical, zoom.perspective.vertical, 1e-8)
        assertEquals(533.3333333333333, full.perspective.focalX(600.0), 1e-8)
    }
    @Test fun shiftedOpticalCentreStillMapsToReticle() {
        val geometry = spaceCompassCameraGeometry(lens.copy(centerX=2100.0, centerY=1450.0),
            1280.0,960.0,0,400.0,600.0,SpaceCompassSunSceneFrame(0.0,0.0,400.0,600.0))!!
        val corners = geometry.destination
        val x = 2100.0/4000.0; val y = 1450.0/3000.0
        fun interpolate(component: (SpaceCompassSunScenePoint)->Double) = component(corners[0]) * (1-x)*(1-y) +
            component(corners[1])*x*(1-y) + component(corners[2])*x*y + component(corners[3])*(1-x)*y
        assertEquals(200.0, interpolate { it.x }, 1e-8)
        assertEquals(300.0, interpolate { it.y }, 1e-8)
    }
    @Test fun allOverlayAndClippingMathUsesCameraPerspective() {
        val perspective = SpaceCompassPerspective(1.5, .8)
        val a = SpaceCompassSunVector(-.2, 1.0, .1).normalized()
        val b = SpaceCompassSunVector(.2, 1.0, .1).normalized()
        val segment = projectSpaceCompassSunPathSegment(a,b,north,400.0,600.0,false,perspective)!!
        assertEquals(80.0, segment.start.x,1e-8)
        assertEquals(320.0,segment.end.x,1e-8)
        assertEquals(252.0, segment.start.y,1e-8)
        val clipped = projectSpaceCompassSunPathSegment(SpaceCompassSunVector(-2.0,1.0,.1),
            SpaceCompassSunVector(2.0,1.0,.1),north,400.0,600.0,false,perspective)!!
        assertEquals(0.0,clipped.start.x,1e-8); assertEquals(400.0,clipped.end.x,1e-8)
        val point = SpaceCompassSunPathPoint(0L,SpaceCompassSunPosition(10.0,5.0))
        val target = projectSpaceCompassCelestialSceneTargets(emptyMap(), mapOf(SpaceCompassCelestialBody.SUN to point),
            north,400.0,600.0,perspective).single()
        val projection = projectSpaceCompassSun(point.position,north,400.0,600.0,perspective)
        assertEquals(projection.x,target.projection.x,1e-8); assertEquals(projection.y,target.projection.y,1e-8)
        val direction = SpaceCompassSunPathDirection(0L,a,b,SpaceCompassSunVector(0.0,1.0,.1).normalized())
        val arrow = projectSpaceCompassSunPathArrow(direction,north,400.0,600.0,5.0,perspective)!!
        assertEquals(200.0,arrow.center.x,1e-8); assertEquals(252.0,arrow.center.y,1e-8)
    }
    @Test fun horizonAndCelestialElevationAgreeWithLensForRollAndPitch() {
        val perspective = SpaceCompassPerspective(1.8,.65)
        val frame = SpaceCompassSunSceneFrame(10.0,50.0,380.0,600.0)
        for (pitch in listOf(-20.0,0.0,20.0)) for (roll in listOf(-30.0,0.0,30.0)) {
            val p = Math.toRadians(pitch); val r = Math.toRadians(roll)
            val right = SpaceCompassSunVector(cos(r),-sin(r)*sin(p),sin(r)*cos(p))
            val up = SpaceCompassSunVector(-sin(r),-cos(r)*sin(p),cos(r)*cos(p))
            val orientation = SpaceCompassSunOrientation(right,up,SpaceCompassSunVector(0.0,cos(p),sin(p)))
            val ground = projectSpaceCompassSunGround(orientation,frame,400.0,900.0,perspective)!!
            assertEquals(2,ground.horizon.size)
            for (point in ground.horizon) {
                val ray = orientation.forward.up + orientation.right.up*(point.x-200)/perspective.focalX(380.0) -
                    orientation.screenUp.up*(point.y-350)/perspective.focalY(600.0)
                assertEquals(0.0,ray,1e-8)
            }
        }
    }
    @Test fun invalidLensCannotInventCameraAlignment() {
        val frame = SpaceCompassSunSceneFrame(0.0,0.0,400.0,600.0)
        assertNull(spaceCompassCameraGeometry(lens.copy(focalX=Double.NaN),1280.0,960.0,0,400.0,600.0,frame))
        assertNull(spaceCompassCameraGeometry(lens,1280.0,960.0,45,400.0,600.0,frame))
        assertNull(spaceCompassCameraGeometry(lens,0.0,960.0,0,400.0,600.0,frame))
        assertNull(spaceCompassCameraGeometry(lens.copy(centerX=5000.0),1280.0,960.0,0,400.0,600.0,frame))
    }
}

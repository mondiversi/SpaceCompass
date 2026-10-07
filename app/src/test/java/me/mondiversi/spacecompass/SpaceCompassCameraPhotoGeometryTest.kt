package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassCameraPhotoGeometryTest {
    private val lens=SpaceCompassCameraLens(0.0,0.0,4000.0,3000.0,3000.0,3000.0,2000.0,1500.0,90)
    private val north=SpaceCompassSunOrientation(SpaceCompassSunVector(1.0,0.0,0.0),
        SpaceCompassSunVector(0.0,0.0,1.0),SpaceCompassSunVector(0.0,1.0,0.0))
    @Test fun photographKeepsTheEntireLensFieldHiddenByTheLandscapeDataColumn() {
        val camera=lens.copy(sensorRotation=0)
        val main=spaceCompassCameraGeometry(camera,1280.0,960.0,0,900.0,600.0,
            SpaceCompassSunSceneFrame(0.0,0.0,400.0,600.0))!!
        val photo=spaceCompassCameraPhotoGeometry(camera,1280,960,0)
        val point=SpaceCompassSunPosition(20.0,0.0)
        assertFalse(projectSpaceCompassSun(point,north,400.0,600.0,main.perspective).visible)
        val captured=projectSpaceCompassSun(point,north,photo.width.toDouble(),photo.height.toDouble(),photo.perspective)
        assertTrue(captured.visible)
        assertEquals(640+960*tan(Math.toRadians(20.0)),captured.x,1e-8)
        assertEquals(1280,photo.width);assertEquals(960,photo.height)
    }
    @Test fun photographRotationPreservesAllPixelsAndFocalAxes() {
        for(sensor in listOf(0,90,180,270)) for(display in listOf(0,90,180,270)) {
            val photo=spaceCompassCameraPhotoGeometry(lens.copy(sensorRotation=sensor),1600,1200,display)
            assertEquals(1600*1200,photo.width*photo.height)
            assertEquals((sensor-display+360)%360,photo.rotation)
            assertEquals(1200.0,photo.perspective.focalX(photo.width.toDouble()),1e-8)
            assertEquals(1200.0,photo.perspective.focalY(photo.height.toDouble()),1e-8)
        }
    }
    @Test fun opticalCentreIsKeptAtItsRealPhotographPixelRatherThanSoftwareRecentred() {
        val photo=spaceCompassCameraPhotoGeometry(lens.copy(centerX=2100.0,centerY=1400.0),1600,1200,0)
        val point=projectSpaceCompassSun(SpaceCompassSunPosition(0.0,0.0),north,
            photo.width.toDouble(),photo.height.toDouble(),photo.perspective)
        assertEquals(640.0,point.x,1e-8);assertEquals(840.0,point.y,1e-8)
        val segment=projectSpaceCompassSunPathSegment(north.forward,north.forward,north,
            photo.width.toDouble(),photo.height.toDouble(),false,photo.perspective)!!
        assertEquals(point.x,segment.start.x,1e-8);assertEquals(point.y,segment.start.y,1e-8)
        val horizon=projectSpaceCompassSunGround(north,SpaceCompassSunSceneFrame(0.0,0.0,
            photo.width.toDouble(),photo.height.toDouble()),photo.width.toDouble(),photo.height.toDouble(),photo.perspective)!!
        assertTrue(horizon.horizon.all { abs(it.y-840)<1e-8 })
    }
    @Test fun exposureUsesNearestDeviceAttitudeAndRejectsAStaleSensor() {
        val history=SpaceCompassCameraAttitudeHistory()
        history.add(SpaceCompassCameraAttitude(1_000_000_000,north,true))
        val changed=north.copy(forward=SpaceCompassSunVector(1.0,0.0,0.0))
        history.add(SpaceCompassCameraAttitude(1_080_000_000,changed,false))
        assertEquals(north,history.at(1_010_000_000,0)!!.orientation)
        assertEquals(changed,history.at(1_070_000_000,0)!!.orientation)
        assertFalse(history.at(1_070_000_000,0)!!.usable)
        assertNull(history.at(2_000_000_000,0))
    }
    @Test fun physicalDeviceAttitudeCanBeRemappedAfterDisplayRotationAtCapture() {
        val landscape=spaceCompassCameraScreenOrientation(north,90)
        assertEquals(north.forward,landscape.forward)
        assertEquals(-north.screenUp.up,landscape.right.up,0.0)
        assertEquals(1.0,landscape.screenUp.east,0.0)
        assertEquals(0.0,landscape.screenUp.north,0.0)
        val restored=spaceCompassCameraScreenOrientation(landscape,270)
        for ((expected,actual) in listOf(north.right to restored.right,north.screenUp to restored.screenUp,north.forward to restored.forward)) {
            assertEquals(expected.east,actual.east,0.0);assertEquals(expected.north,actual.north,0.0);assertEquals(expected.up,actual.up,0.0)
        }
    }
    @Test fun photographedElevationAndBearingFollowTheUprightImageInBothLandscapeDirections() {
        // Physically upright landscape phones looking north: Android device axes
        // stay fixed to the handset while display X is east and display Y is up.
        val east = SpaceCompassSunVector(1.0, 0.0, 0.0)
        val west = SpaceCompassSunVector(-1.0, 0.0, 0.0)
        val up = SpaceCompassSunVector(0.0, 0.0, 1.0)
        val down = SpaceCompassSunVector(0.0, 0.0, -1.0)
        val devices = listOf(90 to SpaceCompassSunOrientation(up, west, north.forward),
            270 to SpaceCompassSunOrientation(down, east, north.forward))
        val perspective = SpaceCompassPerspective(.8, .8)
        for ((rotation, device) in devices) {
            val history = SpaceCompassCameraAttitudeHistory()
            history.add(SpaceCompassCameraAttitude(1_000_000_000, device, true))
            val exposure = history.at(1_000_000_000, rotation)!!.orientation
            for ((expected, actual) in listOf(east to exposure.right, up to exposure.screenUp)) {
                assertEquals(expected.east, actual.east, 1e-12)
                assertEquals(expected.north, actual.north, 1e-12)
                assertEquals(expected.up, actual.up, 1e-12)
            }
            val above = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 20.0), exposure, 1200.0, 900.0, perspective)
            val below = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, -20.0), exposure, 1200.0, 900.0, perspective)
            val right = projectSpaceCompassSun(SpaceCompassSunPosition(20.0, 0.0), exposure, 1200.0, 900.0, perspective)
            val left = projectSpaceCompassSun(SpaceCompassSunPosition(340.0, 0.0), exposure, 1200.0, 900.0, perspective)
            assertTrue(above.visible && above.y < 450)
            assertTrue(below.visible && below.y > 450)
            assertTrue(right.visible && right.x > 600)
            assertTrue(left.visible && left.x < 600)
        }
    }
    @Test fun timestampHistoryIsBoundedAndDoesNotAcceptOutOfOrderMeasurements() {
        val history=SpaceCompassCameraAttitudeHistory()
        for(i in 1..100) history.add(SpaceCompassCameraAttitude(i*40_000_000L,north,true))
        history.add(SpaceCompassCameraAttitude(2_000_000_000,north,false))
        assertTrue(history.at(4_000_000_000,0)!!.usable)
        assertNull(history.at(40_000_000,0))
    }
    @Test fun portraitPanoramaStartsFilledAndCanBeDraggedAcrossTheEntireWidth() {
        val factor=spaceCompassCaptureBaseScale(400f,800f,4096,2192)
        val fit=min(400f/4096,800f/2192)
        assertEquals(800f,2192*fit*factor,.001f)
        assertTrue(4096*fit*factor>400)
        val maxPan=(4096*fit*factor-400)/2
        assertTrue(maxPan>0)
        assertEquals(1f,spaceCompassCaptureBaseScale(800f,400f,4096,2192),0f)
    }
    @Test fun portraitPhotoAlsoFillsWithoutChangingTheExportPixels() {
        val factor=spaceCompassCaptureBaseScale(400f,800f,1200,1600)
        assertEquals(1.5f,factor,1e-6f)
        assertEquals(1f,spaceCompassCaptureBaseScale(0f,800f,1200,1600),0f)
    }
}

package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSceneOpticsTest {
    private val facingNorth = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, 1.0, 0.0))

    @Test fun visibleSkyCentreFollowsTheToolbarInPortraitLandscapeAndCollapsedPanels() {
        for ((width, height, header) in listOf(1046.0 to 1162.0, 1090.0 to 737.0, 1046.0 to 1967.0)
            .map { Triple(it.first, it.second, 162.0) }) {
            val p = spaceCompassSceneDrawingPerspective(width, height, header, null)!!
            val aimed = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 0.0), facingNorth, width, height, p)
            assertEquals(width / 2, aimed.x, 1e-8)
            assertEquals((header + height) / 2, aimed.y, 1e-8)
            assertEquals(aimed.y - header, height - aimed.y, 1e-8)
        }
    }

    @Test fun starsGroundAndObjectsKeepTheSameRayAndHorizonAfterRecentering() {
        val full = SpaceCompassSunSceneFrame(10.0, 117.0, 1046.0, 1162.0)
        val optical = spaceCompassSceneOpticalFrame(full, 162.0)
        val perspective = spaceCompassSceneDrawingPerspective(full.width, full.height, 162.0, null)!!
        val projected = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 12.0), facingNorth,
            full.width, full.height, perspective)
        val ray = spaceCompassStarViewRay(full.left + projected.x, full.top + projected.y, facingNorth, optical)
        val expected = SpaceCompassSunVector(0.0, kotlin.math.cos(Math.toRadians(12.0)), kotlin.math.sin(Math.toRadians(12.0)))
        assertEquals(expected.east, ray.east, 1e-8)
        assertEquals(expected.north, ray.north, 1e-8)
        assertEquals(expected.up, ray.up, 1e-8)
        val ground = projectSpaceCompassSunGround(facingNorth, optical, 1080.0, 2000.0)!!
        val horizon = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 0.0), facingNorth,
            full.width, full.height, perspective)
        assertEquals(full.top + horizon.y, ground.horizonOrigin!!.y, 1e-8)
    }

    @Test fun realCameraPreviewAndProjectedObjectsShareTheRecentredOpticalAxisAtEveryRotation() {
        val full = SpaceCompassSunSceneFrame(10.0, 117.0, 1046.0, 1162.0)
        val optical = spaceCompassSceneOpticalFrame(full, 162.0)
        for (sensor in listOf(0, 90, 180, 270)) for (display in listOf(0, 90, 180, 270)) {
            val lens = SpaceCompassCameraLens(0.0, 0.0, 4000.0, 3000.0, 3000.0, 3000.0, 2000.0, 1500.0, sensor)
            val camera = spaceCompassCameraGeometry(lens, 1280.0, 960.0, display, 1080.0, 2000.0, optical)!!
            val perspective = spaceCompassSceneDrawingPerspective(full.width, full.height, 162.0, camera.perspective)!!
            val aimed = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 0.0), facingNorth,
                full.width, full.height, perspective)
            assertEquals(full.left + aimed.x, camera.destination.map { it.x }.average(), 1e-8)
            assertEquals(full.top + aimed.y, camera.destination.map { it.y }.average(), 1e-8)
            assertEquals(camera.perspective.focalY(optical.height), perspective.focalY(full.height), 1e-8)
        }
    }

    @Test fun focusedPointFollowsTheNewReticleRatherThanTheOldCanvasCentre() {
        val width = 400.0; val height = 600.0
        val p = spaceCompassSceneDrawingPerspective(width, height, 100.0, null)!!
        val current = SpaceCompassSunPathPoint(0L, SpaceCompassSunPosition(0.0, 0.0))
        val targets = projectSpaceCompassCelestialSceneTargets(emptyMap(), mapOf(SpaceCompassCelestialBody.SUN to current),
            facingNorth, width, height, p)
        assertNull(focusedSpaceCompassCelestialSceneTarget(targets, width, height, 19.0))
        assertEquals(current, focusedSpaceCompassCelestialSceneTarget(targets, width, height, 19.0, p)!!.point)
    }

    @Test fun absentOrOversizedToolbarNeverInventsAnInvalidProjectionFrame() {
        val frame = SpaceCompassSunSceneFrame(10.0, 20.0, 400.0, 600.0)
        for (inset in listOf(0.0, -10.0, Double.NaN)) assertEquals(frame, spaceCompassSceneOpticalFrame(frame, inset))
        assertNull(spaceCompassSceneDrawingPerspective(400.0, 600.0, 0.0, null))
        val inset = spaceCompassSceneOpticalFrame(frame, 1000.0)
        assertEquals(1.0, inset.height, 0.0)
        assertEquals(frame.top + frame.height, inset.top + inset.height, 0.0)
        val p = spaceCompassSceneDrawingPerspective(400.0, 600.0, 1000.0, null)!!
        assertTrue(p.horizontal.isFinite() && p.vertical.isFinite() && p.principalY in 0.0..1.0)
    }

    @Test fun fullScenePreservesTheFreeAreaReticleAndRaysInPortraitLandscapeAndRtl() {
        val fullWidth = 2280.0; val fullHeight = 2000.0
        for (viewport in listOf(SpaceCompassSunSceneFrame(10.0, 0.0, 1046.0, 1162.0),
            SpaceCompassSunSceneFrame(10.0, 0.0, 1090.0, 737.0),
            SpaceCompassSunSceneFrame(1180.0, 0.0, 1090.0, 737.0),
            SpaceCompassSunSceneFrame(10.0, 0.0, 1046.0, 1967.0))) {
            val optical = spaceCompassSceneOpticalFrame(viewport, 162.0)
            val local = spaceCompassSceneDrawingPerspective(viewport.width, viewport.height, 162.0, null)!!
            val extended = spaceCompassSceneFramePerspective(fullWidth, fullHeight, optical, null)!!
            for (position in listOf(SpaceCompassSunPosition(0.0, 0.0), SpaceCompassSunPosition(12.0, 18.0),
                SpaceCompassSunPosition(350.0, -10.0))) {
                val original = projectSpaceCompassSun(position, facingNorth, viewport.width, viewport.height, local)
                val background = projectSpaceCompassSun(position, facingNorth, fullWidth, fullHeight, extended)
                assertEquals(viewport.left + original.x, background.x, 1e-8)
                assertEquals(viewport.top + original.y, background.y, 1e-8)
            }
            val aim = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 0.0), facingNorth, fullWidth, fullHeight, extended)
            assertEquals(optical.left + optical.width / 2, aim.x, 1e-8)
            assertEquals(optical.top + optical.height / 2, aim.y, 1e-8)
        }
    }

    @Test fun objectsBeyondTheFreeAreaContinueUnderThePanelInsteadOfBeingClipped() {
        val viewport = SpaceCompassSunSceneFrame(10.0, 0.0, 400.0, 600.0)
        val optical = spaceCompassSceneOpticalFrame(viewport, 100.0)
        val camera = SpaceCompassPerspective(.5, .5)
        val local = spaceCompassSceneDrawingPerspective(viewport.width, viewport.height, 100.0, camera)!!
        val extended = spaceCompassSceneFramePerspective(430.0, 1100.0, optical, camera)!!
        val objectPosition = SpaceCompassSunPosition(0.0, -50.0)
        val original = projectSpaceCompassSun(objectPosition, facingNorth, viewport.width, viewport.height, local)
        val background = projectSpaceCompassSun(objectPosition, facingNorth, 430.0, 1100.0, extended)
        assertFalse(original.visible)
        assertTrue(background.visible)
        assertTrue(background.y > viewport.top + viewport.height)
        // Outside the old viewport, x/y describe its edge arrow, not the object's physical ray.
        val recovered = spaceCompassCameraPixelPosition(background.x, background.y, 430.0, 1100.0,
            facingNorth, extended)
        assertEquals(objectPosition.azimuthDegrees, recovered.azimuthDegrees, 1e-8)
        assertEquals(objectPosition.elevationDegrees, recovered.elevationDegrees, 1e-8)
    }

    @Test fun fullSceneKeepsTheCameraOpticalAxisAndZoomAtEverySensorAndDisplayRotation() {
        val viewport = SpaceCompassSunSceneFrame(10.0, 0.0, 1046.0, 1162.0)
        val optical = spaceCompassSceneOpticalFrame(viewport, 162.0)
        for (sensor in listOf(0, 90, 180, 270)) for (display in listOf(0, 90, 180, 270)) {
            val lens = SpaceCompassCameraLens(0.0, 0.0, 4000.0, 3000.0, 3000.0, 3000.0, 2000.0, 1500.0, sensor)
            val camera = spaceCompassCameraGeometry(lens, 1280.0, 960.0, display, 1080.0, 2000.0, optical)!!
            val extended = spaceCompassSceneFramePerspective(1080.0, 2000.0, optical, camera.perspective)!!
            val aimed = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 0.0), facingNorth, 1080.0, 2000.0, extended)
            assertEquals(camera.destination.map { it.x }.average(), aimed.x, 1e-8)
            assertEquals(camera.destination.map { it.y }.average(), aimed.y, 1e-8)
            assertEquals(camera.perspective.focalY(optical.height), extended.focalY(2000.0), 1e-8)
        }
    }
}

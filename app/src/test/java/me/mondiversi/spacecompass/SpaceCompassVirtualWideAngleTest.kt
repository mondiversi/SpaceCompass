package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.tan

class SpaceCompassVirtualWideAngleTest {
    private val north = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, 1.0, 0.0))

    @Test fun wideVirtualViewIncludesMoreSkyWhileCameraKeepsItsCalibratedField() {
        for ((width, height) in listOf(400.0 to 600.0, 900.0 to 400.0)) {
            val position = SpaceCompassSunPosition(0.0, 30.5)
            val wide = projectSpaceCompassSun(position, north, width, height)
            assertTrue(wide.visible)
            val cameraFocal = height / (2 * tan(Math.toRadians(30.0)))
            val camera = SpaceCompassPerspective(cameraFocal / width, cameraFocal / height)
            val calibrated = projectSpaceCompassSun(position, north, width, height, camera)
            assertFalse(calibrated.visible)
            val inside = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 20.0), north, width, height, camera)
            assertEquals(height / 2 - cameraFocal * tan(Math.toRadians(20.0)), inside.y, 1e-8)
        }
    }

    @Test fun starsAndOrbitTargetsShareWideAngleRaysIncludingTheNewlyVisibleSky() {
        for (frame in listOf(SpaceCompassSunSceneFrame(10.0, 30.0, 400.0, 600.0),
            SpaceCompassSunSceneFrame(10.0, 30.0, 900.0, 400.0))) {
            val point = SpaceCompassSunPathPoint(0L, SpaceCompassSunPosition(10.0, 30.5))
            val target = projectSpaceCompassCelestialSceneTargets(emptyMap(),
                mapOf(SpaceCompassCelestialBody.SUN to point), north, frame.width, frame.height).single()
            assertTrue(target.projection.x in 0.0..frame.width && target.projection.y in 0.0..frame.height)
            val ray = spaceCompassStarViewRay(frame.left + target.projection.x,
                frame.top + target.projection.y, north, frame)
            val az = Math.toRadians(point.position.azimuthDegrees)
            val el = Math.toRadians(point.position.elevationDegrees)
            assertEquals(kotlin.math.cos(el) * kotlin.math.sin(az), ray.east, 1e-8)
            assertEquals(kotlin.math.cos(el) * kotlin.math.cos(az), ray.north, 1e-8)
            assertEquals(kotlin.math.sin(el), ray.up, 1e-8)
        }
    }
}

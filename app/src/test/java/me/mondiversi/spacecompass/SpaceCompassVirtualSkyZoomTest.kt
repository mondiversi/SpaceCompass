package me.mondiversi.spacecompass

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassVirtualSkyZoomTest {
    private val facing = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, 1.0, 0.0))

    @Test fun skyAndAtlasKeepTheSameRayAcrossEveryZoomAndAspectRatio() {
        for ((width, height) in listOf(380.0 to 600.0, 600.0 to 280.0)) {
            val frame = SpaceCompassSunSceneFrame(20.0, 55.0, width, height)
            for (zoom in spaceCompassVirtualZoomRange.presets()) {
                val lens = spaceCompassVirtualPerspective(width, height, zoom)
                for (elevation in listOf(-12.0, 0.0, 15.0)) {
                    val position = SpaceCompassSunPosition(15.0, elevation)
                    val point = projectSpaceCompassSun(position, facing, width, height, lens)
                    assertTrue(point.visible)
                    val ray = spaceCompassStarViewRay(frame.left + point.x, frame.top + point.y, facing, frame, zoom)
                    val expected = SpaceCompassSunVector(cos(Math.toRadians(elevation)) * sin(Math.toRadians(15.0)),
                        cos(Math.toRadians(elevation)) * cos(Math.toRadians(15.0)), sin(Math.toRadians(elevation)))
                    assertEquals(1.0, ray.dot(expected), 1e-12)
                }
            }
        }
    }

    @Test fun wideningTheViewPreservesTheCentreAndHalvesOffAxisOffsets() {
        val standard = spaceCompassVirtualPerspective(400.0, 600.0, 1f)
        val wide = spaceCompassVirtualPerspective(400.0, 600.0, .5f)
        val a = projectSpaceCompassSun(SpaceCompassSunPosition(20.0, 10.0), facing, 400.0, 600.0, standard)
        val b = projectSpaceCompassSun(SpaceCompassSunPosition(20.0, 10.0), facing, 400.0, 600.0, wide)
        assertEquals((a.x - 200) / 2, b.x - 200, 1e-10)
        assertEquals((a.y - 300) / 2, b.y - 300, 1e-10)
        val centre = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 0.0), facing, 400.0, 600.0, wide)
        assertEquals(200.0, centre.x, 0.0); assertEquals(300.0, centre.y, 0.0)
    }

    @Test fun groundBoundaryAndCelestialHorizonCoincideForEachZoom() {
        val pitch = Math.toRadians(15.0)
        val tilted = facing.copy(screenUp = SpaceCompassSunVector(0.0, -sin(pitch), cos(pitch)),
            forward = SpaceCompassSunVector(0.0, cos(pitch), sin(pitch)))
        val frame = SpaceCompassSunSceneFrame(0.0, 0.0, 400.0, 600.0)
        for (zoom in spaceCompassVirtualZoomRange.presets()) {
            val lens = spaceCompassVirtualPerspective(frame.width, frame.height, zoom)
            val ground = requireNotNull(projectSpaceCompassSunGround(tilted, frame, 400.0, 600.0, lens))
            val horizon = requireNotNull(ground.horizonOrigin)
            val objectOnHorizon = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, 0.0), tilted, 400.0, 600.0, lens)
            assertEquals(horizon.y, objectOnHorizon.y, 1e-9)
        }
    }
}

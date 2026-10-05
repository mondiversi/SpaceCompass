package me.mondiversi.spacecompass

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSunGroundProjectionTest {
    private fun facing(tilt: Double = 0.0, roll: Double = 0.0): SpaceCompassSunOrientation {
        val pitch = Math.toRadians(tilt); val rotation = Math.toRadians(roll)
        val right = SpaceCompassSunVector(1.0, 0.0, 0.0)
        val up = SpaceCompassSunVector(0.0, -sin(pitch), cos(pitch))
        fun blend(a: SpaceCompassSunVector, b: SpaceCompassSunVector, x: Double, y: Double) =
            SpaceCompassSunVector(a.east * x + b.east * y, a.north * x + b.north * y, a.up * x + b.up * y)
        return SpaceCompassSunOrientation(blend(right, up, cos(rotation), sin(rotation)),
            blend(up, right, cos(rotation), -sin(rotation)), SpaceCompassSunVector(0.0, cos(pitch), sin(pitch)))
    }
    private fun contains(polygon: List<SpaceCompassSunScenePoint>, point: SpaceCompassSunScenePoint): Boolean {
        if (polygon.size < 3) return false
        val crosses = polygon.indices.map { index ->
            val a = polygon[index]; val b = polygon[(index + 1) % polygon.size]
            (b.x - a.x) * (point.y - a.y) - (b.y - a.y) * (point.x - a.x)
        }
        return crosses.all { it >= -1e-6 } || crosses.all { it <= 1e-6 }
    }

    @Test fun levelHorizonUsesThePointingViewportCenterNotTheWholeScreenCenter() {
        val result = projectSpaceCompassSunGround(facing(), SpaceCompassSunSceneFrame(10.0, 50.0, 380.0, 600.0), 400.0, 900.0)!!
        assertEquals(2, result.horizon.size)
        result.horizon.forEach { assertEquals(350.0, it.y, 1e-8) }
        assertTrue(contains(result.ground, SpaceCompassSunScenePoint(200.0, 800.0)))
        assertFalse(contains(result.ground, SpaceCompassSunScenePoint(200.0, 100.0)))
    }

    @Test fun upwardAndDownwardPitchMoveTheHorizonInOppositeDirections() {
        val frame = SpaceCompassSunSceneFrame(0.0, 0.0, 400.0, 600.0)
        for (tilt in listOf(-20.0, 0.0, 20.0)) {
            val result = projectSpaceCompassSunGround(facing(tilt), frame, 400.0, 600.0)!!
            val expected = 300 + spaceCompassSunProjectionFocalLength(600.0) * tan(Math.toRadians(tilt))
            result.horizon.forEach { assertEquals(expected, it.y, 1e-8) }
        }
    }

    @Test fun straightUpIsAllSkyAndStraightDownIsAllGroundWithoutASingularLine() {
        val frame = SpaceCompassSunSceneFrame(0.0, 0.0, 400.0, 600.0)
        val up = projectSpaceCompassSunGround(facing(90.0), frame, 400.0, 600.0)!!
        assertTrue(up.ground.isEmpty()); assertTrue(up.horizon.isEmpty())
        val down = projectSpaceCompassSunGround(facing(-90.0), frame, 400.0, 600.0)!!
        assertEquals(4, down.ground.size); assertTrue(down.horizon.isEmpty())
        assertTrue(contains(down.ground, SpaceCompassSunScenePoint(200.0, 300.0)))
    }

    @Test fun rollRotatesTheGroundIncludingSidewaysAndUpsideDown() {
        val frame = SpaceCompassSunSceneFrame(0.0, 0.0, 400.0, 600.0)
        val sideways = projectSpaceCompassSunGround(facing(0.0, 90.0), frame, 400.0, 600.0)!!
        sideways.horizon.forEach { assertEquals(200.0, it.x, 1e-8) }
        assertTrue(contains(sideways.ground, SpaceCompassSunScenePoint(20.0, 300.0)))
        assertFalse(contains(sideways.ground, SpaceCompassSunScenePoint(380.0, 300.0)))
        val inverted = projectSpaceCompassSunGround(facing(0.0, 180.0), frame, 400.0, 600.0)!!
        assertTrue(contains(inverted.ground, SpaceCompassSunScenePoint(200.0, 20.0)))
        assertFalse(contains(inverted.ground, SpaceCompassSunScenePoint(200.0, 580.0)))
    }

    @Test fun groundAndSunShareTheSameProjectionWithPortraitLandscapeAndPanelOffsets() {
        for (frame in listOf(SpaceCompassSunSceneFrame(10.0, 60.0, 380.0, 600.0),
            SpaceCompassSunSceneFrame(20.0, 55.0, 600.0, 280.0), SpaceCompassSunSceneFrame(0.0, 0.0, 20.0, 20.0))) {
            for (roll in listOf(-60.0, 0.0, 45.0, 90.0, 180.0)) for (elevation in listOf(-15.0, 15.0)) {
                val orientation = facing(0.0, roll)
                val sun = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, elevation), orientation, frame.width, frame.height)
                assertTrue(sun.visible)
                val ground = projectSpaceCompassSunGround(orientation, frame, frame.left + frame.width + 10,
                    frame.top + frame.height + 180)!!
                assertEquals(elevation < 0, contains(ground.ground,
                    SpaceCompassSunScenePoint(frame.left + sun.x, frame.top + sun.y)))
            }
        }
    }

    @Test fun negativeSunIsStillProjectableAtTheReticleWithinTheGround() {
        val frame = SpaceCompassSunSceneFrame(0.0, 0.0, 400.0, 600.0)
        val orientation = facing(-40.0)
        val sun = projectSpaceCompassSun(SpaceCompassSunPosition(0.0, -40.0), orientation, frame.width, frame.height)
        assertTrue(sun.visible)
        val ground = projectSpaceCompassSunGround(orientation, frame, 400.0, 600.0)!!
        assertTrue(contains(ground.ground, SpaceCompassSunScenePoint(sun.x, sun.y)))
    }

    @Test fun arbitraryAnglesAlwaysReturnClippedFiniteGroundAndHorizonVertices() {
        for (tilt in -90..90 step 5) for (roll in 0..360 step 15) {
            val projection = projectSpaceCompassSunGround(facing(tilt.toDouble(), roll.toDouble()),
                SpaceCompassSunSceneFrame(10.0, 60.0, 400.0, 300.0), 900.0, 500.0)!!
            (projection.ground + projection.horizon).forEach {
                assertTrue(it.x.isFinite() && it.x in -1e-8..900.00000001)
                assertTrue(it.y.isFinite() && it.y in -1e-8..500.00000001)
            }
            assertTrue(projection.horizon.size <= 2)
        }
    }

    @Test fun invalidOrientationOrDimensionsNeverDrawPlausibleGround() {
        assertNull(projectSpaceCompassSunGround(facing(), SpaceCompassSunSceneFrame(0.0, 0.0, 0.0, 600.0), 400.0, 600.0))
        assertNull(projectSpaceCompassSunGround(facing(), SpaceCompassSunSceneFrame(Double.NaN, 0.0, 400.0, 600.0), 400.0, 600.0))
        val invalid = facing().copy(forward = SpaceCompassSunVector(0.0, 0.0, Double.NaN))
        assertNull(projectSpaceCompassSunGround(invalid, SpaceCompassSunSceneFrame(0.0, 0.0, 400.0, 600.0), 400.0, 600.0))
    }

    @Test fun depthOriginAlwaysLiesOnTheRealHorizonEvenOutsideTheScreen() {
        val frame = SpaceCompassSunSceneFrame(10.0, 50.0, 400.0, 600.0)
        for (pitch in listOf(-89.0, -70.0, -20.0, 0.0, 20.0, 70.0, 89.0)) for (roll in 0..360 step 15) {
            val orientation = facing(pitch, roll.toDouble())
            val projection = projectSpaceCompassSunGround(orientation, frame, 900.0, 500.0)!!
            val origin = projection.horizonOrigin!!
            val focal = spaceCompassSunProjectionFocalLength(frame.height)
            fun up(point: SpaceCompassSunScenePoint) = orientation.forward.up +
                orientation.right.up * (point.x - frame.left - frame.width / 2) / focal -
                orientation.screenUp.up * (point.y - frame.top - frame.height / 2) / focal
            assertEquals(0.0, up(origin), 1e-9)
            assertTrue(projection.depthScale.isFinite() && projection.depthScale > 0)
            // The gradient depth parameter equals the negative world-up ray component.
            val near = SpaceCompassSunScenePoint(origin.x + projection.groundDirection.x * projection.depthScale,
                origin.y + projection.groundDirection.y * projection.depthScale)
            assertEquals(-1.0, up(near), 1e-9)
        }
        assertNull(projectSpaceCompassSunGround(facing(-90.0), frame, 400.0, 900.0)!!.horizonOrigin)
        assertNull(projectSpaceCompassSunGround(facing(90.0), frame, 400.0, 900.0)!!.horizonOrigin)
    }
}

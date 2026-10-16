package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassSunOrientationSmoothingTest {
    private fun facing(heading: Double): SpaceCompassSunOrientation {
        val a = Math.toRadians(heading)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a), -sin(a), 0.0),
            SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(sin(a), cos(a), 0.0))
    }

    @Test fun dampingDependsOnElapsedTimeRatherThanTheNumberOfCallbacks() {
        val one = 1 - spaceCompassSunSmoothingWeight(60_000_000)
        val thirds = (1 - spaceCompassSunSmoothingWeight(20_000_000)).pow(3)
        val irregular = listOf(10_000_000L, 17_000_000L, 33_000_000L)
            .fold(1.0) { remaining, elapsed -> remaining * (1 - spaceCompassSunSmoothingWeight(elapsed)) }
        assertEquals(one, thirds, 1e-12)
        assertEquals(one, irregular, 1e-12)
    }

    @Test fun fasterSamplesDoNotTripleMotionOrNoiseResponse() {
        fun settle(interval: Long): SpaceCompassSunOrientation {
            var pose = facing(0.0)
            repeat((120_000_000 / interval).toInt()) {
                pose = smoothSpaceCompassSunOrientation(pose, facing(10.0), interval)
            }
            return pose
        }
        assertEquals(settle(60_000_000).headingDegrees, settle(20_000_000).headingDegrees, .02)
        assertEquals(.35, spaceCompassSunSmoothingWeight(40_000_000), 1e-6)
        assertEquals(.2, spaceCompassSunSmoothingWeight(60_000_000, SPACE_COMPASS_SUN_GRAVITY_SMOOTHING_NS), 1e-6)
    }

    @Test fun northWrapAndRolledPosesRetainUnitPerpendicularAxes() {
        val target = facing(1.0)
        val rolled = target.copy(right = target.screenUp, screenUp = SpaceCompassSunVector(
            -target.right.east, -target.right.north, -target.right.up))
        val pose = smoothSpaceCompassSunOrientation(facing(359.0), rolled, 20_000_000)
        assertTrue(pose.headingDegrees > 359 || pose.headingDegrees < 1)
        for (axis in listOf(pose.right, pose.screenUp, pose.forward)) assertEquals(1.0, axis.dot(axis), 1e-9)
        assertEquals(0.0, pose.right.dot(pose.forward), 1e-9)
        assertEquals(0.0, pose.screenUp.dot(pose.forward), 1e-9)
        assertEquals(0.0, pose.screenUp.dot(pose.right), 1e-9)
    }

    @Test fun restartAndLargeTurnsNeverBlendStaleOrInvertedAxes() {
        val target = facing(170.0)
        assertEquals(target, smoothSpaceCompassSunOrientation(null, target, 20_000_000))
        assertEquals(target, smoothSpaceCompassSunOrientation(facing(0.0), target, 20_000_000))
        assertEquals(facing(10.0), smoothSpaceCompassSunOrientation(facing(0.0), facing(10.0), 300_000_000))
        val rolled = facing(0.0).let { it.copy(right = SpaceCompassSunVector(-1.0, 0.0, 0.0),
            screenUp = SpaceCompassSunVector(0.0, 0.0, -1.0)) }
        assertEquals(rolled, smoothSpaceCompassSunOrientation(facing(0.0), rolled, 20_000_000))
    }

    @Test fun outOfOrderOrDuplicateSamplesDoNotMoveTheView() {
        val previous = facing(0.0)
        for (elapsed in listOf(0L, -1L, -40_000_000L)) {
            assertEquals(previous, smoothSpaceCompassSunOrientation(previous, facing(10.0), elapsed))
        }
    }

    @Test fun locationRefreshRebasesNorthWithoutDroppingFilteredMotion() {
        val filtered = smoothSpaceCompassSunOrientation(facing(358.0), facing(1.0), 20_000_000)
        val rebased = rebaseSpaceCompassSunOrientation(filtered, 4.0)
        assertEquals(wrapSpaceCompassSunDegrees(filtered.headingDegrees + 4.0), rebased.headingDegrees, 1e-9)
        assertEquals(filtered.tiltDegrees, rebased.tiltDegrees, 1e-9)
        assertEquals(filtered, rebaseSpaceCompassSunOrientation(filtered, 0.0))
        assertEquals(0.0, rebased.right.dot(rebased.forward), 1e-9)
        assertEquals(1.0, rebased.screenUp.dot(rebased.screenUp), 1e-9)
    }
}

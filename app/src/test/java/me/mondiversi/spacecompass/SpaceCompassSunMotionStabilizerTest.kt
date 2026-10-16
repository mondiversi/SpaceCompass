package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassSunMotionStabilizerTest {
    private val step = 20_000_000L
    private val start = 1_000_000_000L
    private fun facing(degrees: Double): SpaceCompassSunOrientation {
        val a = Math.toRadians(degrees)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a), -sin(a), 0.0),
            SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(sin(a), cos(a), 0.0))
    }
    private fun gyro(filter: SpaceCompassSunMotionStabilizer, time: Long, yaw: Double = 0.0,
        reliable: Boolean = true) = filter.recordGyroscope(time, 0.0, -Math.toRadians(yaw), 0.0, reliable)
    private fun axesEqual(expected: SpaceCompassSunOrientation, actual: SpaceCompassSunOrientation) {
        for ((a,b) in listOf(expected.right to actual.right, expected.screenUp to actual.screenUp,
            expected.forward to actual.forward)) {
            assertEquals(a.east, b.east, 1e-10)
            assertEquals(a.north, b.north, 1e-10)
            assertEquals(a.up, b.up, 1e-10)
        }
    }
    private fun assertOrthonormal(pose: SpaceCompassSunOrientation) {
        for (axis in listOf(pose.right, pose.screenUp, pose.forward)) assertEquals(1.0, axis.dot(axis), 1e-10)
        assertEquals(0.0, pose.right.dot(pose.forward), 1e-10)
        assertEquals(0.0, pose.right.dot(pose.screenUp), 1e-10)
        assertEquals(0.0, pose.screenUp.dot(pose.forward), 1e-10)
    }

    @Test fun missingOrUnreliableGyroscopePreservesTheLegacyFilter() {
        val filter = SpaceCompassSunMotionStabilizer()
        val previous = facing(0.0);val current = facing(2.0)
        val expected = smoothSpaceCompassSunOrientation(previous, current, step)
        axesEqual(expected, filter.smooth(previous, current, start, start + step, start + step))
        gyro(filter, start);gyro(filter, start + step, reliable = false)
        axesEqual(expected, filter.smooth(previous, current, start, start + step, start + step))
    }

    @Test fun normalAndSlowPansFollowGyroscopeWithoutWaitingForMagneticCorrection() {
        for (speed in listOf(.11, .5, 2.0, 45.0)) {
            val filter = SpaceCompassSunMotionStabilizer()
            var pose = facing(0.0)
            gyro(filter, start, speed)
            for (i in 1..100) {
                val time = start + i * step
                gyro(filter, time, speed)
                val current = facing(speed * i * step / 1_000_000_000.0)
                pose = filter.smooth(pose, current, time - step, time, time)
                axesEqual(current, pose)
            }
        }
    }

    @Test fun handheldMovementDoesNotDisableMagneticNoiseRejection() {
        val filter = SpaceCompassSunMotionStabilizer()
        var truePose = facing(30.0);var adaptive = truePose;var legacy = truePose
        val errors = mutableListOf<Double>();val oldErrors = mutableListOf<Double>()
        gyro(filter, start)
        for (i in 1..1000) {
            val time = start + i * step
            // 0.5–1.5 degrees/s repeatedly exceeded the old stillness thresholds on the phone.
            val speed = sin(i * .12) + .5
            val previousSpeed = if (i == 1) 0.0 else sin((i - 1) * .12) + .5
            truePose = advanceSpaceCompassSunOrientation(truePose, 0.0, -Math.toRadians(previousSpeed), 0.0, step)
            gyro(filter, time, speed)
            val magnetic = rebaseSpaceCompassSunOrientation(truePose, 1.5 * sin(i * .08))
            adaptive = filter.smooth(adaptive, magnetic, time - step, time, time)
            legacy = smoothSpaceCompassSunOrientation(legacy, magnetic, step)
            if (i > 500) {
                errors.add(adaptive.headingDegrees - truePose.headingDegrees)
                oldErrors.add(legacy.headingDegrees - truePose.headingDegrees)
            }
        }
        fun deviation(v: List<Double>) = sqrt(v.map { (it - v.average()).pow(2) }.average())
        assertTrue(deviation(errors) < deviation(oldErrors) * .5)
        assertOrthonormal(adaptive)
    }

    @Test fun stationaryMagneticJitterIsDampedAndNorthStillConverges() {
        val filter = SpaceCompassSunMotionStabilizer()
        var pose = facing(0.0)
        gyro(filter, start)
        for (i in 1..1000) {
            val time = start + i * step
            gyro(filter, time)
            pose = filter.smooth(pose, facing(10.0 + sin(i * .1)), time - step, time, time)
        }
        assertEquals(10.0, pose.headingDegrees, .15)
        assertOrthonormal(pose)
    }

    @Test fun staleGyroscopeAndLongAbsoluteGapsCannotPredictATrustedPose() {
        val filter = SpaceCompassSunMotionStabilizer()
        gyro(filter, start, 20.0)
        val before = facing(0.0);val current = facing(1.0)
        val to = start + 300_000_000
        axesEqual(current, filter.smooth(before, current, start, to, to))
        axesEqual(smoothSpaceCompassSunOrientation(before, current, step),
            filter.smooth(before, current, to - step, to, to))
        gyro(filter, to, 0.0)
        axesEqual(smoothSpaceCompassSunOrientation(before, current, step),
            filter.smooth(before, current, to - step, to, to))
    }

    @Test fun firstPoseResetDuplicatesAndOutOfOrderSamplesNeverInventMovement() {
        val filter = SpaceCompassSunMotionStabilizer()
        gyro(filter, start)
        gyro(filter, start, 180.0);gyro(filter, start - 1, 180.0)
        axesEqual(facing(0.0), filter.smooth(facing(0.0), facing(0.0), start, start + step, start + step))
        axesEqual(facing(4.0), filter.smooth(null, facing(4.0), start, start + step, start + step))
        axesEqual(facing(0.0), filter.smooth(facing(0.0), facing(3.0), start, start, start))
        filter.reset()
        axesEqual(smoothSpaceCompassSunOrientation(facing(0.0), facing(4.0), step),
            filter.smooth(facing(0.0), facing(4.0), start, start + step, start + step))
    }

    @Test fun invalidOrFutureGyroscopeSamplesFallBackSafely() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.MAX_VALUE)) {
            val filter = SpaceCompassSunMotionStabilizer()
            gyro(filter, start)
            filter.recordGyroscope(start + step, value, 0.0, 0.0, true)
            axesEqual(smoothSpaceCompassSunOrientation(facing(0.0), facing(4.0), step),
                filter.smooth(facing(0.0), facing(4.0), start, start + step, start + step))
        }
        val filter = SpaceCompassSunMotionStabilizer()
        gyro(filter, start + 2 * step, 90.0)
        axesEqual(smoothSpaceCompassSunOrientation(facing(0.0), facing(4.0), step),
            filter.smooth(facing(0.0), facing(4.0), start, start + step, start + step))
    }

    @Test fun gyroscopeIntervalsAreAppliedInOrderWithoutUsingFutureRates() {
        val filter = SpaceCompassSunMotionStabilizer()
        gyro(filter, start, 10.0);gyro(filter, start + step / 2, 30.0)
        gyro(filter, start + 2 * step, 180.0)
        val expected = facing(.4)
        axesEqual(expected, filter.smooth(facing(0.0), expected, start, start + step, start + 2 * step))
    }

    @Test fun physicalPitchRollAndYawPreserveOrthonormalAxes() {
        for ((x,y,z) in listOf(Triple(.4, 0.0, 0.0),Triple(0.0, .4, 0.0),Triple(0.0, 0.0, .4),Triple(.2, -.4, .1))) {
            var pose = facing(359.0)
            repeat(500) { pose = advanceSpaceCompassSunOrientation(pose, x, y, z, step) }
            assertOrthonormal(pose)
        }
        assertTrue(advanceSpaceCompassSunOrientation(facing(0.0), .4, 0.0, 0.0, step).tiltDegrees > 0)
        axesEqual(facing(.4 * .02 * 180 / PI),
            advanceSpaceCompassSunOrientation(facing(0.0), 0.0, -.4, 0.0, step))
    }

    @Test fun displayRotationCannotChangePhysicalMotionOrCaptureAxes() {
        for (rotation in 0..3) {
            val filter = SpaceCompassSunMotionStabilizer()
            gyro(filter, start, 10.0);gyro(filter, start + step, 10.0)
            val before = spaceCompassSceneRemapOrientation(facing(359.9), 0, rotation)
            val current = spaceCompassSceneRemapOrientation(facing(.1), 0, rotation)
            val live = filter.smooth(before, current, start, start + step, start + step, rotation)
            axesEqual(current, live)
            val physical = spaceCompassSceneRemapOrientation(live, rotation, 0)
            val history = SpaceCompassCameraAttitudeHistory()
            history.add(SpaceCompassCameraAttitude(start + step, physical, true))
            axesEqual(live, history.at(start + step, rotation * 90)!!.orientation)
        }
    }

    @Test fun fluidityChangesDoNotChangeTheIntegratedMotion() {
        for (rate in SpaceCompassSkyFluidity.entries) {
            val filter = SpaceCompassSunMotionStabilizer()
            val period = rate.sensorPeriodUs * 1_000L
            var pose = facing(0.0)
            gyro(filter, start, 30.0)
            for (i in 1..50) {
                val time = start + i * period
                gyro(filter, time, 30.0)
                val current = facing(i * period / 1_000_000_000.0 * 30)
                pose = filter.smooth(pose, current, time - period, time, time)
                axesEqual(current, pose)
            }
        }
    }

    @Test fun longRunningHistoryIsBoundedAndDoesNotKeepStaleRates() {
        val filter = SpaceCompassSunMotionStabilizer()
        for (i in 0..500) gyro(filter, start + i * step)
        axesEqual(smoothSpaceCompassSunOrientation(facing(0.0), facing(4.0), step),
            filter.smooth(facing(0.0), facing(4.0), start, start + step, start + 500 * step))
        axesEqual(facing(0.0), filter.smooth(facing(0.0), facing(0.0), start + 499 * step,
            start + 500 * step, start + 500 * step))
    }

    @Test fun magneticNorthContinuouslyCorrectsGyroscopeBiasInsteadOfAllowingFreeYawDrift() {
        val filter = SpaceCompassSunMotionStabilizer()
        var pose = facing(0.0)
        gyro(filter, start, .05)
        for (i in 1..2000) {
            val time = start + i * step
            gyro(filter, time, .05)
            pose = filter.smooth(pose, facing(0.0), time - step, time, time)
        }
        assertTrue(pose.headingDegrees in .05.. .15)
        assertOrthonormal(pose)
    }
}

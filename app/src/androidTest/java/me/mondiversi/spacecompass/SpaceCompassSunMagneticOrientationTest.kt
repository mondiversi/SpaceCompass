package me.mondiversi.spacecompass

import android.hardware.SensorManager
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

/** Real Android matrix/remapping APIs, but synthetic fields only: no hardware or user data. */
class SpaceCompassSunMagneticOrientationTest {
    private fun sum(a: SpaceCompassSunVector, b: SpaceCompassSunVector, x: Double, y: Double) =
        SpaceCompassSunVector(a.east * x + b.east * y, a.north * x + b.north * y, a.up * x + b.up * y)
    private fun negative(a: SpaceCompassSunVector) = sum(a, a, -1.0, 0.0)
    private fun pose(azimuth: Double, elevation: Double, roll: Double): SpaceCompassSunOrientation {
        val az = Math.toRadians(azimuth); val el = Math.toRadians(elevation); val r = Math.toRadians(roll)
        val right = SpaceCompassSunVector(cos(az), -sin(az), 0.0)
        val up = SpaceCompassSunVector(-sin(el) * sin(az), -sin(el) * cos(az), cos(el))
        return SpaceCompassSunOrientation(sum(right, up, cos(r), sin(r)), sum(up, right, cos(r), -sin(r)),
            SpaceCompassSunVector(cos(el) * sin(az), cos(el) * cos(az), sin(el)))
    }
    private fun assertVector(expected: SpaceCompassSunVector, actual: SpaceCompassSunVector) {
        assertEquals(expected.east, actual.east, 2e-6)
        assertEquals(expected.north, actual.north, 2e-6)
        assertEquals(expected.up, actual.up, 2e-6)
    }

    @Test fun knownMagneticNorthSurvivesTiltRollDisplayRotationAndTrueNorthCorrection() {
        val screenAxes = listOf(SensorManager.AXIS_X to SensorManager.AXIS_Y,
            SensorManager.AXIS_Y to SensorManager.AXIS_MINUS_X,
            SensorManager.AXIS_MINUS_X to SensorManager.AXIS_MINUS_Y,
            SensorManager.AXIS_MINUS_Y to SensorManager.AXIS_X)
        for (az in 0..315 step 45) for (el in listOf(-90.0, -45.0, 0.0, 45.0, 90.0))
            for (roll in listOf(0.0, 90.0, 180.0, 270.0)) {
                val physical = pose(az.toDouble(), el, roll)
                val axes = listOf(physical.right, physical.screenUp, negative(physical.forward))
                // Independent physical fields: gravity down-reference and northward geomagnetic field.
                val gravity = FloatArray(3) { (9.80665 * axes[it].up).toFloat() }
                val magnetic = FloatArray(3) { (25 * axes[it].north - 40 * axes[it].up).toFloat() }
                val raw = FloatArray(9)
                assertTrue(SensorManager.getRotationMatrix(raw, null, gravity, magnetic))
                // remap's X/Y describe OLD axes in the NEW frame (the inverse basis change).
                val expectedScreen = listOf(physical,
                    SpaceCompassSunOrientation(negative(physical.screenUp), physical.right, physical.forward),
                    SpaceCompassSunOrientation(negative(physical.right), negative(physical.screenUp), physical.forward),
                    SpaceCompassSunOrientation(physical.screenUp, negative(physical.right), physical.forward))
                for ((index, remap) in screenAxes.withIndex()) for (declination in listOf(-12.0, 0.0, 12.0)) {
                    val screen = FloatArray(9)
                    assertTrue(SensorManager.remapCoordinateSystem(raw, remap.first, remap.second, screen))
                    val actual = spaceCompassSunOrientationFromScreenMatrix(screen, declination)!!
                    val expected = expectedScreen[index]
                    assertVector(expected.right.trueNorth(declination), actual.right)
                    assertVector(expected.screenUp.trueNorth(declination), actual.screenUp)
                    assertVector(expected.forward.trueNorth(declination), actual.forward)
                    if (abs(el) < 89.0) {
                        val centered = projectSpaceCompassSun(SpaceCompassSunPosition(wrapSpaceCompassSunDegrees(az + declination), el),
                            actual, 1000.0, 1800.0)
                        assertTrue(centered.visible)
                        assertEquals(500.0, centered.x, 0.001)
                        assertEquals(900.0, centered.y, 0.001)
                    }
                }
            }
    }

    @Test fun contradictoryHandsetYawDoesNotReplaceTheMeasuredMagneticReference() {
        // Actual recorded technical samples (no location/identity). The fused yaw was 81 degrees off.
        val gravity = floatArrayOf(-0.1099432f, 0.017884128f, 9.806018f)
        val magnetic = floatArrayOf(-15.900001f, 24.50625f, -43.4625f)
        val quaternion = floatArrayOf(-0.0013550547f, 0.0054934113f, 0.39089963f, 0.92041594f)
        val direct = FloatArray(9)
        val fused = FloatArray(9)
        assertTrue(SensorManager.getRotationMatrix(direct, null, gravity, magnetic))
        SensorManager.getRotationMatrixFromVector(fused, quaternion)
        val directPose = spaceCompassSunOrientationFromScreenMatrix(direct, 0.0)!!
        val fusedPose = spaceCompassSunOrientationFromScreenMatrix(fused, 0.0)!!
        assertTrue(spaceCompassSunCompassHeadingDifference(directPose, fusedPose)!! > 70.0)
        val androidHeading = wrapSpaceCompassSunDegrees(Math.toDegrees(
            SensorManager.getOrientation(direct, FloatArray(3))[0].toDouble()))
        val topHeading = wrapSpaceCompassSunDegrees(Math.toDegrees(atan2(directPose.screenUp.east, directPose.screenUp.north)))
        assertEquals(androidHeading, topHeading, 1e-4)
        assertEquals(33.68, topHeading, 0.1)
        val field = sqrt(magnetic.sumOf { it.toDouble().pow(2) })
        assertNull(spaceCompassSunTrustedPointingOrientation(directPose,
            spaceCompassSunMagneticReferenceReliable(0, field, 48.0)))
    }
}

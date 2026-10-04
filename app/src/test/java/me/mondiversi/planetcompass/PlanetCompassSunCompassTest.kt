package me.mondiversi.planetcompass

import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassSunCompassTest {
    private fun facing(heading: Double, tilt: Double = 0.0, roll: Double = 0.0): PlanetCompassSunOrientation {
        val az = Math.toRadians(heading); val el = Math.toRadians(tilt); val r = Math.toRadians(roll)
        val right = PlanetCompassSunVector(cos(az), -sin(az), 0.0)
        val up = PlanetCompassSunVector(-sin(el) * sin(az), -sin(el) * cos(az), cos(el))
        fun mix(a: PlanetCompassSunVector, b: PlanetCompassSunVector, x: Double, y: Double) =
            PlanetCompassSunVector(a.east * x + b.east * y, a.north * x + b.north * y, a.up * x + b.up * y)
        return PlanetCompassSunOrientation(mix(right, up, cos(r), sin(r)), mix(up, right, cos(r), -sin(r)),
            planetCompassSunCompassDirection(heading, tilt))
    }

    @Test fun sphereUsesTheSameCenterAndAxesAsTheSolarView() {
        for (az in 0..315 step 45) for (tilt in listOf(-90.0, -45.0, 0.0, 45.0, 90.0))
            for (roll in listOf(0.0, 90.0, 180.0, 270.0)) {
                val orientation = facing(az.toDouble(), tilt, roll)
                val point = projectPlanetCompassSunCompass(orientation.forward, orientation)
                assertEquals(0.0, point.x, 1e-9); assertEquals(0.0, point.y, 1e-9)
                assertEquals(1.0, point.depth, 1e-9)
                val right = projectPlanetCompassSunCompass(orientation.right, orientation)
                assertEquals(1.0, right.x, 1e-9); assertEquals(0.0, right.y, 1e-9)
                val up = projectPlanetCompassSunCompass(orientation.screenUp, orientation)
                assertEquals(-1.0, up.y, 1e-9)
            }
    }

    @Test fun cardinalPointsAndGridRemainFiniteInsideTheSphereAtEveryPose() {
        val curves = PlanetCompassSunCompassParallels + PlanetCompassSunCompassMeridians + listOf(PlanetCompassSunCompassHorizon)
        for (az in 0..330 step 30) for (tilt in listOf(-90.0, 0.0, 90.0)) {
            val orientation = facing(az.toDouble(), tilt, 137.0)
            for (vector in curves.flatten()) {
                val point = projectPlanetCompassSunCompass(vector, orientation)
                assertTrue(point.x.isFinite() && point.y.isFinite() && point.depth.isFinite())
                assertTrue(point.x * point.x + point.y * point.y <= 1.0000001)
                assertEquals(1.0, point.x.pow(2) + point.y.pow(2) + point.depth.pow(2), 1e-8)
            }
        }
    }

    @Test fun horizontalBearingIsNotInventedWhilePointingStraightUpOrDown() {
        assertNull(planetCompassSunPointingHeading(null))
        for (tilt in listOf(-90.0, -89.5, 89.5, 90.0)) assertNull(planetCompassSunPointingHeading(facing(123.0, tilt)))
        assertEquals(123.0, planetCompassSunPointingHeading(facing(123.0, 80.0))!!, 1e-9)
    }

    @Test fun discrepancyDetectionWorksFlatVerticalRolledAndAcrossNorth() {
        for (tilt in listOf(-90.0, -89.5, -4.4, 0.0, 45.0, 90.0))
            for (roll in listOf(0.0, 90.0, 180.0)) {
                assertEquals(132.0, planetCompassSunCompassHeadingDifference(
                    facing(224.0, tilt, roll), facing(92.0, tilt, roll))!!, 1e-8)
                assertEquals(2.0, planetCompassSunCompassHeadingDifference(
                    facing(359.0, tilt, roll), facing(1.0, tilt, roll))!!, 1e-8)
            }
    }

    @Test fun northRequiresACalibratedPlausibleMagneticField() {
        for (accuracy in 0..1) assertFalse(planetCompassSunMagneticReferenceReliable(accuracy, 48.0, 48.0))
        for (accuracy in 2..3) {
            assertTrue(planetCompassSunMagneticReferenceReliable(accuracy, 48.0, 48.0))
            assertTrue(planetCompassSunMagneticReferenceReliable(accuracy, 48.0, null))
        }
        for (field in listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, 9.9, 100.1, 80.0))
            assertFalse(planetCompassSunMagneticReferenceReliable(3, field, 48.0))
        for (expected in listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -1.0))
            assertFalse(planetCompassSunMagneticReferenceReliable(3, 48.0, expected))
    }

    @Test fun unreliableNorthCannotPositionTheSunOrSupplyACardinalBearing() {
        val orientation = facing(224.0, -4.4, 180.0)
        assertNull(planetCompassSunTrustedPointingOrientation(orientation, false))
        assertNull(planetCompassSunTrustedPointingOrientation(null, true))
        assertSame(orientation, planetCompassSunTrustedPointingOrientation(orientation, true))
        assertNull(planetCompassSunPointingHeading(planetCompassSunTrustedPointingOrientation(orientation, false)))
        assertEquals(-4.4, orientation.tiltDegrees, 1e-9) // Gravity/horizon still works.
    }

    @Test fun screenMatrixUsesColumnsAndRearCameraMinusZWithDeclinationOnlyOnce() {
        val identity = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)
        val orientation = planetCompassSunOrientationFromScreenMatrix(identity, 12.0)!!
        assertEquals(sin(Math.toRadians(12.0)), orientation.screenUp.east, 1e-9)
        assertEquals(cos(Math.toRadians(12.0)), orientation.screenUp.north, 1e-9)
        assertEquals(-1.0, orientation.forward.up, 1e-9)
        assertNull(planetCompassSunOrientationFromScreenMatrix(FloatArray(8), 0.0))
        assertNull(planetCompassSunOrientationFromScreenMatrix(identity.copyOf().apply { this[1] = Float.NaN }, 0.0))
        assertNull(planetCompassSunOrientationFromScreenMatrix(identity, Double.NaN))
    }

    @Test fun badDataFailsImmediatelyAndRecoveryRequiresSustainedAgreement() {
        val recovery = PlanetCompassSunCompassRecovery()
        assertFalse(recovery.update(true, 1L))
        assertFalse(recovery.update(true, PLANET_COMPASS_SUN_COMPASS_RECOVERY_NS))
        assertTrue(recovery.update(true, PLANET_COMPASS_SUN_COMPASS_RECOVERY_NS + 1))
        assertFalse(recovery.update(false, PLANET_COMPASS_SUN_COMPASS_RECOVERY_NS + 2))
        assertFalse(recovery.update(true, PLANET_COMPASS_SUN_COMPASS_RECOVERY_NS + 3))
        assertTrue(recovery.update(true, PLANET_COMPASS_SUN_COMPASS_RECOVERY_NS * 2 + 3))
        recovery.reset()
        assertFalse(recovery.update(true, PLANET_COMPASS_SUN_COMPASS_RECOVERY_NS * 2 + 4))
    }
}

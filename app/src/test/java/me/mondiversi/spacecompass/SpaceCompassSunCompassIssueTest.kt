package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSunCompassIssueTest {
    private fun issue(accuracy: Int = 3, field: Double = 48.0, expected: Double? = 48.0,
        fresh: Boolean = true, recovered: Boolean = true) =
        spaceCompassSunCompassIssue(accuracy, field, expected, fresh, recovered)

    @Test fun uncalibratedSensorInAnOrdinaryFieldRequestsCalibrationInsteadOfMagnetRemoval() {
        assertEquals(SpaceCompassSunCompassIssue.CALIBRATION, issue(accuracy = 0))
        assertEquals(SpaceCompassSunCompassIssue.CALIBRATION, issue(accuracy = 0, recovered = false))
        assertFalse(spaceCompassSunMagneticReferenceUsable(0, 48.0, 48.0))
    }

    @Test fun magneticAnomaliesTakePrecedenceOverCalibrationWhenFreshSamplesExist() {
        for (accuracy in 0..3) for (field in listOf(9.0, 12.0, 90.0, 101.0)) {
            assertEquals(SpaceCompassSunCompassIssue.MAGNETIC_INTERFERENCE, issue(accuracy, field))
            assertFalse(spaceCompassSunMagneticReferenceUsable(accuracy, field, 48.0))
        }
    }

    @Test fun fieldCheckPreservesTheExistingLocalToleranceAndFallbackRange() {
        for (field in listOf(31.3, 48.0, 64.7)) assertEquals(SpaceCompassSunCompassIssue.NONE, issue(field = field))
        for (field in listOf(31.1, 64.9)) assertEquals(SpaceCompassSunCompassIssue.MAGNETIC_INTERFERENCE, issue(field = field))
        assertEquals(SpaceCompassSunCompassIssue.NONE, issue(field = 90.0, expected = null))
        assertEquals(SpaceCompassSunCompassIssue.MAGNETIC_INTERFERENCE, issue(field = 101.0, expected = null))
    }

    @Test fun staleIncompleteAndInvalidMeasurementsDoNotAccuseNearbyMagneticObjects() {
        for (accuracy in 0..3) for (field in listOf(48.0, 90.0))
            assertEquals(SpaceCompassSunCompassIssue.WAITING, issue(accuracy, field, fresh = false))
        for (invalid in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            assertEquals(SpaceCompassSunCompassIssue.WAITING, issue(field = invalid))
        for (invalid in listOf(Double.NaN, Double.POSITIVE_INFINITY, 0.0, -48.0))
            assertEquals(SpaceCompassSunCompassIssue.WAITING, issue(expected = invalid))
    }

    @Test fun recoveryWaitsForTheExistingStablePeriodWithoutAFalseMagneticAlarm() {
        val recovery = SpaceCompassSunCompassRecovery()
        assertEquals(SpaceCompassSunCompassIssue.WAITING, issue(recovered = recovery.update(true, 1L)))
        assertEquals(SpaceCompassSunCompassIssue.WAITING,
            issue(recovered = recovery.update(true, SPACE_COMPASS_SUN_COMPASS_RECOVERY_NS)))
        assertEquals(SpaceCompassSunCompassIssue.NONE,
            issue(recovered = recovery.update(true, SPACE_COMPASS_SUN_COMPASS_RECOVERY_NS + 1)))
        assertEquals(SpaceCompassSunCompassIssue.WAITING,
            issue(recovered = recovery.update(false, SPACE_COMPASS_SUN_COMPASS_RECOVERY_NS + 2)))
    }

    @Test fun approximatePointingRemainsDistinctFromCalibrationAndReliableNorth() {
        assertEquals(SpaceCompassSunCompassIssue.REDUCED_ACCURACY, issue(accuracy = 1))
        assertTrue(spaceCompassSunMagneticReferenceUsable(1, 48.0, 48.0))
        assertFalse(spaceCompassSunMagneticReferenceReliable(1, 48.0, 48.0))
        for (accuracy in 2..3) {
            assertEquals(SpaceCompassSunCompassIssue.NONE, issue(accuracy = accuracy))
            assertTrue(spaceCompassSunMagneticReferenceReliable(accuracy, 48.0, 48.0))
        }
    }
}

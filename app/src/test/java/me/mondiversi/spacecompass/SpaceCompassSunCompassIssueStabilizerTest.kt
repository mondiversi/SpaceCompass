package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSunCompassIssueStabilizerTest {
    private val anomaly = SpaceCompassSunCompassIssue.MAGNETIC_INTERFERENCE
    private val waiting = SpaceCompassSunCompassIssue.WAITING
    private val start = 1_000_000_000L
    private val step = 100_000_000L

    private fun sustain(filter: SpaceCompassSunCompassIssueStabilizer, from: Long, duration: Long) {
        var time = from
        while (time - from < duration) {
            assertEquals(waiting, filter.update(anomaly, time))
            time += step
        }
    }

    @Test fun continuousFreshAnomaliesRequireTwoSecondsBeforeWarning() {
        val filter = SpaceCompassSunCompassIssueStabilizer()
        sustain(filter, start, SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS)
        assertEquals(anomaly, filter.update(anomaly, start + SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS))
        assertEquals(anomaly, filter.update(anomaly, start + SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS + step))
    }

    @Test fun singleSpikeDisappearsWithoutAMagneticWarning() {
        val filter = SpaceCompassSunCompassIssueStabilizer()
        assertEquals(waiting, filter.update(anomaly, start))
        assertEquals(SpaceCompassSunCompassIssue.NONE, filter.update(SpaceCompassSunCompassIssue.NONE, start + step))
        sustain(filter, start + 2 * step, SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS)
    }

    @Test fun RepeatedCallbacksForTheSameSampleCannotConfirmInterference() {
        val filter = SpaceCompassSunCompassIssueStabilizer()
        repeat(100) { assertEquals(waiting, filter.update(anomaly, start)) }
        assertEquals(waiting, filter.update(anomaly, start + step))
    }

    @Test fun missingOrStaleSamplesRestartTheConfirmationPeriod() {
        val filter = SpaceCompassSunCompassIssueStabilizer()
        sustain(filter, start, 1_000_000_000L)
        val restart = start + 2_000_000_000L
        sustain(filter, restart, SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS)
        assertEquals(anomaly, filter.update(anomaly, restart + SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS))
        assertEquals(waiting, filter.update(anomaly, 0L))
        assertEquals(waiting, filter.update(anomaly, restart + SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS + step))
    }

    @Test fun timeRollbackAndLifecycleResetDiscardPreviouslyConfirmedAnomalies() {
        val filter = SpaceCompassSunCompassIssueStabilizer()
        sustain(filter, start, SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS)
        assertEquals(anomaly, filter.update(anomaly, start + SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS))
        assertEquals(waiting, filter.update(anomaly, start))
        sustain(filter, start, SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS)
        assertEquals(anomaly, filter.update(anomaly, start + SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS))
        filter.reset()
        assertEquals(waiting, filter.update(anomaly, start + SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS + step))
    }

    @Test fun calibrationReducedAccuracyAndIncompleteReadingsInterruptTheAnomaly() {
        for (issue in listOf(SpaceCompassSunCompassIssue.CALIBRATION,
            SpaceCompassSunCompassIssue.REDUCED_ACCURACY, waiting)) {
            val filter = SpaceCompassSunCompassIssueStabilizer()
            sustain(filter, start, 1_000_000_000L)
            assertEquals(issue, filter.update(issue, start + 1_000_000_000L))
            sustain(filter, start + 1_000_000_000L + step, SPACE_COMPASS_SUN_MAGNETIC_ANOMALY_CONFIRM_NS)
        }
    }

    @Test fun delayedNoticeNeverMakesAnAnomalousBearingUsable() {
        val filter = SpaceCompassSunCompassIssueStabilizer()
        for (accuracy in 0..3) {
            val issue = spaceCompassSunCompassIssue(accuracy, 90.0, 48.0, true, false)
            assertEquals(waiting, filter.update(issue, start))
            assertFalse(spaceCompassSunMagneticReferenceUsable(accuracy, 90.0, 48.0))
            assertFalse(spaceCompassSunMagneticReferenceReliable(accuracy, 90.0, 48.0))
        }
    }
}

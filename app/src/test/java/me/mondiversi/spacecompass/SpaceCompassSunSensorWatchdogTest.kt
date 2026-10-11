package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSunSensorWatchdogTest {
    private val second = 1_000_000_000L
    private val start = 10 * second

    @Test fun stoppingBothStreamsExpiresTrustBeforeTheFirstRestart() {
        val monitor = SpaceCompassSunSensorWatchdog()
        assertFalse(monitor.check(start, start, start).stale)
        val staleAt = start + second
        assertEquals(SpaceCompassSunSensorHealth(stale = true), monitor.check(staleAt, start, start))
        assertFalse(monitor.check(staleAt + 2 * second - 1, start, start).restart)
        assertTrue(monitor.check(staleAt + 2 * second, start, start).restart)
    }

    @Test fun eitherMissingStreamTriggersRecoveryEvenWhileTheOtherKeepsReporting() {
        for (missingMagnetic in listOf(true, false)) {
            val monitor = SpaceCompassSunSensorWatchdog()
            for (step in 0..8) {
                val now = start + step * 250_000_000L
                val state = monitor.check(now, if (missingMagnetic) now else 0, if (missingMagnetic) 0 else now)
                assertTrue(state.stale)
                assertEquals(step == 8, state.restart)
            }
        }
    }

    @Test fun freshTimestampsOnAStationaryPhoneDoNotTriggerRestarts() {
        val monitor = SpaceCompassSunSensorWatchdog()
        repeat(1_000) { step ->
            val now = start + step * 250_000_000L
            assertEquals(SpaceCompassSunSensorHealth(), monitor.check(now, now - 50_000_000L, now))
        }
    }

    @Test fun UnsynchronisedButLiveStreamsLoseTrustWithoutBeingRestarted() {
        val monitor = SpaceCompassSunSensorWatchdog()
        repeat(1_000) { step ->
            val now = start + step * 250_000_000L
            assertEquals(SpaceCompassSunSensorHealth(stale = true), monitor.check(now, now - 250_000_000L, now))
        }
    }

    @Test fun persistentFailureBacksOffToThirtySeconds() {
        val monitor = SpaceCompassSunSensorWatchdog()
        assertFalse(monitor.check(start, 0, 0).restart)
        for (offset in listOf(2L, 7L, 17L, 37L, 67L, 97L)) {
            val whenDue = start + offset * second
            assertFalse(monitor.check(whenDue - 1, 0, 0).restart)
            assertTrue(monitor.check(whenDue, 0, 0).restart)
        }
    }

    @Test fun pausesAndLifecycleResetsGiveTheResumedListenerAnAcquisitionGrace() {
        for (explicitReset in listOf(false, true)) {
            val monitor = SpaceCompassSunSensorWatchdog()
            monitor.check(start, 0, 0)
            if (explicitReset) monitor.reset()
            else assertEquals(SpaceCompassSunSensorHealth(), monitor.check(start + 100 * second, 0, 0, monitoring = false))
            val resume = start + 101 * second
            assertFalse(monitor.check(resume, start, start).restart)
            assertTrue(monitor.check(resume + 2 * second, start, start).restart)
        }
    }

    @Test fun briefSampleBurstsPreserveBackoffWhileStableRecoveryResetsIt() {
        val monitor = SpaceCompassSunSensorWatchdog()
        monitor.check(start, 0, 0)
        assertTrue(monitor.check(start + 2 * second, 0, 0).restart)
        val burst = start + 3 * second
        assertFalse(monitor.check(burst, burst, burst).stale)
        monitor.check(start + 4 * second, 0, 0)
        assertFalse(monitor.check(start + 6 * second, 0, 0).restart)
        assertTrue(monitor.check(start + 7 * second, 0, 0).restart)
        for (step in 0..8) {
            val now = start + 8 * second + step * 250_000_000L
            assertFalse(monitor.check(now, now, now).stale)
        }
        monitor.check(start + 11 * second, 0, 0)
        assertTrue(monitor.check(start + 13 * second, 0, 0).restart)
    }

    @Test fun sampleAgeBoundariesAndInvalidTimestampsNeverEstablishTrust() {
        assertTrue(spaceCompassSunSensorSamplesFresh(start, start - 500_000_000L, start - 300_000_000L))
        assertFalse(spaceCompassSunSensorSamplesFresh(start, start - 500_000_001L, start - 300_000_001L))
        assertFalse(spaceCompassSunSensorSamplesFresh(start, start, start + 1))
        assertFalse(spaceCompassSunSensorSamplesFresh(start, 0, start))
        assertFalse(spaceCompassSunSensorSamplesFresh(start, start, -1))
    }

    @Test fun monotonicClockRollbackDoesNotLeaveRecoveryWaitingForAnOldDeadline() {
        val monitor = SpaceCompassSunSensorWatchdog()
        monitor.check(start, 0, 0)
        assertTrue(monitor.check(start + 2 * second, 0, 0).restart)
        assertFalse(monitor.check(second, 0, 0).restart)
        assertTrue(monitor.check(3 * second, 0, 0).restart)
    }
}

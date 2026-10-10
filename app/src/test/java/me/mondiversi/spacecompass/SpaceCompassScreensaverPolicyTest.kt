package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScreensaverPolicyTest {
    private fun ready(seconds: Int = 300) = SpaceCompassScreensaverPolicy().apply {
        updateSettings(SpaceCompassScreensaverSettings(true, seconds), 0)
        setEligible(true, 0)
    }

    @Test fun newInstallIsEnabledAndWaitsFiveFullMinutes() {
        val policy = SpaceCompassScreensaverPolicy()
        policy.setEligible(true, 0)
        assertTrue(policy.settings.enabled)
        assertEquals(300, policy.settings.delaySeconds)
        assertFalse(policy.tick(299_999))
        assertTrue(policy.tick(300_000))
    }
    @Test fun activatesOnlyAfterTheSelectedFullInterval() {
        for (seconds in spaceCompassScreensaverDelays) {
            val policy = ready(seconds)
            assertFalse(policy.tick(seconds * 1_000L - 1))
            assertTrue(policy.tick(seconds * 1_000L))
        }
    }
    @Test fun everyInteractionRestartsTheDeadline() {
        val policy = ready(30)
        policy.interaction(29_000)
        assertFalse(policy.tick(30_000))
        assertFalse(policy.tick(58_999))
        assertTrue(policy.tick(59_000))
    }
    @Test fun aLongDragNeverActivatesUntilAfterReleaseAndAnotherFullInterval() {
        val policy = ready(30)
        policy.setTouching(true, 10_000)
        assertFalse(policy.tick(1_000_000))
        policy.setTouching(false, 1_000_000)
        assertFalse(policy.tick(1_029_999))
        assertTrue(policy.tick(1_030_000))
    }
    @Test fun menusKeyboardAndBackgroundSuspendIdleAndResumeWithAFreshInterval() {
        val policy = ready(30)
        policy.setEligible(false, 29_000)
        assertFalse(policy.tick(1_000_000))
        policy.setEligible(true, 1_000_000)
        assertFalse(policy.tick(1_029_999))
        assertTrue(policy.tick(1_030_000))
    }
    @Test fun rotationKeepsAnActiveSaverButBackgroundResetDoesNot() {
        val policy = ready(30)
        assertTrue(policy.tick(30_000))
        policy.setEligible(false, 31_000)
        assertTrue(policy.active)
        policy.setEligible(true, 32_000)
        assertTrue(policy.active)
        policy.reset(33_000)
        assertFalse(policy.active)
        assertEquals(30_000L, policy.remainingMillis(33_000))
    }
    @Test fun theFirstDownEventDoesNotDismissOrPassThroughTheCover() {
        val policy = ready(30)
        assertTrue(policy.tick(30_000))
        policy.setTouching(true, 31_000)
        policy.interaction(31_001)
        assertTrue(policy.active)
        policy.dismiss(31_200)
        assertFalse(policy.active)
        assertFalse(policy.tick(61_199))
        assertTrue(policy.tick(61_200))
    }
    @Test fun disablingImmediatelyDismissesAndRetainsTheChosenDelay() {
        val policy = ready(60)
        assertTrue(policy.tick(60_000))
        policy.updateSettings(SpaceCompassScreensaverSettings(false, 60), 61_000)
        assertFalse(policy.active)
        assertEquals(60, policy.settings.delaySeconds)
        assertFalse(policy.tick(1_000_000))
    }
    @Test fun changingTheDelayStartsANewFullIntervalAndInvalidChoicesUseDefault() {
        val policy = ready(30)
        policy.updateSettings(SpaceCompassScreensaverSettings(true, 300), 29_000)
        assertFalse(policy.tick(30_000))
        assertTrue(policy.tick(329_000))
        for (invalid in listOf(-1, 0, 1, 31, Int.MAX_VALUE))
            assertEquals(300, SpaceCompassScreensaverSettings(true, invalid).normalized().delaySeconds)
    }
    @Test fun aClockBeforeTheLastInteractionNeverProducesAnEarlyActivation() {
        val policy = ready(30)
        policy.interaction(100_000)
        assertEquals(30_000L, policy.remainingMillis(99_000))
        assertFalse(policy.tick(99_000))
    }
}

package me.mondiversi.planetcompass

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialRefreshPolicyTest {
    private val body = PlanetCompassCelestialBody.SEDNA
    private val pointing = PlanetCompassCelestialRequest(body)
    private val velocity = PlanetCompassCelestialRequest(body, true)
    private val now = 1_800_000_000_000L

    @Test fun cancelledDownloadDoesNotDelayReturningToTheObject() = runBlocking {
        val policy = PlanetCompassCelestialRefreshPolicy()
        try {
            policy.attempt(velocity, { now }) { throw CancellationException("Changed selection") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertEquals(0L, policy.nextAttempt(velocity, false, 0))
    }

    @Test fun onlyActualFailureStartsBackoffAndItDoesNotBlockPosition() = runBlocking {
        val policy = PlanetCompassCelestialRefreshPolicy()
        val failure = IllegalArgumentException("Invalid geometric velocity")
        assertSame(failure, policy.attempt(velocity, { now }) { throw failure }.exceptionOrNull())
        assertEquals(now + PLANET_COMPASS_CELESTIAL_RETRY_MS, policy.nextAttempt(velocity, false, 0))
        assertEquals(0L, policy.nextAttempt(pointing, false, 0))
        assertEquals(0L, policy.nextAttempt(PlanetCompassCelestialRequest(PlanetCompassCelestialBody.VOYAGER_1, true), false, 0))
    }

    @Test fun successfulRetryClearsFailureWithoutRefreshingTheOtherComponent() = runBlocking {
        val policy = PlanetCompassCelestialRefreshPolicy()
        policy.attempt(velocity, { now }) { error("Network unavailable") }
        assertTrue(policy.attempt(velocity, { now + PLANET_COMPASS_CELESTIAL_RETRY_MS }) {}.isSuccess)
        assertEquals(0L, policy.nextAttempt(velocity, false, 0))
        assertEquals(now + PLANET_COMPASS_CELESTIAL_REFRESH_MS, policy.nextAttempt(pointing, true, now))
    }

    @Test fun savedPositionAndCancelledSpeedOnlyRetryTheMissingSpeed() = runBlocking {
        val policy = PlanetCompassCelestialRefreshPolicy()
        var saved = false
        policy.attempt(pointing, { now }) { saved = true }
        try { policy.attempt(velocity, { now }) { throw CancellationException() } }
        catch (_: CancellationException) { }
        assertTrue(saved)
        assertEquals(now + PLANET_COMPASS_CELESTIAL_REFRESH_MS, policy.nextAttempt(pointing, saved, now))
        assertEquals(0L, policy.nextAttempt(velocity, false, 0))
    }

    @Test fun cancelledBlockingReadCannotOverlapTheNextJplRequest() = runBlocking {
        val policy = PlanetCompassCelestialRefreshPolicy()
        val release = CompletableDeferred<Unit>()
        var active = 0; var peak = 0; var secondStarted = false
        val first = launch(start = CoroutineStart.UNDISPATCHED) {
            policy.attempt(pointing, { now }) {
                active++; peak = maxOf(peak, active)
                try { withContext(NonCancellable) { release.await() } } finally { active-- }
            }
        }
        first.cancel()
        val second = launch(start = CoroutineStart.UNDISPATCHED) {
            policy.attempt(velocity, { now }) { secondStarted = true; active++; peak = maxOf(peak, active); active-- }
        }
        assertFalse(secondStarted)
        release.complete(Unit)
        first.join(); second.join()
        assertTrue(secondStarted)
        assertEquals(1, peak)
        assertEquals(0L, policy.nextAttempt(pointing, false, 0))
    }
}

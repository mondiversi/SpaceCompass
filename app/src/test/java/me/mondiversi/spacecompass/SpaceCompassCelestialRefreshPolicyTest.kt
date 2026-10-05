package me.mondiversi.spacecompass

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialRefreshPolicyTest {
    private val body = SpaceCompassCelestialBody.SEDNA
    private val pointing = SpaceCompassCelestialRequest(body)
    private val velocity = SpaceCompassCelestialRequest(body, true)
    private val now = 1_800_000_000_000L

    @Test fun cancelledDownloadDoesNotDelayReturningToTheObject() = runBlocking {
        val policy = SpaceCompassCelestialRefreshPolicy()
        try {
            policy.attempt(velocity, { now }) { throw CancellationException("Changed selection") }
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        assertEquals(0L, policy.nextAttempt(velocity, false, 0))
    }

    @Test fun onlyActualFailureStartsBackoffAndItDoesNotBlockPosition() = runBlocking {
        val policy = SpaceCompassCelestialRefreshPolicy()
        val failure = IllegalArgumentException("Invalid geometric velocity")
        assertSame(failure, policy.attempt(velocity, { now }) { throw failure }.exceptionOrNull())
        assertEquals(now + SPACE_COMPASS_CELESTIAL_RETRY_MS, policy.nextAttempt(velocity, false, 0))
        assertEquals(0L, policy.nextAttempt(pointing, false, 0))
        assertEquals(0L, policy.nextAttempt(SpaceCompassCelestialRequest(SpaceCompassCelestialBody.VOYAGER_1, true), false, 0))
    }

    @Test fun successfulRetryClearsFailureWithoutRefreshingTheOtherComponent() = runBlocking {
        val policy = SpaceCompassCelestialRefreshPolicy()
        policy.attempt(velocity, { now }) { error("Network unavailable") }
        assertTrue(policy.attempt(velocity, { now + SPACE_COMPASS_CELESTIAL_RETRY_MS }) {}.isSuccess)
        assertEquals(0L, policy.nextAttempt(velocity, false, 0))
        assertEquals(now + SPACE_COMPASS_CELESTIAL_REFRESH_MS, policy.nextAttempt(pointing, true, now))
    }

    @Test fun savedPositionAndCancelledSpeedOnlyRetryTheMissingSpeed() = runBlocking {
        val policy = SpaceCompassCelestialRefreshPolicy()
        var saved = false
        policy.attempt(pointing, { now }) { saved = true }
        try { policy.attempt(velocity, { now }) { throw CancellationException() } }
        catch (_: CancellationException) { }
        assertTrue(saved)
        assertEquals(now + SPACE_COMPASS_CELESTIAL_REFRESH_MS, policy.nextAttempt(pointing, saved, now))
        assertEquals(0L, policy.nextAttempt(velocity, false, 0))
    }

    @Test fun cancelledBlockingReadCannotOverlapTheNextJplRequest() = runBlocking {
        val policy = SpaceCompassCelestialRefreshPolicy()
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

    @Test fun satelliteTransportFailuresRetryAfterFiveMinutesWithoutDiscardingUsableCache() = runBlocking {
        val request = SpaceCompassCelestialRequest(SpaceCompassCelestialBody.STARLINK_V3)
        for (failure in listOf(java.net.SocketTimeoutException(), java.net.UnknownHostException(), java.net.ConnectException())) {
            val policy = SpaceCompassCelestialRefreshPolicy()
            assertTrue(policy.attempt(request, { now }) { throw failure }.isFailure)
            assertEquals(now + SPACE_COMPASS_CELESTIAL_RETRY_MS, policy.nextAttempt(request, false, 0))
            assertEquals(now + SPACE_COMPASS_CELESTIAL_REFRESH_MS, policy.nextAttempt(request, true, now))
        }
    }
    @Test fun satelliteHttpRejectionsStopAutomaticQueriesUntilInvestigated() = runBlocking {
        val request = SpaceCompassCelestialRequest(SpaceCompassCelestialBody.STARLINK_V3)
        for (status in listOf(301, 403, 404, 429, 500, 503)) {
            val policy = SpaceCompassCelestialRefreshPolicy()
            policy.attempt(request, { now }) { throw SpaceCompassCelestialHttpException(status) }
            assertEquals(Long.MAX_VALUE, policy.nextAttempt(request, false, 0))
        }
    }

    @Test fun newNetworkRetriesTransportOnlyAndPreservesHttpStopsAndCacheAge() = runBlocking {
        val policy = SpaceCompassCelestialRefreshPolicy()
        val satellite = SpaceCompassCelestialRequest(SpaceCompassCelestialBody.STARLINK_V3)
        val rejected = SpaceCompassCelestialRequest(SpaceCompassCelestialBody.ISS)
        policy.attempt(satellite, { now }) { throw java.net.SocketTimeoutException() }
        policy.attempt(rejected, { now }) { throw SpaceCompassCelestialHttpException(429) }
        policy.networkChanged()
        assertEquals(0L, policy.nextAttempt(satellite, false, 0))
        assertEquals(Long.MAX_VALUE, policy.nextAttempt(rejected, false, 0))
        assertEquals(now + SPACE_COMPASS_CELESTIAL_REFRESH_MS, policy.nextAttempt(satellite, true, now))
    }
}

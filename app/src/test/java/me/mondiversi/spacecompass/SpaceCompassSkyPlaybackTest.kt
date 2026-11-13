package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSkyPlaybackTest {
    private val now = Instant.parse("2026-11-07T12:00:00Z").toEpochMilli()

    @Test fun importedHoldResumesCustomForwardAndReverseRatesWithoutRewritingThem() {
        for (rate in listOf(-1296000, 10, 1296000)) {
            var elapsed = 0L
            val state = SpaceCompassObserverState(null, elapsedClock = { elapsed })
            val location = spaceCompassObserverPlan(45.0, 9.0, 100.0, now, "Europe/Rome")!!
            state.apply(location)
            val settings = SpaceCompassScenarioTimeSettings(true, rate, SpaceCompassScenarioResume.ACTIVE)
            state.configureTime(settings); state.active(true)
            state.restoreScenePause(now, now + 86_400_000L, elapsed)
            elapsed += 60_000L
            assertEquals(now, state.plan!!.scenarioClock!!.at(now + 86_460_000L, elapsed))
            state.togglePlayback(now + 86_460_000L, elapsed)
            elapsed += 1000L
            assertEquals(now + rate * 1000L, state.plan!!.scenarioClock!!.at(now + 86_461_000L, elapsed))
            assertEquals(settings, state.timeSettings)
            assertEquals(location, state.savedPlan)
        }
    }

    @Test fun importingAHoldReplacesAnEarlierTemporaryPause() {
        var elapsed = 0L
        val state = SpaceCompassObserverState(null, elapsedClock = { elapsed })
        state.active(true); state.togglePlayback(now, elapsed)
        elapsed += 5000L
        state.restoreScenePause(now + 86_400_000L, now + 5000L, elapsed)
        elapsed += 5000L
        assertTrue(state.playbackPaused)
        assertEquals(now + 86_400_000L, state.plan!!.scenarioClock!!.at(now + 10_000L, elapsed))
        state.togglePlayback(now + 10_000L, elapsed)
        assertFalse(state.playbackPaused)
        assertNull(state.plan)
    }

    @Test fun liveTimePausesAndResumesAtTheActualInstantWithoutChangingLocation() {
        var elapsed = 0L
        val state = SpaceCompassObserverState(null, elapsedClock = { elapsed })
        state.active(true)
        val settings = state.timeSettings
        state.togglePlayback(now, elapsed)
        assertTrue(state.playbackPaused)
        assertNull(state.savedPlan)
        assertEquals(0, state.plan.activeSectionCount)
        elapsed += 60_000
        assertEquals(now, state.plan!!.scenarioClock!!.at(now + 60_000, elapsed))
        assertTrue(spaceCompassShowScenarioMoment(state.plan, now, now, ZoneId.of("UTC"), state.playbackPaused))
        state.togglePlayback(now + 60_000, elapsed)
        assertFalse(state.playbackPaused)
        elapsed += 1000
        assertNull(state.plan) // Real time is supplied directly by the main screen again.
        assertEquals(settings, state.timeSettings)
        assertNull(state.savedPlan)
    }

    @Test fun acceleratedAndReversePlaybackRetainTheirConfiguredRate() {
        for (rate in listOf(-1296000, 10, 1296000)) {
            var elapsed = 0L
            val state = SpaceCompassObserverState(null, elapsedClock = { elapsed })
            val location = spaceCompassObserverPlan(45.0, 9.0, 100.0, now, "Europe/Rome")!!
            state.apply(location)
            state.configureTime(SpaceCompassScenarioTimeSettings(true, rate, SpaceCompassScenarioResume.ACTIVE))
            state.active(true)
            val wall = System.currentTimeMillis()
            val start = state.plan!!.scenarioClock!!.at(wall, elapsed)
            state.togglePlayback(wall, elapsed)
            elapsed += 60_000
            assertEquals(start, state.plan!!.scenarioClock!!.at(wall + 60_000, elapsed))
            state.togglePlayback(wall + 60_000, elapsed)
            elapsed += 1000
            assertEquals(start + rate * 1000L, state.plan!!.scenarioClock!!.at(wall + 61_000, elapsed))
            assertEquals(rate, state.timeSettings.rate)
            assertEquals(location, state.savedPlan)
        }
    }

    @Test fun anAlreadyPausedLocationCanPlayWithoutOverwritingItsZeroRate() {
        var elapsed = 0L
        val state = SpaceCompassObserverState(null, elapsedClock = { elapsed })
        state.configureTime(SpaceCompassScenarioTimeSettings(true, 0, SpaceCompassScenarioResume.ACTIVE))
        state.active(true)
        assertTrue(state.playbackPaused)
        val wall = System.currentTimeMillis()
        val start = state.plan!!.scenarioClock!!.at(wall, elapsed)
        state.togglePlayback(wall, elapsed)
        assertFalse(state.playbackPaused)
        elapsed += 1000
        assertEquals(start + 1000, state.plan!!.scenarioClock!!.at(wall + 1000, elapsed))
        assertEquals(0, state.timeSettings.rate)
    }

    @Test fun explicitLocationEditsReplaceTemporaryPlayback() {
        val state = SpaceCompassObserverState(null, elapsedClock = { 0L })
        state.togglePlayback(now, 0L)
        val selected = spaceCompassObserverPlan(0.0, 0.0, 10.0, now + 86_400_000, "UTC")!!
        state.apply(selected)
        assertFalse(state.playbackPaused)
        assertEquals(selected, state.plan)
        assertEquals(selected, state.savedPlan)
    }

    @Test fun preferenceReattachmentAndLifecycleChangesDoNotLoseThePausedSession() {
        val state = SpaceCompassObserverState(null, elapsedClock = { 0L })
        state.active(true)
        state.togglePlayback(now, 0L)
        val held = state.plan
        state.active(false); state.reload(); state.active(true)
        assertTrue(state.playbackPaused)
        assertSame(held, state.plan)
        assertEquals(now, state.plan!!.scenarioClock!!.at(now + 60_000, 60_000))
    }

    @Test fun spatialOverridesAloneResumeLiveTimeAndKeepTheirValues() {
        val state = SpaceCompassObserverState(null, elapsedClock = { 0L })
        val spatial = spaceCompassObserverPlan(28.0, -14.0, 100.0, now, "Atlantic/Canary",
            simulateDate = false, simulateTime = false)!!
        state.apply(spatial); state.active(true)
        state.togglePlayback(now, 0L)
        assertTrue(state.playbackPaused)
        state.togglePlayback(now + 60_000, 60_000L)
        assertFalse(state.playbackPaused)
        assertEquals(spatial, state.savedPlan)
        assertEquals(spatial, state.plan)
        assertNull(state.plan!!.scenarioClock)
        assertEquals(now + 60_000, state.plan!!.observationTimeMs(now + 60_000))
    }

    @Test fun normalOneTimesSpeedWithoutDateOrTimeAlsoReturnsToNow() {
        var elapsed = 0L
        val state = SpaceCompassObserverState(null, elapsedClock = { elapsed })
        state.configureTime(SpaceCompassScenarioTimeSettings(true, 1, SpaceCompassScenarioResume.ACTIVE))
        state.active(true)
        val wall = System.currentTimeMillis()
        state.togglePlayback(wall, elapsed)
        elapsed += 60_000
        state.togglePlayback(wall + 60_000, elapsed)
        assertEquals(wall + 60_000, state.plan!!.scenarioClock!!.at(wall + 60_000, elapsed))
        assertEquals(1, state.timeSettings.rate)
        assertTrue(state.timeSettings.enabled)
    }

    @Test fun aCustomSpeedAloneResumesFromTheHeldInstant() {
        for (rate in listOf(-10, 1296000)) {
            var elapsed = 0L
            val state = SpaceCompassObserverState(null, elapsedClock = { elapsed })
            state.configureTime(SpaceCompassScenarioTimeSettings(true, rate, SpaceCompassScenarioResume.ACTIVE))
            state.active(true)
            val wall = System.currentTimeMillis()
            val start = state.plan!!.scenarioClock!!.at(wall, elapsed)
            state.togglePlayback(wall, elapsed)
            elapsed += 60_000
            state.togglePlayback(wall + 60_000, elapsed)
            assertEquals(start, state.plan!!.scenarioClock!!.at(wall + 60_000, elapsed))
            elapsed += 1000
            assertEquals(start + rate * 1000L, state.plan!!.scenarioClock!!.at(wall + 61_000, elapsed))
        }
    }

    @Test fun pausedReadoutIsVisibleEvenTodayWithoutAnyLocationOverrides() {
        val zone = ZoneId.of("UTC")
        assertTrue(spaceCompassShowScenarioMoment(null, now, now, zone, paused = true))
        assertFalse(spaceCompassShowScenarioMoment(null, now, now, zone, paused = false))
        val state = SpaceCompassObserverState(null, elapsedClock = { 0L })
        state.active(true)
        state.togglePlayback(now, 0L)
        state.togglePlayback(now + 172_800_000L, 0L)
        assertFalse(state.playbackPaused)
        assertNull(state.plan)
        assertFalse(spaceCompassShowScenarioMoment(state.plan, now + 172_800_000L, now + 172_800_000L, zone))
    }
}

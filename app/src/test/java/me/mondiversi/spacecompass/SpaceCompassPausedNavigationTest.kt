package me.mondiversi.spacecompass

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPausedNavigationTest {
    private val now = Instant.parse("2026-11-25T12:00:00Z").toEpochMilli()
    private val moment = spaceCompassObserverPlan(45.0, 9.0, 100.0, now, "Europe/Rome")!!

    @Test fun pausedLiveTimeSurvivesRepeatedEditorReturnsWithAndWithoutPreferences() {
        for (preferences in listOf(null, store())) {
            var elapsed = 0L
            val state = SpaceCompassObserverState(preferences, elapsedClock = { elapsed })
            state.active(true); state.togglePlayback(now, elapsed)
            repeat(3) {
                val held = state.plan
                state.beginEditing(); elapsed += 60_000L; state.reload(); state.finishEditing()
                assertTrue(state.playbackPaused)
                assertSame(held, state.plan)
                assertEquals(now, state.plan!!.scenarioClock!!.at(now + elapsed, elapsed))
                assertNull(state.savedPlan)
            }
            state.togglePlayback(now + elapsed, elapsed)
            assertFalse(state.playbackPaused); assertNull(state.plan)
        }
    }

    @Test fun customForwardAndReverseHoldsKeepTheirInstantAndResumeTheirSavedRate() {
        for (rate in listOf(-3600, 1, 86400)) {
            var elapsed = 0L
            val state = SpaceCompassObserverState(store(), elapsedClock = { elapsed })
            state.apply(moment)
            val settings = SpaceCompassScenarioTimeSettings(true, rate, SpaceCompassScenarioResume.ACTIVE)
            state.configureTime(settings); state.active(true)
            state.togglePlayback(System.currentTimeMillis(), elapsed)
            val instant = state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed)
            state.beginEditing(); elapsed += 60_000L; state.finishEditing()
            assertTrue(state.playbackPaused)
            assertEquals(instant, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
            assertEquals(settings, state.timeSettings)
            state.togglePlayback(System.currentTimeMillis(), elapsed); elapsed += 1000L
            assertFalse(state.playbackPaused)
            assertEquals(instant + rate * 1000L, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
        }
    }

    @Test fun spatialEditsApplyWithoutAdvancingOrDiscardingThePausedInstant() {
        var elapsed = 0L
        val state = SpaceCompassObserverState(store(), elapsedClock = { elapsed })
        state.active(true); state.togglePlayback(now, elapsed)
        val spatial = moment.copy(latitude = -33.0, longitude = 151.0, simulateDate = false, simulateTime = false)
        val editor = state.beginEditing(); editor.apply(spatial)
        elapsed += 60_000L; state.finishEditing()
        assertTrue(state.playbackPaused)
        assertEquals(spatial, state.savedPlan)
        assertEquals(-33.0, state.plan!!.latitude, 0.0)
        assertEquals(now, state.plan!!.scenarioClock!!.at(now + elapsed, elapsed))
        state.togglePlayback(now + elapsed, elapsed)
        assertFalse(state.playbackPaused); assertEquals(spatial, state.plan)
    }

    @Test fun editedDateAndTimeStayPausedAtTheNewPreviewMoment() {
        var elapsed = 0L
        val state = SpaceCompassObserverState(store(), elapsedClock = { elapsed })
        state.apply(moment); state.configureTime(SpaceCompassScenarioTimeSettings(true, 60, SpaceCompassScenarioResume.ACTIVE))
        state.active(true); state.togglePlayback(System.currentTimeMillis(), elapsed)
        val editor = state.beginEditing()
        editor.apply(moment.copy(timeMs = now + 86_400_000L))
        val preview = editor.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed)
        elapsed += 60_000L; state.finishEditing()
        assertTrue(state.playbackPaused)
        assertEquals(preview, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
        state.togglePlayback(System.currentTimeMillis(), elapsed); elapsed += 1000L
        assertEquals(preview + 60_000L, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
    }

    @Test fun playingAConfiguredZeroRateDoesNotBecomePausedOnAnUneditedReturn() {
        var elapsed = 0L
        val state = SpaceCompassObserverState(store(), elapsedClock = { elapsed })
        state.configureTime(SpaceCompassScenarioTimeSettings(true, 0, SpaceCompassScenarioResume.ACTIVE))
        state.active(true); state.togglePlayback(System.currentTimeMillis(), elapsed)
        elapsed += 1000L
        val before = state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed)
        state.beginEditing(); elapsed += 60_000L; state.finishEditing()
        assertFalse(state.playbackPaused)
        elapsed += 1000L
        assertEquals(before + 1000L, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
        assertEquals(0, state.timeSettings.rate)
    }

    private fun store(): SharedPreferences {
        val values = mutableMapOf<String, Any?>(); val pending = mutableMapOf<String, Any?>()
        val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args -> when (method.name) {
            "putString", "putBoolean", "putInt" -> { pending[args!![0] as String] = args[1]; proxy }
            "remove" -> { pending[args!![0] as String] = null; proxy }
            "apply", "commit" -> { pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                pending.clear(); if (method.name == "commit") true else null }
            else -> error(method.name)
        } } as SharedPreferences.Editor
        return Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args -> when (method.name) {
            "getString", "getBoolean", "getInt" -> values[args!![0] as String] ?: args[1]
            "edit" -> editor
            else -> error(method.name)
        } } as SharedPreferences
    }
}

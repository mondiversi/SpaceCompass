package me.mondiversi.spacecompass

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassDeferredScenarioTest {
    private val original = spaceCompassObserverPlan(45.0, 9.0, 100.0,
        Instant.parse("2027-06-21T12:00:00Z").toEpochMilli(), "Europe/Rome")!!
    private val changed = spaceCompassObserverPlan(-33.0, 151.0, 250.0,
        Instant.parse("2028-12-21T05:30:00Z").toEpochMilli(), "Australia/Sydney")!!

    @Test fun everyConfiguredFieldAndTheClockWaitForTheMainView() {
        val store = Store(); var elapsed = 0L
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { elapsed })
        state.apply(original); state.active(true)
        val oldSettings = state.timeSettings
        val editor = state.beginEditing()
        assertSame(editor, state.beginEditing()) // Parameter navigation and rotation reuse this session.
        editor.storeEditorConfiguration(changed)
        editor.configureTime(editor.timeSettings.copy(enabled = true))
        editor.configureTime(editor.timeSettings.copy(rate = 86400, resume = SpaceCompassScenarioResume.ACTIVE))
        state.reload() // SharedPreferences notifications must not leak prepared edits into the live sky.
        assertEquals(original, state.plan)
        assertEquals(original, state.savedPlan)
        assertEquals(oldSettings, state.timeSettings)
        assertEquals(changed, editor.plan)
        val selected = editor.plan!!.observationTimeMs(System.currentTimeMillis())
        elapsed += 120_000L
        assertEquals(selected, editor.plan!!.observationTimeMs(System.currentTimeMillis() + 86_400_000L))
        assertEquals(original.timeMs, state.plan!!.observationTimeMs(System.currentTimeMillis() + 86_400_000L))
        val exported = SpaceCompassScenarioArchive.capture(editor, "Prepared")
        assertEquals(changed, exported.configuration)
        assertEquals(86400, exported.timeSettings.rate)
        state.finishEditing()
        assertEquals(changed, state.plan)
        assertEquals(86400, state.timeSettings.rate)
        assertEquals(selected, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
        elapsed += 1000L
        assertEquals(selected + 86_400_000L, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
    }

    @Test fun disablingAllOverridesAlsoWaitsUntilTheMainView() {
        val state = SpaceCompassObserverState(null, elapsedClock = { 0L })
        state.apply(original)
        val editor = state.beginEditing()
        editor.apply(null)
        assertNull(editor.plan)
        assertEquals(original, state.plan)
        state.finishEditing()
        assertNull(state.plan)
        assertEquals(original, state.savedPlan)
    }

    @Test fun noResumePolicyConsumesTheTimeSpentInsideTheEditor() {
        val wall = Instant.parse("2026-11-07T12:00:00Z").toEpochMilli()
        for (mode in SpaceCompassScenarioResume.entries) {
            val clock = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, 86400, mode),
                "held", original.timeMs, wall, 0L) { 0L }
            clock.setActive(true, wall, 0L)
            clock.hold(wall, 0L)
            assertEquals(original.timeMs, clock.at(wall + 120_000L, 120_000L))
            assertEquals(original.timeMs, clock.atWallTime(wall + 60_000L, wall + 120_000L, 120_000L))
            clock.setActive(false, wall + 120_000L, 120_000L)
            clock.setActive(true, wall + 180_000L, 180_000L)
            clock.release(wall + 180_000L, 180_000L)
            assertEquals(original.timeMs, clock.at(wall + 180_000L, 180_000L))
            assertEquals(original.timeMs + 86_400_000L, clock.at(wall + 181_000L, 181_000L))
        }
    }

    @Test fun persistedPreparedChangesStayStoppedAcrossProcessRecreation() {
        val store = Store(); var elapsed = 0L
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { elapsed })
        state.apply(original)
        val editor = state.beginEditing()
        editor.apply(changed)
        editor.configureTime(editor.timeSettings.copy(enabled = true))
        editor.configureTime(editor.timeSettings.copy(rate = -86400, resume = SpaceCompassScenarioResume.ACTIVE))
        val selected = editor.plan!!.observationTimeMs(System.currentTimeMillis())
        elapsed = 600_000L
        val reopened = SpaceCompassObserverState(store.preferences, elapsedClock = { elapsed })
        assertEquals(selected, reopened.plan!!.observationTimeMs(System.currentTimeMillis() + 86_400_000L))
        reopened.active(true)
        reopened.finishEditing()
        assertEquals(selected, reopened.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
        elapsed += 1000L
        assertEquals(selected - 86_400_000L, reopened.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
        assertFalse(store.values.containsKey("scenario_prepared_clock"))
    }

    @Test fun anAdvancingHourIsAnchoredAtReturnRatherThanWhenItWasEntered() {
        val store = Store(); var elapsed = 0L
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { elapsed })
        state.apply(original); state.active(true)
        val editor = state.beginEditing()
        editor.configureTime(editor.timeSettings.copy(resume = SpaceCompassScenarioResume.ACTIVE))
        editor.apply(changed.copy(advanceTime = true, clockOffsetMs = 12345L))
        val selected = editor.plan!!.observationTimeMs(System.currentTimeMillis())
        elapsed += 120_000L
        state.finishEditing()
        assertEquals(selected, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
        assertTrue(kotlin.math.abs(state.savedPlan!!.observationTimeMs(System.currentTimeMillis()) - selected) < 1000L)
        elapsed += 1000L
        assertEquals(selected + 1000L, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
    }

    @Test fun aLoadedLocationIsPreviewedWithoutReplacingTheRunningObserver() {
        val state = SpaceCompassObserverState(null, elapsedClock = { 0L })
        state.apply(original)
        val editor = state.beginEditing()
        val imported = SpaceCompassScenarioArchive("Loaded", changed,
            SpaceCompassScenarioTimeSettings(true, 43200, SpaceCompassScenarioResume.ALWAYS))
        editor.importArchive(imported)
        assertEquals(original, state.plan)
        assertEquals(changed, editor.plan)
        assertEquals(changed.timeMs, editor.plan!!.observationTimeMs(System.currentTimeMillis() + 600_000L))
        state.finishEditing()
        assertEquals(changed, state.plan)
        assertEquals(imported.timeSettings, state.timeSettings)
        assertEquals("Loaded", state.archiveName)
    }

    @Test fun reloadingTheSameLocationStillRestartsAtItsArchivedMoment() {
        val store = Store(); var elapsed = 0L
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { elapsed })
        state.apply(original)
        state.configureTime(state.timeSettings.copy(enabled = true))
        state.configureTime(state.timeSettings.copy(rate = 3600, resume = SpaceCompassScenarioResume.ACTIVE))
        state.active(true); elapsed += 1000L
        val editor = state.beginEditing()
        assertEquals(original.timeMs + 3_600_000L, editor.plan!!.observationTimeMs(System.currentTimeMillis()))
        editor.importArchive(SpaceCompassScenarioArchive("Restart", original, editor.timeSettings))
        assertEquals(original.timeMs, editor.plan!!.observationTimeMs(System.currentTimeMillis()))
        state.finishEditing()
        assertEquals(original.timeMs, state.plan!!.scenarioClock!!.at(System.currentTimeMillis(), elapsed))
    }

    @Test fun speedReactivationRetainsPauseAndBothDirectionsAcrossEditorSessions() {
        for (rate in listOf(-86400, 0, 86400)) {
            val store = Store()
            val state = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
            state.apply(original)
            state.configureTime(SpaceCompassScenarioTimeSettings(true, rate, SpaceCompassScenarioResume.ACTIVE))
            val editor = state.beginEditing()
            editor.configureTime(editor.timeSettings.copy(enabled = false))
            assertEquals(rate, editor.timeSettings.rate)
            editor.configureTime(editor.timeSettings.copy(enabled = true))
            assertEquals(rate, editor.timeSettings.rate)
            state.finishEditing()
            val again = state.beginEditing()
            assertEquals(rate, again.timeSettings.rate)
            assertTrue(again.timeSettings.enabled)
            state.finishEditing()
            val reopened = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
            assertEquals(rate, reopened.timeSettings.rate)
        }
    }

    @Test fun disablingAndReenablingAllSectionsKeepsEveryPreparedValueAndResumeMode() {
        val store = Store()
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        state.apply(original)
        state.configureTime(SpaceCompassScenarioTimeSettings(true, -43200, SpaceCompassScenarioResume.ACTIVE))
        val editor = state.beginEditing()
        val inactive = original.copy(simulatePosition = false, simulateAltitude = false,
            simulateZone = false, simulateDate = false, simulateTime = false)
        editor.storeEditorConfiguration(inactive)
        editor.configureTime(editor.timeSettings.copy(enabled = false))
        state.finishEditing()
        assertNull(state.plan)
        assertEquals(inactive, state.savedPlan)
        val next = state.beginEditing()
        assertEquals(inactive, next.savedPlan)
        next.storeEditorConfiguration(next.savedPlan!!.copy(simulatePosition = true, simulateAltitude = true,
            simulateZone = true, simulateDate = true, simulateTime = true))
        next.configureTime(next.timeSettings.copy(enabled = true))
        state.finishEditing()
        assertEquals(original, state.savedPlan)
        assertEquals(-43200, state.timeSettings.rate)
        assertEquals(SpaceCompassScenarioResume.ACTIVE, state.timeSettings.resume)
    }

    private class Store {
        val values = mutableMapOf<String, Any?>()
        private val pending = mutableMapOf<String, Any?>()
        private val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args -> when (method.name) {
            "putString", "putBoolean", "putInt" -> { pending[args!![0] as String] = args[1]; proxy }
            "remove" -> { pending[args!![0] as String] = null; proxy }
            "apply", "commit" -> { pending.forEach { (key, value) -> if (value == null) values.remove(key) else values[key] = value }
                pending.clear(); if (method.name == "commit") true else null }
            else -> error(method.name)
        } } as SharedPreferences.Editor
        val preferences = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args -> when (method.name) {
            "getString", "getBoolean", "getInt" -> values[args!![0] as String] ?: args[1]
            "edit" -> editor
            else -> error(method.name)
        } } as SharedPreferences
    }
}

package me.mondiversi.spacecompass

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.Instant
import java.time.ZoneId

class SpaceCompassObservationMomentSelectionTest {
    private val deviceZone = ZoneId.of("Europe/Rome")
    private val selected = Instant.parse("2026-10-25T01:30:15.321Z").toEpochMilli()
    private val custom = SpaceCompassObserverPlan(28.3, -14.1, 1700.0, selected - 50_000, "Atlantic/Canary",
        simulatePosition = true, simulateTime = false, simulateDate = false, simulateAltitude = true,
        simulateZone = false, automaticZoneId = "Atlantic/Canary")

    @Test fun selectingAnEventPreservesTheEffectivePlaceHeightAndAutomaticZone() {
        val changed = spaceCompassObserverPlanAtMoment(custom, custom, selected, deviceZone)!!
        assertEquals(custom.position, changed.position)
        assertEquals(custom.simulateAltitude, changed.simulateAltitude)
        assertEquals(custom.observationZone(deviceZone), changed.observationZone(deviceZone))
        assertTrue(changed.simulateDate && changed.simulateTime)
        assertFalse(changed.advanceTime)
        assertNull(changed.clockOffsetMs)
        assertEquals(selected, changed.observationTimeMs(selected + 9_000_000, deviceZone))
    }

    @Test fun liveModeDoesNotReactivateOldSavedSpatialOverrides() {
        val changed = spaceCompassObserverPlanAtMoment(null, custom, selected, deviceZone)!!
        assertFalse(changed.simulatePosition)
        assertFalse(changed.simulateAltitude)
        assertFalse(changed.simulateZone)
        assertEquals(deviceZone.id, changed.momentZoneId)
    }

    @Test fun accelerationOnlyLivePlanDoesNotEraseThePreparedInactivePlace() {
        val liveClockPlan = SpaceCompassObserverPlan(0.0, 0.0, 0.0, selected, deviceZone.id,
            simulatePosition = false, simulateTime = false, simulateDate = false,
            simulateAltitude = false, simulateZone = false)
        val changed = spaceCompassObserverPlanAtMoment(liveClockPlan, custom, selected, deviceZone)!!
        assertEquals(custom.latitude, changed.latitude, 0.0)
        assertEquals(custom.longitude, changed.longitude, 0.0)
        assertEquals(custom.altitudeMeters, changed.altitudeMeters, 0.0)
        assertFalse(changed.simulatePosition || changed.simulateAltitude || changed.simulateZone)
        assertEquals(deviceZone.id, changed.momentZoneId)
    }

    @Test fun exactSecondOccurrenceOfDstTimeIsKeptAcrossEncoding() {
        val changed = spaceCompassObserverPlanAtMoment(null, null, selected, deviceZone)!!
        assertEquals(selected, decodeSpaceCompassObserverPlan(changed.encode())!!.observationTimeMs(selected + 1000, deviceZone))
        assertNull(spaceCompassObserverPlanAtMoment(null, null,
            Instant.parse("2102-01-01T00:00:00Z").toEpochMilli(), deviceZone))
    }

    /** Synchronous preference listeners observe the same atomic edit as Android. */
    private class Store {
        val values = mutableMapOf<String, Any?>()
        private val pending = mutableMapOf<String, Any?>()
        private val listeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()
        private val editor = Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
            arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when (method.name) {
                "putString", "putBoolean", "putInt" -> { pending[args!![0] as String] = args[1]; proxy }
                "remove" -> { pending[args!![0] as String] = null; proxy }
                "apply", "commit" -> {
                    val keys = pending.keys.toList()
                    values.putAll(pending); pending.clear()
                    keys.forEach { key -> listeners.forEach { it.onSharedPreferenceChanged(preferences, key) } }
                    if (method.name == "commit") true else null
                }
                else -> throw AssertionError(method.name)
            }
        } as SharedPreferences.Editor
        val preferences: SharedPreferences = Proxy.newProxyInstance(SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString", "getBoolean", "getInt" -> values[args!![0] as String] ?: args[1]
                "edit" -> editor
                else -> throw AssertionError(method.name)
            }
        } as SharedPreferences
        fun listen(value: SharedPreferences.OnSharedPreferenceChangeListener) { listeners += value }
    }

    @Test fun pausedSelectionPersistsAndNeverResumesAnOldFastCheckpointInAnyResumeMode() {
        for (mode in SpaceCompassScenarioResume.entries) {
            val store = Store()
            val state = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
            state.apply(custom)
            state.configureTime(SpaceCompassScenarioTimeSettings(true, resume = mode))
            state.configureTime(state.timeSettings.copy(rate = -3600))
            store.listen(SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key != "scenario_clock_checkpoint") state.reload() })
            assertTrue(state.observeAt(selected, deviceZone))
            assertEquals(0, state.timeSettings.multiplier)
            assertEquals(selected, state.plan!!.observationTimeMs(selected + 999_999, deviceZone))
            assertEquals(custom.position, state.plan!!.position)
            val reopened = SpaceCompassObserverState(store.preferences, elapsedClock = { 4000L })
            assertEquals(0, reopened.timeSettings.multiplier)
            assertEquals(mode, reopened.timeSettings.resume)
            assertEquals(selected, reopened.plan!!.observationTimeMs(selected + 86_400_000, deviceZone))
            assertFalse(reopened.observeAt(selected, deviceZone))
        }
    }

    @Test fun previewWithoutAndroidPreferencesCanAlsoFreezeAnEvent() {
        val state = SpaceCompassObserverState(null, elapsedClock = { 0L })
        assertTrue(state.observeAt(selected, deviceZone))
        assertEquals(0, state.timeSettings.multiplier)
        assertEquals(selected, state.plan!!.observationTimeMs(selected + 3600_000, deviceZone))
    }
}

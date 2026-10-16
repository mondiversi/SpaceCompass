package me.mondiversi.spacecompass

import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.Instant

class SpaceCompassObserverAutosaveTest {
    private val now = Instant.parse("2026-10-06T08:00:00Z").toEpochMilli()
    private val original = spaceCompassObserverPlan(28.35, -14.1, 102.5,
        Instant.parse("2027-04-06T20:00:00Z").toEpochMilli(), "Atlantic/Canary")!!
    private val draft = SpaceCompassObserverDraft(true, true, "28.35", "-14.1", "102.5",
        "Atlantic/Canary", "2027-04-06", "21:00")

    @Test fun completeValuesRetainTheChosenInstantAndCanBeSavedWithoutConfirmation() {
        assertEquals(original, draft.resolve(original, now, "Europe/Rome").getOrThrow())
        val changed = draft.copy(time = "21:05").resolve(original, now, "Europe/Rome").getOrThrow()!!
        assertEquals(original.timeMs + 5 * 60_000, changed.timeMs)
        assertEquals(changed, decodeSpaceCompassObserverPlan(changed.encode()))
    }

    @Test fun incompleteAndInvalidEnabledInputsNeverReplaceThePreviousPlan() {
        val incomplete = listOf(draft.copy(latitude = "-"), draft.copy(longitude = ""),
            draft.copy(altitude = ""), draft.copy(zone = "Atlantic/"), draft.copy(time = "21:"),
            draft.copy(date = "2027-02-30"), draft.copy(latitude = "91"),
            draft.copy(altitude = "NaN"), draft.copy(longitude = "Infinity"))
        for (candidate in incomplete) assertTrue(candidate.resolve(original, now, "Europe/Rome").isFailure)
        assertEquals(102.5, original.altitudeMeters, 0.0)
    }

    @Test fun disabledIncompleteInputsCannotBlockTurningSimulationOffOrUsingOnlyDate() {
        val invalidPlace = draft.copy(latitude = "-", longitude = "", altitude = "", zone = "invalid")
        assertNull(invalidPlace.copy(simulatePosition = false, simulateTime = false, simulateDate = false, simulateAltitude = false, simulateZone = false)
            .resolve(original, now, "Europe/Rome").getOrThrow())
        val dateOnly = invalidPlace.copy(simulatePosition = false, simulateAltitude = false, simulateZone = false)
            .resolve(original, now, "Europe/Rome").getOrThrow()!!
        assertFalse(dateOnly.simulatePosition)
        assertTrue(dateOnly.simulateTime)
        assertEquals(original.latitude, dateOnly.latitude, 0.0)
        assertEquals(Instant.parse("2027-04-06T19:00:00Z").toEpochMilli(), dateOnly.timeMs)
    }

    @Test fun locationOnlyUsesTheRealClockAndRetainsThePreviousFixedInstantForReuse() {
        val plan = draft.copy(simulateTime = false, simulateDate = false, date = "", time = "")
            .resolve(original, now, "Europe/Rome").getOrThrow()!!
        assertTrue(plan.simulatePosition)
        assertFalse(plan.simulateTime)
        assertNull(plan.timeOverrideMs)
        assertEquals(original.timeMs, plan.timeMs)
        assertEquals("Atlantic/Canary", plan.observationZone(java.time.ZoneId.of("Europe/Rome")).id)
    }

    @Test fun feetAndDecimalCommaAreConvertedOnceIntoStoredMetres() {
        val plan = draft.copy(altitude = "328,0839895013123", feet = true)
            .resolve(original, now, "Europe/Rome").getOrThrow()!!
        assertEquals(100.0, plan.altitudeMeters, 1e-9)
        assertTrue(draft.copy(altitude = "-2000", feet = true).resolve(original, now, "Europe/Rome").isFailure)
    }

    @Test fun invalidDstTimeStaysUncommittedAndAnAmbiguousTimeUsesTheFirstOccurrence() {
        assertTrue(draft.copy(zone = "Europe/Rome", date = "2027-03-28", time = "02:30")
            .resolve(original, now, "Europe/Rome").isFailure)
        assertEquals(Instant.parse("2027-10-31T00:30:00Z").toEpochMilli(),
            draft.copy(zone = "Europe/Rome", date = "2027-10-31", time = "02:30")
                .resolve(original, now, "Europe/Rome").getOrThrow()!!.timeMs)
    }

    @Test fun liveObserverWithNoPreviousPlanDoesNotInventAnOverrideForInvalidInput() {
        assertTrue(draft.copy(date = "invalid").resolve(null, now, "UTC").isFailure)
        val positionOnly = draft.copy(simulateTime = false, simulateDate = false).resolve(null, now, "UTC").getOrThrow()!!
        assertEquals(now, positionOnly.timeMs)
        assertNull(positionOnly.timeOverrideMs)
    }

    /** Interface-only preferences spy: no Android storage/mock framework or network. */
    private class Store {
        val values = mutableMapOf<String, Any?>()
        var writes = 0
        private val pending = mutableMapOf<String, Any?>()
        private val editor: SharedPreferences.Editor = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when (method.name) {
                "putString", "putBoolean", "putInt" -> { pending[args!![0] as String] = args[1]; proxy }
                "remove" -> { pending[args!![0] as String] = null; proxy }
                "apply", "commit" -> {
                    // Clock checkpoints are operational state, not another settings edit/notification.
                    if (pending.keys.any { it != "scenario_clock_checkpoint" }) writes++
                    values.putAll(pending); pending.clear()
                    if (method.name == "commit") true else null
                }
                else -> throw AssertionError("Unexpected edit: ${method.name}")
            }
        } as SharedPreferences.Editor
        val preferences: SharedPreferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString", "getBoolean", "getInt" -> values[args!![0] as String] ?: args[1]
                "edit" -> editor
                else -> throw AssertionError("Unexpected access: ${method.name}")
            }
        } as SharedPreferences
    }

    @Test fun actualObserverStatePersistsAllFiveIndependentModesAcrossNewInstances() {
        val store = Store()
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        for (mask in 1..31) {
            val plan = original.copy(simulatePosition = (mask and 1) != 0, simulateTime = (mask and 2) != 0,
                simulateAltitude = (mask and 4) != 0, simulateZone = (mask and 8) != 0, simulateDate = (mask and 16) != 0)
            assertTrue(state.apply(plan))
            assertEquals(plan, SpaceCompassObserverState(store.preferences, elapsedClock = { 0L }).plan)
            assertEquals(plan, SpaceCompassObserverState(store.preferences, elapsedClock = { 0L }).savedPlan)
        }
        assertEquals(31, store.writes)
    }

    @Test fun reapplyingTheSamePlanDoesNotWritePreferencesOrRequestAnotherSavedNotice() {
        val store = Store()
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        assertFalse(state.apply(null))
        assertTrue(state.apply(original))
        assertFalse(state.apply(original))
        assertEquals(1, store.writes)
        assertEquals(original, state.plan)
    }

    @Test fun disablingBothModesReturnsToLiveButRetainsSavedPlaceAndMoment() {
        val store = Store()
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        state.apply(original)
        assertTrue(state.apply(null))
        val reopened = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        assertNull(reopened.plan)
        assertEquals(original, reopened.savedPlan)
        assertFalse(state.apply(null))
        assertEquals(2, store.writes)
    }
    @Test fun accelerationAndResumptionPersistIndependentlyEvenWithoutDateOrTimeOverrides() {
        val store = Store()
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        assertNull(state.plan)
        state.configureTime(SpaceCompassScenarioTimeSettings(true,3600,SpaceCompassScenarioResume.ALWAYS))
        assertNotNull(state.plan?.scenarioClock)
        assertEquals(1,state.plan.activeSectionCount)
        val reopened=SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        assertEquals(state.timeSettings,reopened.timeSettings)
        reopened.configureTime(reopened.timeSettings.copy(enabled=false))
        assertNull(reopened.plan)
        assertNull(store.values["scenario_clock_checkpoint"])
        val disabled=SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        assertEquals(3600,disabled.timeSettings.rate)
        assertEquals(SpaceCompassScenarioResume.ALWAYS,disabled.timeSettings.resume)
        assertFalse(disabled.timeSettings.enabled)
    }
    @Test fun observerWithoutAndroidStorageStillSupportsPreviewAndUiTestEdits() {
        val state=SpaceCompassObserverState(null,elapsedClock={ 0L })
        assertTrue(state.apply(original))
        assertEquals(original,state.plan)
        assertFalse(state.apply(original))
        assertTrue(state.apply(null))
        assertNull(state.plan)
        assertEquals(original,state.savedPlan)
        state.configureTime(SpaceCompassScenarioTimeSettings(true,60))
        assertEquals(60,state.timeSettings.multiplier)
        assertNotNull(state.plan?.scenarioClock)
    }
    @Test fun activeOnlyLifecycleCheckpointRestoresTheReachedInstantWithoutClosedTime() {
        val store=Store()
        var elapsed=0L
        val state=SpaceCompassObserverState(store.preferences,elapsedClock={ elapsed })
        state.configureTime(state.timeSettings.copy(resume=SpaceCompassScenarioResume.ACTIVE))
        state.active(true)
        state.apply(original)
        val start=state.plan!!.observationTimeMs(System.currentTimeMillis())
        elapsed+=1000
        state.active(false)
        elapsed+=100_000
        assertEquals(start+1000,state.plan!!.observationTimeMs(System.currentTimeMillis()))
        elapsed=0
        val reopened=SpaceCompassObserverState(store.preferences,elapsedClock={ elapsed })
        reopened.active(true); elapsed+=500
        assertEquals(start+1500,reopened.plan!!.observationTimeMs(System.currentTimeMillis()))
    }

    @Test fun newScenarioRestartsAtItsConfiguredMomentAfterProcessRecreationByDefault() {
        val store = Store()
        var elapsed = 0L
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { elapsed })
        assertEquals(SpaceCompassScenarioResume.RESTART, state.timeSettings.resume)
        assertEquals(SpaceCompassScenarioResume.RESTART, SpaceCompassObserverState(null).timeSettings.resume)
        state.active(true)
        state.apply(original)
        val start = state.plan!!.observationTimeMs(System.currentTimeMillis())
        elapsed += 1000
        state.active(false)
        assertEquals(start + 1000, state.plan!!.observationTimeMs(System.currentTimeMillis()))
        assertNull(store.values["scenario_clock_checkpoint"])
        val reopened = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        assertEquals(start, reopened.plan!!.observationTimeMs(System.currentTimeMillis()))
    }

    @Test fun anExplicitResumptionChoiceSurvivesReloadForAllThreeModes() {
        val store = Store()
        for (mode in listOf(SpaceCompassScenarioResume.ACTIVE, SpaceCompassScenarioResume.ALWAYS,
            SpaceCompassScenarioResume.RESTART)) {
            val state = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
            state.configureTime(state.timeSettings.copy(resume = mode))
            assertEquals(mode, SpaceCompassObserverState(store.preferences, elapsedClock = { 0L }).timeSettings.resume)
        }
    }

    @Test fun aPreviouslyChosenLegacyClockOffsetRetainsItsAlwaysPolicy() {
        val store = Store()
        store.values[SPACE_COMPASS_OBSERVER_PLAN] = original.copy(advanceTime = true, clockOffsetMs = 6 * 3600_000L).encode()
        store.values[SPACE_COMPASS_OBSERVER_MODE] = "custom"
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        assertEquals(SpaceCompassScenarioResume.ALWAYS, state.timeSettings.resume)
        assertNotNull(state.plan?.scenarioClock)
    }
}

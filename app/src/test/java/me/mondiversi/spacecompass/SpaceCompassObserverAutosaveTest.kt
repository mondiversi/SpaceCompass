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
        state.configureTime(SpaceCompassScenarioTimeSettings(true,resume=SpaceCompassScenarioResume.ALWAYS))
        state.configureTime(state.timeSettings.copy(rate=3600))
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
        state.configureTime(SpaceCompassScenarioTimeSettings(true))
        state.configureTime(state.timeSettings.copy(rate=60))
        assertEquals(60,state.timeSettings.multiplier)
        assertNotNull(state.plan?.scenarioClock)
    }
    @Test fun reEnablingAccelerationResetsEverySavedSpeedAndPersistsOneTimesAcrossAllResumeModes() {
        for (mode in SpaceCompassScenarioResume.entries) {
            for (rate in spaceCompassScenarioRates) {
                val store = Store()
                store.values["scenario_speed_enabled"] = false
                store.values["scenario_speed_rate"] = rate
                store.values["scenario_resume"] = mode.name
                val state = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
                state.configureTime(state.timeSettings.copy(enabled = true))
                val reopened = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
                assertTrue(reopened.timeSettings.enabled)
                assertEquals(1, reopened.timeSettings.rate)
                assertEquals(1, reopened.timeSettings.multiplier)
                assertEquals(mode, reopened.timeSettings.resume)
            }
        }
    }

    @Test fun speedToggleKeepsTheReachedScenarioInstantAndThenAdvancesAtNormalSpeed() {
        for (persistent in listOf(false, true)) {
            val store = Store()
            var elapsed = 0L
            val state = SpaceCompassObserverState(if (persistent) store.preferences else null,
                elapsedClock = { elapsed })
            state.configureTime(state.timeSettings.copy(enabled = true, resume = SpaceCompassScenarioResume.ACTIVE))
            state.configureTime(state.timeSettings.copy(rate = 3600))
            state.apply(original)
            state.active(true)
            val start = state.plan!!.observationTimeMs(System.currentTimeMillis())
            elapsed += 1000
            assertEquals(start + 3600_000, state.plan!!.observationTimeMs(System.currentTimeMillis()))
            state.configureTime(state.timeSettings.copy(enabled = false))
            elapsed += 2000
            val reached = state.plan!!.observationTimeMs(System.currentTimeMillis())
            assertEquals(start + 3602_000, reached)
            state.configureTime(state.timeSettings.copy(enabled = true))
            assertEquals(1, state.timeSettings.multiplier)
            assertEquals(reached, state.plan!!.observationTimeMs(System.currentTimeMillis()))
            assertEquals(original, state.savedPlan)
            elapsed += 1000
            assertEquals(reached + 1000, state.plan!!.observationTimeMs(System.currentTimeMillis()))
            state.reload()
            assertEquals(1, state.timeSettings.multiplier)
            state.configureTime(state.timeSettings.copy(rate = 60))
            state.reload()
            assertEquals(60, state.timeSettings.multiplier)
        }
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

    @Test fun reverseSpeedAndReachedInstantSurviveActiveOnlyReopening() {
        val store = Store()
        var elapsed = 0L
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { elapsed })
        state.configureTime(state.timeSettings.copy(enabled = true, resume = SpaceCompassScenarioResume.ACTIVE))
        state.apply(original)
        state.active(true)
        state.configureTime(state.timeSettings.copy(rate = -3600))
        val start = state.plan!!.observationTimeMs(System.currentTimeMillis())
        elapsed += 1000
        assertEquals(start - 3_600_000, state.plan!!.observationTimeMs(System.currentTimeMillis()))
        state.active(false)
        elapsed = 0L
        val reopened = SpaceCompassObserverState(store.preferences, elapsedClock = { elapsed })
        assertEquals(-3600, reopened.timeSettings.rate)
        assertTrue(reopened.timeSettings.accelerated)
        assertEquals(start - 3_600_000, reopened.plan!!.observationTimeMs(System.currentTimeMillis()))
        reopened.active(true)
        elapsed += 1000
        assertEquals(start - 7_200_000, reopened.plan!!.observationTimeMs(System.currentTimeMillis()))
        assertEquals(original, reopened.savedPlan)
    }

    @Test fun anEnabledZeroRateSurvivesReloadAndReopeningButReactivationStillStartsAtOne() {
        val store = Store()
        var elapsed = 0L
        val state = SpaceCompassObserverState(store.preferences, elapsedClock = { elapsed })
        state.configureTime(state.timeSettings.copy(enabled = true, resume = SpaceCompassScenarioResume.ACTIVE))
        state.apply(original)
        state.active(true)
        state.configureTime(state.timeSettings.copy(rate = 0))
        val frozen = state.plan!!.observationTimeMs(System.currentTimeMillis())
        elapsed += 10_000
        state.reload()
        assertEquals(frozen, state.plan!!.observationTimeMs(System.currentTimeMillis()))
        state.active(false)
        val reopened = SpaceCompassObserverState(store.preferences, elapsedClock = { 0L })
        assertTrue(reopened.timeSettings.enabled)
        assertEquals(0, reopened.timeSettings.rate)
        assertEquals(frozen, reopened.plan!!.observationTimeMs(System.currentTimeMillis()))
        reopened.configureTime(reopened.timeSettings.copy(enabled = false))
        assertEquals(1, reopened.timeSettings.multiplier)
        reopened.configureTime(reopened.timeSettings.copy(enabled = true))
        assertEquals(1, reopened.timeSettings.rate)
        assertEquals(original, reopened.savedPlan)
    }
    @Test fun preparedAltitudePersistsWithoutEnablingAnyLocationOverride() {
        val store=Store();val state=SpaceCompassObserverState(store.preferences,elapsedClock={0L})
        val off=draft.copy(simulatePosition=false,simulateDate=false,simulateTime=false,simulateAltitude=false,simulateZone=false)
        val prepared=off.copy(altitude="432.5").resolveForEditor(SpaceCompassScenarioEditorSection.ALTITUDE,null,now,"Europe/Rome").getOrThrow()!!
        assertTrue(prepared.activeSections.isEmpty())
        assertEquals(432.5,prepared.altitudeMeters,0.0)
        assertEquals(draft.latitude.toDouble(),prepared.latitude,0.0)
        assertEquals(draft.longitude.toDouble(),prepared.longitude,0.0)
        assertEquals(draft.automaticAltitudeMeters,prepared.automaticAltitudeMeters)
        assertEquals(draft.automaticZoneId,prepared.automaticZoneId)
        assertTrue(state.storeEditorConfiguration(prepared))
        assertNull(state.plan)
        val reopened=SpaceCompassObserverState(store.preferences,elapsedClock={0L})
        assertNull(reopened.plan)
        assertEquals(prepared,reopened.savedPlan)
        assertTrue(reopened.apply(prepared.copy(simulateAltitude=true)))
        assertEquals(432.5,reopened.plan!!.resolvePlace(SpaceCompassDevicePlace(45.0,9.0,20.0))!!.altitude!!,0.0)
    }

    @Test fun preparedZoneDoesNotReinterpretAnAlreadyActiveFixedMoment() {
        val input=draft.copy(simulatePosition=false,simulateAltitude=false,simulateZone=false,zone="Pacific/Honolulu")
        val live=input.resolve(original,now,"Europe/Rome").getOrThrow()!!
        val prepared=input.resolveForEditor(SpaceCompassScenarioEditorSection.ZONE,original,now,"Europe/Rome").getOrThrow()!!
        assertFalse(prepared.simulateZone)
        assertEquals("Pacific/Honolulu",prepared.zoneId)
        assertEquals(live.observationTimeMs(now),prepared.observationTimeMs(now))
        assertEquals("Europe/Rome",prepared.observationZone(java.time.ZoneId.of("Europe/Rome")).id)
    }

    @Test fun preparingAnInactiveDateKeepsTheCurrentTimeOnlyBehavior() {
        val input=draft.copy(simulatePosition=false,simulateAltitude=false,simulateZone=false,simulateDate=false,date="2030-08-12")
        val live=input.resolve(original,now,"Europe/Rome").getOrThrow()!!
        val prepared=input.resolveForEditor(SpaceCompassScenarioEditorSection.DATE,original,now,"Europe/Rome").getOrThrow()!!
        assertFalse(prepared.simulateDate);assertTrue(prepared.simulateTime)
        assertEquals(live.observationTimeMs(now),prepared.observationTimeMs(now))
        assertEquals("2030-08-12",java.time.Instant.ofEpochMilli(prepared.timeMs).atZone(java.time.ZoneId.of(prepared.momentZoneId)).toLocalDate().toString())
    }

    @Test fun preparingAnInactiveTimeKeepsTheCurrentDateOnlyBehavior() {
        val input=draft.copy(simulatePosition=false,simulateAltitude=false,simulateZone=false,simulateTime=false,time="04:35")
        val live=input.resolve(original,now,"Europe/Rome").getOrThrow()!!
        val prepared=input.resolveForEditor(SpaceCompassScenarioEditorSection.TIME,original,now,"Europe/Rome").getOrThrow()!!
        assertTrue(prepared.simulateDate);assertFalse(prepared.simulateTime)
        assertEquals(live.observationTimeMs(now),prepared.observationTimeMs(now))
        assertEquals("04:35",java.time.Instant.ofEpochMilli(prepared.timeMs).atZone(java.time.ZoneId.of(prepared.momentZoneId)).toLocalTime().toString())
    }

    @Test fun malformedPreparedValueCannotReplaceTheSavedConfiguration() {
        val off=draft.copy(simulatePosition=false,simulateDate=false,simulateTime=false,simulateAltitude=false,simulateZone=false)
        assertTrue(off.copy(altitude="-").resolveForEditor(SpaceCompassScenarioEditorSection.ALTITUDE,original,now,"Europe/Rome").isFailure)
        assertTrue(off.copy(date="2030-02-30").resolveForEditor(SpaceCompassScenarioEditorSection.DATE,original,now,"Europe/Rome").isFailure)
        assertNull(off.copy(altitude="-").resolveForEditor(null,original,now,"Europe/Rome").getOrThrow())
    }

}

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
        assertNull(invalidPlace.copy(simulatePosition = false, simulateTime = false, simulateAltitude = false, simulateZone = false)
            .resolve(original, now, "Europe/Rome").getOrThrow())
        val dateOnly = invalidPlace.copy(simulatePosition = false, simulateAltitude = false, simulateZone = false)
            .resolve(original, now, "Europe/Rome").getOrThrow()!!
        assertFalse(dateOnly.simulatePosition)
        assertTrue(dateOnly.simulateTime)
        assertEquals(original.latitude, dateOnly.latitude, 0.0)
        assertEquals(Instant.parse("2027-04-06T19:00:00Z").toEpochMilli(), dateOnly.timeMs)
    }

    @Test fun locationOnlyUsesTheRealClockAndRetainsThePreviousFixedInstantForReuse() {
        val plan = draft.copy(simulateTime = false, date = "", time = "")
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
        val positionOnly = draft.copy(simulateTime = false).resolve(null, now, "UTC").getOrThrow()!!
        assertEquals(now, positionOnly.timeMs)
        assertNull(positionOnly.timeOverrideMs)
    }

    /** Interface-only preferences spy: no Android storage/mock framework or network. */
    private class Store {
        val values = mutableMapOf<String, String>()
        var writes = 0
        private val pending = mutableMapOf<String, String>()
        private val editor: SharedPreferences.Editor = Proxy.newProxyInstance(
            SharedPreferences.Editor::class.java.classLoader, arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
            when (method.name) {
                "putString" -> { pending[args!![0] as String] = args[1] as String; proxy }
                "apply" -> { values.putAll(pending); pending.clear(); writes++; null }
                else -> throw AssertionError("Unexpected edit: ${method.name}")
            }
        } as SharedPreferences.Editor
        val preferences: SharedPreferences = Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader, arrayOf(SharedPreferences::class.java)) { _, method, args ->
            when (method.name) {
                "getString" -> values[args!![0] as String] ?: args[1]
                "edit" -> editor
                else -> throw AssertionError("Unexpected access: ${method.name}")
            }
        } as SharedPreferences
    }

    @Test fun actualObserverStatePersistsAllFourIndependentModesAcrossNewInstances() {
        val store = Store()
        val state = SpaceCompassObserverState(store.preferences)
        for (mask in 1..15) {
            val plan = original.copy(simulatePosition = (mask and 1) != 0, simulateTime = (mask and 2) != 0,
                simulateAltitude = (mask and 4) != 0, simulateZone = (mask and 8) != 0)
            assertTrue(state.apply(plan))
            assertEquals(plan, SpaceCompassObserverState(store.preferences).plan)
            assertEquals(plan, SpaceCompassObserverState(store.preferences).savedPlan)
        }
        assertEquals(15, store.writes)
    }

    @Test fun reapplyingTheSamePlanDoesNotWritePreferencesOrRequestAnotherSavedNotice() {
        val store = Store()
        val state = SpaceCompassObserverState(store.preferences)
        assertFalse(state.apply(null))
        assertTrue(state.apply(original))
        assertFalse(state.apply(original))
        assertEquals(1, store.writes)
        assertEquals(original, state.plan)
    }

    @Test fun disablingBothModesReturnsToLiveButRetainsSavedPlaceAndMoment() {
        val store = Store()
        val state = SpaceCompassObserverState(store.preferences)
        state.apply(original)
        assertTrue(state.apply(null))
        val reopened = SpaceCompassObserverState(store.preferences)
        assertNull(reopened.plan)
        assertEquals(original, reopened.savedPlan)
        assertFalse(state.apply(null))
        assertEquals(2, store.writes)
    }
}

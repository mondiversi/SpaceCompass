package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSettingsExitConfirmationTest {
    @Test fun openingOrRecomposingOrLeavingUnchangedSettingsIsSilent() {
        val state = SpaceCompassSettingsExitConfirmation()
        val values = mapOf("theme" to "system")
        assertFalse(state.moveTo("appearance", values))
        repeat(3) { assertFalse(state.moveTo("appearance", values)) }
        assertFalse(state.moveTo(null, values))
    }
    @Test fun severalChangesProduceOneConfirmationOnlyOnExit() {
        val state = SpaceCompassSettingsExitConfirmation()
        state.moveTo("observer", mapOf("rate" to "1"))
        assertFalse(state.moveTo("observer", mapOf("rate" to "60")))
        assertFalse(state.moveTo("observer", mapOf("rate" to "3600", "zone" to "UTC")))
        assertTrue(state.moveTo(null, mapOf("rate" to "3600", "zone" to "UTC")))
        assertFalse(state.moveTo(null, mapOf("rate" to "3600", "zone" to "UTC")))
    }
    @Test fun aVisitSurvivesRecreationAndTheNextPageStartsClean() {
        val initial = mapOf("language" to "system")
        val before = SpaceCompassSettingsExitConfirmation(); before.moveTo("language", initial)
        val restored = SpaceCompassSettingsExitConfirmation(before.page, before.baseline)
        assertFalse(restored.moveTo("language", mapOf("language" to "it")))
        assertTrue(restored.moveTo("units", mapOf("language" to "it")))
        assertFalse(restored.moveTo(null, mapOf("language" to "it")))
    }
    @Test fun revertedValuesAndAnInvalidFinalDraftDoNotShowSuccess() {
        val state = SpaceCompassSettingsExitConfirmation()
        state.moveTo("observer", mapOf("position" to "original"))
        state.moveTo("observer", mapOf("position" to "edited"))
        assertFalse(state.moveTo(null, mapOf("position" to "original")))
        state.moveTo("observer", mapOf("position" to "original"))
        state.rejectExit()
        assertFalse(state.moveTo(null, mapOf("position" to "edited")))
    }
}

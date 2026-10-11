package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassControlNoticesTest {
    @Test fun closingFullStopsAndTrailingWhitespaceAreRemoved() {
        assertEquals("Settings saved", spaceCompassBottomMessageText("Settings saved.  "))
    }

    @Test fun nonLatinFullStopsAreRemoved() {
        for (message in listOf("已启用天体。", "चालू है।", "فعال شد۔"))
            assertEquals(message.dropLast(1), spaceCompassBottomMessageText(message))
    }

    @Test fun progressEllipsesArePreserved() {
        assertEquals("Preparing image…", spaceCompassBottomMessageText("Preparing image…  "))
        assertEquals("Preparing image...", spaceCompassBottomMessageText("Preparing image..."))
    }

    @Test fun InteriorPeriodsAndDecimalValuesArePreserved() {
        assertEquals("Space Compass 1.2.5: 1.5×", spaceCompassBottomMessageText("Space Compass 1.2.5: 1.5×."))
    }

    @Test fun otherPunctuationAndUnpunctuatedActionsArePreserved() {
        for (message in listOf("Retry", "Permission required!", "Update available?"))
            assertEquals(message, spaceCompassBottomMessageText(message))
    }

    @Test fun emptyMessagesAreSafe() {
        assertEquals("", spaceCompassBottomMessageText("  "))
    }

    private fun draft() = SpaceCompassObserverDraft(false, false, "45", "9", "100", "UTC",
        "2026-10-11", "20:32", simulateAltitude = false, simulateZone = false)

    @Test fun scenarioEditsOnlyAnnounceTheFirstEnabledAndLastDisabledOverride() {
        val initial = draft()
        val edits = listOf(initial, initial.copy(simulateTime = true),
            initial.copy(simulateTime = true, time = "20:33"),
            initial.copy(simulateTime = true, simulateAltitude = true),
            initial.copy(simulateAltitude = true), initial)
        var plan: SpaceCompassObserverPlan? = null
        val notices = mutableListOf<Boolean>()
        for (edit in edits) {
            val next = edit.resolve(plan, 1791748800000L, "UTC").getOrThrow()
            spaceCompassActivationNotice(plan != null, next != null)?.let(notices::add)
            plan = next
        }
        assertEquals(listOf(true, false), notices)
    }

    @Test fun incompleteEnabledScenarioCannotAnnounceDeactivation() {
        val original = draft().copy(simulatePosition = true)
        val plan = original.resolve(null, 1791748800000L, "UTC").getOrThrow()
        val pending = original.copy(latitude = "").resolve(plan, 1791748800000L, "UTC")
        assertTrue(pending.isFailure)
        // A failed draft retains the saved plan; getOrNull alone must not mean deactivation.
        val effective = if (pending.isSuccess) pending.getOrNull() else plan
        assertNull(spaceCompassActivationNotice(plan != null, effective != null))
    }

    @Test fun objectEditsAndActiveBodyChangesOnlyAnnounceEmptySelectionTransitions() {
        val empty = SpaceCompassCelestialSelection(emptySet(), null)
        val sun = empty.toggle(SpaceCompassCelestialBody.SUN)
        val both = sun.toggle(SpaceCompassCelestialBody.MOON)
        val moonActive = both.copy(active = SpaceCompassCelestialBody.MOON)
        val moon = moonActive.toggle(SpaceCompassCelestialBody.SUN)
        val edits = listOf(empty, sun, both, moonActive, moon, moon.toggleVisible(setOf(SpaceCompassCelestialBody.MOON)))
        val notices = edits.zipWithNext().mapNotNull { (before, after) ->
            spaceCompassActivationNotice(before.selected.isNotEmpty(), after.selected.isNotEmpty())
        }
        assertEquals(listOf(true, false), notices)
    }
}

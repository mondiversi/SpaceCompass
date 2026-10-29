package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialSelectionTest {
    @Test fun staleRemovalNeverRechecksAnObjectAndLeavesOtherSelectionsAlone() {
        val before = SpaceCompassCelestialSelection().toggle(SpaceCompassCelestialBody.MARS)
        val after = before.deselect(SpaceCompassCelestialBody.MOON)
        assertEquals(before.selected - SpaceCompassCelestialBody.MOON, after.selected)
        assertEquals(before.active, after.active)
        assertSame(after, after.deselect(SpaceCompassCelestialBody.MOON))
        assertSame(after, after.deselect(SpaceCompassCelestialBody.ISS))
    }

    @Test fun removingTheLastObjectRestoresAsAnExplicitlyEmptySelection() {
        val last = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.MOON), SpaceCompassCelestialBody.MOON)
        val empty = last.deselect(SpaceCompassCelestialBody.MOON)
        assertTrue(empty.selected.isEmpty())
        assertNull(empty.active)
        assertEquals(empty, restoreSpaceCompassCelestialSelection(empty.ordered.map { it.name }.toSet(), empty.active?.name))
    }

    @Test fun sunOnlyImmediatelyExcludesOtherCachedPositionsAndPaths() {
        val catalogue = SpaceCompassCelestialBody.entries.associateWith { it.name }
        val visible = spaceCompassSelectedCelestialEntries(catalogue, setOf(SpaceCompassCelestialBody.SUN))
        assertEquals(mapOf(SpaceCompassCelestialBody.SUN to "SUN"), visible)
        assertEquals(SpaceCompassCelestialBody.entries.size, catalogue.size)
    }
    @Test fun emptySelectionHasNoLiveMarkersPathsOrInteractiveTargets() {
        val catalogue = SpaceCompassCelestialBody.entries.associateWith { it.name }
        assertTrue(spaceCompassSelectedCelestialEntries(catalogue, emptySet()).isEmpty())
    }
    @Test fun changingSelectionFiltersOldCachedEntriesWithoutWaitingForRefresh() {
        val catalogue = SpaceCompassCelestialBody.entries.associateWith { it.name }
        val checked = setOf(SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.MARS)
        assertEquals(checked, spaceCompassSelectedCelestialEntries(catalogue, checked).keys)
        assertEquals(setOf(SpaceCompassCelestialBody.MARS),
            spaceCompassSelectedCelestialEntries(catalogue, checked - SpaceCompassCelestialBody.MOON).keys)
    }
    @Test fun missingSelectedEphemeridesDoNotCreateOtherObjectsOrFakePositions() {
        val cached = mapOf(SpaceCompassCelestialBody.SUN to "SUN", SpaceCompassCelestialBody.MOON to "MOON")
        assertTrue(spaceCompassSelectedCelestialEntries(cached, setOf(SpaceCompassCelestialBody.SEDNA)).isEmpty())
    }
    @Test fun sceneReceivesOnlyCheckedCachedObjects() {
        val screen = java.io.File("src/main/java/me/mondiversi/spacecompass/SpaceCompassSunFinderScreen.kt").readText()
        assertTrue(screen.contains("spaceCompassSelectedCelestialEntries(allOverlays, selectedBodies)"))
        assertTrue(screen.contains("val overlays = selectedOverlays - body"))
        assertTrue(screen.contains("val activeOverlay = selectedOverlays[body]"))
        assertFalse(screen.contains("val overlays = if (hasActiveBody) allOverlays - body else allOverlays"))
    }
    @Test fun startsWithSunAndMoonAndInspectsTheSun() {
        val state = SpaceCompassCelestialSelection()
        assertEquals(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON), state.selected)
        assertEquals(SpaceCompassCelestialBody.SUN, state.active)
        assertEquals(listOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON), state.ordered)
    }
    @Test fun newlyCheckedObjectBecomesTheInspectedObjectWithoutLosingOtherChecks() {
        val state = SpaceCompassCelestialSelection().toggle(SpaceCompassCelestialBody.ISS)
        assertEquals(SpaceCompassCelestialBody.ISS, state.active)
        assertEquals(listOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.MOON), state.ordered)
    }
    @Test fun lastNewlyCheckedObjectRemainsActiveAfterRestoringTheSavedSelection() {
        var state = SpaceCompassCelestialSelection()
        for (body in listOf(SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.VOYAGER_1)) {
            val previousChecks = state.selected
            state = state.toggle(body)
            assertEquals(previousChecks + body, state.selected)
            assertEquals(body, state.active)
            assertEquals(state, restoreSpaceCompassCelestialSelection(
                state.ordered.map { it.name }.toSet(), state.active?.name))
        }
    }
    @Test fun arrowsCycleCheckedObjectsAndWrapInBothDirections() {
        val state = SpaceCompassCelestialSelection().toggle(SpaceCompassCelestialBody.VOYAGER_1)
            .copy(active = SpaceCompassCelestialBody.SUN)
        assertEquals(SpaceCompassCelestialBody.MOON, state.step(1).active)
        assertEquals(SpaceCompassCelestialBody.VOYAGER_1, state.step(-1).active)
        assertEquals(state.active, state.step(3).active)
        assertEquals(state.selected, state.step(1).selected)
    }
    @Test fun removingTheActiveObjectChoosesAnotherCheckedObject() {
        val state = SpaceCompassCelestialSelection().deselect(SpaceCompassCelestialBody.SUN)
        assertEquals(SpaceCompassCelestialBody.MOON, state.active)
        assertEquals(setOf(SpaceCompassCelestialBody.MOON), state.selected)
    }
    @Test fun noSelectionIsValidAndNavigationIsANoop() {
        val state = SpaceCompassCelestialSelection().toggle(SpaceCompassCelestialBody.SUN).toggle(SpaceCompassCelestialBody.MOON)
        assertTrue(state.selected.isEmpty()); assertNull(state.active)
        assertEquals(state, state.step(1)); assertEquals(state, state.step(-1))
        assertEquals(SpaceCompassCelestialBody.ISS, state.toggle(SpaceCompassCelestialBody.ISS).active)
    }
    @Test fun masterSelectsAllFromEmptyOrPartialThenClearsEverything() {
        for (state in listOf(SpaceCompassCelestialSelection(), SpaceCompassCelestialSelection(emptySet(), null))) {
            val all = state.toggleAll()
            assertEquals(spaceCompassCelestialCatalogOrder.toSet(), all.selected)
            assertNotNull(all.active)
            assertEquals(SpaceCompassCelestialSelection(emptySet(), null), all.toggleAll())
        }
    }
    @Test fun masterKeepsAnAlreadyActiveObject() {
        val state = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.MOON), SpaceCompassCelestialBody.MOON)
        assertEquals(SpaceCompassCelestialBody.MOON, state.toggleAll().active)
    }
    @Test fun deselectingAnInactiveObjectDoesNotSwitchTheControls() {
        val state = SpaceCompassCelestialSelection().toggle(SpaceCompassCelestialBody.ISS)
        val after = state.toggle(SpaceCompassCelestialBody.MOON)
        assertEquals(state.selected - SpaceCompassCelestialBody.MOON, after.selected)
        assertEquals(SpaceCompassCelestialBody.ISS, after.active)
    }
    @Test(expected = IllegalArgumentException::class) fun activeObjectCannotBeOutsideSelection() {
        SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.MOON), SpaceCompassCelestialBody.SUN)
    }
}

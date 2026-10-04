package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialSelectionTest {
    @Test fun sunOnlyImmediatelyExcludesOtherCachedPositionsAndPaths() {
        val catalogue = PlanetCompassCelestialBody.entries.associateWith { it.name }
        val visible = planetCompassSelectedCelestialEntries(catalogue, setOf(PlanetCompassCelestialBody.SUN))
        assertEquals(mapOf(PlanetCompassCelestialBody.SUN to "SUN"), visible)
        assertEquals(PlanetCompassCelestialBody.entries.size, catalogue.size)
    }
    @Test fun emptySelectionHasNoLiveMarkersPathsOrInteractiveTargets() {
        val catalogue = PlanetCompassCelestialBody.entries.associateWith { it.name }
        assertTrue(planetCompassSelectedCelestialEntries(catalogue, emptySet()).isEmpty())
    }
    @Test fun changingSelectionFiltersOldCachedEntriesWithoutWaitingForRefresh() {
        val catalogue = PlanetCompassCelestialBody.entries.associateWith { it.name }
        val checked = setOf(PlanetCompassCelestialBody.MOON, PlanetCompassCelestialBody.MARS)
        assertEquals(checked, planetCompassSelectedCelestialEntries(catalogue, checked).keys)
        assertEquals(setOf(PlanetCompassCelestialBody.MARS),
            planetCompassSelectedCelestialEntries(catalogue, checked - PlanetCompassCelestialBody.MOON).keys)
    }
    @Test fun missingSelectedEphemeridesDoNotCreateOtherObjectsOrFakePositions() {
        val cached = mapOf(PlanetCompassCelestialBody.SUN to "SUN", PlanetCompassCelestialBody.MOON to "MOON")
        assertTrue(planetCompassSelectedCelestialEntries(cached, setOf(PlanetCompassCelestialBody.SEDNA)).isEmpty())
    }
    @Test fun sceneReceivesOnlyCheckedCachedObjects() {
        val screen = java.io.File("src/main/java/me/mondiversi/planetcompass/PlanetCompassSunFinderScreen.kt").readText()
        assertTrue(screen.contains("planetCompassSelectedCelestialEntries(allOverlays, selectedBodies)"))
        assertTrue(screen.contains("val overlays = selectedOverlays - body"))
        assertTrue(screen.contains("val activeOverlay = selectedOverlays[body]"))
        assertFalse(screen.contains("val overlays = if (hasActiveBody) allOverlays - body else allOverlays"))
    }
    @Test fun startsWithSunAndMoonAndInspectsTheSun() {
        val state = PlanetCompassCelestialSelection()
        assertEquals(setOf(PlanetCompassCelestialBody.SUN, PlanetCompassCelestialBody.MOON), state.selected)
        assertEquals(PlanetCompassCelestialBody.SUN, state.active)
        assertEquals(listOf(PlanetCompassCelestialBody.SUN, PlanetCompassCelestialBody.MOON), state.ordered)
    }
    @Test fun productionUsesTheDefaultOnlyToInitializeRestorableSelection() {
        val screen = java.io.File("src/main/java/me/mondiversi/planetcompass/PlanetCompassSunFinderScreen.kt")
            .readText().replace("\r\n", "\n")
        assertTrue(screen.contains("var selectedNames by rememberSaveable {\n        mutableStateOf(PlanetCompassCelestialSelection().ordered.map { it.name })\n    }"))
        assertTrue(screen.contains("selectedNames = selection.ordered.map { it.name }"))
    }
    @Test fun addingObjectsDoesNotChangeTheInspectedObject() {
        val state = PlanetCompassCelestialSelection().toggle(PlanetCompassCelestialBody.ISS)
        assertEquals(PlanetCompassCelestialBody.SUN, state.active)
        assertEquals(listOf(PlanetCompassCelestialBody.SUN, PlanetCompassCelestialBody.ISS, PlanetCompassCelestialBody.MOON), state.ordered)
    }
    @Test fun arrowsCycleCheckedObjectsAndWrapInBothDirections() {
        val state = PlanetCompassCelestialSelection().toggle(PlanetCompassCelestialBody.VOYAGER_1)
        assertEquals(PlanetCompassCelestialBody.MOON, state.step(1).active)
        assertEquals(PlanetCompassCelestialBody.VOYAGER_1, state.step(-1).active)
        assertEquals(state.active, state.step(3).active)
        assertEquals(state.selected, state.step(1).selected)
    }
    @Test fun removingTheActiveObjectChoosesAnotherCheckedObject() {
        val state = PlanetCompassCelestialSelection().toggle(PlanetCompassCelestialBody.SUN)
        assertEquals(PlanetCompassCelestialBody.MOON, state.active)
        assertEquals(setOf(PlanetCompassCelestialBody.MOON), state.selected)
    }
    @Test fun noSelectionIsValidAndNavigationIsANoop() {
        val state = PlanetCompassCelestialSelection().toggle(PlanetCompassCelestialBody.SUN).toggle(PlanetCompassCelestialBody.MOON)
        assertTrue(state.selected.isEmpty()); assertNull(state.active)
        assertEquals(state, state.step(1)); assertEquals(state, state.step(-1))
        assertEquals(PlanetCompassCelestialBody.ISS, state.toggle(PlanetCompassCelestialBody.ISS).active)
    }
    @Test fun masterSelectsAllFromEmptyOrPartialThenClearsEverything() {
        for (state in listOf(PlanetCompassCelestialSelection(), PlanetCompassCelestialSelection(emptySet(), null))) {
            val all = state.toggleAll()
            assertEquals(planetCompassCelestialCatalogOrder.toSet(), all.selected)
            assertNotNull(all.active)
            assertEquals(PlanetCompassCelestialSelection(emptySet(), null), all.toggleAll())
        }
    }
    @Test fun masterKeepsAnAlreadyActiveObject() {
        val state = PlanetCompassCelestialSelection(setOf(PlanetCompassCelestialBody.MOON), PlanetCompassCelestialBody.MOON)
        assertEquals(PlanetCompassCelestialBody.MOON, state.toggleAll().active)
    }
    @Test fun deselectingAnInactiveObjectDoesNotSwitchTheControls() {
        val state = PlanetCompassCelestialSelection().toggle(PlanetCompassCelestialBody.MOON).toggle(PlanetCompassCelestialBody.MOON)
        assertEquals(PlanetCompassCelestialSelection(), state)
    }
    @Test(expected = IllegalArgumentException::class) fun activeObjectCannotBeOutsideSelection() {
        PlanetCompassCelestialSelection(setOf(PlanetCompassCelestialBody.MOON), PlanetCompassCelestialBody.SUN)
    }
}

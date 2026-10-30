package me.mondiversi.spacecompass

import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogViewStateTest {
    private val sun = SpaceCompassCelestialBody.SUN
    private val moon = SpaceCompassCelestialBody.MOON
    private val mars = SpaceCompassCelestialBody.MARS
    private val selected = setOf(moon)
    private val emptyDistances = SpaceCompassCatalogDistances(emptyMap(), emptyMap(), emptyMap())
    private fun observation(elevation: Double) = SpaceCompassCelestialObservation(SpaceCompassSunPosition(90.0, elevation), 100.0)
    private fun query(observations: Map<SpaceCompassCelestialBody, SpaceCompassCelestialObservation?> =
        mapOf(sun to observation(20.0), moon to observation(25.0), mars to observation(-10.0)),
        preferences: SpaceCompassCatalogPreferences = SpaceCompassCatalogPreferences(
            visibility = SpaceCompassCatalogVisibility.ABOVE, pointing = SpaceCompassCatalogPointing.AVAILABLE),
        search: String = "", selection: Set<SpaceCompassCelestialBody> = selected,
        favorites: Set<SpaceCompassCelestialBody> = emptySet(),
        distances: SpaceCompassCatalogDistances = emptyDistances) = SpaceCompassCatalogQuery(
            preferences, search, selection, selected, favorites, observations, distances)
    private suspend fun presenter() = SpaceCompassCatalogPresenter.create(listOf(sun, moon, mars),
        Locale.ENGLISH, { it.name }, { it.name })

    @Test fun skyRefreshKeepsRowsAndControlsUntilTheCompleteResultIsPublished() = runBlocking {
        val calculator = presenter()
        val first = query()
        val published = calculator.present(first, true)
        val state = SpaceCompassCatalogViewState().publish(published)
        assertEquals(listOf(moon, sun), state.presentation!!.visible)
        val refresh = query(mapOf(sun to observation(-2.0), moon to observation(-3.0), mars to observation(5.0)),
            distances = SpaceCompassCatalogDistances(emptyMap(), emptyMap(), mapOf(moon to 102.0)))
        assertNotSame(first, refresh)
        assertTrue(state.presentation.query.hasSameControlsAs(refresh))
        assertSame(published, state.filterPresentation)
        assertEquals(listOf(moon, sun), state.presentation.visible)
        val completed = calculator.present(refresh, true)
        val updated = state.publish(completed)
        assertEquals(listOf(moon, mars), updated.presentation!!.visible)
        assertSame(completed, updated.filterPresentation)
        assertSame(published, state.presentation)
    }

    @Test fun closingThePanelPreservesItsLastFacetsWithoutDoingHiddenFacetWork() = runBlocking {
        val calculator = presenter()
        val first = query()
        val closed = calculator.present(first, false)
        assertNull(SpaceCompassCatalogViewState().publish(closed).filterPresentation)
        val opened = calculator.present(first, true)
        assertNotNull(opened.counts)
        val state = SpaceCompassCatalogViewState().publish(opened)
        val refresh = calculator.present(query(distances = SpaceCompassCatalogDistances(
            emptyMap(), emptyMap(), mapOf(sun to 200.0))), false)
        assertNull(refresh.counts)
        assertTrue(refresh.typeOrder.isEmpty())
        val updated = state.publish(refresh)
        assertSame(refresh, updated.presentation)
        assertSame(opened, updated.filterPresentation)
        assertEquals(opened.visible.size, updated.filterPresentation!!.visible.size)
        assertSame(opened.counts, updated.filterPresentation.counts)
    }

    @Test fun onlyActualUserScopeChangesInvalidateBulkSelection() {
        val original = query()
        assertTrue(original.hasSameControlsAs(query(selection = selected.toMutableSet())))
        assertFalse(original.hasSameControlsAs(query(search = "Mars")))
        assertFalse(original.hasSameControlsAs(query(selection = setOf(moon, mars))))
        assertFalse(original.hasSameControlsAs(query(favorites = setOf(sun))))
        assertFalse(original.hasSameControlsAs(query(preferences = original.preferences.copy(
            visibility = SpaceCompassCatalogVisibility.BELOW))))
    }
}

package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SpaceCompassCatalogDisplayOrderTest {
    private val sun = SpaceCompassCelestialBody.SUN
    private val moon = SpaceCompassCelestialBody.MOON
    private val mars = SpaceCompassCelestialBody.MARS
    private val venus = SpaceCompassCelestialBody.VENUS
    private val hidden = SpaceCompassCelestialBody.LV_426
    private val bodies = listOf(sun, venus, moon, mars)
    private val names = mapOf(sun to "Sole", moon to "Luna", mars to "Marte", venus to "Venere")

    @Test fun checkedObjectsBypassCombinedTypeHorizonAndSearchFilters() {
        val selected = setOf(sun, moon)
        val types = setOf(SpaceCompassCatalogType.PLANET)
        val elevations = mapOf(sun to -5.0, moon to null, mars to 12.0, venus to 4.0)
        val matching = spaceCompassSearchCatalog(spaceCompassFilterCatalog(types,
            SpaceCompassCatalogVisibility.ABOVE, elevations, bodies), names, "Marte").toSet()
        assertEquals(setOf(mars), matching)
        assertEquals(listOf(sun, moon, mars), spaceCompassCatalogDisplayOrder(bodies, matching, selected))
        assertEquals(setOf(sun, moon), selected)
        assertEquals(setOf(SpaceCompassCatalogType.PLANET), types)
    }

    @Test fun deselectingAnExcludedObjectHidesItWhileMatchingObjectsRemain() {
        val matching = setOf(mars)
        assertEquals(listOf(moon, mars), spaceCompassCatalogDisplayOrder(bodies, matching, setOf(moon, mars)))
        assertEquals(listOf(mars), spaceCompassCatalogDisplayOrder(bodies, matching, setOf(mars)))
        assertEquals(listOf(mars), spaceCompassCatalogDisplayOrder(bodies, matching, emptySet()))
    }

    @Test fun eachSortCriterionAndDirectionIsPreservedInsideBothGroups() {
        val selected = setOf(moon, venus)
        val solar = mapOf(sun to 0.0, moon to 1.0, venus to .7, mars to 1.5)
        val earth = mapOf(sun to 149e6, moon to 384400.0, venus to 42e6, mars to 80e6)
        for (sort in SpaceCompassCatalogSort.entries) {
            val sorted = spaceCompassSortCatalog(bodies, sort, names, solar, Locale.ITALIAN, earth)
            val actual = spaceCompassCatalogDisplayOrder(sorted, bodies.toSet(), selected)
            assertEquals(sort.name, sorted.filter { it in selected } + sorted.filterNot { it in selected }, actual)
            assertEquals(selected, actual.take(selected.size).toSet())
        }
    }

    @Test fun alphabeticalOrderingIsRetainedForCheckedAndUncheckedRows() {
        val selected = setOf(venus, moon)
        fun visible(sort: SpaceCompassCatalogSort) = spaceCompassCatalogDisplayOrder(
            spaceCompassSortCatalog(bodies, sort, names, emptyMap(), Locale.ITALIAN), bodies.toSet(), selected)
        assertEquals(listOf(moon, venus, mars, sun), visible(SpaceCompassCatalogSort.NAME_ASC))
        assertEquals(listOf(venus, moon, sun, mars), visible(SpaceCompassCatalogSort.NAME_DESC))
    }

    @Test fun missingDistancesStayLastInTheirOwnSelectionGroupInEitherDirection() {
        val selected = setOf(venus, moon)
        val distances = mapOf(moon to 1.0, mars to 1.5)
        for (sort in listOf(SpaceCompassCatalogSort.DISTANCE_ASC, SpaceCompassCatalogSort.DISTANCE_DESC)) {
            val sorted = spaceCompassSortCatalog(bodies, sort, names, distances, Locale.ROOT)
            assertEquals(listOf(moon, venus, mars, sun),
                spaceCompassCatalogDisplayOrder(sorted, bodies.toSet(), selected))
        }
    }

    @Test fun defaultOrderAndEqualSortKeysRemainStable() {
        val selected = setOf(mars, moon)
        assertEquals(listOf(moon, mars, sun, venus), spaceCompassCatalogDisplayOrder(bodies, bodies.toSet(), selected))
        val sorted = spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.DISTANCE_DESC,
            names, bodies.associateWith { 1.0 }, Locale.ROOT)
        assertEquals(listOf(moon, mars, sun, venus), spaceCompassCatalogDisplayOrder(sorted, bodies.toSet(), selected))
    }

    @Test fun emptyFiltersOrSelectionsProduceNoDuplicatesAndNoInventedRows() {
        assertEquals(listOf(sun, moon), spaceCompassCatalogDisplayOrder(bodies, setOf(sun), setOf(sun, moon)))
        assertEquals(bodies, spaceCompassCatalogDisplayOrder(bodies, bodies.toSet(), emptySet()))
        assertEquals(emptyList<SpaceCompassCelestialBody>(), spaceCompassCatalogDisplayOrder(bodies, emptySet(), emptySet()))
        assertEquals(emptyList<SpaceCompassCelestialBody>(), spaceCompassCatalogDisplayOrder(emptyList(), bodies.toSet(), bodies.toSet()))
        assertEquals(emptyList<SpaceCompassCelestialBody>(), spaceCompassCatalogDisplayOrder(bodies, setOf(hidden), setOf(hidden)))
    }

    @Test fun hiddenObjectStaysHiddenUntilRevealedAndDisappearsWhenUnchecked() {
        val selected = setOf(sun, hidden)
        val available = spaceCompassAvailableCelestialCatalog(selected)
        assertEquals(listOf(sun, hidden), spaceCompassCatalogDisplayOrder(available, emptySet(), selected))
        assertEquals(listOf(sun), spaceCompassCatalogDisplayOrder(
            spaceCompassAvailableCelestialCatalog(setOf(sun)), emptySet(), setOf(sun)))
    }

    @Test fun selectAllOnlyTouchesDisplayedObjectsIncludingCheckedFilterExceptions() {
        val original = SpaceCompassCelestialSelection(setOf(sun, moon), moon)
        val visible = spaceCompassCatalogDisplayOrder(bodies, setOf(mars), original.selected).toSet()
        val checked = original.toggleVisible(visible)
        assertEquals(setOf(sun, moon, mars), checked.selected)
        assertEquals(moon, checked.active)
        assertFalse(venus in checked.selected)
        assertTrue(checked.toggleVisible(visible).selected.isEmpty())
    }
}

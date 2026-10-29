package me.mondiversi.spacecompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogFavoriteFilterTest {
    private val sun = SpaceCompassCelestialBody.SUN
    private val moon = SpaceCompassCelestialBody.MOON
    private val mars = SpaceCompassCelestialBody.MARS
    private val bodies = listOf(sun, moon, mars)

    @Test fun checkedNonFavoritesCannotBypassTheFavoritesFilter() {
        assertEquals(listOf(moon), spaceCompassCatalogDisplayOrder(bodies, bodies.toSet(), setOf(sun, moon),
            favorites = setOf(moon)))
    }

    @Test fun anEmptyFavoritesFilterShowsNothingAndDisablingItRestoresTheCatalog() {
        assertTrue(spaceCompassCatalogDisplayOrder(bodies, bodies.toSet(), setOf(sun), favorites = emptySet()).isEmpty())
        assertEquals(bodies, spaceCompassCatalogDisplayOrder(bodies, bodies.toSet(), setOf(sun), favorites = null))
    }

    @Test fun favoritesCombineWithSearchAndExistingCheckedFilterExceptions() {
        assertEquals(listOf(moon, mars), spaceCompassCatalogDisplayOrder(bodies, setOf(mars), setOf(sun, moon),
            prioritySelected = setOf(sun, moon), favorites = setOf(moon, mars)))
        assertEquals(listOf(mars), spaceCompassCatalogDisplayOrder(bodies, setOf(mars), setOf(sun),
            favorites = setOf(moon, mars)))
    }

    @Test fun favoritesKeepTheChosenSortAndDeferredCheckPriority() {
        val names = mapOf(sun to "Sun", moon to "Moon", mars to "Mars")
        val sorted = spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.NAME_ASC, names, emptyMap(), Locale.ENGLISH)
        assertEquals(listOf(mars, moon), spaceCompassCatalogDisplayOrder(sorted, bodies.toSet(), setOf(moon),
            prioritySelected = emptySet(), favorites = setOf(moon, mars)))
        assertEquals(listOf(moon, mars), spaceCompassCatalogDisplayOrder(sorted, bodies.toSet(), setOf(moon),
            favorites = setOf(moon, mars)))
    }

    @Test fun missingFavoritesPreferenceIsOffAndBothStatesRestoreAlongsideOtherFilters() {
        assertFalse(restoreSpaceCompassCatalogPreferences(null, null, null).favoritesOnly)
        for (enabled in listOf(false, true)) {
            val restored = restoreSpaceCompassCatalogPreferences("NAME_DESC", setOf("STAR"), "ABOVE",
                "REAL", "AVAILABLE", enabled)
            assertEquals(enabled, restored.favoritesOnly)
            assertEquals(SpaceCompassCatalogSort.NAME_DESC, restored.sort)
            assertEquals(setOf(SpaceCompassCatalogType.STAR), restored.types)
        }
    }
}

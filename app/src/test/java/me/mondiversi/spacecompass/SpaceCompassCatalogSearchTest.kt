package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogSearchTest {
    private val available = listOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON,
        SpaceCompassCelestialBody.ALPHA_CENTAURI, SpaceCompassCelestialBody.VENUS,
        SpaceCompassCelestialBody.COMET_67P)
    private val names = mapOf(SpaceCompassCelestialBody.SUN to "Sole", SpaceCompassCelestialBody.MOON to "Luna",
        SpaceCompassCelestialBody.ALPHA_CENTAURI to "Alfa Centauri", SpaceCompassCelestialBody.VENUS to "Venere",
        SpaceCompassCelestialBody.COMET_67P to "67P/Churyumov–Gerasimenko")

    @Test fun searchUsesLocalizedNamesAndScientificAliasesWithoutCaseAccentsOrSeparatorDifferences() {
        assertEquals(available, spaceCompassSearchCatalog(available, names, "  "))
        assertEquals(listOf(SpaceCompassCelestialBody.ALPHA_CENTAURI), spaceCompassSearchCatalog(available, names, " ÁLFA cén "))
        assertEquals(listOf(SpaceCompassCelestialBody.ALPHA_CENTAURI), spaceCompassSearchCatalog(available, names, "Alpha-Centauri"))
        assertEquals(listOf(SpaceCompassCelestialBody.VENUS), spaceCompassSearchCatalog(available, names, "VENERE"))
        assertEquals(listOf(SpaceCompassCelestialBody.COMET_67P), spaceCompassSearchCatalog(available, names, "67p/Chur"))
        assertTrue(spaceCompassSearchCatalog(available, names, "nothing matches").isEmpty())
    }

    @Test fun searchIntersectsOtherFiltersAndSelectionPreservesTheHiddenCheckedObjects() {
        val elevations = mapOf(SpaceCompassCelestialBody.SUN to 15.0, SpaceCompassCelestialBody.ALPHA_CENTAURI to -5.0)
        val filtered = spaceCompassFilterCatalog(setOf(SpaceCompassCatalogType.STAR),
            SpaceCompassCatalogVisibility.ABOVE, elevations, available)
        assertTrue(spaceCompassSearchCatalog(filtered, names, "centauri").isEmpty())
        val shown = spaceCompassSearchCatalog(filtered, names, "sole")
        assertEquals(listOf(SpaceCompassCelestialBody.SUN), shown)
        val original = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.MOON), SpaceCompassCelestialBody.MOON)
        val changed = original.toggleVisible(shown.toSet())
        assertEquals(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON), changed.selected)
        assertEquals(original.active, changed.active)
        assertEquals(original, changed.toggleVisible(shown.toSet()))
    }

    @Test fun searchCannotExposeTheHiddenObjectAndHandlesNonLatinNames() {
        assertTrue(spaceCompassSearchCatalog(available, names, "LV426").isEmpty())
        assertEquals(listOf(SpaceCompassCelestialBody.LV_426),
            spaceCompassSearchCatalog(available + SpaceCompassCelestialBody.LV_426, names, "LV-426"))
        assertEquals(listOf(SpaceCompassCelestialBody.MOON),
            spaceCompassSearchCatalog(available, mapOf(SpaceCompassCelestialBody.MOON to "القَمَر"), "قمر"))
        assertEquals(listOf(SpaceCompassCelestialBody.ALPHA_CENTAURI, SpaceCompassCelestialBody.MOON),
            spaceCompassSearchCatalog(listOf(SpaceCompassCelestialBody.ALPHA_CENTAURI, SpaceCompassCelestialBody.MOON),
                mapOf(SpaceCompassCelestialBody.ALPHA_CENTAURI to "Test", SpaceCompassCelestialBody.MOON to "Test"), "test"))
    }
}

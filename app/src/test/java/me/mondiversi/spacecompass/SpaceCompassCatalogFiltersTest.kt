package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogFiltersTest {
    @Test fun allBodiesHaveAnExplicitTypeAndMultipleTypesKeepTheRequestedOrder() {
        val groups = spaceCompassCelestialCatalogOrder.groupBy { it.catalogType }
        assertEquals(SpaceCompassCatalogType.entries.toSet(), groups.keys)
        assertEquals(listOf(SpaceCompassCelestialBody.ISS), groups[SpaceCompassCatalogType.SPACE_STATION])
        assertEquals(listOf(SpaceCompassCelestialBody.STARLINK_V3), groups[SpaceCompassCatalogType.ARTIFICIAL_SATELLITE])
        assertEquals(listOf(SpaceCompassCelestialBody.RX_J1856, SpaceCompassCelestialBody.PSR_J0437), groups[SpaceCompassCatalogType.NEUTRON_STAR])
        val types = setOf(SpaceCompassCatalogType.STAR, SpaceCompassCatalogType.BLACK_HOLE)
        assertEquals(spaceCompassCelestialCatalogOrder.filter { it.catalogType in types },
            spaceCompassFilterCatalog(types, SpaceCompassCatalogVisibility.ALL, emptyMap()))
    }
    @Test fun visibilityUsesGeometricHorizonAndUnknownPositionsRemainOnlyInAll() {
        val elevations = mapOf(SpaceCompassCelestialBody.SUN to 0.0, SpaceCompassCelestialBody.MOON to -1.0,
            SpaceCompassCelestialBody.EARTH_CENTER to -89.8, SpaceCompassCelestialBody.MARS to Double.NaN)
        assertEquals(spaceCompassCelestialCatalogOrder, spaceCompassFilterCatalog(emptySet(), SpaceCompassCatalogVisibility.ALL, elevations))
        assertEquals(listOf(SpaceCompassCelestialBody.SUN), spaceCompassFilterCatalog(emptySet(), SpaceCompassCatalogVisibility.ABOVE, elevations))
        assertEquals(listOf(SpaceCompassCelestialBody.MOON),
            spaceCompassFilterCatalog(emptySet(), SpaceCompassCatalogVisibility.BELOW, elevations))
        assertTrue(spaceCompassFilterCatalog(setOf(SpaceCompassCatalogType.STAR), SpaceCompassCatalogVisibility.BELOW, elevations).isEmpty())
    }
    @Test fun selectAllFilteredPreservesHiddenChecksAndActiveObjectAndEmptyResultsDoNothing() {
        val original = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.MOON), SpaceCompassCelestialBody.MOON)
        val visible = setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MARS)
        val checked = original.toggleVisible(visible)
        assertEquals(visible + original.selected, checked.selected)
        assertEquals(original.active, checked.active)
        assertEquals(original, checked.toggleVisible(visible))
        assertEquals(original, original.toggleVisible(emptySet()))
        val cleared = checked.toggleVisible(checked.selected)
        assertTrue(cleared.selected.isEmpty()); assertNull(cleared.active)
        assertEquals(setOf(SpaceCompassCelestialBody.MARS), checked.toggleVisible(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON)).selected)
    }
}

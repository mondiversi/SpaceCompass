package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogExtraFiltersTest {
    private val sun = SpaceCompassCelestialBody.SUN
    private val moon = SpaceCompassCelestialBody.MOON
    private val mars = SpaceCompassCelestialBody.MARS
    private val fiction = SpaceCompassCelestialBody.ARRAKIS
    private val bodies = listOf(fiction, mars, moon, sun)

    private fun filter(origin: SpaceCompassCatalogOrigin = SpaceCompassCatalogOrigin.ALL,
        pointing: SpaceCompassCatalogPointing = SpaceCompassCatalogPointing.ALL,
        ready: Map<SpaceCompassCelestialBody, Boolean> = emptyMap(),
        visibility: SpaceCompassCatalogVisibility = SpaceCompassCatalogVisibility.ALL,
        types: Set<SpaceCompassCatalogType> = emptySet(),
        elevations: Map<SpaceCompassCelestialBody, Double?> = emptyMap()) =
        spaceCompassFilterCatalog(types, visibility, elevations, bodies, origin, pointing, ready)

    @Test fun originIntersectsPhysicalTypeWithoutLosingInputOrder() {
        val planets = setOf(SpaceCompassCatalogType.PLANET)
        assertEquals(listOf(fiction, mars), filter(types = planets))
        assertEquals(listOf(mars), filter(origin = SpaceCompassCatalogOrigin.REAL, types = planets))
        assertEquals(listOf(fiction), filter(origin = SpaceCompassCatalogOrigin.FICTION, types = planets))
        assertEquals(listOf(mars, moon, sun), filter(origin = SpaceCompassCatalogOrigin.REAL))
    }

    @Test fun pointingRequiresValidAnglesButNotADistanceOrAnAboveHorizonPosition() {
        fun observation(azimuth: Double, elevation: Double) =
            SpaceCompassCelestialObservation(SpaceCompassSunPosition(azimuth, elevation), Double.NaN)
        assertTrue(spaceCompassCatalogHasPointing(observation(180.0, -45.0)))
        assertTrue(spaceCompassCatalogHasPointing(observation(0.0, 90.0)))
        assertFalse(spaceCompassCatalogHasPointing(null))
        assertFalse(spaceCompassCatalogHasPointing(observation(Double.NaN, 0.0)))
        assertFalse(spaceCompassCatalogHasPointing(observation(0.0, Double.POSITIVE_INFINITY)))
        assertFalse(spaceCompassCatalogHasPointing(observation(0.0, 91.0)))
    }

    @Test fun pendingResultsAreNeitherUnavailableNorAvailable() {
        assertEquals(bodies, filter())
        assertTrue(filter(pointing = SpaceCompassCatalogPointing.AVAILABLE).isEmpty())
        assertTrue(filter(pointing = SpaceCompassCatalogPointing.UNAVAILABLE).isEmpty())
        val ready = mapOf(mars to true, moon to false)
        assertEquals(listOf(mars), filter(pointing = SpaceCompassCatalogPointing.AVAILABLE, ready = ready))
        assertEquals(listOf(moon), filter(pointing = SpaceCompassCatalogPointing.UNAVAILABLE, ready = ready))
    }

    @Test fun allFourFiltersIntersectAndUnavailableDataDoesNotBecomeAHorizonGuess() {
        val ready = mapOf(fiction to true, mars to true, moon to false, sun to true)
        val elevations = mapOf(fiction to 20.0, mars to -10.0, moon to null, sun to 30.0)
        assertEquals(listOf(mars), filter(SpaceCompassCatalogOrigin.REAL, SpaceCompassCatalogPointing.AVAILABLE,
            ready, SpaceCompassCatalogVisibility.BELOW, setOf(SpaceCompassCatalogType.PLANET), elevations))
        assertTrue(filter(pointing = SpaceCompassCatalogPointing.UNAVAILABLE, ready = ready,
            visibility = SpaceCompassCatalogVisibility.ABOVE, elevations = elevations).isEmpty())
    }

    @Test fun selectedObjectsStillBypassTheNewFiltersWithoutDuplicatesOrSelectionChanges() {
        val selected = setOf(sun, fiction)
        val matching = filter(SpaceCompassCatalogOrigin.REAL, SpaceCompassCatalogPointing.UNAVAILABLE,
            mapOf(moon to false)).toSet()
        assertEquals(listOf(fiction, sun, moon), spaceCompassCatalogDisplayOrder(bodies, matching, selected))
        assertEquals(setOf(sun, fiction), selected)
    }

    @Test fun everyNewPreferenceCombinationRestoresAlongsideTheExistingFilters() {
        for (origin in SpaceCompassCatalogOrigin.entries) for (pointing in SpaceCompassCatalogPointing.entries) {
            val expected = SpaceCompassCatalogPreferences(SpaceCompassCatalogSort.NAME_DESC,
                setOf(SpaceCompassCatalogType.PLANET), SpaceCompassCatalogVisibility.BELOW, origin, pointing)
            assertEquals(expected, restoreSpaceCompassCatalogPreferences("NAME_DESC", setOf("PLANET"), "BELOW",
                origin.name, pointing.name))
            assertTrue(expected.hasFilters)
        }
        assertFalse(SpaceCompassCatalogPreferences().hasFilters)
        assertTrue(SpaceCompassCatalogPreferences(origin = SpaceCompassCatalogOrigin.REAL).hasFilters)
        assertTrue(SpaceCompassCatalogPreferences(pointing = SpaceCompassCatalogPointing.AVAILABLE).hasFilters)
    }

    @Test fun missingOrRemovedNewPreferencesDoNotDiscardOldChoices() {
        val expected = SpaceCompassCatalogPreferences(SpaceCompassCatalogSort.DISTANCE_DESC,
            setOf(SpaceCompassCatalogType.STAR), SpaceCompassCatalogVisibility.ABOVE)
        assertEquals(expected, restoreSpaceCompassCatalogPreferences("DISTANCE_DESC", setOf("STAR"), "ABOVE"))
        assertEquals(expected, restoreSpaceCompassCatalogPreferences("DISTANCE_DESC", setOf("STAR"), "ABOVE", "OLD", "OLD"))
    }
}

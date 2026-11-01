package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialSceneOrderTest {
    @Test fun selectedMarkersKeepCatalogOrderInsteadOfIdentityOrSelectionOrder() {
        val bodies = linkedSetOf(SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.ISS,
            SpaceCompassCelestialBody.MERCURY, SpaceCompassCelestialBody.SUN)
        assertEquals(listOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MERCURY,
            SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.MOON), spaceCompassOrderedCelestialSubset(bodies))
    }

    @Test fun smallAndLargeSubsetsMatchTheExistingCatalogAcrossEveryIdentity() {
        val catalog = spaceCompassAllCelestialOrder
        for (stride in listOf(1, 7, 101, catalog.size - 1)) {
            val bodies = catalog.filterIndexed { index, _ -> index % stride == 0 }.asReversed().toSet()
            assertEquals(catalog.filter { it in bodies }, spaceCompassOrderedCelestialSubset(bodies))
        }
    }

    @Test fun emptyAndRetiredBodiesCannotIntroduceGhostMarkers() {
        assertTrue(spaceCompassOrderedCelestialSubset(emptySet()).isEmpty())
        assertTrue(spaceCompassOrderedCelestialSubset(setOf(SpaceCompassCelestialBody.EARTH_CENTER)).isEmpty())
    }

    @Test fun locatorPlacementIsIndependentOfProjectionMapInsertionOrder() {
        val bodies = listOf(SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.ISS,
            SpaceCompassCelestialBody.MERCURY, SpaceCompassCelestialBody.SUN)
        val projection = SpaceCompassSunProjection(false, -50.0, 200.0, 90.0)
        val forward = bodies.associateWith { projection }
        val reverse = bodies.asReversed().associateWith { projection }
        val expected = placeSpaceCompassCelestialOffscreenMarkers(forward, 400.0, 600.0, 56.0)
        val actual = placeSpaceCompassCelestialOffscreenMarkers(reverse, 400.0, 600.0, 56.0)
        assertEquals(spaceCompassOrderedCelestialSubset(bodies.toSet()), actual.keys.toList())
        assertEquals(expected, actual)
        assertEquals(bodies.size, actual.values.map { it.center }.toSet().size)
    }
}

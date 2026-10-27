package me.mondiversi.spacecompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogOverlappingTypesTest {
    @Test fun fictionalObjectsAlsoBelongToTheirPhysicalKindWithoutChangingScientificClassification() {
        val expected = mapOf(
            SpaceCompassCatalogType.PLANET to setOf(SpaceCompassCelestialBody.ARRAKIS,
                SpaceCompassCelestialBody.CALADAN, SpaceCompassCelestialBody.GIEDI_PRIME,
                SpaceCompassCelestialBody.CALPAMOS, SpaceCompassCelestialBody.POLYPHEMUS,
                SpaceCompassCelestialBody.VULCAN),
            SpaceCompassCatalogType.NATURAL_SATELLITE to setOf(SpaceCompassCelestialBody.LV_426,
                SpaceCompassCelestialBody.LV_223, SpaceCompassCelestialBody.PANDORA),
            SpaceCompassCatalogType.SPACECRAFT to setOf(SpaceCompassCelestialBody.NOSTROMO,
                SpaceCompassCelestialBody.DISCOVERY_ONE))
        assertEquals(spaceCompassFictionalReferences.map { it.body }.toSet(), expected.values.flatten().toSet())
        for ((kind, bodies) in expected) for (body in bodies) {
            assertEquals(setOf(kind, SpaceCompassCatalogType.SCIENCE_FICTION), body.catalogFilterTypes)
            assertEquals(SpaceCompassCatalogType.SCIENCE_FICTION, body.catalogType)
        }
        for (body in spaceCompassCelestialCatalogOrder.filterNot { it.isFictional }) {
            assertEquals(setOf(body.catalogType), body.catalogFilterTypes)
        }
    }

    @Test fun overlappingSelectionsCountTheUnionAndKeepCatalogueOrderWithoutDuplicates() {
        val catalogue = spaceCompassCelestialCatalogOrder
        val types = setOf(SpaceCompassCatalogType.PLANET, SpaceCompassCatalogType.SCIENCE_FICTION)
        val rows = spaceCompassFilterCatalog(types, SpaceCompassCatalogVisibility.ALL, emptyMap())
        assertEquals(18, rows.size)
        assertEquals(rows.size, rows.distinct().size)
        assertEquals(catalogue.filter { it.isFictional || it.catalogType == SpaceCompassCatalogType.PLANET }, rows)
        assertEquals(rows.size, spaceCompassCatalogTypeMatchCount(catalogue, types))
        val counts = spaceCompassCatalogTypeCounts(catalogue, emptyMap(), Locale.ROOT).associate { it.type to it.count }
        assertEquals(13, counts.getValue(SpaceCompassCatalogType.PLANET))
        assertEquals(83, counts.getValue(SpaceCompassCatalogType.NATURAL_SATELLITE))
        assertEquals(11, counts.getValue(SpaceCompassCatalogType.SCIENCE_FICTION))
        assertEquals(2, counts.getValue(SpaceCompassCatalogType.SPACECRAFT))
        assertEquals(1009, spaceCompassCatalogTypeMatchCount(catalogue, emptySet()))
        assertEquals(1009, spaceCompassCatalogTypeMatchCount(catalogue, SpaceCompassCatalogType.entries.toSet()))
        assertEquals(catalogue, spaceCompassFilterCatalog(SpaceCompassCatalogType.entries.toSet(),
            SpaceCompassCatalogVisibility.ALL, emptyMap()))
    }

    @Test fun savedCombinedTypesStillRespectHorizonVisibilityAndCheckedObjectPriority() {
        val settings = restoreSpaceCompassCatalogPreferences(null,
            setOf("SCIENCE_FICTION", "SPACECRAFT", "NATURAL_SATELLITE"), "ABOVE")
        val checked = setOf(SpaceCompassCelestialBody.MOON)
        val catalogue = listOf(SpaceCompassCelestialBody.NOSTROMO, SpaceCompassCelestialBody.ARRAKIS,
            SpaceCompassCelestialBody.PANDORA, SpaceCompassCelestialBody.MOON,
            SpaceCompassCelestialBody.DISCOVERY_ONE, SpaceCompassCelestialBody.MARS)
        val elevations = mapOf(SpaceCompassCelestialBody.NOSTROMO to 5.0,
            SpaceCompassCelestialBody.ARRAKIS to -10.0, SpaceCompassCelestialBody.PANDORA to 0.0,
            SpaceCompassCelestialBody.MOON to -1.0, SpaceCompassCelestialBody.MARS to 20.0)
        val matching = spaceCompassFilterCatalog(settings.types, settings.visibility, elevations, catalogue)
        assertEquals(listOf(SpaceCompassCelestialBody.NOSTROMO, SpaceCompassCelestialBody.PANDORA), matching)
        assertEquals(listOf(SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.NOSTROMO,
            SpaceCompassCelestialBody.PANDORA), spaceCompassCatalogDisplayOrder(catalogue, matching.toSet(), checked))
        assertEquals(5, spaceCompassCatalogTypeMatchCount(catalogue, settings.types))
        assertEquals(setOf(SpaceCompassCatalogType.SCIENCE_FICTION, SpaceCompassCatalogType.SPACECRAFT,
            SpaceCompassCatalogType.NATURAL_SATELLITE), settings.types)
    }
}

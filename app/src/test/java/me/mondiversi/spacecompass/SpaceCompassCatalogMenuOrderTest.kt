package me.mondiversi.spacecompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogMenuOrderTest {
    @Test fun typeChoicesCountTheWholeCatalogueAndPutLargerGroupsFirst() {
        val available = spaceCompassAvailableCelestialCatalog(setOf(SpaceCompassCelestialBody.MOON))
        val choices = spaceCompassCatalogTypeCounts(available, emptyMap(), Locale.ROOT)
        assertEquals(SpaceCompassCatalogType.entries.toSet(), choices.map { it.type }.toSet())
        assertEquals(available.size, choices.sumOf { it.count })
        assertTrue(choices.zipWithNext().all { (a, b) -> a.count >= b.count })
        val stars = choices.single { it.type == SpaceCompassCatalogType.STAR }
        assertEquals(available.count { it.catalogType == SpaceCompassCatalogType.STAR }, stars.count)
        assertEquals(choices, spaceCompassCatalogTypeCounts(
            spaceCompassAvailableCelestialCatalog(emptySet()), emptyMap(), Locale.ROOT))
    }

    @Test fun publicFictionalMoonCountsIndependentlyOfItsCheckedState() {
        val ordinary = spaceCompassCatalogTypeCounts(spaceCompassAvailableCelestialCatalog(emptySet()),
            emptyMap(), Locale.ROOT).associate { it.type to it.count }
        val revealed = spaceCompassCatalogTypeCounts(spaceCompassAvailableCelestialCatalog(
            setOf(SpaceCompassCelestialBody.LV_426)), emptyMap(), Locale.ROOT).associate { it.type to it.count }
        for (type in SpaceCompassCatalogType.entries) {
            assertEquals(ordinary.getValue(type),
                revealed.getValue(type))
        }
    }

    @Test fun equalSizeTypesUseLocalizedAlphabeticalOrderIncludingAccents() {
        val choices = spaceCompassCatalogTypeCounts(listOf(SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.SUN),
            mapOf(SpaceCompassCatalogType.STAR to "Étoiles", SpaceCompassCatalogType.PLANET to "Planètes"), Locale.FRENCH)
        assertEquals(listOf(SpaceCompassCatalogType.STAR, SpaceCompassCatalogType.PLANET), choices.take(2).map { it.type })
        assertTrue(choices.drop(2).all { it.count == 0 })
    }

    @Test fun sortCriteriaUseTheirDisplayedNamesRatherThanTheirStoredEnumOrder() {
        val labels = mapOf(
            SpaceCompassCatalogSortField.NAME to "Alfabetico",
            SpaceCompassCatalogSortField.DISTANCE to "Distanza dal Sole",
            SpaceCompassCatalogSortField.EARTH_DISTANCE to "Distanza dalla Terra",
            SpaceCompassCatalogSortField.MASS to "Massa",
            SpaceCompassCatalogSortField.DIAMETER to "Diametro",
            SpaceCompassCatalogSortField.PRESSURE to "Pressione atmosferica",
            SpaceCompassCatalogSortField.GRAVITY to "Gravità",
            SpaceCompassCatalogSortField.DAY_TEMPERATURE to "Temperatura di giorno",
            SpaceCompassCatalogSortField.NIGHT_TEMPERATURE to "Temperatura di notte")
        assertEquals(listOf(SpaceCompassCatalogSortField.NAME, SpaceCompassCatalogSortField.DIAMETER,
            SpaceCompassCatalogSortField.EARTH_DISTANCE, SpaceCompassCatalogSortField.DISTANCE,
            SpaceCompassCatalogSortField.GRAVITY, SpaceCompassCatalogSortField.MASS,
            SpaceCompassCatalogSortField.PRESSURE, SpaceCompassCatalogSortField.DAY_TEMPERATURE,
            SpaceCompassCatalogSortField.NIGHT_TEMPERATURE), spaceCompassCatalogSortFields(labels, Locale.ITALIAN))
        assertEquals(SpaceCompassCatalogSortField.entries.toSet(), spaceCompassCatalogSortFields(labels, Locale.ENGLISH).toSet())
    }
}

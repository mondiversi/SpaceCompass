package me.mondiversi.spacecompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogPhysicalSortTest {
    private fun sorted(bodies: List<SpaceCompassCelestialBody>, sort: SpaceCompassCatalogSort) =
        spaceCompassSortCatalog(bodies, sort, emptyMap(), emptyMap(), Locale.ITALIAN)

    @Test fun massesUseKilogramsForBothSolarMassAndOrdinaryMassReferences() {
        val bodies = listOf(SpaceCompassCelestialBody.SAGITTARIUS_A, SpaceCompassCelestialBody.STEPHENSON_2_18,
            SpaceCompassCelestialBody.PROXIMA_CENTAURI, SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.COMET_67P)
        val ascending = listOf(bodies[4], bodies[2], bodies[3], bodies[0], bodies[1])
        assertEquals(ascending, sorted(bodies, SpaceCompassCatalogSort.MASS_ASC))
        assertEquals(ascending.dropLast(1).reversed() + bodies[1], sorted(bodies, SpaceCompassCatalogSort.MASS_DESC))
    }

    @Test fun negativeNightTemperaturesRemainNumericalAndUnavailableValuesStayLast() {
        val bodies = listOf(SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.MERCURY)
        assertEquals(listOf(bodies[2], bodies[1], bodies[0]), sorted(bodies, SpaceCompassCatalogSort.NIGHT_TEMPERATURE_ASC))
        assertEquals(listOf(bodies[0], bodies[1], bodies[2]), sorted(bodies, SpaceCompassCatalogSort.NIGHT_TEMPERATURE_DESC))
        assertEquals(464.0, spaceCompassCatalogPhysicalSortValue(bodies[0], SpaceCompassCatalogSortField.DAY_TEMPERATURE)!!, 0.0)
    }

    @Test fun dayNightUsesQualifiedReferenceValuesWhenExactExtremesAreUnavailable() {
        for (body in listOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.JUPITER,
            SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.EUROPA)) {
            val day = spaceCompassCatalogTemperatureReference(body, SpaceCompassCatalogSortField.DAY_TEMPERATURE)!!
            val night = spaceCompassCatalogTemperatureReference(body, SpaceCompassCatalogSortField.NIGHT_TEMPERATURE)!!
            assertFalse(day.exactDayNight)
            assertFalse(night.exactDayNight)
            assertEquals(day.sortCelsius, night.sortCelsius, 0.0)
        }
        val bodies = listOf(SpaceCompassCelestialBody.MERCURY, SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.MOON)
        assertEquals(listOf(bodies[2], bodies[0], bodies[1]), sorted(bodies, SpaceCompassCatalogSort.DAY_TEMPERATURE_ASC))
        assertEquals(listOf(bodies[1], bodies[0], bodies[2]), sorted(bodies, SpaceCompassCatalogSort.DAY_TEMPERATURE_DESC))
    }

    @Test fun pressureGravityAndDiametersSortReferenceValuesWithoutFabricatingSpacecraftValues() {
        val bodies = listOf(SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.MARS)
        for (sort in listOf(SpaceCompassCatalogSort.PRESSURE_ASC, SpaceCompassCatalogSort.GRAVITY_ASC,
            SpaceCompassCatalogSort.DIAMETER_ASC)) assertEquals(listOf(bodies[2], bodies[1], bodies[0]), sorted(bodies, sort))
        for (sort in listOf(SpaceCompassCatalogSort.PRESSURE_DESC, SpaceCompassCatalogSort.GRAVITY_DESC,
            SpaceCompassCatalogSort.DIAMETER_DESC)) assertEquals(listOf(bodies[1], bodies[2], bodies[0]), sorted(bodies, sort))
    }

    @Test fun everyNumericCriterionKeepsMultipleMissingValuesLastAndStableInBothDirections() {
        val expectations = mapOf(
            SpaceCompassCatalogSortField.DISTANCE to listOf(SpaceCompassCelestialBody.VENUS, SpaceCompassCelestialBody.MARS),
            SpaceCompassCatalogSortField.MASS to listOf(SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.VENUS),
            SpaceCompassCatalogSortField.DIAMETER to listOf(SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.VENUS),
            SpaceCompassCatalogSortField.PRESSURE to listOf(SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.VENUS),
            SpaceCompassCatalogSortField.GRAVITY to listOf(SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.VENUS),
            SpaceCompassCatalogSortField.DAY_TEMPERATURE to listOf(SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.MERCURY),
            SpaceCompassCatalogSortField.NIGHT_TEMPERATURE to listOf(SpaceCompassCelestialBody.MERCURY, SpaceCompassCelestialBody.MOON))
        val distances = mapOf(SpaceCompassCelestialBody.VENUS to 0.72, SpaceCompassCelestialBody.MARS to 1.52)
        for ((field, known) in expectations) {
            val missing = spaceCompassCelestialCatalogOrder.filter { body -> body !in known &&
                if (field == SpaceCompassCatalogSortField.DISTANCE) body.isEarthSatellite
                else spaceCompassCatalogPhysicalSortValue(body, field) == null }.take(2)
            assertEquals("Two real catalog entries without $field", 2, missing.size)
            // Unknown inputs start interleaved and in reverse catalog order.
            val input = listOf(missing[1], known[1], missing[0], known[0])
            for (descending in listOf(false, true)) {
                val sort = SpaceCompassCatalogSort.entries.single { it.field == field && it.descending == descending }
                assertEquals("$field / descending=$descending", (if (descending) known.reversed() else known) + missing,
                    spaceCompassSortCatalog(input, sort, emptyMap(), distances, Locale.ITALIAN))
            }
        }
    }
}

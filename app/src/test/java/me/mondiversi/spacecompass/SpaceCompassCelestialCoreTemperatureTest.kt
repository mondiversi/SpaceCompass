package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialCoreTemperatureTest {
    private val numeric = SpaceCompassNumericFormat.AMERICAN

    @Test fun curatedReferencesKeepTheirPhysicalLayerAndSource() {
        val expected = setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON,
            SpaceCompassCelestialBody.MERCURY, SpaceCompassCelestialBody.MARS,
            SpaceCompassCelestialBody.JUPITER, SpaceCompassCelestialBody.SATURN,
            SpaceCompassCelestialBody.URANUS, SpaceCompassCelestialBody.NEPTUNE)
        for (body in SpaceCompassCelestialBody.entries) {
            val reference = spaceCompassCelestialCoreTemperature(body)
            assertEquals(body.toString(), body in expected, reference != null)
            if (reference != null) {
                assertTrue(reference.temperature.kind.isCore)
                assertTrue(reference.temperature.celsius.isFinite() && reference.temperature.celsius > 0)
                assertTrue(reference.sourceUrl.startsWith("https://"))
                assertTrue(reference.citation.isNotBlank())
                assertTrue(reference.temperature.significantDigits in 2..3)
                reference.temperature.maximumCelsius?.let { assertTrue(it >= reference.temperature.celsius) }
            }
        }
        assertEquals(15_000_000.0, spaceCompassCelestialCoreTemperature(SpaceCompassCelestialBody.SUN)!!.temperature.celsius, 0.0)
        assertEquals(SpaceCompassCelestialTemperatureKind.CORE_BOUNDARY_ESTIMATE,
            spaceCompassCelestialCoreTemperature(SpaceCompassCelestialBody.MOON)!!.temperature.kind)
        assertEquals(1650.0, spaceCompassCelestialCoreTemperature(SpaceCompassCelestialBody.MOON)!!.temperature.celsius + 273.15, 1e-9)
        for (body in listOf(SpaceCompassCelestialBody.URANUS, SpaceCompassCelestialBody.NEPTUNE)) {
            assertTrue(spaceCompassCelestialCoreTemperature(body)!!.adiabaticReference)
            assertEquals(SpaceCompassCelestialTemperatureKind.CORE_ADIABATIC_REFERENCE,
                spaceCompassCelestialCoreTemperature(body)!!.temperature.kind)
        }
    }

    @Test fun coreNeverReplacesSurfaceOrAtmosphereInEitherCatalogSort() {
        for (body in SpaceCompassCelestialBody.entries) {
            assertFalse(spaceCompassCelestialTemperatures(body).any { it.kind.isCore })
            for (field in listOf(SpaceCompassCatalogSortField.DAY_TEMPERATURE, SpaceCompassCatalogSortField.NIGHT_TEMPERATURE))
                assertFalse(spaceCompassCatalogTemperatureReference(body, field)?.temperature?.kind?.isCore == true)
        }
        for (field in listOf(SpaceCompassCatalogSortField.DAY_TEMPERATURE, SpaceCompassCatalogSortField.NIGHT_TEMPERATURE)) {
            assertEquals(-110.0, spaceCompassCatalogPhysicalSortValue(SpaceCompassCelestialBody.JUPITER, field)!!, 0.0)
            assertEquals(-195.0, spaceCompassCatalogPhysicalSortValue(SpaceCompassCelestialBody.URANUS, field)!!, 0.0)
            assertEquals(5500.0, spaceCompassCatalogPhysicalSortValue(SpaceCompassCelestialBody.SUN, field)!!, 0.0)
        }
        assertEquals(127.0, spaceCompassCatalogPhysicalSortValue(SpaceCompassCelestialBody.MOON, SpaceCompassCatalogSortField.DAY_TEMPERATURE)!!, 0.0)
        assertEquals(-173.0, spaceCompassCatalogPhysicalSortValue(SpaceCompassCelestialBody.MOON, SpaceCompassCatalogSortField.NIGHT_TEMPERATURE)!!, 0.0)
    }

    @Test fun coreConversionPrecedesRoundingAndKeepsKelvinAsReference() {
        val sun = spaceCompassCelestialCoreTemperature(SpaceCompassCelestialBody.SUN)!!.temperature
        assertEquals("≈1.5 × 10⁷ °C (1.5 × 10⁷ K)", formatSpaceCompassCelestialTemperature(sun, numeric))
        assertEquals("≈2.7 × 10⁷ °F (1.5 × 10⁷ K)", formatSpaceCompassCelestialTemperature(sun, numeric, true))
        assertEquals("≈1,5 × 10⁷ °C (1,5 × 10⁷ K)",
            formatSpaceCompassCelestialTemperature(sun, SpaceCompassNumericFormat.EUROPEAN))
        val moon = spaceCompassCelestialCoreTemperature(SpaceCompassCelestialBody.MOON)!!.temperature
        assertEquals("≈1,380 °C (1,650 K)", formatSpaceCompassCelestialTemperature(moon, numeric))
        assertEquals("≈2,510 °F (1,650 K)", formatSpaceCompassCelestialTemperature(moon, numeric, true))
        val mars = spaceCompassCelestialCoreTemperature(SpaceCompassCelestialBody.MARS)!!.temperature
        assertEquals("≈1,700 … 2,100 °C (2,000 … 2,400 K)", formatSpaceCompassCelestialTemperature(mars, numeric))
        assertEquals("≈3,100 … 3,900 °F (2,000 … 2,400 K)", formatSpaceCompassCelestialTemperature(mars, numeric, true))
    }

    @Test fun invalidInteriorPrecisionAndRangesRemainUnavailable() {
        val value = spaceCompassCelestialCoreTemperature(SpaceCompassCelestialBody.MARS)!!.temperature
        for (digits in listOf(0, -1, 7))
            assertEquals("—", formatSpaceCompassCelestialTemperature(value.copy(significantDigits = digits), numeric))
        assertEquals("—", formatSpaceCompassCelestialTemperature(value.copy(maximumCelsius = 100.0), numeric))
        assertEquals("—", formatSpaceCompassCelestialTemperature(value.copy(celsius = Double.NaN), numeric))
        assertEquals("—", formatSpaceCompassCoreTemperatureNumber(Double.POSITIVE_INFINITY, 2, numeric))
    }
}

package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogValueColumnTest {
    private val numeric = SpaceCompassNumericFormat.INTERNATIONAL
    private fun value(body: SpaceCompassCelestialBody, field: SpaceCompassCatalogSortField,
        units: SpaceCompassUnits = SpaceCompassUnits()) = formatSpaceCompassCatalogPhysicalValue(body, field, numeric, units) { "reference" }

    @Test fun massColumnUsesSelectedMassUnitsAndRetainsSolarThresholdAndUncertainty() {
        assertEquals("4.870 × 10²⁴ kg", value(SpaceCompassCelestialBody.VENUS, SpaceCompassCatalogSortField.MASS))
        assertEquals("1.074 × 10²⁵ lb", value(SpaceCompassCelestialBody.VENUS, SpaceCompassCatalogSortField.MASS,
            SpaceCompassUnits(pounds = true)))
        assertEquals("≈ 21 ± 3 M☉", value(SpaceCompassCelestialBody.RIGEL, SpaceCompassCatalogSortField.MASS,
            SpaceCompassUnits(pounds = true)))
        assertEquals("—", value(SpaceCompassCelestialBody.STEPHENSON_2_18, SpaceCompassCatalogSortField.MASS))
    }

    @Test fun diameterColumnConvertsRealDiametersAndDoesNotSubstituteSpacecraftSize() {
        assertEquals("4\u202f879 km", value(SpaceCompassCelestialBody.MERCURY, SpaceCompassCatalogSortField.DIAMETER))
        assertEquals("3\u202f032 mi", value(SpaceCompassCelestialBody.MERCURY, SpaceCompassCatalogSortField.DIAMETER,
            SpaceCompassUnits(feet = true)))
        assertEquals("—", value(SpaceCompassCelestialBody.ISS, SpaceCompassCatalogSortField.DIAMETER))
        assertEquals("—", value(SpaceCompassCelestialBody.EARTH_CENTER, SpaceCompassCatalogSortField.DIAMETER))
    }

    @Test fun pressureColumnRetainsAtmosphericReferenceAndUpperLimitWithSelectedUnits() {
        assertEquals("≈ 92 bar", value(SpaceCompassCelestialBody.VENUS, SpaceCompassCatalogSortField.PRESSURE))
        assertEquals("≈ 9.2 × 10⁶ Pa", value(SpaceCompassCelestialBody.VENUS, SpaceCompassCatalogSortField.PRESSURE,
            SpaceCompassUnits(pressure = SpaceCompassPressureUnit.PASCAL)))
        assertTrue(value(SpaceCompassCelestialBody.MERCURY, SpaceCompassCatalogSortField.PRESSURE).startsWith("< ≈ "))
        assertEquals("—", value(SpaceCompassCelestialBody.ISS, SpaceCompassCatalogSortField.PRESSURE))
    }

    @Test fun gravityColumnConvertsAccelerationWithoutChangingEarthGravityRatio() {
        assertEquals("1.6 m/s² (0.163 g)", value(SpaceCompassCelestialBody.MOON, SpaceCompassCatalogSortField.GRAVITY))
        assertEquals("5.2 ft/s² (0.163 g)", value(SpaceCompassCelestialBody.MOON, SpaceCompassCatalogSortField.GRAVITY,
            SpaceCompassUnits(feet = true)))
        assertEquals("—", value(SpaceCompassCelestialBody.STARLINK_V3, SpaceCompassCatalogSortField.GRAVITY))
    }

    @Test fun dayAndNightColumnsPreferActualExtremaAndLabelMeanFallbacks() {
        assertEquals("≈127 °C", value(SpaceCompassCelestialBody.MOON, SpaceCompassCatalogSortField.DAY_TEMPERATURE))
        assertEquals("≈-173 °C", value(SpaceCompassCelestialBody.MOON, SpaceCompassCatalogSortField.NIGHT_TEMPERATURE))
        assertEquals("≈261 °F", value(SpaceCompassCelestialBody.MOON, SpaceCompassCatalogSortField.DAY_TEMPERATURE,
            SpaceCompassUnits(fahrenheit = true)))
        assertEquals("≈-279 °F", value(SpaceCompassCelestialBody.MOON, SpaceCompassCatalogSortField.NIGHT_TEMPERATURE,
            SpaceCompassUnits(fahrenheit = true)))
        for (field in listOf(SpaceCompassCatalogSortField.DAY_TEMPERATURE, SpaceCompassCatalogSortField.NIGHT_TEMPERATURE))
            assertEquals("≈464 °C\nreference", value(SpaceCompassCelestialBody.VENUS, field))
    }
}

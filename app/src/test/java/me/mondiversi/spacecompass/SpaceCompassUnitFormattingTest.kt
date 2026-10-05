package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassUnitFormattingTest {
    private val numeric = SpaceCompassNumericFormat.AMERICAN
    @Test fun imperialAltitudeAlsoConvertsVerticalUncertainty() {
        assertEquals("10 ft (±5 ft)", formatSpaceCompassCelestialAltitude(3.048, 1.524, numeric, true))
    }
    @Test fun milesAndAstronomicalUnits() {
        assertEquals("1.00 mi", formatSpaceCompassSelectedDistance(SpaceCompassCelestialBody.MOON, 1.609344, numeric, "mi"))
        assertEquals("1 AU", formatSpaceCompassSelectedDistance(SpaceCompassCelestialBody.SUN, SPACE_COMPASS_AU_KM, numeric, "au"))
        assertNull(formatSpaceCompassSelectedDistance(SpaceCompassCelestialBody.SUN, Double.NaN, numeric, "mi"))
        assertEquals("0.10 km", formatSpaceCompassSelectedDistance(SpaceCompassCelestialBody.ISS, 0.1, numeric, "mmi"))
    }
    @Test fun nearEarthDistancesIgnoreAstronomicalUnitsAndFollowNormalUnits() {
        for (body in listOf(SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3))
            for (unit in listOf("default", "mkm", "mmi")) {
                assertEquals("1.61 km", formatSpaceCompassSelectedDistance(body, 1.609344, numeric, unit))
                assertEquals("1.00 mi", formatSpaceCompassSelectedDistance(body, 1.609344, numeric, unit, true))
                assertNull(formatSpaceCompassSelectedDistance(body, Double.NaN, numeric, unit))
            }
        val moon = spaceCompassNearbyCatalogDistanceKm(SpaceCompassCelestialBody.MOON, 1770000000000L, SpaceCompassCelestialRemoteData())!!
        assertTrue(moon in 330000.0..420000.0)
        assertNull(spaceCompassNearbyCatalogDistanceKm(SpaceCompassCelestialBody.ISS, 1770000000000L, SpaceCompassCelestialRemoteData()))
    }
    @Test fun temperaturesAndStellarKelvin() {
        val surface = SpaceCompassCelestialTemperature(SpaceCompassCelestialTemperatureKind.SURFACE_MEAN, 0.0)
        assertEquals("≈32 °F", formatSpaceCompassCelestialTemperature(surface, numeric, true))
        val star = SpaceCompassCelestialTemperature(SpaceCompassCelestialTemperatureKind.STELLAR_EFFECTIVE, 26.85)
        assertEquals("≈80 °F (300 K)", formatSpaceCompassCelestialTemperature(star, numeric, true))
    }
    @Test fun coordinatesCarryRoundedSecondsAndRejectInvalidFixes() {
        assertEquals("13° 00′ 0.0″ S\n180° 00′ 0.0″ E", formatSpaceCompassSelectedCoordinates(-12.9999999, 180.0, numeric, true))
        assertNull(formatSpaceCompassSelectedCoordinates(91.0, 0.0, numeric, true))
    }

    @Test fun physicalSizesAndTheirUncertaintyUseTheSameNormalDistanceFamily() {
        assertEquals("10.0 ft", formatSpaceCompassPhysicalLength(3.048, 1, numeric, true))
        assertEquals("3.0 m", formatSpaceCompassPhysicalLength(3.048, 1, numeric, false))
        assertEquals("1.0 mi", formatSpaceCompassPhysicalLength(1609.344, 1, numeric, true, large = true))
        assertEquals("1.6 km", formatSpaceCompassPhysicalLength(1609.344, 1, numeric, false, large = true))
        assertEquals("1,6 km", formatSpaceCompassPhysicalLength(1609.344, 1, SpaceCompassNumericFormat.EUROPEAN, false, large = true))
        for (bad in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, -1.0))
            assertEquals("—", formatSpaceCompassPhysicalLength(bad, 1, numeric, true))
    }
    @Test fun temperatureRangesConvertBothEndsAndKeepNegativeValues() {
        val range = SpaceCompassCelestialTemperature(SpaceCompassCelestialTemperatureKind.SURFACE_RANGE, -40.0, 0.0)
        assertEquals("≈-40 … 32 °F", formatSpaceCompassCelestialTemperature(range, numeric, true))
        assertEquals("≈-40 … 0 °C", formatSpaceCompassCelestialTemperature(range, numeric, false))
    }

    @Test fun catalogDistancesApplySelectedMillionsWithoutLosingTheAuReference() {
        val km = formatSpaceCompassCelestialCatalogDistance(1.0, numeric, "mkm")
        val mi = formatSpaceCompassCelestialCatalogDistance(1.0, numeric, "mmi")
        assertEquals("149.6 Mkm\n1 AU", km)
        assertEquals("92.96 Mmi\n1 AU", mi)
        assertEquals("—", formatSpaceCompassCelestialCatalogDistance(null, numeric, unit = "mmi"))
    }

    @Test fun dmsUsesOneConsistentDigitSystemIncludingLeadingZeroMinutes() {
        val previous = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("ar-EG"))
            val result = formatSpaceCompassSelectedCoordinates(12.5, 0.0, SpaceCompassNumericFormat.SYSTEM, true)!!
            assertTrue(result.startsWith("١٢° ٣٠′ ٠٫٠″ N"))
            assertTrue(result.endsWith("٠° ٠٠′ ٠٫٠″ E"))
            val explicit = formatSpaceCompassSelectedCoordinates(12.5, 0.0, numeric, true)!!
            assertEquals("12° 30′ 0.0″ N\n0° 00′ 0.0″ E", explicit)
        } finally { java.util.Locale.setDefault(previous) }
    }

    @Test fun extrasolarRangesUseOnlyLightYearsAcrossEveryDistancePresentation() {
        val distance = 447.0 * SPACE_COMPASS_LIGHT_YEAR_KM
        for (format in listOf(numeric, SpaceCompassNumericFormat.EUROPEAN)) {
            val expected = "${formatSpaceCompassNumber(447.0, 1, format)} ly"
            for (unit in listOf("default", "mkm", "mmi", "km", "mi", "au")) {
                assertEquals(expected, formatSpaceCompassSelectedDistance(SpaceCompassCelestialBody.POLARIS, distance, format, unit))
                assertEquals(expected, formatSpaceCompassCelestialCatalogDistance(distance / SPACE_COMPASS_AU_KM, format, unit))
            }
            assertEquals(expected, formatSpaceCompassCelestialTableDistance(SpaceCompassCelestialBody.POLARIS, distance, format))
            assertEquals(expected, formatSpaceCompassCelestialObserverDistance(distance, format))
            assertEquals(expected, formatSpaceCompassCelestialDistance(SpaceCompassCelestialBody.POLARIS, distance, format))
        }
        assertNull(formatSpaceCompassExtrasolarDistance(-1.0, numeric))
        assertNull(formatSpaceCompassExtrasolarDistance(Double.NaN, numeric))
        assertNull(formatSpaceCompassExtrasolarDistance(200 * SPACE_COMPASS_AU_KM, numeric))
        assertTrue(formatSpaceCompassSelectedDistance(SpaceCompassCelestialBody.VOYAGER_1,
            200 * SPACE_COMPASS_AU_KM, numeric, "mmi")!!.contains("Mmi"))
    }
}

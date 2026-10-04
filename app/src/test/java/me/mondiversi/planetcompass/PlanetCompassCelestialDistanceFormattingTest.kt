package me.mondiversi.planetcompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialDistanceFormattingTest {
    private fun distance(au: Double, format: PlanetCompassNumericFormat = PlanetCompassNumericFormat.INTERNATIONAL,
        body: PlanetCompassCelestialBody = PlanetCompassCelestialBody.SUN) =
        formatPlanetCompassCelestialDistance(body, au * PLANET_COMPASS_AU_KM, format)

    @Test fun astronomicalUnitsRoundToFourDecimalPlacesWithoutUnnecessaryZeroes() {
        assertTrue(distance(1.234567).endsWith(" · 1.2346 AU"))
        assertTrue(distance(1.23454).endsWith(" · 1.2345 AU"))
        assertTrue(distance(1.23456, PlanetCompassNumericFormat.EUROPEAN).endsWith(" · 1,2346 AU"))
        assertTrue(distance(1.2).endsWith(" · 1.2 AU"))
        assertTrue(distance(1.0).endsWith(" · 1 AU"))
        assertTrue(distance(9.99999).endsWith(" · 10 AU"))
    }

    @Test fun everyBodyAndNumberFormatHasTheSameAuLimitAndNoRepeatedObjectName() {
        for (body in PlanetCompassCelestialBody.entries) for (format in PlanetCompassNumericFormat.entries) {
            val text = distance(123.456789, format, body)
            assertEquals(2, text.split(" · ").size)
            assertFalse(text.contains(body.name))
            assertTrue(text.endsWith("${formatPlanetCompassNumber(123.4568, 4, format)} AU"))
        }
    }

    @Test fun aVerySmallPositiveDistanceIsNotMisrepresentedAsZeroAu() {
        assertEquals("800 km · < 0.0001 AU", formatPlanetCompassCelestialDistance(
            PlanetCompassCelestialBody.ISS, 800.0, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("800 km · < 0,0001 AU", formatPlanetCompassCelestialDistance(
            PlanetCompassCelestialBody.ISS, 800.0, PlanetCompassNumericFormat.EUROPEAN))
        assertEquals("0 km · 0 AU", distance(0.0, body = PlanetCompassCelestialBody.ISS))
        assertTrue(distance(0.0001).endsWith(" · 0.0001 AU"))
        assertTrue(distance(0.002569, body = PlanetCompassCelestialBody.MOON).endsWith(" · 0.0026 AU"))
    }

    @Test fun invalidAndMissingDistancesShowOnlyThePlaceholder() {
        for (body in PlanetCompassCelestialBody.entries) for (km in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertEquals("—", formatPlanetCompassCelestialDistance(body, km, PlanetCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun systemFormattingKeepsTheDeviceDecimalSeparator() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            assertTrue(distance(1.234567, PlanetCompassNumericFormat.SYSTEM).endsWith(" · 1,2346 AU"))
            Locale.setDefault(Locale.US)
            assertTrue(distance(1.234567, PlanetCompassNumericFormat.SYSTEM).endsWith(" · 1.2346 AU"))
        } finally { Locale.setDefault(previous) }
    }

    @Test fun theSharedFormatterRetainsFixedPrecisionForExistingCallers() {
        assertEquals("1.2000", formatPlanetCompassNumber(1.2, 4, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("1.2", formatPlanetCompassNumber(1.2, 4, PlanetCompassNumericFormat.INTERNATIONAL, minimumDigits = 0))
        assertEquals("1.2346", formatPlanetCompassNumber(1.234567, 4, PlanetCompassNumericFormat.INTERNATIONAL, minimumDigits = 0))
    }

    @Test fun observerDetailsHaveTwoRowsWithTwoAndFourDecimalLimits() {
        assertEquals("184.69 Mkm\n1.2346 AU", formatPlanetCompassCelestialObserverDistance(
            1.234567 * PLANET_COMPASS_AU_KM, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("184,69 Mkm\n1,2346 AU", formatPlanetCompassCelestialObserverDistance(
            1.234567 * PLANET_COMPASS_AU_KM, PlanetCompassNumericFormat.EUROPEAN))
        assertEquals("184.68 Mkm\n1.2345 AU", formatPlanetCompassCelestialObserverDistance(
            1.23454 * PLANET_COMPASS_AU_KM, PlanetCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun observerDetailsAlwaysUseMillionsOfKilometersWithoutInventingZeroRanges() {
        assertEquals("< 0.01 Mkm\n< 0.0001 AU", formatPlanetCompassCelestialObserverDistance(
            800.0, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("0.38 Mkm\n0.0026 AU", formatPlanetCompassCelestialObserverDistance(
            384_400.0, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("0 Mkm\n0 AU", formatPlanetCompassCelestialObserverDistance(
            0.0, PlanetCompassNumericFormat.INTERNATIONAL))
        for (format in PlanetCompassNumericFormat.entries) {
            val rows = formatPlanetCompassCelestialObserverDistance(20_000_000_000.123, format).lines()
            assertEquals(2, rows.size)
            assertTrue(rows[0].endsWith(" Mkm"))
            assertTrue(rows[1].endsWith(" AU"))
        }
    }

    @Test fun observerDetailsRejectMissingOrInvalidDistances() {
        for (km in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals("—", formatPlanetCompassCelestialObserverDistance(km, PlanetCompassNumericFormat.INTERNATIONAL))
        }
    }

    @Test fun observerDetailsFollowTheSystemDecimalSeparatorOnBothRows() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            assertEquals("149,6 Mkm\n1 AU", formatPlanetCompassCelestialObserverDistance(
                PLANET_COMPASS_AU_KM, PlanetCompassNumericFormat.SYSTEM))
            Locale.setDefault(Locale.US)
            assertEquals("149.6 Mkm\n1 AU", formatPlanetCompassCelestialObserverDistance(
                PLANET_COMPASS_AU_KM, PlanetCompassNumericFormat.SYSTEM))
        } finally { Locale.setDefault(previous) }
    }
}

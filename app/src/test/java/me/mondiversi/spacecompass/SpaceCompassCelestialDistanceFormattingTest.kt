package me.mondiversi.spacecompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialDistanceFormattingTest {
    private fun distance(au: Double, format: SpaceCompassNumericFormat = SpaceCompassNumericFormat.INTERNATIONAL,
        body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.SUN) =
        formatSpaceCompassCelestialDistance(body, au * SPACE_COMPASS_AU_KM, format)

    @Test fun astronomicalUnitsRoundToFourDecimalPlacesWithoutUnnecessaryZeroes() {
        assertTrue(distance(1.234567).endsWith(" · 1.2346 AU"))
        assertTrue(distance(1.23454).endsWith(" · 1.2345 AU"))
        assertTrue(distance(1.23456, SpaceCompassNumericFormat.EUROPEAN).endsWith(" · 1,2346 AU"))
        assertTrue(distance(1.2).endsWith(" · 1.2 AU"))
        assertTrue(distance(1.0).endsWith(" · 1 AU"))
        assertTrue(distance(9.99999).endsWith(" · 10 AU"))
    }

    @Test fun everyBodyAndNumberFormatHasTheSameAuLimitAndNoRepeatedObjectName() {
        for (body in SpaceCompassCelestialBody.entries.filter { it != SpaceCompassCelestialBody.EARTH_CENTER }) for (format in SpaceCompassNumericFormat.entries) {
            val text = distance(123.456789, format, body)
            assertEquals(2, text.split(" · ").size)
            assertFalse(text.contains(body.name))
            assertTrue(text.endsWith("${formatSpaceCompassNumber(123.4568, 4, format)} AU"))
        }
    }

    @Test fun aVerySmallPositiveDistanceIsNotMisrepresentedAsZeroAu() {
        assertEquals("800 km · < 0.0001 AU", formatSpaceCompassCelestialDistance(
            SpaceCompassCelestialBody.ISS, 800.0, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("800 km · < 0,0001 AU", formatSpaceCompassCelestialDistance(
            SpaceCompassCelestialBody.ISS, 800.0, SpaceCompassNumericFormat.EUROPEAN))
        assertEquals("0 km · 0 AU", distance(0.0, body = SpaceCompassCelestialBody.ISS))
        assertTrue(distance(0.0001).endsWith(" · 0.0001 AU"))
        assertTrue(distance(0.002569, body = SpaceCompassCelestialBody.MOON).endsWith(" · 0.0026 AU"))
    }

    @Test fun invalidAndMissingDistancesShowOnlyThePlaceholder() {
        for (body in SpaceCompassCelestialBody.entries) for (km in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertEquals("—", formatSpaceCompassCelestialDistance(body, km, SpaceCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun systemFormattingKeepsTheDeviceDecimalSeparator() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            assertTrue(distance(1.234567, SpaceCompassNumericFormat.SYSTEM).endsWith(" · 1,2346 AU"))
            Locale.setDefault(Locale.US)
            assertTrue(distance(1.234567, SpaceCompassNumericFormat.SYSTEM).endsWith(" · 1.2346 AU"))
        } finally { Locale.setDefault(previous) }
    }

    @Test fun theSharedFormatterRetainsFixedPrecisionForExistingCallers() {
        assertEquals("1.2000", formatSpaceCompassNumber(1.2, 4, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("1.2", formatSpaceCompassNumber(1.2, 4, SpaceCompassNumericFormat.INTERNATIONAL, minimumDigits = 0))
        assertEquals("1.2346", formatSpaceCompassNumber(1.234567, 4, SpaceCompassNumericFormat.INTERNATIONAL, minimumDigits = 0))
    }

    @Test fun observerDetailsHaveTwoRowsWithTwoAndFourDecimalLimits() {
        assertEquals("184.69 Mkm\n1.2346 AU", formatSpaceCompassCelestialObserverDistance(
            1.234567 * SPACE_COMPASS_AU_KM, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("184,69 Mkm\n1,2346 AU", formatSpaceCompassCelestialObserverDistance(
            1.234567 * SPACE_COMPASS_AU_KM, SpaceCompassNumericFormat.EUROPEAN))
        assertEquals("184.68 Mkm\n1.2345 AU", formatSpaceCompassCelestialObserverDistance(
            1.23454 * SPACE_COMPASS_AU_KM, SpaceCompassNumericFormat.INTERNATIONAL))
    }

    @Test fun observerDetailsAlwaysUseMillionsOfKilometersWithoutInventingZeroRanges() {
        assertEquals("< 0.01 Mkm\n< 0.0001 AU", formatSpaceCompassCelestialObserverDistance(
            800.0, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("0.38 Mkm\n0.0026 AU", formatSpaceCompassCelestialObserverDistance(
            384_400.0, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("0 Mkm\n0 AU", formatSpaceCompassCelestialObserverDistance(
            0.0, SpaceCompassNumericFormat.INTERNATIONAL))
        for (format in SpaceCompassNumericFormat.entries) {
            val rows = formatSpaceCompassCelestialObserverDistance(20_000_000_000.123, format).lines()
            assertEquals(2, rows.size)
            assertTrue(rows[0].endsWith(" Mkm"))
            assertTrue(rows[1].endsWith(" AU"))
        }
    }

    @Test fun observerDetailsRejectMissingOrInvalidDistances() {
        for (km in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertEquals("—", formatSpaceCompassCelestialObserverDistance(km, SpaceCompassNumericFormat.INTERNATIONAL))
        }
    }

    @Test fun observerDetailsFollowTheSystemDecimalSeparatorOnBothRows() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ITALY)
            assertEquals("149,6 Mkm\n1 AU", formatSpaceCompassCelestialObserverDistance(
                SPACE_COMPASS_AU_KM, SpaceCompassNumericFormat.SYSTEM))
            Locale.setDefault(Locale.US)
            assertEquals("149.6 Mkm\n1 AU", formatSpaceCompassCelestialObserverDistance(
                SPACE_COMPASS_AU_KM, SpaceCompassNumericFormat.SYSTEM))
        } finally { Locale.setDefault(previous) }
    }
}

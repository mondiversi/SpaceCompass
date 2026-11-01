package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialAltitudeFormattingTest {
    @Test fun verticalPrecisionIsParenthesizedAndKeepsMeters() {
        assertEquals("112 m (±8 m)", formatSpaceCompassCelestialAltitude(112.4, 7.6, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("-12 m (±5 m)", formatSpaceCompassCelestialAltitude(-12.0, 5.0, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("0 m (±0 m)", formatSpaceCompassCelestialAltitude(0.0, 0.0, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("112 m\n(±8 m)", formatSpaceCompassCelestialAltitude(112.4, 7.6,
            SpaceCompassNumericFormat.INTERNATIONAL, accuracyOnNewLine = true))
        assertEquals("100 ft\n(±10 ft)", formatSpaceCompassCelestialAltitude(30.48, 3.048,
            SpaceCompassNumericFormat.INTERNATIONAL, feet = true, accuracyOnNewLine = true))
        assertEquals("112 m", formatSpaceCompassCelestialAltitude(112.0, null,
            SpaceCompassNumericFormat.INTERNATIONAL, accuracyOnNewLine = true))
    }
    @Test fun missingOrInvalidPrecisionIsNotInvented() {
        for (accuracy in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertEquals("112 m", formatSpaceCompassCelestialAltitude(112.0, accuracy, SpaceCompassNumericFormat.INTERNATIONAL))
    }
    @Test fun missingOrInvalidAltitudeStaysUnknown() {
        for (altitude in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            assertNull(formatSpaceCompassCelestialAltitude(altitude, 10.0, SpaceCompassNumericFormat.INTERNATIONAL))
    }
    @Test fun numericFormattingFollowsTheChosenLocale() {
        for (numeric in SpaceCompassNumericFormat.entries) assertEquals(
            "${formatSpaceCompassNumber(1234.0, 0, numeric)} m (±${formatSpaceCompassNumber(12.0, 0, numeric)} m)",
            formatSpaceCompassCelestialAltitude(1234.0, 12.0, numeric))
    }
}

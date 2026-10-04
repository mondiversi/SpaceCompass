package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialAltitudeFormattingTest {
    @Test fun verticalPrecisionIsParenthesizedAndKeepsMeters() {
        assertEquals("112 m (±8 m)", formatPlanetCompassCelestialAltitude(112.4, 7.6, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("-12 m (±5 m)", formatPlanetCompassCelestialAltitude(-12.0, 5.0, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("0 m (±0 m)", formatPlanetCompassCelestialAltitude(0.0, 0.0, PlanetCompassNumericFormat.INTERNATIONAL))
    }
    @Test fun missingOrInvalidPrecisionIsNotInvented() {
        for (accuracy in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertEquals("112 m", formatPlanetCompassCelestialAltitude(112.0, accuracy, PlanetCompassNumericFormat.INTERNATIONAL))
    }
    @Test fun missingOrInvalidAltitudeStaysUnknown() {
        for (altitude in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY))
            assertNull(formatPlanetCompassCelestialAltitude(altitude, 10.0, PlanetCompassNumericFormat.INTERNATIONAL))
    }
    @Test fun numericFormattingFollowsTheChosenLocale() {
        for (numeric in PlanetCompassNumericFormat.entries) assertEquals(
            "${formatPlanetCompassNumber(1234.0, 0, numeric)} m (±${formatPlanetCompassNumber(12.0, 0, numeric)} m)",
            formatPlanetCompassCelestialAltitude(1234.0, 12.0, numeric))
    }
}

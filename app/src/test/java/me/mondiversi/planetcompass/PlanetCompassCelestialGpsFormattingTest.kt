package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test

class PlanetCompassCelestialGpsFormattingTest {
    @Test fun latitudeAndLongitudeAreSeparateDmsLinesWithHemisphereAndLocalizedSeconds() {
        assertEquals("45° 30′ 0.0″ N\n9° 15′ 0.0″ E",
            formatPlanetCompassCelestialGpsCoordinates(45.5, 9.25, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("45° 30′ 0,0″ S\n9° 15′ 0,0″ W",
            formatPlanetCompassCelestialGpsCoordinates(-45.5, -9.25, PlanetCompassNumericFormat.EUROPEAN))
    }
    @Test fun roundingCarriesSecondsMinutesAndDegreesWithoutShowingSixty() {
        assertEquals("46° 00′ 0.0″ N\n180° 00′ 0.0″ E",
            formatPlanetCompassCelestialGpsCoordinates(45.9999999, 179.9999999, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("0° 01′ 0.0″ N\n0° 01′ 0.0″ E",
            formatPlanetCompassCelestialGpsCoordinates(59.96 / 3600, 59.96 / 3600, PlanetCompassNumericFormat.INTERNATIONAL))
    }
    @Test fun polesAntimeridianAndZeroRemainValid() {
        assertEquals("90° 00′ 0.0″ S\n180° 00′ 0.0″ W",
            formatPlanetCompassCelestialGpsCoordinates(-90.0, -180.0, PlanetCompassNumericFormat.INTERNATIONAL))
        assertEquals("0° 00′ 0.0″ N\n0° 00′ 0.0″ E",
            formatPlanetCompassCelestialGpsCoordinates(0.0, 0.0, PlanetCompassNumericFormat.INTERNATIONAL))
    }
    @Test fun invalidOrMissingFixesAreUnknownRatherThanFabricatedZeroCoordinates() {
        for (latitude in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, 90.01, -90.01))
            assertNull(formatPlanetCompassCelestialGpsCoordinates(latitude, 9.0, PlanetCompassNumericFormat.INTERNATIONAL))
        for (longitude in listOf(null, Double.NaN, Double.NEGATIVE_INFINITY, 180.01, -180.01))
            assertNull(formatPlanetCompassCelestialGpsCoordinates(45.0, longitude, PlanetCompassNumericFormat.INTERNATIONAL))
    }
}

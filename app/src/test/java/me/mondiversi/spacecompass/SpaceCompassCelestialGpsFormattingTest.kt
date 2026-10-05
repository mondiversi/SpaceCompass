package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCelestialGpsFormattingTest {
    @Test fun latitudeAndLongitudeAreSeparateDmsLinesWithHemisphereAndLocalizedSeconds() {
        assertEquals("45° 30′ 0.0″ N\n9° 15′ 0.0″ E",
            formatSpaceCompassCelestialGpsCoordinates(45.5, 9.25, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("45° 30′ 0,0″ S\n9° 15′ 0,0″ W",
            formatSpaceCompassCelestialGpsCoordinates(-45.5, -9.25, SpaceCompassNumericFormat.EUROPEAN))
    }
    @Test fun roundingCarriesSecondsMinutesAndDegreesWithoutShowingSixty() {
        assertEquals("46° 00′ 0.0″ N\n180° 00′ 0.0″ E",
            formatSpaceCompassCelestialGpsCoordinates(45.9999999, 179.9999999, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("0° 01′ 0.0″ N\n0° 01′ 0.0″ E",
            formatSpaceCompassCelestialGpsCoordinates(59.96 / 3600, 59.96 / 3600, SpaceCompassNumericFormat.INTERNATIONAL))
    }
    @Test fun polesAntimeridianAndZeroRemainValid() {
        assertEquals("90° 00′ 0.0″ S\n180° 00′ 0.0″ W",
            formatSpaceCompassCelestialGpsCoordinates(-90.0, -180.0, SpaceCompassNumericFormat.INTERNATIONAL))
        assertEquals("0° 00′ 0.0″ N\n0° 00′ 0.0″ E",
            formatSpaceCompassCelestialGpsCoordinates(0.0, 0.0, SpaceCompassNumericFormat.INTERNATIONAL))
    }
    @Test fun invalidOrMissingFixesAreUnknownRatherThanFabricatedZeroCoordinates() {
        for (latitude in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, 90.01, -90.01))
            assertNull(formatSpaceCompassCelestialGpsCoordinates(latitude, 9.0, SpaceCompassNumericFormat.INTERNATIONAL))
        for (longitude in listOf(null, Double.NaN, Double.NEGATIVE_INFINITY, 180.01, -180.01))
            assertNull(formatSpaceCompassCelestialGpsCoordinates(45.0, longitude, SpaceCompassNumericFormat.INTERNATIONAL))
    }
}

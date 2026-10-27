package me.mondiversi.spacecompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTravelCompactFormattingTest {
    private val format = SpaceCompassTravelTimeFormatter(SpaceCompassNumericFormat.AMERICAN, Locale.US)
    @Test fun everyTimeScaleUsesAnUnambiguousShortSymbol() {
        for (unit in SpaceCompassTravelTimeUnit.entries)
            assertEquals("≈ 1 ${unit.symbol}", format.duration(unit.seconds))
        assertEquals("mo", spaceCompassTravelDuration(SPACE_COMPASS_TRAVEL_YEAR_SECONDS / 12)!!.unit.symbol)
    }
    @Test fun zeroAndUnavailableDurationsDoNotBecomeApproximateOrInventedTimes() {
        assertEquals("0 s", format.duration(0.0))
        for (value in listOf(null, -1.0, Double.NaN, Double.POSITIVE_INFINITY)) assertEquals("—", format.duration(value))
    }
    @Test fun subSecondAndRoundedBoundaryValuesRemainCorrect() {
        assertEquals("≈ 250 ms", format.duration(.25))
        assertEquals("≈ 1 min", format.duration(59.999))
        assertEquals("≈ 1.5 h", format.duration(5400.0))
    }
    @Test fun numberPreferencesAndLargeScientificValuesArePreserved() {
        val european = SpaceCompassTravelTimeFormatter(SpaceCompassNumericFormat.EUROPEAN, Locale.US)
        assertEquals("≈ 1,5 h", european.duration(5400.0))
        assertEquals("≈ 1×10^8 y", format.duration(SPACE_COMPASS_TRAVEL_YEAR_SECONDS * 1e8))
    }
}

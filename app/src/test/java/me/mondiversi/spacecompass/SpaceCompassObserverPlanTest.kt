package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class SpaceCompassObserverPlanTest {
    private val instant = Instant.parse("2027-04-06T20:00:00Z").toEpochMilli()
    @Test fun selectedPlaceAndMomentRoundTripWithoutUsingTheDeviceClock() {
        val plan = spaceCompassObserverPlan(28.35, -14.1, 102.5, instant, "Atlantic/Canary")!!
        assertEquals(plan, decodeSpaceCompassObserverPlan(plan.encode()))
        assertEquals(instant, plan.timeMs)
    }
    @Test fun missingOrMalformedPreferencesFallBackToLive() {
        for (stored in listOf(null, "", "custom", "NaN|0|0|0|UTC", "0|0|0|0|UTC|extra"))
            assertNull(decodeSpaceCompassObserverPlan(stored))
    }
    @Test fun coordinateBoundsAndNonfiniteInputsAreRejected() {
        for (a in listOf(-90.01, 90.01, Double.NaN, Double.POSITIVE_INFINITY))
            assertNull(spaceCompassObserverPlan(a, 0.0, 0.0, instant, "UTC"))
        for (b in listOf(-180.01, 180.01, Double.NaN))
            assertNull(spaceCompassObserverPlan(0.0, b, 0.0, instant, "UTC"))
    }
    @Test fun RealEarthHeightRangeIncludesBelowSeaLevelAndRejectsBadHeight() {
        assertNotNull(spaceCompassObserverPlan(0.0, 0.0, -430.0, instant, "UTC"))
        for (h in listOf(-501.0, 20_001.0, Double.NaN))
            assertNull(spaceCompassObserverPlan(0.0, 0.0, h, instant, "UTC"))
    }
    @Test fun invalidZonesAndUnsupportedYearsCannotBecomeActivePlans() {
        assertNull(spaceCompassObserverPlan(0.0, 0.0, 0.0, instant, "Nowhere/Invalid"))
        assertNull(spaceCompassObserverPlan(0.0, 0.0, 0.0, Instant.parse("2200-01-01T00:00:00Z").toEpochMilli(), "UTC"))
    }
    @Test fun canaryLocalTimeUsesItsOwnSeasonalOffset() {
        assertEquals(Instant.parse("2027-04-06T20:00:00Z").toEpochMilli(),
            spaceCompassObserverMoment("2027-04-06", "21:00", "Atlantic/Canary"))
        assertEquals(Instant.parse("2027-01-06T21:00:00Z").toEpochMilli(),
            spaceCompassObserverMoment("2027-01-06", "21:00", "Atlantic/Canary"))
    }
    @Test fun fractionalTimeZonesAreAppliedToTheSelectedMoment() {
        assertEquals(Instant.parse("2027-04-06T15:15:00Z").toEpochMilli(),
            spaceCompassObserverMoment("2027-04-06", "21:00", "Asia/Kathmandu"))
    }
    @Test fun nonexistentDaylightSavingTimeIsRejected() {
        assertNull(spaceCompassObserverMoment("2027-03-28", "02:30", "Europe/Rome"))
    }
    @Test fun repeatedDaylightSavingTimeUsesFirstOccurrence() {
        assertEquals(Instant.parse("2027-10-31T00:30:00Z").toEpochMilli(),
            spaceCompassObserverMoment("2027-10-31", "02:30", "Europe/Rome"))
    }
    @Test fun invalidCalendarDatesAndAmbiguousInputAreRejected() {
        for (pair in listOf("2027-02-30" to "20:00", "06/04/2027" to "20:00", "2027-04-06" to "24:10",
            "2027-04-06" to "8:30", "1899-01-01" to "00:00"))
            assertNull(spaceCompassObserverMoment(pair.first, pair.second, "UTC"))
    }
    @Test fun observerChoiceChangesActualCelestialPointing() {
        val italy = calculateSpaceCompassSunPosition(instant, 45.1, 9.0, 100.0)
        val island = calculateSpaceCompassSunPosition(instant, 28.35, -14.1, 100.0)
        assertTrue(kotlin.math.abs(italy.elevationDegrees - island.elevationDegrees) > 5.0)
    }
}

package me.mondiversi.spacecompass

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTravelTimeTest {
    private val speeds = spaceCompassTravelSpeeds
    private val light = speeds.single { it.key == "light" }
    @Test fun discretePresetsAreNonlinearMonotonicAndHaveStableSavedIdentities() {
        assertEquals(speeds.size, speeds.map { it.key }.toSet().size)
        assertTrue(speeds.zipWithNext().all { (a, b) -> b.kmPerSecond > a.kmPerSecond })
        assertEquals(5.0, speeds.first().kmPerSecond * 3600, 1e-10)
        assertEquals(9.999, speeds.last().warp!!, 0.0)
        assertEquals(200_000.0, speeds.last().kmPerSecond / light.kmPerSecond, 1e-9)
        val ratios = speeds.zipWithNext().map { (a, b) -> b.kmPerSecond / a.kmPerSecond }
        assertTrue(ratios.max() - ratios.min() > 10)
    }
    @Test fun distanceOverSpeedUsesSecondsForWalkingAircraftAndTheMoon() {
        assertEquals(3600.0, spaceCompassTravelSeconds(5.0, speeds.first())!!, 1e-9)
        assertEquals(3600.0, spaceCompassTravelSeconds(900.0, speeds.single { it.key == "aircraft" })!!, 1e-9)
        assertEquals(1.282220381941695, spaceCompassTravelSeconds(384400.0, light)!!, 1e-12)
    }
    @Test fun aLightYearAtLightSpeedIsAJulianYearAndWarpUsesItsDeclaredComparisonFactor() {
        val lightYear = light.kmPerSecond * SPACE_COMPASS_TRAVEL_YEAR_SECONDS
        assertEquals(SPACE_COMPASS_TRAVEL_YEAR_SECONDS, spaceCompassTravelSeconds(lightYear, light)!!, 1e-6)
        assertEquals(SPACE_COMPASS_TRAVEL_YEAR_SECONDS / 200_000,
            spaceCompassTravelSeconds(lightYear, speeds.last())!!, 1e-6)
    }
    @Test fun durationsAdaptFromNanosecondsToYearsAndRoundedBoundariesPromote() {
        for ((seconds, unit) in listOf(1e-7 to SpaceCompassTravelTimeUnit.NANOSECOND,
            1e-4 to SpaceCompassTravelTimeUnit.MICROSECOND, .5 to SpaceCompassTravelTimeUnit.MILLISECOND,
            1.0 to SpaceCompassTravelTimeUnit.SECOND, 60.0 to SpaceCompassTravelTimeUnit.MINUTE,
            3600.0 to SpaceCompassTravelTimeUnit.HOUR, 86400.0 to SpaceCompassTravelTimeUnit.DAY,
            SPACE_COMPASS_TRAVEL_YEAR_SECONDS to SpaceCompassTravelTimeUnit.YEAR)) {
            assertEquals(unit, spaceCompassTravelDuration(seconds)!!.unit)
        }
        assertEquals(SpaceCompassTravelTimeUnit.MINUTE, spaceCompassTravelDuration(59.999)!!.unit)
        assertEquals(SpaceCompassTravelTimeUnit.SECOND, spaceCompassTravelDuration(0.0)!!.unit)
    }
    @Test fun missingUnmeasuredAndFictionalDistancesNeverBecomeTravelEstimates() {
        for (value in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, -1.0)) {
            assertNull(spaceCompassTravelSeconds(value, light))
            assertNull(spaceCompassTravelDuration(value))
        }
        assertNull(spaceCompassTravelDistanceKm(SpaceCompassCelestialBody.ARRAKIS, 123.0))
        assertNull(spaceCompassTravelDistanceKm(SpaceCompassCelestialBody.SOUTHERN_CROSS, 123.0))
        assertEquals(384400.0, spaceCompassTravelDistanceKm(SpaceCompassCelestialBody.MOON, 384400.0)!!, 0.0)
        for (speed in listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY))
            assertNull(spaceCompassTravelSeconds(100.0, SpaceCompassTravelSpeed("invalid", speed)))
        assertNull(spaceCompassTravelSeconds(Double.MAX_VALUE, speeds.first()))
    }
    @Test fun veryDistantTargetsRemainFiniteAndDoNotOverflowIntegerTimeTypes() {
        val seconds = spaceCompassTravelSeconds(1e30, speeds.first())!!
        val duration = spaceCompassTravelDuration(seconds)!!
        assertTrue(seconds.isFinite())
        assertEquals(SpaceCompassTravelTimeUnit.YEAR, duration.unit)
        assertTrue(duration.amount > Long.MAX_VALUE.toDouble())
        assertEquals(seconds, duration.amount * duration.unit.seconds, seconds * 1e-14)
    }
    @Test fun displayConversionHonoursMetricImperialAndUserNumberFormats() {
        assertEquals("5 km/h", formatSpaceCompassTravelSpeed(speeds.first(), SpaceCompassNumericFormat.AMERICAN, false, Locale.US))
        assertEquals("3.11 mph", formatSpaceCompassTravelSpeed(speeds.first(), SpaceCompassNumericFormat.AMERICAN, true, Locale.US))
        assertEquals("299.792,46 km/s", formatSpaceCompassTravelSpeed(light, SpaceCompassNumericFormat.EUROPEAN, false, Locale.US))
        assertEquals("186,282.4 mi/s", formatSpaceCompassTravelSpeed(light, SpaceCompassNumericFormat.AMERICAN, true, Locale.ITALIAN))
        val shown = formatSpaceCompassTravelSpeed(speeds.last(), SpaceCompassNumericFormat.EUROPEAN, false, Locale.US)
        assertTrue(shown.contains("×10^") && shown.endsWith(" km/s"))
    }
    @Test fun savedSpeedRestoresAcrossObjectsAndUnknownOrAbsentKeysUseWalking() {
        assertEquals(0, spaceCompassRestoreTravelSpeed(null))
        assertEquals(0, spaceCompassRestoreTravelSpeed("removed"))
        assertEquals(speeds.lastIndex - 1, spaceCompassRestoreTravelSpeed("warp-9.99"))
        assertEquals(speeds.lastIndex, spaceCompassRestoreTravelSpeed("warp-9.999"))
        for (index in speeds.indices) assertEquals(index, spaceCompassRestoreTravelSpeed(speeds[index].key))
    }
}

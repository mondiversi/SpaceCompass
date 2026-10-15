package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.*

class SpaceCompassScreensaverSeasonTest {
    private fun ms(text: String) = Instant.parse(text).toEpochMilli()
    private val summer = ms("2026-06-21T12:00:00Z")
    private val winter = ms("2026-12-21T12:00:00Z")
    private fun degrees(value: Double) = Math.toDegrees(value)

    @Test fun juneAndDecemberExposeOppositePolesWithTheRealAxialTilt() {
        val june = spaceCompassScreensaverSeason(summer)
        val december = spaceCompassScreensaverSeason(winter)
        assertEquals(23.44, degrees(june.pitchRadians), .03)
        assertEquals(-23.44, degrees(december.pitchRadians), .03)
        assertEquals(0.0, degrees(june.rollRadians), .2)
        assertEquals(0.0, degrees(december.rollRadians), .2)
    }

    @Test fun equinoxesKeepTheAxisInTheImagePlaneAndReverseItsApparentRoll() {
        val march = spaceCompassScreensaverSeason(ms("2026-03-20T15:00:00Z"))
        val september = spaceCompassScreensaverSeason(ms("2026-09-23T01:00:00Z"))
        assertEquals(0.0, degrees(march.pitchRadians), .15)
        assertEquals(0.0, degrees(september.pitchRadians), .15)
        assertEquals(march.obliquityDegrees, degrees(march.rollRadians), .01)
        assertEquals(-september.obliquityDegrees, degrees(september.rollRadians), .01)
    }

    @Test fun theThreeDimensionalAxisKeepsItsObliquityThroughoutEverySeason() {
        for (year in listOf(1900, 2000, 2026, 2100)) for (month in 1..12) {
            val instant = LocalDate.of(year, month, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val view = spaceCompassScreensaverSeason(instant)
            val x = sin(view.rollRadians) * cos(view.pitchRadians)
            val y = cos(view.rollRadians) * cos(view.pitchRadians)
            val z = sin(view.pitchRadians)
            assertEquals(1.0, x*x + y*y + z*z, 1e-12)
            assertEquals(view.obliquityDegrees, degrees(acos(y)), 1e-10)
        }
    }

    @Test fun paviaMovesNearerTheDiscCentreInSummerAndNearerTheTopLimbInWinter() {
        fun latitudeOnDisc(time: Long): Double {
            val view = spaceCompassScreensaverSeason(time)
            val latitude = Math.toRadians(45.185)
            return (sin(latitude)*cos(view.pitchRadians)-cos(latitude)*sin(view.pitchRadians))*cos(view.rollRadians)
        }
        assertEquals(.371, latitudeOnDisc(summer), .005)
        assertEquals(.931, latitudeOnDisc(winter), .005)
        assertTrue(latitudeOnDisc(summer) < latitudeOnDisc(winter))
    }

    @Test fun southernLocationsHaveTheOppositeSeasonalShiftWithoutChangingTheGlobalAxis() {
        val latitude = Math.toRadians(-34.6)
        fun position(time: Long): Double {
            val view = spaceCompassScreensaverSeason(time)
            return sin(latitude-view.pitchRadians)*cos(view.rollRadians)
        }
        assertTrue(abs(position(winter)) < abs(position(summer)))
    }

    @Test fun aFixedScenarioUsesItsChosenSeasonEvenWhenTheActualMonthIsDifferent() {
        val plan = SpaceCompassObserverPlan(45.185, 9.16, 80.0, summer, "UTC",
            simulatePosition = false, simulateAltitude = false, simulateZone = false)
        assertEquals(spaceCompassScreensaverSeason(summer), spaceCompassScreensaverSeason(winter, plan))
    }

    @Test fun anAdvancingScenarioUsesItsPersistedClockOffsetWithoutGps() {
        val plan = SpaceCompassObserverPlan(45.185, 9.16, 80.0, summer, "UTC",
            simulatePosition = false, simulateAltitude = false, simulateZone = false,
            advanceTime = true, clockOffsetMs = summer-winter)
        assertEquals(spaceCompassScreensaverSeason(summer), spaceCompassScreensaverSeason(winter, plan))
        assertEquals(spaceCompassScreensaverSeason(summer+86_400_000),
            spaceCompassScreensaverSeason(winter+86_400_000, plan))
    }

    @Test fun yearAndSeasonBoundariesHaveNoVisualJump() {
        for (boundary in listOf("2026-01-01T00:00:00Z", "2026-03-20T15:00:00Z",
            "2026-06-21T12:00:00Z", "2026-09-23T01:00:00Z", "2026-12-21T12:00:00Z")) {
            val t = ms(boundary)
            val before = spaceCompassScreensaverSeason(t-60_000)
            val after = spaceCompassScreensaverSeason(t+60_000)
            assertTrue(abs(before.rollRadians-after.rollRadians) < .0001)
            assertTrue(abs(before.pitchRadians-after.pitchRadians) < .0001)
        }
    }
}

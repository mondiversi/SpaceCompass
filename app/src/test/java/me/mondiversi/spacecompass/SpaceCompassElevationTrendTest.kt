package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SpaceCompassElevationTrendTest {
    private fun p(time: Long, elevation: Double, event: SpaceCompassSunPathEvent = SpaceCompassSunPathEvent.HOUR) =
        SpaceCompassSunPathPoint(time, SpaceCompassSunPosition(100.0, elevation), event)

    @Test fun everyHourlyAndCurrentPointUsesElevationSlopeIncludingNegativeElevations() {
        val samples = listOf(p(0, -40.0), p(1000, -20.0), p(2000, -30.0))
        assertEquals(SpaceCompassElevationTrend.RISING, spaceCompassElevationTrend(samples[0], samples))
        assertEquals(SpaceCompassElevationTrend.RISING, spaceCompassElevationTrend(p(500, -30.0), samples))
        assertEquals(SpaceCompassElevationTrend.FALLING, spaceCompassElevationTrend(samples[1], samples))
        assertEquals(SpaceCompassElevationTrend.FALLING, spaceCompassElevationTrend(p(1500, -25.0), samples))
        assertEquals(SpaceCompassElevationTrend.FALLING, spaceCompassElevationTrend(samples[2], samples))
    }

    @Test fun turningPointsUseOutgoingPhaseAndCoincidentEventsArePreserved() {
        assertEquals(SpaceCompassElevationTrend.FALLING,
            spaceCompassElevationTrend(p(0, 50.0, SpaceCompassSunPathEvent.CULMINATION), emptyList()))
        assertEquals(SpaceCompassElevationTrend.RISING,
            spaceCompassElevationTrend(p(0, -50.0).copy(coincidentEvents = setOf(SpaceCompassSunPathEvent.MINIMUM)), emptyList()))
        assertEquals(SpaceCompassElevationTrend.RISING,
            spaceCompassElevationTrend(p(0, -.833, SpaceCompassSunPathEvent.SUNRISE), emptyList()))
        assertEquals(SpaceCompassElevationTrend.FALLING,
            spaceCompassElevationTrend(p(0, -.833, SpaceCompassSunPathEvent.SUNSET), emptyList()))
    }

    @Test fun unavailableFlatInvalidAndOutOfWindowDataDoNotInventAnArrow() {
        val flat = listOf(p(0, 20.0), p(1000, 20.0))
        assertNull(spaceCompassElevationTrend(p(500, 20.0), flat))
        assertNull(spaceCompassElevationTrend(p(-1, 20.0), flat))
        assertNull(spaceCompassElevationTrend(p(1001, 20.0), flat))
        assertNull(spaceCompassElevationTrend(p(500, 20.0), listOf(p(0, Double.NaN), p(1000, 30.0))))
        assertNull(spaceCompassElevationTrend(p(500, 100.0), flat))
        assertNull(spaceCompassElevationTrend(p(500, 20.0), emptyList()))
    }

    @Test fun realDailySunCurveRetainsCurrentAndKeyRowsWithCorrectTrend() {
        val zone = ZoneId.of("Europe/Rome")
        val sun = calculateSpaceCompassSunDailyPath(LocalDate.parse("2026-10-16"), zone, 45.0, 9.0)
        val rise = sun.markers.first { it.event == SpaceCompassSunPathEvent.SUNRISE }
        val set = sun.markers.first { it.event == SpaceCompassSunPathEvent.SUNSET }
        assertEquals(SpaceCompassElevationTrend.RISING, spaceCompassElevationTrend(rise, sun.samples))
        assertEquals(SpaceCompassElevationTrend.FALLING, spaceCompassElevationTrend(set, sun.samples))
        val current = spaceCompassCurrentPathPoint(sun.body, rise.timeMs, rise.position)!!
        val rows = spaceCompassDailyPathListEntries(sun, current)
        assertEquals(1, rows.count { it.isCurrent })
        assertTrue(rows.any { it.point.event == SpaceCompassSunPathEvent.SUNRISE })
        assertEquals(SpaceCompassElevationTrend.RISING, spaceCompassElevationTrend(current, sun.samples))
    }

    @Test fun badgeDigitsChangeOnlyAtNightForBothCyanAndOrangeCounters() {
        assertEquals(androidx.compose.ui.graphics.Color.White, spaceCompassCounterTextColor(false))
        val dark = spaceCompassCounterTextColor(true)
        assertEquals(1f, dark.alpha)
        assertTrue(dark.red < .1f && dark.green < .1f && dark.blue < .1f)
    }

    @Test fun currentOnEitherSideOfAnExactPeakDoesNotWaitForTheNextCoarseSample() {
        val samples = listOf(p(0, 49.0), p(1000, 49.0))
        val peak = p(400, 50.0, SpaceCompassSunPathEvent.CULMINATION)
        assertEquals(SpaceCompassElevationTrend.RISING, spaceCompassElevationTrend(p(300, 49.9), samples, listOf(peak)))
        assertEquals(SpaceCompassElevationTrend.FALLING, spaceCompassElevationTrend(p(500, 49.9), samples, listOf(peak)))
        assertEquals(SpaceCompassElevationTrend.FALLING, spaceCompassElevationTrend(p(400, 50.0), samples, listOf(peak)))
    }
}

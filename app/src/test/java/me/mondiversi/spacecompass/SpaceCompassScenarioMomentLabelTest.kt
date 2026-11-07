package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScenarioMomentLabelTest {
    private val now = Instant.parse("2026-11-07T12:00:00Z").toEpochMilli()
    private val zone = ZoneId.of("Europe/Rome")
    private val plan = spaceCompassObserverPlan(45.0, 9.0, 100.0, now, zone.id)!!

    @Test fun aDifferentCivilDayShowsTheLabelForEitherDirection() {
        assertFalse(spaceCompassShowScenarioMoment(null, now + 86_400_000L, now, zone))
        assertFalse(spaceCompassShowScenarioMoment(plan.copy(simulatePosition = false, simulateAltitude = false,
            simulateZone = false, simulateDate = false, simulateTime = false), now + 86_400_000L, now, zone))
        assertFalse(spaceCompassShowScenarioMoment(plan, now + 3_600_000L, now, zone))
        assertTrue(spaceCompassShowScenarioMoment(plan, now - 86_400_000L, now, zone))
        assertTrue(spaceCompassShowScenarioMoment(plan, now + 86_400_000L, now, zone))
    }

    @Test fun theRunningMomentControlsVisibilityEvenWhenTheSavedDateIsUnchanged() {
        val clock = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, 86400), "running", now, now, 0L) { 0L }
        clock.setActive(true, now, 0L)
        val running = plan.copy(simulatePosition = false, simulateAltitude = false, simulateZone = false,
            simulateDate = false, simulateTime = false).also { it.scenarioClock = clock }
        assertTrue(spaceCompassShowScenarioMoment(running, clock.at(now + 1000L, 1000L), now, zone))
        assertEquals(now, running.timeMs)
        assertFalse(spaceCompassShowScenarioMoment(running, now, now, zone))
    }

    @Test fun futureDayComparisonUsesTheLocationZoneForBothInstants() {
        val real = Instant.parse("2026-11-07T23:30:00Z").toEpochMilli()
        val simulated = Instant.parse("2026-11-08T00:30:00Z").toEpochMilli()
        assertTrue(spaceCompassShowScenarioMoment(plan, simulated, real, ZoneId.of("UTC")))
        assertFalse(spaceCompassShowScenarioMoment(plan, simulated, real, ZoneId.of("America/Los_Angeles")))
    }

    @Test fun pauseSymbolPrecedesTheDateOnlyWhenTimeIsPaused() {
        val formatting = SpaceCompassPanoramaFormatting(locale = Locale.ITALIAN,
            date = SpaceCompassDateFormat.EUROPEAN, time = SpaceCompassTimeFormat.H24)
        val moving = formatSpaceCompassScenarioMoment(now, zone, formatting)
        val paused = formatSpaceCompassScenarioMoment(now, zone, formatting, paused = true)
        assertEquals("⏸ $moving", paused)
        assertFalse(moving.startsWith("⏸"))
    }

    @Test fun selectedDateTimeFormatsAndTheOffsetAtTheSimulatedMomentArePreserved() {
        val summer = Instant.parse("2027-06-21T11:45:30Z").toEpochMilli()
        val winter = Instant.parse("2027-12-21T11:45:30Z").toEpochMilli()
        val formatting = SpaceCompassPanoramaFormatting(locale = Locale.ITALIAN,
            date = SpaceCompassDateFormat.EUROPEAN, time = SpaceCompassTimeFormat.H24)
        assertEquals("21/06/2027 · 13:45:30 · UTC+02:00", formatSpaceCompassScenarioMoment(summer, zone, formatting))
        assertEquals("21/12/2027 · 12:45:30 · UTC+01:00", formatSpaceCompassScenarioMoment(winter, zone, formatting))
        assertTrue(formatSpaceCompassScenarioMoment(summer, zone, formatting.copy(date = SpaceCompassDateFormat.INTERNATIONAL))
            .startsWith("2027-06-21 · "))
    }
}

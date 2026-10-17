package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassReverseScenarioTest {
    private val wall = Instant.parse("2026-10-17T10:00:00Z").toEpochMilli()
    private var elapsed = 0L
    private fun clock(start: String, mode: SpaceCompassScenarioResume = SpaceCompassScenarioResume.ACTIVE,
        rate: Int = -3600, checkpoint: SpaceCompassScenarioCheckpoint? = null) = SpaceCompassScenarioClock(
        SpaceCompassScenarioTimeSettings(true, rate, mode), "reverse", Instant.parse(start).toEpochMilli(),
        wall, elapsed, checkpoint) { elapsed }

    @Test fun pauseIsTheExactMiddleAndEveryOldForwardSpeedHasAnEqualReverseStep() {
        val rates = spaceCompassScenarioRates
        assertEquals(0, rates[rates.lastIndex / 2])
        assertEquals(1, rates[rates.lastIndex / 2 + 1])
        assertEquals(-1, rates[rates.lastIndex / 2 - 1])
        assertEquals(-3600, rates.first()); assertEquals(3600, rates.last())
        assertEquals(rates, rates.reversed().map { -it })
        for (rate in rates) {
            val settings = SpaceCompassScenarioTimeSettings(true, rate).normalized()
            assertEquals(rate, settings.multiplier)
            assertEquals(kotlin.math.abs(rate) > 1, settings.accelerated)
            assertFalse(settings.copy(enabled = false).accelerated)
        }
    }

    @Test fun reversePassesLocalMidnightIntoThePreviousMonthAndYear() {
        val c = clock("2027-01-01T00:30:00Z")
        c.setActive(true, wall); elapsed += 1000
        assertEquals(Instant.parse("2026-12-31T23:30:00Z").toEpochMilli(), c.at(wall + 1000))
        val zone = ZoneId.of("America/New_York")
        val local = clock("2026-10-17T04:00:30Z", rate = -1)
        local.setActive(true, wall); elapsed += 60_000
        val reached = Instant.ofEpochMilli(local.at(wall + 60_000)).atZone(zone)
        assertEquals(LocalDate.parse("2026-10-16"), reached.toLocalDate())
        assertEquals(LocalTime.of(23, 59, 30), reached.toLocalTime())
    }

    @Test fun reverseFollowsTheRealTimelineAcrossLeapDayAndDaylightSaving() {
        val leap = clock("2028-03-01T00:30:00Z")
        leap.setActive(true, wall); elapsed += 1000
        assertEquals(Instant.parse("2028-02-29T23:30:00Z").toEpochMilli(), leap.at(wall + 1000))
        val dst = clock("2026-03-29T01:30:00Z")
        dst.setActive(true, wall); elapsed += 1000
        val local = Instant.ofEpochMilli(dst.at(wall + 1000)).atZone(ZoneId.of("Europe/Rome"))
        assertEquals(LocalTime.of(1, 30), local.toLocalTime())
        assertEquals(3600, local.offset.totalSeconds)
    }

    @Test fun reverseActiveOnlyPausesAndRestoresWithoutReversingClosedTime() {
        val start = "2026-10-17T12:00:00Z"
        val c = clock(start, rate = -60)
        c.setActive(true, wall); elapsed += 1000
        c.setActive(false, wall + 1000)
        val reached = c.at(wall + 1000)
        elapsed += 60_000
        assertEquals(reached, c.at(wall + 61_000))
        val saved = c.checkpoint(wall + 61_000)
        elapsed = 0
        val restored = SpaceCompassScenarioClock(c.settings, "reverse", 0, wall + 86_400_000,
            elapsed, saved) { elapsed }
        assertEquals(reached, restored.at(wall + 86_400_000))
        restored.setActive(true, wall + 86_400_000); elapsed += 1000
        assertEquals(reached - 60_000, restored.at(wall + 86_401_000))
    }

    @Test fun reverseAlwaysIncludesClosedTimeAndRestartStillUsesTheConfiguredInstant() {
        val start = "2026-10-17T12:00:00Z"
        val initial = Instant.parse(start).toEpochMilli()
        val c = clock(start, SpaceCompassScenarioResume.ALWAYS, -60)
        val checkpoint = c.checkpoint(wall + 1000)
        val restored = SpaceCompassScenarioClock(c.settings, "reverse", initial, wall + 60_000,
            0L, checkpoint) { 0L }
        assertEquals(initial - 3_600_000, restored.at(wall + 60_000))
        val restarted = clock(start, SpaceCompassScenarioResume.RESTART, -60, checkpoint)
        assertEquals(initial, restarted.at(wall + 60_000))
        restarted.setActive(true, wall); elapsed += 1000
        assertEquals(initial - 60_000, restarted.at(wall + 1000))
    }

    @Test fun aReversePhotographUsesExposureTimeRatherThanLaterProcessingCompletion() {
        val c = clock("2026-10-17T12:00:00Z", rate = -60)
        c.setActive(true, wall); elapsed += 3000
        val initial = Instant.parse("2026-10-17T12:00:00Z").toEpochMilli()
        assertEquals(initial - 180_000, c.at(wall + 3000))
        assertEquals(initial - 60_000, c.atWallTime(wall + 1000, wall + 3000, elapsed))
    }

    @Test fun changingFromForwardToReverseAndPauseKeepsTheReachedInstant() {
        val start = Instant.parse("2026-10-17T12:00:00Z").toEpochMilli()
        var instant = start
        for (rate in listOf(60, -3600, 0, -1, 1)) {
            val c = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, rate),
                "switch", instant, wall, elapsed) { elapsed }
            c.setActive(true, wall)
            assertEquals(instant, c.at(wall))
            elapsed += 1000
            instant += rate * 1000L
            assertEquals(instant, c.at(wall + 1000))
        }
    }

    @Test fun reverseClampsAtTheSupportedFirstYearAndFeedsEarlierAstronomyAndWeatherTimes() {
        val c = clock("2026-10-17T12:00:00Z", SpaceCompassScenarioResume.ALWAYS)
        assertEquals(1900, Instant.ofEpochMilli(c.at(Long.MAX_VALUE / 2)).atZone(ZoneId.of("UTC")).year)
        val plan = spaceCompassObserverPlan(45.0, 9.0, 100.0, wall, "UTC")!!
            .also { it.scenarioClock = clock("2026-10-17T12:00:00Z", SpaceCompassScenarioResume.ALWAYS) }
        plan.scenarioClock!!.setActive(true, wall)
        val before = plan.observationTimeMs(wall)
        elapsed += 1000
        val after = plan.observationTimeMs(wall + 1000)
        assertEquals(before - 3_600_000, after)
        assertTrue(plan.dataRequestTimeMs(wall + 1000)!! < before)
        assertNotEquals(calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.MOON, before, 45.0, 9.0),
            calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.MOON, after, 45.0, 9.0))
    }
}

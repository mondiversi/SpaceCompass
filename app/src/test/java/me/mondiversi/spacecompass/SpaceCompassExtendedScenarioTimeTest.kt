package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassExtendedScenarioTimeTest {
    private val wall = Instant.parse("2026-11-07T12:00:00Z").toEpochMilli()
    private val start = Instant.parse("2028-01-01T00:30:00Z").toEpochMilli()

    @Test fun maximumSpeedTraversesOneDayInBothDirectionsForEveryResumePolicy() {
        for (mode in SpaceCompassScenarioResume.entries) for (rate in listOf(-86400, 86400)) {
            val clock = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, rate, mode),
                "day", start, wall, 0L) { 0L }
            clock.setActive(true, wall, 0L)
            assertEquals(start + rate * 1000L, clock.at(wall + 1000L, 1000L))
            // A minute of real time spans 60 days, exceeding a signed Int in milliseconds.
            assertEquals(start + rate * 60_000L, clock.at(wall + 60_000L, 60_000L))
            assertEquals(start + rate * 500L,
                clock.atWallTime(wall + 500L, wall + 1000L, 1000L))
        }
    }

    @Test fun fastForwardAndReverseRespectLeapDaysYearBoundariesAndTheSelectedZone() {
        for ((origin, rate, expected) in listOf(
            Triple("2028-02-28T12:00:00Z", 86400, "2028-02-29T12:00:00Z"),
            Triple("2028-03-01T12:00:00Z", -86400, "2028-02-29T12:00:00Z"),
            Triple("2027-12-31T12:00:00Z", 86400, "2028-01-01T12:00:00Z"),
            Triple("2028-01-01T12:00:00Z", -86400, "2027-12-31T12:00:00Z")
        )) {
            val clock = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, rate),
                "calendar", Instant.parse(origin).toEpochMilli(), wall, 0L) { 0L }
            clock.setActive(true, wall, 0L)
            assertEquals(Instant.parse(expected).toEpochMilli(), clock.at(wall + 1000L, 1000L))
        }
        val zone = ZoneId.of("Europe/Rome")
        val origin = Instant.parse("2028-03-25T12:00:00Z")
        val clock = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, 86400),
            "dst", origin.toEpochMilli(), wall, 0L) { 0L }
        clock.setActive(true, wall, 0L)
        val reached = Instant.ofEpochMilli(clock.at(wall + 1000L, 1000L)).atZone(zone)
        assertEquals(origin.atZone(zone).hour + 1, reached.hour)
        assertEquals(origin.atZone(zone).toLocalDate().plusDays(1), reached.toLocalDate())
    }

    @Test fun newSpeedsRoundTripAndResumeWithoutAddingTheAgeOfAnArchive() {
        val plan = spaceCompassObserverPlan(45.0, 9.0, 100.0, start, "UTC")!!
        for (rate in listOf(-86400, -43200, -21600, -7200, -900, -5, 5, 900, 7200, 21600, 43200, 86400)) {
            val settings = SpaceCompassScenarioTimeSettings(true, rate, SpaceCompassScenarioResume.ALWAYS)
            val archive = SpaceCompassScenarioArchive("Time", plan, settings)
            val decoded = SpaceCompassScenarioArchive.decode(archive.encode())
            assertEquals(archive, decoded)
            val receiver = SpaceCompassObserverState(null, elapsedClock = { 0L })
            receiver.importArchive(decoded)
            assertEquals(settings, receiver.timeSettings)
            assertEquals(start, receiver.savedPlan!!.timeMs)
            assertEquals(start, decoded.configurationAtImport(wall, ZoneId.of("UTC")).timeMs)
        }
    }

    @Test fun legacyArchivesKeepTheirDirectionWithoutIncreasingSpeed() {
        val plan = spaceCompassObserverPlan(45.0, 9.0, 100.0, start, "UTC")!!
        val migrated = mapOf(2 to 1, 3 to 1, 6 to 5, 120 to 60, 600 to 300, 1800 to 900)
        for ((old, next) in migrated) for (sign in listOf(-1, 1))
        for (enabled in listOf(false, true)) for (mode in SpaceCompassScenarioResume.entries) {
            val settings = SpaceCompassScenarioTimeSettings(enabled, sign * old, mode)
            val archive = SpaceCompassScenarioArchive("Legacy", plan, settings)
            val decoded = SpaceCompassScenarioArchive.decode(archive.encode())
            assertEquals(settings.copy(rate = sign * next), decoded.timeSettings)
            assertEquals(decoded, SpaceCompassScenarioArchive.decode(decoded.encode()))
        }
        for (invalid in listOf(Int.MIN_VALUE, Int.MAX_VALUE, -86401, 86401, 59)) {
            assertEquals(1, SpaceCompassScenarioTimeSettings(true, invalid).normalized().rate)
            assertThrows(IllegalArgumentException::class.java) {
                SpaceCompassScenarioArchive("Invalid", plan, SpaceCompassScenarioTimeSettings(true, invalid)).encode()
            }
        }
    }

    @Test fun maximumSpeedClampsAtBothSupportedTimelineBoundaries() {
        for ((rate, expected) in listOf(-86400 to "1900-01-01T00:00:00Z", 86400 to "2100-12-31T23:59:59.999Z")) {
            val clock = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, rate,
                SpaceCompassScenarioResume.ALWAYS), "limit", start, wall, 0L) { 0L }
            assertEquals(Instant.parse(expected).toEpochMilli(), clock.at(Long.MAX_VALUE / 2, 0L))
        }
    }
}

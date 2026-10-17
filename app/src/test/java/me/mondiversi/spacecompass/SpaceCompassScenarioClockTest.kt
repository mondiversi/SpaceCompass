package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SpaceCompassScenarioClockTest {
    private val initial = Instant.parse("2026-10-16T23:30:00Z").toEpochMilli()
    private val wall = Instant.parse("2026-10-16T12:00:00Z").toEpochMilli()
    private var elapsed = 100L
    private fun clock(mode: SpaceCompassScenarioResume = SpaceCompassScenarioResume.ACTIVE, rate: Int = 1,
        checkpoint: SpaceCompassScenarioCheckpoint? = null) = SpaceCompassScenarioClock(
        SpaceCompassScenarioTimeSettings(rate != 1,rate,mode), "test",initial,wall,elapsed,checkpoint) { elapsed }

    @Test fun normalRateIsIndependentFromAccelerationSwitch() {
        val c=clock(); c.setActive(true,wall); elapsed+=12_000
        assertEquals(initial+12_000,c.at(wall+12_000))
        assertEquals(1,SpaceCompassScenarioTimeSettings(false,3600).multiplier)
        assertEquals(initial+10_000,c.atWallTime(wall+10_000,wall+12_000,elapsed))
    }
    @Test fun rate3600RollsIntoNextDayMonthAndYear() {
        val start=Instant.parse("2026-12-31T23:30:00Z").toEpochMilli()
        val c=SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true,3600),"year",start,wall,elapsed) { elapsed }
        c.setActive(true,wall); elapsed+=1000
        assertEquals(Instant.parse("2027-01-01T00:30:00Z").toEpochMilli(),c.at(wall+1000))
    }
    @Test fun activeOnlyPausesForBackgroundAndSaverAndRestoresAfterReboot() {
        val c=clock(rate=60); c.setActive(true,wall); elapsed+=1000
        c.setActive(false,wall+1000)
        elapsed+=100_000
        assertEquals(initial+60_000,c.at(wall+101_000))
        val saved=SpaceCompassScenarioCheckpoint.decode(c.checkpoint(wall+101_000).encode())!!
        elapsed=5 // A reboot resets Android elapsedRealtime.
        val restored=SpaceCompassScenarioClock(c.settings,"test",initial,wall+50_000_000,elapsed,saved) { elapsed }
        restored.setActive(true,wall+50_000_000); elapsed+=1000
        assertEquals(initial+120_000,restored.at(wall+50_001_000))
    }
    @Test fun alwaysAccountsForClosedTimeAtSelectedSpeedWithoutBackgroundWork() {
        val c=clock(SpaceCompassScenarioResume.ALWAYS,60)
        assertEquals(initial+12*3600_000L,c.at(wall+12*60_000L))
        val restored=SpaceCompassScenarioClock(c.settings,"test",initial,wall+3600_000,5,
            c.checkpoint(wall+60_000)) { 5 }
        assertEquals(initial+60*3600_000L,restored.at(wall+3600_000))
    }
    @Test fun restartDiscardsCheckpointButAdvancesDuringSession() {
        val c=clock(SpaceCompassScenarioResume.RESTART,60,SpaceCompassScenarioCheckpoint("test",initial+999_000,wall))
        assertEquals(initial,c.at(wall)); c.setActive(true,wall); elapsed+=1000
        assertEquals(initial+60_000,c.at(wall+1000))
    }
    @Test fun differentConfiguredMomentRejectsPreviousCheckpoint() {
        val c=clock(checkpoint=SpaceCompassScenarioCheckpoint("different",initial+99_000,wall))
        assertEquals(initial,c.at(wall))
        assertNull(SpaceCompassScenarioCheckpoint.decode("broken"))
    }
    @Test fun activeTimeUsesMonotonicClockWhenSystemTimeChanges() {
        val c=clock(); c.setActive(true,wall); elapsed+=5000
        assertEquals(initial+5000,c.at(wall-86_400_000))
    }
    @Test fun speedChangeCanReanchorWithoutJumpAndDateCrossesDstCorrectly() {
        val c=clock(rate=60); c.setActive(true,wall); elapsed+=60_000
        val progressed=c.at(wall+60_000)
        val next=SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true,3600),"new",progressed,wall+60_000,elapsed) { elapsed }
        next.setActive(true,wall+60_000); elapsed+=1000
        assertEquals(progressed+3600_000,next.at(wall+61_000))
        val zone=ZoneId.of("Europe/Rome")
        val dst=Instant.parse("2026-03-29T00:30:00Z").toEpochMilli()
        val day=SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true,3600),"dst",dst,wall,elapsed) { elapsed }
        day.setActive(true,wall); elapsed+=1000
        assertEquals(3,Instant.ofEpochMilli(day.at(wall+1000)).atZone(zone).hour)
    }
    @Test fun runtimeClockDoesNotChangeSavedCivilFieldsOrLegacyPlanEncoding() {
        val p=spaceCompassObserverPlan(45.0,9.0,100.0,initial,"Europe/Rome")!!
        val withClock=p.copy().also { it.scenarioClock=clock() }
        assertEquals(p.encode(),withClock.encode())
        assertEquals(initial,withClock.observationTimeMs(wall))
        assertEquals(p,decodeSpaceCompassObserverPlan(withClock.encode()))
        assertEquals(spaceCompassScenarioClockSignature(p,SpaceCompassScenarioTimeSettings(),ZoneId.of("UTC")),
            spaceCompassScenarioClockSignature(p.copy(latitude=46.0),SpaceCompassScenarioTimeSettings(),ZoneId.of("UTC")))
        val deviceZone=p.copy(simulateZone=false,simulatePosition=false)
        assertEquals(spaceCompassScenarioClockSignature(deviceZone,SpaceCompassScenarioTimeSettings(),ZoneId.of("UTC")),
            spaceCompassScenarioClockSignature(deviceZone,SpaceCompassScenarioTimeSettings(),ZoneId.of("America/New_York")))
    }
    @Test fun invalidRatesNormalizeAndHugeElapsedIntervalsStayInSupportedYears() {
        assertEquals(1,SpaceCompassScenarioTimeSettings(true,1234).normalized().rate)
        val c=clock(SpaceCompassScenarioResume.ALWAYS,3600)
        assertEquals(2100,Instant.ofEpochMilli(c.at(Long.MAX_VALUE/2)).atZone(ZoneId.of("UTC")).year)
    }

    @Test fun zeroRateFreezesTimeAndExposureInstantsForEveryResumptionPolicy() {
        for (mode in SpaceCompassScenarioResume.entries) {
            elapsed = 100L
            val c = clock(mode, rate = 0)
            c.setActive(true, wall)
            elapsed += 2 * 86_400_000L
            assertEquals(initial, c.at(wall + 2 * 86_400_000L))
            assertEquals(initial, c.atWallTime(wall + 1000, wall + 2 * 86_400_000L, elapsed))
            c.setActive(false, wall + 2 * 86_400_000L)
            elapsed += 86_400_000L
            assertEquals(initial, c.at(wall + 3 * 86_400_000L))
            val checkpoint = SpaceCompassScenarioCheckpoint.decode(c.checkpoint(wall + 3 * 86_400_000L).encode())!!
            val restored = SpaceCompassScenarioClock(c.settings, "test", initial,
                wall + 10 * 86_400_000L, 0L, checkpoint) { 0L }
            assertEquals(initial, restored.at(wall + 20 * 86_400_000L))
        }
    }

    @Test fun pausingAndResumingRetainTheReachedInstantWithoutJumping() {
        val running = clock(rate = 60)
        running.setActive(true, wall)
        elapsed += 1000
        val reached = running.at(wall + 1000)
        val paused = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, 0,
            SpaceCompassScenarioResume.ACTIVE), "paused", reached, wall + 1000, elapsed) { elapsed }
        paused.setActive(true, wall + 1000)
        elapsed += 5000
        assertEquals(reached, paused.at(wall + 6000))
        val resumed = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(true, 2,
            SpaceCompassScenarioResume.ACTIVE), "resumed", paused.at(wall + 6000), wall + 6000, elapsed) { elapsed }
        resumed.setActive(true, wall + 6000)
        assertEquals(reached, resumed.at(wall + 6000))
        elapsed += 1000
        assertEquals(reached + 2000, resumed.at(wall + 7000))
    }

    @Test fun aPausedClockKeepsCelestialPositionsAndWeatherRequestsAtTheFrozenMoment() {
        val plan = spaceCompassObserverPlan(45.0, 9.0, 100.0, initial, "UTC")!!
            .also { it.scenarioClock = clock(rate = 0) }
        plan.scenarioClock!!.setActive(true, wall)
        val before = plan.observationTimeMs(wall)
        elapsed += 86_400_000L
        val after = plan.observationTimeMs(wall + 86_400_000L)
        assertEquals(before, after)
        assertEquals(plan.dataRequestTimeMs(wall), plan.dataRequestTimeMs(wall + 86_400_000L))
        assertEquals(calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.MOON, before, 45.0, 9.0),
            calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.MOON, after, 45.0, 9.0))
        assertEquals(0, SpaceCompassScenarioTimeSettings(true, 0).normalized().multiplier)
        assertEquals(1, SpaceCompassScenarioTimeSettings(false, 0).normalized().multiplier)
    }

    @Test fun ordinarySpeedAlsoRollsTheDateAtLocalMidnight() {
        val zone = ZoneId.of("America/New_York")
        val start = Instant.parse("2026-10-17T03:59:30Z").toEpochMilli()
        val c = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(false), "midnight", start, wall, elapsed) { elapsed }
        c.setActive(true, wall)
        elapsed += 60_000
        val reached = Instant.ofEpochMilli(c.at(wall + 60_000)).atZone(zone)
        assertEquals(java.time.LocalDate.parse("2026-10-17"), reached.toLocalDate())
        assertEquals(java.time.LocalTime.of(0, 0, 30), reached.toLocalTime())
    }
}

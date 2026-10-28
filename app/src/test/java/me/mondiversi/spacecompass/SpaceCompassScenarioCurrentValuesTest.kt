package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScenarioCurrentValuesTest {
    private val now = Instant.parse("2026-10-28T23:59:00Z").toEpochMilli()
    private val deviceZone = ZoneId.of("Europe/Rome")
    private val device = SpaceCompassDevicePlace(45.0, 9.0, 100.0)
    private fun values(plan: SpaceCompassObserverPlan? = null, place: SpaceCompassDevicePlace? = device,
        settings: SpaceCompassScenarioTimeSettings = SpaceCompassScenarioTimeSettings(), instant: Long = now) =
        spaceCompassScenarioCurrentValues(plan, place, settings, instant, deviceZone)

    @Test fun liveValuesUseTheRealDeviceAndCurrentMoment() {
        val current = values()
        assertEquals(device, current.place)
        assertEquals(100.0, current.altitudeMeters)
        assertEquals(deviceZone, current.zone)
        assertEquals(now, current.instant)
        assertEquals(1, current.rate)
    }

    @Test fun selectedPositionUsesAutomaticHeightAndZone() {
        val plan = SpaceCompassObserverPlan(28.0, -14.0, 999.0, now, "America/New_York",
            simulateTime = false, simulateAltitude = false, simulateZone = false, simulateDate = false,
            automaticAltitudeMeters = 42.0, automaticZoneId = "Atlantic/Canary")
        val current = values(plan)
        assertEquals(SpaceCompassDevicePlace(28.0, -14.0, 42.0), current.place)
        assertEquals(42.0, current.altitudeMeters)
        assertEquals(ZoneId.of("Atlantic/Canary"), current.zone)
        assertEquals(now, current.instant)
    }

    @Test fun independentAltitudeRemainsKnownWithoutAGpsFix() {
        val plan = SpaceCompassObserverPlan(0.0, 0.0, 250.0, now, "UTC",
            simulatePosition = false, simulateTime = false, simulateAltitude = true,
            simulateZone = false, simulateDate = false)
        val current = values(plan, place = null)
        assertNull(current.place)
        assertEquals(250.0, current.altitudeMeters)
    }

    @Test fun absentHeightIsNotReplacedWithZero() {
        val current = values(place = device.copy(altitude = null))
        assertNull(current.altitudeMeters)
        assertNull(values(place = null).place)
    }

    @Test fun movingClockSummaryCrossesMidnightInsteadOfShowingConfiguredDate() {
        val settings = SpaceCompassScenarioTimeSettings(true, 60, SpaceCompassScenarioResume.ALWAYS)
        val plan = SpaceCompassObserverPlan(45.0, 9.0, 100.0, now, "UTC")
        plan.scenarioClock = SpaceCompassScenarioClock(settings, "test", now, now, 0L, elapsed = { 0L })
        val current = values(plan, settings = settings, instant = now + 2_000L)
        assertEquals(Instant.parse("2026-10-29T00:01:00Z").toEpochMilli(), current.instant)
        assertEquals(60, current.rate)
    }

    @Test fun rateDistinguishesFrozenCustomTimeAndReversePlayback() {
        val plan = SpaceCompassObserverPlan(45.0, 9.0, 100.0, now, "UTC")
        assertEquals(0, values(plan).rate)
        assertEquals(-60, values(plan, settings = SpaceCompassScenarioTimeSettings(true, -60)).rate)
    }

    @Test fun runtimeClockAtNormalSpeedIsShownAsOneEvenWithCustomTime() {
        val settings = SpaceCompassScenarioTimeSettings(resume = SpaceCompassScenarioResume.ALWAYS)
        val plan = SpaceCompassObserverPlan(45.0, 9.0, 100.0, now, "UTC")
        plan.scenarioClock = SpaceCompassScenarioClock(settings, "normal", now, now, 0L, elapsed = { 0L })
        val current = values(plan, instant = now + 60_000L)
        assertEquals(1, current.rate)
        assertEquals(now + 60_000L, current.instant)
    }
}

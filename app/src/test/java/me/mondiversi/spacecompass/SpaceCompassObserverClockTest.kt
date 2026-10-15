package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverClockTest {
    private val rome = ZoneId.of("Europe/Rome")
    private fun ms(value: String) = Instant.parse(value).toEpochMilli()
    private val now = ms("2026-10-15T18:23:45.123Z")
    private val saved = ms("2027-04-06T19:05:00Z")
    private fun plan(date: Boolean, time: Boolean, zone: String = "Europe/Rome") =
        spaceCompassObserverPlan(45.0, 9.0, 100.0, saved, zone,
            simulatePosition = false, simulateTime = time, simulateAltitude = false,
            simulateZone = true, simulateDate = date, momentZoneId = "Europe/Rome")!!
    private fun draft(date: Boolean, time: Boolean) = SpaceCompassObserverDraft(false, time,
        "invalid", "", "", "bad", "2027-04-06", "21:05", simulateAltitude = false,
        simulateZone = false, simulateDate = date)

    @Test fun bothUncheckedUseTheLiveInstantIncludingSeconds() {
        val plan = plan(false, false)
        assertEquals(now, plan.observationTimeMs(now, rome))
        assertEquals(now + 86_400_000, plan.observationTimeMs(now + 86_400_000, rome))
        assertNull(plan.dataRequestTimeMs(now, rome))
    }

    @Test fun bothCheckedPreserveTheExactFixedInstantIncludingDstOverlap() {
        val second = ms("2026-11-01T06:30:10Z")
        val plan = plan(true, true, "America/New_York").copy(timeMs = second)
        assertEquals(second, plan.observationTimeMs(now, rome))
        assertEquals(second, plan.observationTimeMs(now + 86_400_000, rome))
        assertEquals(second, plan.timeOverrideMs)
    }

    @Test fun dateOnlyKeepsTheChosenCalendarDateButAdvancesTheCurrentLocalClock() {
        val plan = plan(true, false)
        assertNull(plan.timeOverrideMs)
        // Rome is UTC+2 on both dates; seconds must keep advancing.
        assertEquals(ms("2027-04-06T18:23:45.123Z"), plan.observationTimeMs(now, rome))
        assertEquals(ms("2027-04-06T18:24:45.123Z"), plan.observationTimeMs(now + 60_000, rome))
    }

    @Test fun timeOnlyUsesTodaysDateAndTheChosenClockWithoutAdvancingTheCustomTime() {
        val plan = plan(false, true)
        assertEquals(ms("2026-10-15T19:05:00Z"), plan.observationTimeMs(now, rome))
        assertEquals(ms("2026-10-15T19:05:00Z"), plan.observationTimeMs(now + 60_000, rome))
        assertEquals(ms("2026-10-16T19:05:00Z"), plan.observationTimeMs(now + 86_400_000, rome))
    }

    @Test fun currentDateIsTakenInTheSelectedPlaceNotFromThePhonesCalendar() {
        val plan = plan(false, true, "Pacific/Kiritimati")
        assertEquals(ms("2026-10-16T07:05:00Z"), plan.observationTimeMs(now, rome))
        val kathmandu = plan(false, true, "Asia/Kathmandu")
        assertEquals(ms("2026-10-16T15:20:00Z"), kathmandu.observationTimeMs(now, rome))
    }

    @Test fun partialTimeKeepsTheChosenCivilTimeWhenTheDeviceZoneChanges() {
        val plan = plan(false, true).copy(simulateZone = false)
        assertEquals(ms("2026-10-15T19:05:00Z"), plan.observationTimeMs(now, rome))
        assertEquals(ms("2026-10-16T01:05:00Z"), plan.observationTimeMs(now, ZoneId.of("America/New_York")))
    }

    @Test fun legacyElevenFieldPlansMigrateTheCombinedSwitchWithoutChangingTheirInstant() {
        for (enabled in listOf(false, true)) {
            val old = "45.0|9.0|100.0|$saved|Europe/Rome|true|$enabled|true|true|100.0|Europe/Rome"
            val restored = decodeSpaceCompassObserverPlan(old)!!
            assertEquals(enabled, restored.simulateDate)
            assertEquals(enabled, restored.simulateTime)
            assertEquals(if (enabled) saved else now, restored.observationTimeMs(now, rome))
            assertEquals(restored, decodeSpaceCompassObserverPlan(restored.encode()))
        }
    }

    @Test fun onlyEnabledDateAndTimeFieldsAreValidated() {
        val date = draft(true, false).copy(time = "invalid").resolve(null, now, rome.id).getOrThrow()!!
        assertEquals(ms("2027-04-06T18:23:45.123Z"), date.observationTimeMs(now, rome))
        val time = draft(false, true).copy(date = "invalid").resolve(null, now, rome.id).getOrThrow()!!
        assertEquals(ms("2026-10-15T19:05:00Z"), time.observationTimeMs(now, rome))
        assertTrue(draft(true, false).copy(date = "2027-02-30").resolve(null, now, rome.id).isFailure)
        assertTrue(draft(false, true).copy(time = "25:01").resolve(null, now, rome.id).isFailure)
        assertNull(draft(false, false).copy(date = "invalid", time = "bad").resolve(null, now, rome.id).getOrThrow())
    }

    @Test fun aDisabledManualZoneCannotChangeTheInterpretationOfPartialDatesOrTimes() {
        val previous = plan(true, true, "America/New_York")
        val resolved = draft(false, true).resolve(previous, now, rome.id).getOrThrow()!!
        assertEquals("America/New_York", resolved.zoneId)
        assertEquals("UTC", resolved.momentZoneId)
        assertEquals(ms("2026-10-15T19:05:00Z"), resolved.observationTimeMs(now, rome))
        assertEquals(resolved, decodeSpaceCompassObserverPlan(resolved.encode()))
    }

    @Test fun automaticPlaceZoneSuppliesTheClockWithoutEnablingTheManualZone() {
        val draft = draft(false, true).copy(simulatePosition = true, latitude = "27.7", longitude = "85.3",
            automaticAltitudeMeters = 1400.0, automaticZoneId = "Asia/Kathmandu")
        val resolved = draft.resolve(null, now, rome.id).getOrThrow()!!
        assertEquals("UTC", resolved.momentZoneId)
        assertEquals(ms("2026-10-16T15:20:00Z"), resolved.observationTimeMs(now, rome))
    }

    @Test fun zoneDetectionDoesNotRewriteAnIndependentlySelectedDateOrTime() {
        for ((date, time) in listOf(true to false, false to true)) {
            val draft = draft(date, time).copy(simulateZone = true, zone = "Europe/Rome")
            val changed = draft.withDetectedMetadata(SpaceCompassObserverMetadataField.ZONE,
                SpaceCompassObserverMetadata(100.0, "America/New_York"), SpaceCompassNumericFormat.INTERNATIONAL, rome.id)
            assertEquals(draft.date, changed.date)
            assertEquals(draft.time, changed.time)
            assertEquals("America/New_York", changed.zone)
        }
    }

    @Test fun automaticClockCrossingADstGapAdvancesToARealLocalTime() {
        val date = plan(true, false).copy(timeMs = ms("2027-03-28T12:00:00Z"))
        assertEquals(ms("2027-03-28T01:30:00Z"), date.observationTimeMs(ms("2027-03-27T01:30:00Z"), rome))
        val time = plan(false, true).copy(timeMs = ms("2027-01-15T01:30:00Z"))
        assertEquals(ms("2027-03-28T01:30:00Z"), time.observationTimeMs(ms("2027-03-28T12:00:00Z"), rome))
    }

    @Test fun dataRequestsStayInTheSameHourUntilTheClockCrossesTheProviderWindow() {
        val plan = plan(true, false)
        assertEquals(plan.dataRequestTimeMs(now, rome), plan.dataRequestTimeMs(now + 60_000, rome))
        assertEquals(plan.dataRequestTimeMs(now, rome)!! + 3_600_000,
            plan.dataRequestTimeMs(now + 3_600_000, rome))
        val beforeEpoch = plan.copy(timeMs = ms("1969-12-31T12:00:00Z"))
        assertEquals(ms("1969-12-31T19:00:00Z"), beforeEpoch.dataRequestTimeMs(now, rome))
    }

    @Test fun screensaverUsesTheSameMovingScenarioClockAsTheForegroundSky() {
        val plan = plan(true, false)
        val observer = SpaceCompassScreensaverSolarObserver(45.0, 9.0, 100.0, observerPlan = plan)
        assertEquals(calculateSpaceCompassSunPosition(plan.observationTimeMs(now), 45.0, 9.0, 100.0).elevationDegrees,
            observer.elevation(now), 0.0)
    }

    @Test fun disablingDateRetainsItsCivilValueForReopeningWithoutFreezingTodaysDate() {
        val input = draft(false, true)
        val restored = decodeSpaceCompassObserverPlan(input.resolve(null, now, rome.id).getOrThrow()!!.encode())!!
        val local = Instant.ofEpochMilli(restored.timeMs).atZone(ZoneId.of(restored.momentZoneId))
        assertEquals("2027-04-06", local.toLocalDate().toString())
        assertEquals("21:05", local.toLocalTime().toString())
        assertEquals(ms("2026-10-15T19:05:00Z"), restored.observationTimeMs(now, rome))
    }

    @Test fun disablingTimeRetainsItsCivilValueForReopeningWhileTheClockKeepsMoving() {
        val input = draft(true, false)
        val restored = decodeSpaceCompassObserverPlan(input.resolve(null, now, rome.id).getOrThrow()!!.encode())!!
        val local = Instant.ofEpochMilli(restored.timeMs).atZone(ZoneId.of(restored.momentZoneId))
        assertEquals("21:05", local.toLocalTime().toString())
        assertEquals(ms("2027-04-06T18:23:45.123Z"), restored.observationTimeMs(now, rome))
    }

    @Test fun partialClockKeepsTheRequestedWallTimeEvenIfTodayContainsADstGap() {
        val dstNow = ms("2027-03-28T12:00:00Z")
        val input = draft(false, true).copy(date = "2027-03-28", time = "02:30")
        val resolved = input.resolve(null, dstNow, rome.id).getOrThrow()!!
        assertEquals(ms("2027-03-28T01:30:00Z"), resolved.observationTimeMs(dstNow, rome))
        assertEquals(ms("2027-03-29T00:30:00Z"), resolved.observationTimeMs(dstNow + 86_400_000, rome))
        assertEquals("02:30", Instant.ofEpochMilli(resolved.timeMs).atZone(ZoneId.of(resolved.momentZoneId)).toLocalTime().toString())
    }

}

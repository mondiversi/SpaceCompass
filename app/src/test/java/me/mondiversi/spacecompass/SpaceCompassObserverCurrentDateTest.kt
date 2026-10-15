package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverCurrentDateTest {
    private fun ms(value: String) = Instant.parse(value).toEpochMilli()
    private val now = ms("2026-10-15T22:30:45Z")
    private val draft = SpaceCompassObserverDraft(false, true, "", "", "", "Europe/Rome",
        "2027-04-06", "12:15", simulateAltitude = false, simulateZone = true, simulateDate = true)
    private fun saved(value: SpaceCompassObserverDraft, instant: Long = now) =
        value.resolve(null, instant, "America/New_York").getOrThrow()!!

    @Test fun currentDateUsesTheScenarioZoneRatherThanTheDeviceCalendarDay() {
        val result = draft.withCurrentDate(now, "America/New_York").getOrThrow()
        assertEquals("2026-10-16", result.date)
        assertEquals("12:15", result.time)
        assertEquals(ms("2026-10-16T10:15:00Z"), saved(result).timeOverrideMs)
    }

    @Test fun currentDateUsesAutomaticPositionZoneWhenManualZoneIsOff() {
        val result = draft.copy(simulatePosition = true, simulateZone = false,
            automaticZoneId = "Pacific/Kiritimati").withCurrentDate(now, "America/New_York").getOrThrow()
        assertEquals("2026-10-16", result.date)
        assertEquals("12:15", result.time)
    }

    @Test fun currentDateFallsBackToDeviceZoneWhenBothPositionAndZoneAreOff() {
        val result = draft.copy(simulateZone = false).withCurrentDate(now, "America/New_York").getOrThrow()
        assertEquals("2026-10-15", result.date)
        assertEquals("12:15", result.time)
        assertEquals(ms("2026-10-15T16:15:00Z"), saved(result).timeOverrideMs)
    }

    @Test fun currentDateDoesNotEnableTheTimeOverrideOrOverwriteItsHiddenValue() {
        val result = draft.copy(simulateTime = false).withCurrentDate(now, "America/New_York").getOrThrow()
        assertFalse(result.simulateTime)
        assertEquals("12:15", result.time)
        assertEquals(now, saved(result).observationTimeMs(now, ZoneId.of("America/New_York")))
        assertNull(result.clockOffsetMs)
    }

    @Test fun currentDateReanchorsARunningClockAndTheNewOffsetSurvivesReload() {
        val result = draft.copy(advanceTime = true, clockOffsetMs = 1L)
            .withCurrentDate(now, "America/New_York").getOrThrow()
        val plan = saved(result)
        val expected = ms("2026-10-16T10:15:00Z")
        assertEquals(expected - now, plan.clockOffsetMs)
        val restored = decodeSpaceCompassObserverPlan(plan.encode())!!
        assertEquals(expected + 123_456, restored.observationTimeMs(now + 123_456))
    }

    @Test fun fixedDateUsesTheNewDaysActualDaylightSavingOffset() {
        val winter = ms("2027-01-15T09:00:00Z")
        val result = draft.withCurrentDate(winter, "America/New_York").getOrThrow()
        assertEquals("12:15", result.time)
        assertEquals(ms("2027-01-15T11:15:00Z"), saved(result, winter).timeOverrideMs)
    }

    @Test fun nonexistentManualTimeIsRejectedWithoutSilentlyMovingTheChosenClock() {
        val spring = ms("2027-03-28T00:30:00Z")
        assertTrue(draft.copy(time = "02:30").withCurrentDate(spring, "Europe/Rome").isFailure)
    }

    @Test fun overlappingManualTimeKeepsThePreviouslySelectedOccurrence() {
        val autumn = ms("2026-10-25T00:00:00Z")
        val second = ms("2026-10-25T01:30:00Z")
        val result = draft.copy(time = "02:30", preservedMomentMs = second)
            .withCurrentDate(autumn, "Europe/Rome").getOrThrow()
        assertEquals(second, saved(result, autumn).timeOverrideMs)
    }

    @Test fun currentDateDoesNotRequireAValidPreviousDateButStillValidatesTheSelectedTime() {
        assertEquals("2026-10-16", draft.copy(date = "-").withCurrentDate(now, "Europe/Rome").getOrThrow().date)
        assertTrue(draft.copy(time = "25:00").withCurrentDate(now, "Europe/Rome").isFailure)
        assertTrue(draft.copy(zone = "invalid").withCurrentDate(now, "Europe/Rome").isFailure)
    }

    @Test fun repeatedCurrentDateActionsAreSemanticallyUnchanged() {
        val first = draft.withCurrentDate(now, "Europe/Rome").getOrThrow()
        val again = first.withCurrentDate(now + 30_000, "Europe/Rome").getOrThrow()
        assertTrue(spaceCompassObserverSectionChanges(first, again).isEmpty())
        assertEquals(saved(first), saved(again))
    }
}

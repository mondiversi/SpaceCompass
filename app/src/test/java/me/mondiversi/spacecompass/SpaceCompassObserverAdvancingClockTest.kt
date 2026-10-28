package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverAdvancingClockTest {
    private val rome = ZoneId.of("Europe/Rome")
    private fun ms(value: String) = Instant.parse(value).toEpochMilli()
    private val now = ms("2026-10-15T18:23:45Z")
    private val draft = SpaceCompassObserverDraft(false, true, "", "", "", "Europe/Rome",
        "2026-10-15", "21:23", simulateAltitude = false, simulateZone = true, simulateDate = false)
    private fun running(date: Boolean = false) = draft.copy(simulateDate = date, advanceTime = true)
        .resolve(null, now, rome.id).getOrThrow()!!

    @Test fun advancingClockContinuesByElapsedRealTimeInsteadOfHoldingTheChosenHour() {
        val plan = running()
        assertTrue(plan.advancesClock)
        val first = ms("2026-10-15T19:23:00Z")
        assertEquals(first, plan.observationTimeMs(now, rome))
        assertEquals(first + 23_456, plan.observationTimeMs(now + 23_456, rome))
        assertEquals(first + 4 * 86_400_000L, plan.observationTimeMs(now + 4 * 86_400_000L, rome))
    }

    @Test fun fiveHourOffsetSurvivesReloadMidnightAndDeviceRestartWithoutAProcessClock() {
        val initial = running().copy(clockOffsetMs = 5 * 3_600_000L)
        val restored = decodeSpaceCompassObserverPlan(initial.encode())!!
        assertEquals(initial, restored)
        val later = now + 12 * 86_400_000L + 12_345
        assertEquals(later + 5 * 3_600_000L, restored.observationTimeMs(later, rome))
        assertEquals(later + 5 * 3_600_000L, restored.observationTimeMs(later, ZoneId.of("America/New_York")))
    }

    @Test fun customDateAndTimeAdvanceAcrossDaysAndDoNotReportAFixedOverride() {
        val plan = draft.copy(date = "2027-04-06", simulateDate = true, advanceTime = true)
            .resolve(null, now, rome.id).getOrThrow()!!
        assertNull(plan.timeOverrideMs)
        assertEquals(ms("2027-04-08T19:23:00Z"), plan.observationTimeMs(now + 2 * 86_400_000L, rome))
    }

    @Test fun clockOffsetIsEstablishedOnceAndUnrelatedEditsCannotReanchorIt() {
        val plan = running()
        val editing = draft.copy(advanceTime = true, clockOffsetMs = plan.clockOffsetMs, altitude = "42")
        val later = now + 360_000
        val saved = editing.resolve(plan, later, rome.id).getOrThrow()!!
        assertEquals(plan.clockOffsetMs, saved.clockOffsetMs)
        assertEquals(plan.observationTimeMs(later, rome), saved.observationTimeMs(later, rome))
    }

    @Test fun changingTheChosenTimeExplicitlyEstablishesANewOffset() {
        val old = running()
        val changed = draft.copy(time = "22:23", advanceTime = true, clockOffsetMs = null)
            .resolve(old, now + 60_000, rome.id).getOrThrow()!!
        assertEquals(ms("2026-10-15T20:23:00Z"), changed.observationTimeMs(now + 60_000, rome))
        assertNotEquals(old.clockOffsetMs, changed.clockOffsetMs)
    }

    @Test fun stoppingAdvanceFreezesTheProgressedMomentInsteadOfRewindingToTheOriginalHour() {
        for (date in listOf(false, true)) {
            val plan = running(date)
            val editing = draft.copy(simulateDate = date, advanceTime = true, clockOffsetMs = plan.clockOffsetMs)
            val later = now + 125_123
            val stopped = editing.withClockAdvance(false, plan, later, rome.id)
                .resolve(plan, later, rome.id).getOrThrow()!!
            assertFalse(stopped.advancesClock)
            assertEquals(plan.observationTimeMs(later, rome), stopped.observationTimeMs(later, rome))
            assertEquals(stopped.observationTimeMs(later, rome), stopped.observationTimeMs(later + 60_000, rome))
        }
    }

    @Test fun disablingTheTimeOverrideUsesTheNormalClockEvenIfAdvanceRemainsSaved() {
        val plan = running().copy(simulateTime = false)
        assertFalse(plan.advancesClock)
        assertEquals(now, plan.observationTimeMs(now, rome))
        assertEquals(plan, decodeSpaceCompassObserverPlan(plan.encode()))
    }

    @Test fun oldThirteenFieldPreferencesKeepTheirFixedClockAndNeverEnableAdvancing() {
        val old = running(true).copy(advanceTime = false, clockOffsetMs = null)
        val restored = decodeSpaceCompassObserverPlan(old.encode().split('|').take(13).joinToString("|"))!!
        assertFalse(restored.advanceTime)
        assertEquals(old, restored)
        assertEquals(old.timeMs, restored.observationTimeMs(now + 86_400_000, rome))
    }

    @Test fun invalidAdvancingPreferencesFallBackSafely() {
        val fields = running().encode().split('|')
        for (offset in listOf("", "NaN", Long.MAX_VALUE.toString(), Long.MIN_VALUE.toString())) {
            val bad = fields.toMutableList().apply { this[14] = offset }.joinToString("|")
            assertNull(decodeSpaceCompassObserverPlan(bad))
        }
        assertNull(decodeSpaceCompassObserverPlan(fields.toMutableList().apply { this[13] = "TRUE" }.joinToString("|")))
    }

    @Test fun currentTimeLinkUsesTheObserverZoneAndCreatesAnExactZeroOffsetWithoutACustomDate() {
        val editing = draft.copy(zone = "Asia/Kathmandu", advanceTime = true)
            .withCurrentTime(now, rome.id).getOrThrow()
        assertEquals("00:08", editing.time)
        val plan = editing.resolve(null, now, rome.id).getOrThrow()!!
        assertEquals(0L, plan.clockOffsetMs)
        assertEquals(now + 86_400_000, plan.observationTimeMs(now + 86_400_000, rome))
    }

    @Test fun currentTimeLinkPreservesACustomDateAndUsesItsActualDaylightSavingOffset() {
        val editing = draft.copy(date = "2027-01-15", simulateDate = true, advanceTime = true)
            .withCurrentTime(now, rome.id).getOrThrow()
        assertEquals("2027-01-15", editing.date)
        assertEquals("20:23", editing.time)
        val plan = editing.resolve(null, now, rome.id).getOrThrow()!!
        assertEquals(ms("2027-01-15T19:23:45Z"), plan.observationTimeMs(now, rome))
    }

    @Test fun reopeningTheEditorShowsTheProgressedClockWithoutChangingTheOffset() {
        val plan = running()
        val later = now + 12 * 3_600_000
        val shown = plan.editorMoment(later, rome)
        assertEquals(plan.observationTimeMs(later, rome), shown.toInstant().toEpochMilli())
        assertEquals("2026-10-16", shown.toLocalDate().toString())
        assertEquals(9, shown.hour)
        val request = plan.dataRequestTimeMs(now, rome)
        assertEquals(request, plan.dataRequestTimeMs(now + 60_000, rome))
    }

    @Test fun scenarioClockKeepsItsOffsetButScreensaverFollowsRealTimeAcrossDaylightSaving() {
        val plan = running().copy(clockOffsetMs = 5 * 3_600_000L)
        val before = ms("2026-10-25T00:30:00Z")
        assertEquals(3_600_000L, plan.observationTimeMs(before + 3_600_000L, rome) - plan.observationTimeMs(before, rome))
        val screensaver = SpaceCompassScreensaverSolarObserver(45.0, 9.0, 100.0)
        assertEquals(calculateSpaceCompassSunPosition(before, 45.0, 9.0, 100.0).elevationDegrees,
            screensaver.elevation(before), 0.0)
    }
}

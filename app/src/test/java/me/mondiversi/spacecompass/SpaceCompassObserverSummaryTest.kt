package me.mondiversi.spacecompass

import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverSummaryTest {
    private val now = Instant.parse("2027-04-06T20:00:00Z").toEpochMilli()
    private val draft = SpaceCompassObserverDraft(true, false, "28.35", "-14.1", "900", "Atlantic/Canary",
        "2027-04-06", "21:00", simulateAltitude = false, simulateZone = false,
        automaticAltitudeMeters = 102.5, automaticZoneId = "Atlantic/Canary", simulateDate = false)
    private val metadata = SpaceCompassObserverMetadata(68.0, "Europe/Rome")
    private fun plan(value: SpaceCompassObserverDraft) = value.resolve(null, now, "Europe/Rome").getOrThrow()!!

    @Test fun badgeAndSummaryHaveExactlyTheSameSectionsForEverySwitchCombination() {
        val base = plan(draft)
        assertEquals(0, (null as SpaceCompassObserverPlan?).activeSectionCount)
        for (mask in 0..31) {
            val selected = base.copy(simulatePosition = mask and 1 != 0, simulateAltitude = mask and 2 != 0,
                simulateZone = mask and 4 != 0, simulateDate = mask and 8 != 0, simulateTime = mask and 16 != 0)
            assertEquals(Integer.bitCount(mask), selected.activeSectionCount)
            assertEquals(selected.activeSectionCount, selected.activeSections.toSet().size)
            assertEquals(selected.activeSections.sortedBy { it.ordinal }, selected.activeSections)
            if (mask != 0) assertEquals(selected.activeSections, decodeSpaceCompassObserverPlan(selected.encode())!!.activeSections)
        }
    }

    @Test fun dateAndTimeCountSeparatelyAndClockAdvanceIsNotAnExtraIsland() {
        val selected = plan(draft.copy(simulateDate = true, simulateTime = true,
            advanceTime = true, clockOffsetMs = 1L))
        assertEquals(3, selected.activeSectionCount)
        assertEquals(listOf(SpaceCompassObserverSection.POSITION, SpaceCompassObserverSection.DATE,
            SpaceCompassObserverSection.TIME), selected.activeSections)
        assertEquals(2, selected.copy(simulatePosition = false).activeSectionCount)
    }

    @Test fun revealingSectionsSkipsTheNewSummaryWithoutChangingSectionOrder() {
        assertEquals(listOf(1, 2, 3, 4, 5), SpaceCompassObserverSection.entries.map { it.gridIndex })
    }

    @Test fun metadataActionActivatesBothFieldsAndLeavesDateTimeOff() {
        val changed = draft.withPositionMetadata(metadata, SpaceCompassNumericFormat.INTERNATIONAL, "Europe/Rome")
        assertTrue(changed.simulatePosition); assertTrue(changed.simulateAltitude); assertTrue(changed.simulateZone)
        assertFalse(changed.simulateDate); assertFalse(changed.simulateTime)
        val selected = plan(changed)
        assertEquals(3, selected.activeSectionCount)
        assertEquals(68.0, selected.altitudeMeters, 0.0)
        assertEquals("Europe/Rome", selected.zoneId)
        assertEquals(68.0, selected.automaticAltitudeMeters!!, 0.0)
        assertEquals("Europe/Rome", selected.automaticZoneId)
        assertEquals(draft.latitude, changed.latitude); assertEquals(draft.longitude, changed.longitude)
    }

    @Test fun metadataActionPreservesFixedObservationInstantWhenConvertingToAnotherZone() {
        val before = draft.copy(simulateDate = true, simulateTime = true)
        val changed = before.withPositionMetadata(metadata, SpaceCompassNumericFormat.INTERNATIONAL, "Europe/Rome")
        assertEquals(plan(before).timeOverrideMs, plan(changed).timeOverrideMs)
        assertEquals("22:00", changed.time)
        assertEquals(before.date, changed.date)
    }

    @Test fun partialCalendarChoicesAndTheirStoredClockAdvancementRemainIndependent() {
        for ((date, time) in listOf(true to false, false to true, false to false)) {
            val before = draft.copy(simulateDate = date, simulateTime = time, advanceTime = true, clockOffsetMs = 12345L)
            val changed = before.withPositionMetadata(metadata, SpaceCompassNumericFormat.INTERNATIONAL, "Europe/Rome")
            assertEquals(date, changed.simulateDate); assertEquals(time, changed.simulateTime)
            assertEquals(before.date, changed.date); assertEquals(before.time, changed.time)
            assertEquals(before.advanceTime, changed.advanceTime); assertEquals(before.clockOffsetMs, changed.clockOffsetMs)
        }
    }

    @Test fun changingMetadataNeverReanchorsAnAdvancingClock() {
        val before = draft.copy(simulateDate = true, simulateTime = true, advanceTime = true, clockOffsetMs = 18_000_000L)
        val changed = before.withPositionMetadata(metadata, SpaceCompassNumericFormat.INTERNATIONAL, "Europe/Rome")
        assertEquals(plan(before).observationTimeMs(now + 100_000), plan(changed).observationTimeMs(now + 100_000))
    }

    @Test fun altitudeUsesSelectedFeetForEditingAndMetersForStoredCalculations() {
        val changed = draft.copy(feet = true).withPositionMetadata(metadata, SpaceCompassNumericFormat.EUROPEAN, "Europe/Rome")
        assertTrue(changed.altitude.contains(','))
        assertEquals(68.0, plan(changed).altitudeMeters, .002)
    }

    @Test fun invalidProviderValuesCannotPartiallyActivateEitherField() {
        for (bad in listOf(metadata.copy(altitude = Double.NaN), metadata.copy(altitude = 21_000.0),
            metadata.copy(zoneId = "invalid"))) {
            assertTrue(runCatching { draft.withPositionMetadata(bad, SpaceCompassNumericFormat.INTERNATIONAL, "Europe/Rome") }.isFailure)
            assertFalse(draft.simulateAltitude); assertFalse(draft.simulateZone)
        }
        assertTrue(runCatching { draft.copy(simulatePosition = false).withPositionMetadata(metadata,
            SpaceCompassNumericFormat.INTERNATIONAL, "Europe/Rome") }.isFailure)
    }

    @Test fun applyingMetadataToExistingOverridesProducesModificationRatherThanActivation() {
        val before = draft.copy(simulateAltitude = true, simulateZone = true)
        val changed = before.withPositionMetadata(metadata, SpaceCompassNumericFormat.INTERNATIONAL, "Europe/Rome")
        val notices = spaceCompassObserverSectionChanges(before, changed)
        assertEquals(listOf(SpaceCompassObserverSection.ALTITUDE, SpaceCompassObserverSection.ZONE), notices.map { it.section })
        assertTrue(notices.all { it.change == SpaceCompassObserverChange.MODIFIED })
    }

    @Test fun historicalDatesAreReadableInSummaryWithoutChangingUnknownTimestampDefaultsElsewhere() {
        val old = Instant.parse("1900-01-02T03:04:00Z").toEpochMilli()
        val zone = TimeZone.getTimeZone("UTC")
        assertEquals("1900-01-02", formatSpaceCompassDateOnly(old, SpaceCompassDateFormat.INTERNATIONAL,
            Locale.US, zone, SpaceCompassNumericFormat.INTERNATIONAL, Locale.US, allowHistorical = true))
        assertEquals("03:04", formatSpaceCompassTimeOnly(old, SpaceCompassTimeFormat.H24,
            Locale.US, zone, SpaceCompassNumericFormat.INTERNATIONAL, Locale.US, includeSeconds = false, allowHistorical = true))
        assertEquals("—", formatSpaceCompassDateOnly(old, SpaceCompassDateFormat.INTERNATIONAL))
        assertEquals("—", formatSpaceCompassTimeOnly(old, SpaceCompassTimeFormat.H24))
    }

    @Test fun epochItselfIsAValidSummaryDateAndRespectsDateAndTimePreferences() {
        val zone = TimeZone.getTimeZone("UTC")
        assertEquals("01/01/1970", formatSpaceCompassDateOnly(0L, SpaceCompassDateFormat.EUROPEAN,
            Locale.US, zone, SpaceCompassNumericFormat.INTERNATIONAL, Locale.US, allowHistorical = true))
        assertEquals("12:00 AM", formatSpaceCompassTimeOnly(0L, SpaceCompassTimeFormat.H12,
            Locale.US, zone, SpaceCompassNumericFormat.INTERNATIONAL, Locale.US, includeSeconds = false, allowHistorical = true))
    }
}

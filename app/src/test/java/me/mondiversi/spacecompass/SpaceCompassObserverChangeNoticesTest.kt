package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverChangeNoticesTest {
    private val off = SpaceCompassObserverDraft(false, false, "45", "9", "120", "Europe/Rome",
        "2026-10-15", "12:15", simulateAltitude = false, simulateZone = false, simulateDate = false)
    private val all = off.withAllOverrides(true)
    private fun expect(section: SpaceCompassObserverSection, change: SpaceCompassObserverChange,
        before: SpaceCompassObserverDraft, after: SpaceCompassObserverDraft) =
        assertEquals(listOf(SpaceCompassObserverSectionChange(section, change)), spaceCompassObserverSectionChanges(before, after))

    @Test fun everyIndependentSectionReportsActivation() {
        val values = listOf(off.copy(simulatePosition = true), off.copy(simulateAltitude = true),
            off.copy(simulateZone = true), off.copy(simulateDate = true), off.copy(simulateTime = true))
        SpaceCompassObserverSection.entries.zip(values).forEach { (section, next) ->
            expect(section, SpaceCompassObserverChange.ACTIVATED, off, next)
        }
    }

    @Test fun everyIndependentSectionReportsDeactivation() {
        val values = listOf(all.copy(simulatePosition = false), all.copy(simulateAltitude = false),
            all.copy(simulateZone = false), all.copy(simulateDate = false), all.copy(simulateTime = false))
        SpaceCompassObserverSection.entries.zip(values).forEach { (section, next) ->
            expect(section, SpaceCompassObserverChange.DEACTIVATED, all, next)
        }
    }

    @Test fun everyIndependentSectionReportsOnlyItsOwnValueChanges() {
        val values = listOf(all.copy(longitude = "10"), all.copy(altitude = "130"),
            all.copy(zone = "Atlantic/Canary"), all.copy(date = "2026-10-16"), all.copy(time = "13:15"))
        SpaceCompassObserverSection.entries.zip(values).forEach { (section, next) ->
            expect(section, SpaceCompassObserverChange.MODIFIED, all, next)
        }
    }

    @Test fun latitudeChangesAlsoCountAsPositionChanges() {
        expect(SpaceCompassObserverSection.POSITION, SpaceCompassObserverChange.MODIFIED,
            all, all.copy(latitude = "46"))
    }

    @Test fun advancementCheckboxIsPartOfTheTimeSection() {
        expect(SpaceCompassObserverSection.TIME, SpaceCompassObserverChange.MODIFIED,
            all, all.copy(advanceTime = true))
        assertTrue(spaceCompassObserverSectionChanges(off, off.copy(advanceTime = true)).isEmpty())
    }

    @Test fun HiddenValuesAndAutomaticMetadataDoNotAnnounceManualOverrides() {
        val hidden = off.copy(latitude = "47", altitude = "456", zone = "Asia/Tokyo", date = "2027-02-03", time = "13:00")
        assertTrue(spaceCompassObserverSectionChanges(off, hidden).isEmpty())
        assertTrue(spaceCompassObserverSectionChanges(all, all.copy(
            automaticAltitudeMeters = 999.0, automaticZoneId = "Asia/Tokyo")).isEmpty())
    }

    @Test fun equivalentNumericFormattingAndWhitespaceDoNotTriggerNotices() {
        assertTrue(spaceCompassObserverSectionChanges(all,
            all.copy(latitude = " 45.0 ", longitude = "9,0", altitude = "120.00", zone = " Europe/Rome ",
                date = " 2026-10-15 ", time = " 12:15 ")).isEmpty())
        assertTrue(spaceCompassObserverSectionChanges(all.copy(latitude = "0"), all.copy(latitude = "-0.0")).isEmpty())
    }

    @Test fun offsetAndPrecisionBookkeepingCannotAnnounceANewClockValue() {
        assertTrue(spaceCompassObserverSectionChanges(all, all.copy(
            clockOffsetMs = 12345L, preservedMomentMs = 54321L)).isEmpty())
        assertTrue(spaceCompassObserverSectionChanges(all, all).isEmpty())
    }

    @Test fun bulkChangesKeepEverySectionOnceInStableDisplayOrder() {
        val on = spaceCompassObserverSectionChanges(off, all)
        val disabled = spaceCompassObserverSectionChanges(all, off)
        assertEquals(SpaceCompassObserverSection.entries.toList(), on.map { it.section })
        assertTrue(on.all { it.change == SpaceCompassObserverChange.ACTIVATED })
        assertTrue(disabled.all { it.change == SpaceCompassObserverChange.DEACTIVATED })
        val mixed = spaceCompassObserverSectionChanges(all, all.copy(simulatePosition = false, altitude = "121"))
        assertEquals(listOf(SpaceCompassObserverChange.DEACTIVATED, SpaceCompassObserverChange.MODIFIED), mixed.map { it.change })
    }

    @Test fun allFifteenMessagesHaveDistinctResources() {
        val resources = SpaceCompassObserverSection.entries.flatMap { section ->
            SpaceCompassObserverChange.entries.map { change -> SpaceCompassObserverSectionChange(section, change).messageId }
        }
        assertEquals(15, resources.toSet().size)
        assertTrue(resources.all { it != 0 })
    }
}

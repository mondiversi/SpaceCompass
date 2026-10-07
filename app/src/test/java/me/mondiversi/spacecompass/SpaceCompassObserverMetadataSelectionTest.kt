package me.mondiversi.spacecompass

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverMetadataSelectionTest {
    private val now = Instant.parse("2027-04-06T20:00:00Z").toEpochMilli()
    private val draft = SpaceCompassObserverDraft(true, true, "28.35", "-14.1", "900", "Atlantic/Canary",
        "2027-04-06", "21:00", simulateAltitude = true, simulateZone = true,
        automaticAltitudeMeters = 102.5, automaticZoneId = "Atlantic/Canary")
    private val metadata = SpaceCompassObserverMetadata(68.0, "Europe/Rome")

    @Test fun detectingOnlyHeightKeepsRemotePositionZoneMomentAndItsAutomaticMetadata() {
        val original = draft.resolve(null, now, "Europe/Rome").getOrThrow()!!
        val result = draft.withDetectedMetadata(SpaceCompassObserverMetadataField.ALTITUDE, metadata,
            SpaceCompassNumericFormat.EUROPEAN, "Europe/Rome").resolve(original, now, "Europe/Rome").getOrThrow()!!
        assertEquals(original.latitude, result.latitude, 0.0); assertEquals(original.longitude, result.longitude, 0.0)
        assertEquals(68.0, result.altitudeMeters, 0.0)
        assertEquals(original.zoneId, result.zoneId); assertEquals(original.timeMs, result.timeMs)
        assertEquals(original.automaticAltitudeMeters, result.automaticAltitudeMeters)
        assertEquals(original.automaticZoneId, result.automaticZoneId)
    }

    @Test fun detectingOnlyZonePreservesAbsoluteMomentManualHeightAndRemoteCoordinates() {
        val original = draft.resolve(null, now, "Europe/Rome").getOrThrow()!!
        val changed = draft.withDetectedMetadata(SpaceCompassObserverMetadataField.ZONE, metadata,
            SpaceCompassNumericFormat.INTERNATIONAL, "Europe/Rome")
        assertEquals("22:00", changed.time)
        val result = changed.resolve(original, now, "Europe/Rome").getOrThrow()!!
        assertEquals(now, result.timeMs); assertEquals("Europe/Rome", result.zoneId)
        assertEquals(original.latitude, result.latitude, 0.0); assertEquals(original.longitude, result.longitude, 0.0)
        assertEquals(original.altitudeMeters, result.altitudeMeters, 0.0)
        assertEquals(original.automaticAltitudeMeters, result.automaticAltitudeMeters)
        assertEquals(original.automaticZoneId, result.automaticZoneId)
    }

    @Test fun detectingZoneKeepsTheSecondOccurrenceOfADaylightSavingOverlap() {
        val second = Instant.parse("2026-11-01T06:30:00Z").toEpochMilli()
        val repeated = draft.copy(zone = "America/New_York", date = "2026-11-01", time = "01:30",
            preservedMomentMs = second)
        val changed = repeated.withDetectedMetadata(SpaceCompassObserverMetadataField.ZONE, metadata,
            SpaceCompassNumericFormat.INTERNATIONAL, "Europe/Rome")
        val result = changed.resolve(null, now, "Europe/Rome").getOrThrow()!!
        assertEquals(second, result.timeMs); assertEquals("07:30", changed.time)
    }

    @Test fun detectingHeightInFeetStoresMetersAndNeverFreezesALiveClock() {
        val changed = draft.copy(feet = true, simulateTime = false).withDetectedMetadata(
            SpaceCompassObserverMetadataField.ALTITUDE, metadata, SpaceCompassNumericFormat.EUROPEAN, "Europe/Rome")
        val result = changed.resolve(null, now, "Europe/Rome").getOrThrow()!!
        assertEquals(68.0, result.altitudeMeters, .002)
        assertNull(result.timeOverrideMs)
        assertEquals("Atlantic/Canary", result.zoneId)
    }
}

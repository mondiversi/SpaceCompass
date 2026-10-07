package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverMetadataOverridesTest {
    private val now = Instant.parse("2026-10-06T08:00:00Z").toEpochMilli()
    private val selected = Instant.parse("2027-04-06T20:00:00Z").toEpochMilli()
    private val device = SpaceCompassDevicePlace(45.1, 9.0, 118.0)
    private val plan = spaceCompassObserverPlan(28.35, -14.1, 900.0, selected, "America/New_York",
        automaticAltitudeMeters = 102.5, automaticZoneId = "Atlantic/Canary")!!

    @Test fun legacySevenFieldPlansPreserveTheirAltitudeZoneAndSwitches() {
        val restored = decodeSpaceCompassObserverPlan("28.35|-14.1|900.0|$selected|Atlantic/Canary|true|false")!!
        assertTrue(restored.simulateAltitude); assertTrue(restored.simulateZone)
        assertNull(restored.automaticAltitudeMeters); assertNull(restored.automaticZoneId)
        assertEquals(900.0, restored.resolvePlace(device)!!.altitude!!, 0.0)
        assertEquals(ZoneId.of("Atlantic/Canary"), restored.observationZone(ZoneId.of("Europe/Rome")))
        assertNull(restored.timeOverrideMs)
    }

    @Test fun allIndependentModesAndBothManualAutomaticValuesSurviveEncoding() {
        for (mask in 1..15) {
            val input = plan.copy(simulatePosition = (mask and 1) != 0, simulateTime = (mask and 2) != 0,
                simulateAltitude = (mask and 4) != 0, simulateZone = (mask and 8) != 0)
            assertEquals(input, decodeSpaceCompassObserverPlan(input.encode()))
        }
    }

    @Test fun disablingManualMetadataUsesTheSelectedPlaceWithoutDiscardingManualValues() {
        val automatic = plan.copy(simulateAltitude = false, simulateZone = false)
        assertEquals(SpaceCompassDevicePlace(28.35, -14.1, 102.5), automatic.resolvePlace(device))
        assertEquals(ZoneId.of("Atlantic/Canary"), automatic.observationZone(ZoneId.of("Europe/Rome")))
        assertEquals(selected, automatic.timeOverrideMs)
        val manual = decodeSpaceCompassObserverPlan(automatic.encode())!!.copy(simulateAltitude = true, simulateZone = true)
        assertEquals(900.0, manual.resolvePlace(device)!!.altitude!!, 0.0)
        assertEquals(ZoneId.of("America/New_York"), manual.observationZone(ZoneId.of("Europe/Rome")))
    }

    @Test fun altitudeOnlyKeepsRealCoordinatesAndRequiresARealFix() {
        val heightOnly = plan.copy(simulatePosition = false, simulateTime = false, simulateZone = false)
        assertEquals(device.copy(altitude = 900.0), heightOnly.resolvePlace(device))
        assertNull(heightOnly.resolvePlace(null))
        assertNull(heightOnly.timeOverrideMs)
        assertEquals(ZoneId.of("Europe/Rome"), heightOnly.observationZone(ZoneId.of("Europe/Rome")))
    }

    @Test fun zoneOnlyChangesTheClockPresentationAndKeepsGpsAndTheLiveInstant() {
        val zoneOnly = plan.copy(simulatePosition = false, simulateTime = false, simulateAltitude = false)
        assertSame(device, zoneOnly.resolvePlace(device))
        val instant = zoneOnly.timeOverrideMs ?: now
        val clock = Instant.ofEpochMilli(instant).atZone(zoneOnly.observationZone(ZoneId.of("Europe/Rome")))
        assertEquals(4, clock.hour)
        assertEquals(now, clock.toInstant().toEpochMilli())
        val customTime = zoneOnly.copy(simulateTime = true)
        assertEquals(selected, customTime.timeOverrideMs)
    }

    @Test fun incompleteDisabledFieldsDoNotBlockAnAltitudeOrZoneOnlyDraft() {
        val draft = SpaceCompassObserverDraft(false, false, "invalid", "", "328,0839895013123", "bad",
            "invalid", "", feet = true, simulateAltitude = true, simulateZone = false,
            automaticAltitudeMeters = null, automaticZoneId = null)
        val height = draft.resolve(plan, now, "Europe/Rome").getOrThrow()!!
        assertEquals(100.0, height.resolvePlace(device)!!.altitude!!, 1e-9)
        val zone = draft.copy(simulateAltitude = false, simulateZone = true, zone = "Asia/Kathmandu", altitude = "")
            .resolve(plan, now, "Europe/Rome").getOrThrow()!!
        assertSame(device, zone.resolvePlace(device))
        assertEquals(ZoneId.of("Asia/Kathmandu"), zone.observationZone(ZoneId.of("Europe/Rome")))
        assertNull(draft.copy(simulateAltitude = false).resolve(plan, now, "Europe/Rome").getOrThrow())
    }

    @Test fun missingMetadataCannotActivateAutomaticValuesForAnUnresolvedNewPlace() {
        val draft = SpaceCompassObserverDraft(true, false, "40.71", "-74.0", "900", "America/New_York",
            "", "", simulateAltitude = false, simulateZone = false,
            automaticAltitudeMeters = null, automaticZoneId = null)
        assertTrue(draft.resolve(plan, now, "Europe/Rome").isFailure)
        val manual = draft.copy(simulateAltitude = true, simulateZone = true)
            .resolve(plan, now, "Europe/Rome").getOrThrow()!!
        assertEquals(SpaceCompassDevicePlace(40.71, -74.0, 900.0), manual.resolvePlace(device))
        assertEquals(manual, decodeSpaceCompassObserverPlan(manual.encode()))
        assertEquals(102.5, plan.automaticAltitudeMeters!!, 0.0)
    }

    @Test fun dateInterpretationUsesTheEnabledZoneOverrideOrTheMatchingAutomaticZone() {
        val draft = SpaceCompassObserverDraft(true, true, "28.35", "-14.1", "900", "America/New_York",
            "2027-04-06", "21:00", simulateAltitude = false, simulateZone = false,
            automaticAltitudeMeters = 102.5, automaticZoneId = "Atlantic/Canary")
        assertEquals(selected, draft.resolve(plan, now, "Europe/Rome").getOrThrow()!!.timeMs)
        assertEquals(Instant.parse("2027-04-07T01:00:00Z").toEpochMilli(),
            draft.copy(simulateZone = true).resolve(plan, now, "Europe/Rome").getOrThrow()!!.timeMs)
    }

    @Test fun changingZonePreservesTheSecondDstOccurrenceUntilDateOrTimeIsEdited() {
        val second = Instant.parse("2026-11-01T06:30:00Z").toEpochMilli()
        val draft = SpaceCompassObserverDraft(false, true, "", "", "", "America/New_York",
            "2026-11-01", "01:30", simulateAltitude = false, simulateZone = true,
            preservedMomentMs = second)
        assertEquals(second, draft.resolve(plan, now, "Europe/Rome").getOrThrow()!!.timeMs)
        assertEquals(Instant.parse("2026-11-01T05:30:00Z").toEpochMilli(),
            draft.copy(preservedMomentMs = null).resolve(plan, now, "Europe/Rome").getOrThrow()!!.timeMs)
        assertEquals(Instant.parse("2026-11-01T05:31:00Z").toEpochMilli(),
            draft.copy(time = "01:31").resolve(plan, now, "Europe/Rome").getOrThrow()!!.timeMs)
    }
}

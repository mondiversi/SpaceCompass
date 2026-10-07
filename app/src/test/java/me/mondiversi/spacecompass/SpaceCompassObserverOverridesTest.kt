package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverOverridesTest {
    private val selected = Instant.parse("2027-04-06T20:00:00Z").toEpochMilli()
    private val now = Instant.parse("2026-10-06T06:00:00Z").toEpochMilli()
    private val previous = spaceCompassObserverPlan(28.35, -14.1, 102.5, selected, "Atlantic/Canary")!!

    @Test fun existingFiveFieldPreferencesKeepBothOverridesAndExactValues() {
        val restored = decodeSpaceCompassObserverPlan("28.35|-14.1|102.5|$selected|Atlantic/Canary")!!
        assertEquals(previous.copy(automaticAltitudeMeters = null, automaticZoneId = null), restored)
        assertTrue(restored.simulatePosition)
        assertTrue(restored.simulateTime)
    }

    @Test fun eachIndependentModeSurvivesEncodingAndReload() {
        for ((position, time) in listOf(true to false, false to true, true to true)) {
            val plan = previous.copy(simulatePosition = position, simulateTime = time,
                simulateAltitude = position, simulateZone = position)
            assertEquals(plan, decodeSpaceCompassObserverPlan(plan.encode()))
        }
    }

    @Test fun corruptSwitchesAndInactiveCustomPlansFallBackSafely() {
        for (flags in listOf("true|false|extra", "TRUE|false", "1|true", "true|", "false|false"))
            assertNull(decodeSpaceCompassObserverPlan("28.35|-14.1|102.5|$selected|Atlantic/Canary|$flags"))
        assertNull(spaceCompassObserverPlan(0.0, 0.0, 0.0, now, "UTC", false, false))
    }

    @Test fun positionOnlyUsesTheCurrentAbsoluteInstantAndKeepsAdvancing() {
        val plan = previous.copy(simulateTime = false)
        assertEquals(SpaceCompassDevicePlace(28.35, -14.1, 102.5), plan.position)
        assertNull(plan.timeOverrideMs)
        assertEquals(now, plan.timeOverrideMs ?: now)
        assertEquals(now + 60_000, plan.timeOverrideMs ?: (now + 60_000))
        assertEquals(ZoneId.of("Atlantic/Canary"), plan.observationZone(ZoneId.of("Europe/Rome")))
    }

    @Test fun dateOnlyLeavesGpsUntouchedAndUsesTheDeviceZone() {
        val plan = previous.copy(simulatePosition = false, simulateAltitude = false, simulateZone = false)
        assertNull(plan.position)
        assertEquals(selected, plan.timeOverrideMs)
        assertEquals(ZoneId.of("Europe/Rome"), plan.observationZone(ZoneId.of("Europe/Rome")))
        assertEquals(ZoneId.of("Asia/Kathmandu"), plan.observationZone(ZoneId.of("Asia/Kathmandu")))
    }

    @Test fun bothEnabledRetainTheOriginalFixedPlaceAndMoment() {
        assertEquals(selected, previous.timeOverrideMs)
        assertEquals(SpaceCompassDevicePlace(28.35, -14.1, 102.5), previous.position)
        assertEquals(ZoneId.of("Atlantic/Canary"), previous.observationZone(ZoneId.of("Europe/Rome")))
    }

    @Test fun positionOnlyIgnoresAnInvalidHiddenDateAndPreservesItsSavedDraft() {
        val plan = spaceCompassObserverDraftPlan(true, false, 40.71, -74.0, 12.0,
            null, "America/New_York", previous, now, "Europe/Rome")!!
        assertFalse(plan.simulateTime)
        assertEquals(selected, plan.timeMs)
        assertEquals("America/New_York", plan.zoneId)
        assertNull(plan.timeOverrideMs)
    }

    @Test fun dateOnlyIgnoresInvalidHiddenPlaceFieldsAndRetainsTheLastCustomPlace() {
        val plan = spaceCompassObserverDraftPlan(false, true, Double.NaN, null, null,
            now, "Invalid/Hidden", previous, now, "Europe/Rome")!!
        assertFalse(plan.simulatePosition)
        assertEquals(previous.latitude, plan.latitude, 0.0)
        assertEquals(previous.longitude, plan.longitude, 0.0)
        assertEquals(previous.zoneId, plan.zoneId)
        assertEquals(now, plan.timeOverrideMs)
        assertNull(plan.position)
    }

    @Test fun enabledFieldsStillRequireValidInputsAndBothOffMeansLive() {
        assertNull(spaceCompassObserverDraftPlan(true, false, null, -74.0, 12.0,
            null, "America/New_York", previous, now, "Europe/Rome"))
        assertNull(spaceCompassObserverDraftPlan(false, true, null, null, null,
            null, "", previous, now, "Europe/Rome"))
        assertNull(spaceCompassObserverDraftPlan(false, false, null, null, null,
            null, "", previous, now, "Europe/Rome"))
        val firstDateOnly = spaceCompassObserverDraftPlan(false, true, null, null, null,
            now, "", null, now, "Europe/Rome")!!
        assertNull(firstDateOnly.position)
        assertEquals(ZoneId.of("Europe/Rome"), firstDateOnly.observationZone(ZoneId.of("Europe/Rome")))
    }

    @Test fun newYorkShowsTheSameInstantInItsOwnZoneAndItsActualNightSky() {
        val plan = spaceCompassObserverPlan(40.71, -74.0, 12.0, selected,
            "America/New_York", true, false)!!
        val instant = plan.timeOverrideMs ?: now
        val local = Instant.ofEpochMilli(instant).atZone(plan.observationZone(ZoneId.of("Europe/Rome")))
        assertEquals(2, local.hour)
        assertEquals(now, local.toInstant().toEpochMilli())
        assertTrue(calculateSpaceCompassSunPosition(instant, 40.71, -74.0, 12.0).elevationDegrees < 0.0)
        assertTrue(calculateSpaceCompassSunPosition(instant, 45.1, 9.0, 100.0).elevationDegrees > 0.0)
    }
}

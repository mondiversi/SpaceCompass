package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverOverrideSelectionTest {
    private val now = Instant.parse("2026-10-15T04:00:00Z").toEpochMilli()
    private val draft = SpaceCompassObserverDraft(false, false, "28.35", "-14.1", "120",
        "Atlantic/Canary", "2027-04-06", "12:15", simulateAltitude = false, simulateZone = false,
        simulateDate = false, advanceTime = true, clockOffsetMs = 18_000_000L)

    @Test fun everyPartialSelectionCanEnableAndDisableAllWithoutChangingAnyCustomValue() {
        for (mask in 0..31) {
            val input = draft.copy(simulatePosition = mask and 1 != 0, simulateAltitude = mask and 2 != 0,
                simulateZone = mask and 4 != 0, simulateDate = mask and 8 != 0, simulateTime = mask and 16 != 0)
            assertEquals(Integer.bitCount(mask), input.enabledOverrideCount)
            val enabled = input.withAllOverrides(true)
            val disabled = input.withAllOverrides(false)
            assertEquals(5, enabled.enabledOverrideCount)
            assertEquals(0, disabled.enabledOverrideCount)
            assertEquals(draft, disabled)
            assertEquals(enabled, disabled.withAllOverrides(true))
        }
    }

    @Test fun disableAllAlwaysReturnsToLiveEvenWithIncompleteFields() {
        val invalid = draft.copy(latitude = "-", longitude = "", date = "invalid", time = "25:", zone = "bad")
        assertNull(invalid.withAllOverrides(false).resolve(null, now, "Europe/Rome").getOrThrow())
        assertTrue(invalid.withAllOverrides(true).resolve(null, now, "Europe/Rome").isFailure)
    }

    @Test fun enablingEveryManualOverrideDoesNotRequireAutomaticMetadataOrNetworkLookups() {
        val input = draft.copy(automaticAltitudeMeters = null, automaticZoneId = null,
            advanceTime = false, clockOffsetMs = null).withAllOverrides(true)
        val plan = input.resolve(null, now, "Europe/Rome").getOrThrow()!!
        assertEquals(SpaceCompassDevicePlace(28.35, -14.1, 120.0), plan.position)
        assertEquals(ZoneId.of("Atlantic/Canary"), plan.observationZone(ZoneId.of("Europe/Rome")))
        assertEquals(spaceCompassObserverMoment("2027-04-06", "12:15", "Atlantic/Canary"), plan.timeOverrideMs)
        assertEquals(plan, decodeSpaceCompassObserverPlan(plan.encode()))
    }

    @Test fun bulkTogglingKeepsTheChosenClockProgressionAndItsPersistedOffset() {
        val enabled = draft.withAllOverrides(true)
        val original = enabled.resolve(null, now, "Europe/Rome").getOrThrow()!!
        assertTrue(original.advancesClock)
        val later = now + 86_400_000
        assertNull(enabled.withAllOverrides(false).resolve(original, later, "Europe/Rome").getOrThrow())
        val restarted = enabled.withAllOverrides(false).withAllOverrides(true)
            .resolve(original, later, "Europe/Rome").getOrThrow()!!
        assertEquals(original.clockOffsetMs, restarted.clockOffsetMs)
        assertEquals(later + 18_000_000, restarted.observationTimeMs(later))
    }
}

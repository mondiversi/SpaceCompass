package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPanoramaLabelModeTest {
    @Test fun defaultCurveNamesDoNotEnableHourlyPillsOrThumbnailNamesAndNoneStillHidesThem() {
        val mode = SpaceCompassPanoramaLabelMode.DEFAULT
        assertTrue(mode.showsOrbitName(activeObject = false, selectedObject = false))
        assertFalse(mode.showsObject(activeObject = false))
        val hour = SpaceCompassSunPathPoint(1_000L, SpaceCompassSunPosition(15.0, 25.0))
        assertFalse(mode.showsPathPoint(hour, activeObject = false))
        for (active in listOf(false, true)) for (selected in listOf(false, true)) {
            assertFalse(SpaceCompassPanoramaLabelMode.NONE.showsOrbitName(active, selected))
            assertEquals(selected, SpaceCompassPanoramaLabelMode.SELECTED.showsOrbitName(active, selected))
            assertEquals(active || selected, SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED.showsOrbitName(active, selected))
        }
    }

    @Test fun selectedOnlyCurveNamesBelongToTheExactLiveOrFrozenInstance() {
        val body = SpaceCompassCelestialBody.SUN
        val time = java.time.Instant.parse("2026-06-21T12:00:00Z").toEpochMilli()
        val c = SpaceCompassTemporalComparison.create(body, time, java.time.ZoneId.of("UTC"),
            SpaceCompassComparisonSource.HEIGHT, "MONTHLY:2026-06-21")
        val live = SpaceCompassPanoramaObject(body,"Sun",null)
        val frozen = SpaceCompassPanoramaObject(body,"Sun · 21/06/26 · 12:00",null,c)
        val snapshot = SpaceCompassPanoramaSnapshot(time,0.0,0.0,0.0,SpaceCompassSunSkyPhase.AFTERNOON,null,
            listOf(live,frozen),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList(),
            labelMode=SpaceCompassPanoramaLabelMode.SELECTED)
        val point = SpaceCompassSunPathPoint(time,SpaceCompassSunPosition(180.0,60.0))
        val pin = SpaceCompassPanoramaSelectedPoint(body,point,null,true,comparison=c)
        assertTrue(snapshot.showsOrbitName(frozen,listOf(pin)))
        assertFalse(snapshot.showsOrbitName(live,listOf(pin)))
        assertFalse(snapshot.showsOrbitName(frozen,emptyList()))
        assertTrue(snapshot.showsOrbitName(live,listOf(pin.copy(comparison=null))))
    }
    @Test fun freshAndUnknownSettingsUseKeyPointsWhileExistingChoicesArePreserved() {
        val mode = SpaceCompassPanoramaLabelMode.KEY_SELECTED
        assertEquals(mode, SpaceCompassPanoramaLabelMode.DEFAULT)
        assertEquals(mode, SpaceCompassPanoramaLabelMode.fromStored(null))
        assertEquals(mode, SpaceCompassPanoramaLabelMode.fromStored("unknown"))
        assertEquals(SpaceCompassPanoramaLabelMode.ALL, SpaceCompassPanoramaLabelMode.fromStored("all"))
        assertEquals(SpaceCompassPanoramaLabelMode.NONE, SpaceCompassPanoramaLabelMode.fromStored(null, false))
    }

    @Test fun keyModeIncludesEveryObjectsEventsAndCurrentPointButOmitsUnselectedHours() {
        val mode = SpaceCompassPanoramaLabelMode.KEY_SELECTED
        for (active in SpaceCompassCelestialBody.entries) for (body in SpaceCompassCelestialBody.entries) {
            val activeObject = active == body
            for (event in SpaceCompassSunPathEvent.entries) {
                val point = SpaceCompassSunPathPoint(1_000L, SpaceCompassSunPosition(15.0, -25.0), event)
                assertEquals(event != SpaceCompassSunPathEvent.HOUR, mode.showsPathPoint(point, activeObject))
            }
            assertTrue(mode.showsCurrentPoint(activeObject))
            assertTrue(mode.showsPoint(selected = true, activeObject = activeObject))
            assertFalse(mode.showsPoint(selected = false, activeObject = activeObject))
        }
        assertFalse(mode.showsAll)
        assertTrue(mode.showsSelected)
    }

    @Test fun anHourlyPointCoincidentWithAKeyEventKeepsItsLabel() {
        val mode = SpaceCompassPanoramaLabelMode.KEY_SELECTED
        for (event in SpaceCompassSunPathEvent.entries.filter { it != SpaceCompassSunPathEvent.HOUR }) {
            val merged = SpaceCompassSunPathPoint(1_000L, SpaceCompassSunPosition(15.0, 0.0),
                coincidentEvents = setOf(event))
            assertTrue(mode.showsPathPoint(merged))
        }
    }

    @Test fun existingModesRetainTheirEventAndCurrentRules() {
        val hour = SpaceCompassSunPathPoint(1_000L, SpaceCompassSunPosition(15.0, 25.0))
        val key = hour.copy(event = SpaceCompassSunPathEvent.CULMINATION)
        for (active in listOf(false, true)) {
            for (mode in SpaceCompassPanoramaLabelMode.entries.filter { it != SpaceCompassPanoramaLabelMode.KEY_SELECTED }) {
                assertEquals(mode.showsPoint(false, active), mode.showsPathPoint(hour, active))
                assertEquals(mode.showsPoint(false, active), mode.showsPathPoint(key, active))
                assertEquals(mode.showsPoint(false, active), mode.showsCurrentPoint(active))
            }
        }
    }
    @Test fun anUpgradeRetainsThePreviousSwitchChoice() {
        assertEquals(SpaceCompassPanoramaLabelMode.ALL, SpaceCompassPanoramaLabelMode.fromStored(null, true))
        assertEquals(SpaceCompassPanoramaLabelMode.NONE, SpaceCompassPanoramaLabelMode.fromStored(null, false))
        assertEquals(SpaceCompassPanoramaLabelMode.KEY_SELECTED, SpaceCompassPanoramaLabelMode.fromStored(null))
    }

    @Test fun aSavedChoiceOverridesTheLegacySwitchInBothDirections() {
        for (legacy in listOf(false, true)) for (mode in SpaceCompassPanoramaLabelMode.entries) {
            assertEquals(mode, SpaceCompassPanoramaLabelMode.fromStored(mode.key, legacy))
        }
    }

    @Test fun anUnknownStoredChoiceFallsBackWithoutInventingSelectedPoints() {
        assertEquals(SpaceCompassPanoramaLabelMode.NONE, SpaceCompassPanoramaLabelMode.fromStored("invalid", false))
        assertEquals(SpaceCompassPanoramaLabelMode.ALL, SpaceCompassPanoramaLabelMode.fromStored("invalid", true))
    }

    @Test fun objectAndSelectedModeIncludesTheActiveOrbitAndOnlyOtherPinnedPoints() {
        val mode = SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED
        assertTrue(mode.showsObject(activeObject = true))
        assertFalse(mode.showsObject(activeObject = false))
        assertTrue(mode.showsSelected)
        assertFalse(mode.showsAll)
        assertTrue(mode.showsPoint(true))
        assertFalse(mode.showsPoint(false))
        assertTrue(mode.showsPoint(false, activeObject = true))
        assertEquals(mode, SpaceCompassPanoramaLabelMode.fromStored(mode.key, false))
        assertTrue(SpaceCompassPanoramaLabelMode.ALL.showsObject(activeObject = false))
        assertFalse(SpaceCompassPanoramaLabelMode.SELECTED.showsObject(activeObject = true))
        assertFalse(SpaceCompassPanoramaLabelMode.NONE.showsObject(activeObject = true))
    }

    @Test fun theActiveBodyKeepsItsHourlyEventAndCurrentLabelsWithoutAnyPinnedPoints() {
        for (active in SpaceCompassCelestialBody.entries) for (body in SpaceCompassCelestialBody.entries) {
            val activeObject = body == active
            val mode = SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED
            assertEquals(activeObject, mode.showsObject(activeObject))
            assertEquals(activeObject, mode.showsPoint(selected = false, activeObject = activeObject))
            assertTrue(mode.showsPoint(selected = true, activeObject = activeObject))
        }
    }

    @Test fun aMissingActiveBodyDoesNotChooseTheFirstCheckedBodyOrHidePinnedPoints() {
        val mode = SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED
        assertFalse(mode.showsObject(activeObject = false))
        assertFalse(mode.showsPoint(selected = false, activeObject = false))
        assertTrue(mode.showsPoint(selected = true, activeObject = false))
    }

    @Test fun allSelectedAndNoneKeepTheirMeaningRegardlessOfTheActiveBody() {
        for (active in listOf(false, true)) for (selected in listOf(false, true)) {
            assertTrue(SpaceCompassPanoramaLabelMode.ALL.showsPoint(selected, active))
            assertEquals(selected, SpaceCompassPanoramaLabelMode.SELECTED.showsPoint(selected, active))
            assertFalse(SpaceCompassPanoramaLabelMode.NONE.showsPoint(selected, active))
        }
    }

    @Test fun onlyExplicitlySelectedPointsReceiveLabelsInSelectedMode() {
        assertTrue(SpaceCompassPanoramaLabelMode.ALL.showsPoint(false))
        assertTrue(SpaceCompassPanoramaLabelMode.ALL.showsPoint(true))
        assertFalse(SpaceCompassPanoramaLabelMode.SELECTED.showsAll)
        assertFalse(SpaceCompassPanoramaLabelMode.SELECTED.showsPoint(false))
        assertTrue(SpaceCompassPanoramaLabelMode.SELECTED.showsPoint(true))
        assertFalse(SpaceCompassPanoramaLabelMode.NONE.showsPoint(false))
        assertFalse(SpaceCompassPanoramaLabelMode.NONE.showsPoint(true))
        assertFalse(SpaceCompassPanoramaLabelMode.NONE.showsSelected)
    }
}

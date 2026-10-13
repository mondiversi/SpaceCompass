package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPanoramaLabelModeTest {
    @Test fun anUpgradeRetainsThePreviousSwitchChoice() {
        assertEquals(SpaceCompassPanoramaLabelMode.ALL, SpaceCompassPanoramaLabelMode.fromStored(null, true))
        assertEquals(SpaceCompassPanoramaLabelMode.NONE, SpaceCompassPanoramaLabelMode.fromStored(null, false))
        assertEquals(SpaceCompassPanoramaLabelMode.ALL, SpaceCompassPanoramaLabelMode.fromStored(null))
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

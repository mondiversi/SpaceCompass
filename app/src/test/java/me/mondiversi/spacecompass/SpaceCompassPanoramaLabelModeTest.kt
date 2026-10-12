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

    @Test fun objectAndSelectedModeKeepsNamesWithoutAddingUnselectedPointLabels() {
        val mode = SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED
        assertTrue(mode.showsObjects)
        assertTrue(mode.showsSelected)
        assertFalse(mode.showsAll)
        assertTrue(mode.showsPoint(true))
        assertFalse(mode.showsPoint(false))
        assertEquals(mode, SpaceCompassPanoramaLabelMode.fromStored(mode.key, false))
        assertTrue(SpaceCompassPanoramaLabelMode.ALL.showsObjects)
        assertFalse(SpaceCompassPanoramaLabelMode.SELECTED.showsObjects)
        assertFalse(SpaceCompassPanoramaLabelMode.NONE.showsObjects)
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

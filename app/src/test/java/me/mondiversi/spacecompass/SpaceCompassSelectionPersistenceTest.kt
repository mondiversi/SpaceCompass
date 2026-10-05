package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSelectionPersistenceTest {
    @Test fun savedSelectionAndActiveObjectAreRestoredByStableIds() {
        val selected = setOf("MARS", "TON_618", "MOON")
        val restored = restoreSpaceCompassCelestialSelection(selected, "TON_618")
        assertEquals(selected, restored.selected.map { it.name }.toSet())
        assertEquals(SpaceCompassCelestialBody.TON_618, restored.active)
        assertEquals(restored, restoreSpaceCompassCelestialSelection(restored.selected.map { it.name }.toSet(), restored.active?.name))
    }
    @Test fun emptySelectionIsDistinctFromFirstLaunchAndInvalidIdsAreSafe() {
        assertEquals(SpaceCompassCelestialSelection(), restoreSpaceCompassCelestialSelection(null, null))
        assertTrue(restoreSpaceCompassCelestialSelection(emptySet(), "SUN").selected.isEmpty())
        assertNull(restoreSpaceCompassCelestialSelection(emptySet(), "SUN").active)
        assertEquals(SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.MOON), SpaceCompassCelestialBody.MOON),
            restoreSpaceCompassCelestialSelection(setOf("REMOVED", "MOON"), "REMOVED"))
        assertNull(restoreSpaceCompassCelestialSelection(setOf("REMOVED"), "REMOVED").active)
    }
}

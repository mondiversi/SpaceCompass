package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassSettingsActionPaletteTest {
    @Test fun emphasizedGlyphsAreConfinedToActionsAndPreserveFilledLogoGeometry() {
        for (resource in listOf(R.drawable.ic_check, R.drawable.ic_update, R.drawable.ic_repository)) {
            assertEquals(resource, spaceCompassActionIconResource(resource, false))
        }
        assertEquals(R.drawable.ic_action_check, spaceCompassActionIconResource(R.drawable.ic_check, true))
        assertEquals(R.drawable.ic_action_update, spaceCompassActionIconResource(R.drawable.ic_update, true))
        assertEquals(R.drawable.ic_repository, spaceCompassActionIconResource(R.drawable.ic_repository, true))
        assertEquals(1.25f, SpaceCompassSettingsActionIconStrokeScale)
    }
}

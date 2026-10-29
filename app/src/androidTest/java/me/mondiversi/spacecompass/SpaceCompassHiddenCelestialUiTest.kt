package me.mondiversi.spacecompass

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class SpaceCompassHiddenCelestialUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lv426IsPublicAndUncheckingItKeepsTheCatalogEntry() {
        val selection = mutableStateOf(SpaceCompassCelestialSelection())
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalSpaceCompassPreferences provides null) {
                    SpaceCompassCelestialCatalogPage(1_791_288_000_000L, SpaceCompassCelestialRemoteData(),
                        selection.value.selected, Color.White, Color.Black, {}, {},
                        onSelect = { selection.value = selection.value.toggle(it) },
                        latitude = -33.0, longitude = 151.0)
                }
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("celestial-body-SUN").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("celestial-catalog-scroll").performScrollToNode(hasTestTag("celestial-body-LV_426"))
        compose.onNodeWithTag("catalog-check-LV_426").assertIsOff().performClick()
        compose.onNodeWithTag("catalog-check-LV_426").assertIsOn().performClick()
        compose.onNodeWithTag("catalog-check-LV_426").assertIsOff()
        compose.runOnIdle { assertFalse(SpaceCompassCelestialBody.LV_426 in selection.value.selected) }
    }
}

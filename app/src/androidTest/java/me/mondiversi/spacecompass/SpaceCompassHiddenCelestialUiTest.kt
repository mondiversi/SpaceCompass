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

    @Test fun aThreeSecondTitleHoldRevealsACheckedRowAndUncheckingItRemovesTheRow() {
        val selection = mutableStateOf(SpaceCompassCelestialSelection())
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalSpaceCompassPreferences provides null) {
                    SpaceCompassCelestialCatalogPage(1_791_288_000_000L, SpaceCompassCelestialRemoteData(),
                        selection.value.selected, Color.White, Color.Black, {}, {},
                        onSelect = { selection.value = selection.value.toggle(it) },
                        latitude = -33.0, longitude = 151.0,
                        onRevealHiddenObject = { selection.value = selection.value.revealHiddenObject() })
                }
            }
        }
        compose.onNodeWithTag("catalog-title").performTouchInput { down(center) }
        Thread.sleep(400)
        compose.onNodeWithTag("catalog-title").performTouchInput { up() }
        compose.runOnIdle { assertFalse(SpaceCompassCelestialBody.LV_426 in selection.value.selected) }
        compose.onNodeWithTag("catalog-title").performTouchInput { down(center) }
        compose.waitUntil(5_000) { SpaceCompassCelestialBody.LV_426 in selection.value.selected }
        compose.onNodeWithTag("catalog-title").performTouchInput { up() }
        compose.onNodeWithTag("celestial-catalog-scroll").performScrollToNode(hasTestTag("celestial-body-LV_426"))
        compose.onNodeWithTag("celestial-body-LV_426").assertIsOn().performClick()
        compose.onNodeWithTag("celestial-body-LV_426").assertDoesNotExist()
        compose.runOnIdle { assertFalse(SpaceCompassCelestialBody.LV_426 in selection.value.selected) }
    }
}

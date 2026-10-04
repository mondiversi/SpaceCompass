package me.mondiversi.planetcompass

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Disposable selector fixture only; no network, personal archive, preferences or location. */
class PlanetCompassStarlinkSelectionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun starlinkIsVisibleSelectableAndIndependentFromIss() {
        val selection = mutableStateOf(PlanetCompassCelestialSelection(setOf(PlanetCompassCelestialBody.ISS),PlanetCompassCelestialBody.ISS))
        compose.setContent { MaterialTheme {
            PlanetCompassCelestialSelector(selection.value.active!!,MaterialTheme.colorScheme.onSurface,
                MaterialTheme.colorScheme.background,selectedBodies = selection.value.selected,
                onToggleAll = { selection.value = selection.value.toggleAll() }) {
                selection.value = selection.value.toggle(it)
            }
        } }
        compose.onNodeWithTag("celestial-select").performClick()
        compose.onNodeWithTag("celestial-body-STARLINK_V3").performScrollTo().assertIsOff()
            .assert(hasText("Starlink V3 · 40083")).performClick().assertIsOn()
        compose.onNodeWithTag("celestial-menu").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(setOf(PlanetCompassCelestialBody.ISS,PlanetCompassCelestialBody.STARLINK_V3),selection.value.selected)
            assertEquals(PlanetCompassCelestialBody.STARLINK_V3,selection.value.step(1).active)
        }
    }
}

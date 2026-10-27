package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Verifies touch routing independently of catalog sorting, providers and saved preferences. */
class SpaceCompassCatalogInteractionUiTest {
    @get:Rule val compose = createComposeRule()
    private val checked = mutableStateOf(false)
    private var observations = 0
    private var toggles = 0

    private fun show() {
        compose.setContent { MaterialTheme {
            Surface(Modifier.width(320.dp)) {
                SpaceCompassCatalogObjectRow(SpaceCompassCelestialBody.HALLEY, "Halley", checked.value,
                    "1.5 Mkm", MaterialTheme.colorScheme.onSurface,
                    onToggle = { toggles++; checked.value = !checked.value },
                    onObserve = { observations++ })
            }
        } }
    }

    @Test fun onlyTheCheckboxChangesSelection() {
        show()
        compose.onNodeWithTag("celestial-body-HALLEY").assert(hasClickAction().not())
        compose.onNodeWithTag("catalog-check-HALLEY").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("catalog-check-HALLEY").performClick().assertIsOff()
        compose.runOnIdle { assertEquals(2, toggles); assertEquals(0, observations) }
    }

    @Test fun tappingTheUncheckedNameOpensObservationWithoutSelecting() {
        show()
        compose.onNodeWithTag("catalog-name-HALLEY", useUnmergedTree = true).performTouchInput { click(center) }
        compose.onNodeWithTag("catalog-check-HALLEY").assertIsOff()
        compose.runOnIdle { assertEquals(1, observations); assertEquals(0, toggles) }
    }

    @Test fun tappingTheThumbnailOpensObservationWithoutChangingSelection() {
        checked.value = true
        show()
        compose.onNodeWithTag("catalog-thumbnail-HALLEY", useUnmergedTree = true).performTouchInput { click(center) }
        compose.onNodeWithTag("catalog-check-HALLEY").assertIsOn()
        compose.runOnIdle { assertEquals(1, observations); assertEquals(0, toggles) }
    }

    @Test fun touchingTheDistanceHasNoAction() {
        show()
        compose.onNodeWithTag("catalog-value-HALLEY").assert(hasClickAction().not())
            .performTouchInput { click(center) }
        compose.onNodeWithTag("catalog-check-HALLEY").assertIsOff()
        compose.runOnIdle { assertEquals(0, observations); assertEquals(0, toggles) }
    }
}

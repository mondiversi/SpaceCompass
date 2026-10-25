package me.mondiversi.spacecompass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic input routing only: no device sensors, location, network or saved preferences. */
class SpaceCompassMainIslandInputTest {
    @get:Rule val compose = createComposeRule()

    @Test fun blankIslandSurfacesAbsorbSceneTapsWhileChildActionsAndGapsKeepTheirBehavior() {
        var sceneTaps = 0
        var previous = 0
        var next = 0
        var details = 0
        var info = 0
        val compact = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(340.dp, 480.dp).testTag("island-input-fixture")
                    .pointerInput(Unit) { detectTapGestures { sceneTaps++ } }) {
                    SpaceCompassSunFinderDataPanel(
                        listOf(SpaceCompassSunDataRow("Distance", "1 km", "Distance: 1 km", "fixture-distance")),
                        emptyList(), null, false, "", false, Color.Black, Color.Gray, Color.White,
                        Modifier.fillMaxWidth().align(Alignment.BottomCenter), compact.value,
                        bodyName = "Sun", bodyNavigation = SpaceCompassCelestialBodyNavigation("Previous", "Next",
                            { previous++ }, { next++ }), onInfo = { info++ },
                        onBodyNameClick = { details++ })
                }
            }
        }
        for (small in listOf(false, true)) {
            compose.runOnIdle { compact.value = small }
            for (tag in listOf("sun-finder-body-data", "sun-finder-environment-data")) {
                val surface = compose.onNodeWithTag(tag, useUnmergedTree = true)
                surface.assert(!hasClickAction()) // Do not introduce inert TalkBack buttons.
                val before = sceneTaps
                surface.performTouchInput { click(Offset(width / 2f, height - 2f)) }
                compose.runOnIdle { assertEquals(before, sceneTaps) }
            }
            // The compass drawing is passive, unlike the GPS/details link beside it.
            val before = sceneTaps
            compose.onNodeWithTag("sun-finder-compass", useUnmergedTree = true).performTouchInput { click(center) }
            compose.runOnIdle { assertEquals(before, sceneTaps) }
            compose.onNodeWithTag("celestial-body-previous", useUnmergedTree = true).performTouchInput { click(center) }
            compose.onNodeWithTag("celestial-body-next", useUnmergedTree = true).performTouchInput { click(center) }
            compose.onNodeWithTag("celestial-body-island-title", useUnmergedTree = true)
                .assertHasClickAction().performTouchInput { click(center) }
            compose.onNode(hasClickAction() and hasAnyDescendant(hasTestTag("sun-finder-info")), useUnmergedTree = true)
                .performTouchInput { click(center) }
            compose.runOnIdle {
                assertEquals(if (small) 2 else 1, previous)
                assertEquals(previous, next); assertEquals(previous, details); assertEquals(previous, info)
                assertEquals(before, sceneTaps)
            }
            val body = compose.onNodeWithTag("sun-finder-body-data", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val environment = compose.onNodeWithTag("sun-finder-environment-data", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val fixture = compose.onNodeWithTag("island-input-fixture", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            compose.onNodeWithTag("island-input-fixture", useUnmergedTree = true).performTouchInput {
                click(Offset(body.center.x - fixture.left, (body.bottom + environment.top) / 2f - fixture.top))
            }
            compose.runOnIdle { assertEquals(before + 1, sceneTaps) }
        }
    }

    @Test fun draggingInsideTheShieldedIslandStillScrollsLargeContent() {
        compose.setContent {
            MaterialTheme {
                SpaceCompassSunFinderDataPanel((0..30).map {
                    SpaceCompassSunDataRow("Value $it", "$it", "Value $it: $it", "scroll-row-$it")
                }, emptyList(), null, false, "", false, Color.Black, Color.Gray, Color.White,
                    Modifier.size(320.dp, 220.dp), false, bodyName = "Sun")
            }
        }
        val first = compose.onNodeWithTag("scroll-row-0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithTag("sun-finder-details", useUnmergedTree = true).performTouchInput { swipeUp() }
        val after = compose.onNodeWithTag("scroll-row-0", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top
        assertTrue("The tap shield must yield when a scroll gesture begins", after < first)
    }
}

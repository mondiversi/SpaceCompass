package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Use pointer events: semantic clicks cannot detect blank-area interception of child controls. */
class SpaceCompassMainControlTouchUiTest {
    @get:Rule val compose = createComposeRule()
    private var pauses = 0
    private var actions = 0

    private fun show(content: @Composable () -> Unit) {
        compose.setContent { MaterialTheme {
            val regions = remember { SpaceCompassMainGestureRegions() }
            CompositionLocalProvider(LocalSpaceCompassMainGestureRegions provides regions) {
                // Place these controls outside the sky, where the parent owns blank-area gestures.
                Column(Modifier.fillMaxSize().spaceCompassMainFreeAreaGestures(regions,
                    { SpaceCompassSunSceneFrame(-2.0, -2.0, 1.0, 1.0) }, true,
                    onTap = { pauses++ }, onDoubleTap = {})) {
                    content()
                    Box(Modifier.fillMaxWidth().height(80.dp).testTag("blank-surface"))
                }
            }
        } }
    }

    private fun touch(tag: String) {
        compose.onNodeWithTag(tag).performTouchInput { click() }
    }

    @Test fun horizontalObjectNameChevronsAndDailyPathWinOverPause() {
        show {
            SpaceCompassCelestialBodyDataIsland("Sun", Color.White, Color.Black, false,
                SpaceCompassCelestialBodyNavigation("Previous", "Next", { actions++ }, { actions++ }),
                onBodyNameClick = { actions++ }, onDailyPath = { actions++ }) {}
        }
        listOf("celestial-body-island-title", "celestial-body-previous", "celestial-body-next",
            "celestial-body-daily-path").forEach { touch(it) }
        compose.runOnIdle { assertEquals(4, actions); assertEquals(0, pauses) }
        touch("blank-surface")
        compose.waitUntil(2_000) { pauses == 1 }
    }

    @Test fun compactLandscapeNameAndActionsWinOverPause() {
        show {
            Box(Modifier.size(72.dp, 350.dp)) {
                SpaceCompassVerticalObjectSelector("Sun", Color.Yellow, Color.White,
                    SpaceCompassCelestialBodyNavigation("Previous", "Next", { actions++ }, { actions++ }),
                    onName = { actions++ }, onDailyPath = { actions++ })
            }
        }
        listOf("celestial-body-island-title", "celestial-body-previous", "celestial-body-next",
            "celestial-body-daily-path").forEach { touch(it) }
        compose.runOnIdle { assertEquals(4, actions); assertEquals(0, pauses) }
    }

    @Test fun realPositionLinkKeepsItsOwnAction() {
        show {
            SpaceCompassSunFinderDataPanel(
                listOf(SpaceCompassSunDataRow("Distance", "1 AU", "Distance: 1 AU", "distance")),
                emptyList(), null, true, "", false, Color.White, Color.Gray, Color.Black,
                Modifier.fillMaxWidth(), false, bodyName = "Sun", onInfo = { actions++ })
        }
        compose.onNodeWithTag("sun-finder-info").onChildren().filter(hasClickAction())
            .onFirst().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, actions); assertEquals(0, pauses) }
    }

    @Test fun locationNoticeOpensItsActionInsteadOfPausing() {
        show {
            SpaceCompassSunStatusNotices(null, null, null, {}, Color.Black,
                simulationLabel = "Location", onSimulation = { actions++ })
        }
        touch("observer-simulation-banner")
        compose.runOnIdle { assertEquals(1, actions); assertEquals(0, pauses) }
    }

    @Test fun inlineRetryLinkKeepsItsNativeTargetAndAction() {
        show { SpaceCompassSunStatusNotices(null, "Offline", "Retry", { actions++ }, Color.Black) }
        compose.onNode(hasClickAction() and hasAnyAncestor(hasTestTag("sun-finder-status-island")),
            useUnmergedTree = true).performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, actions); assertEquals(0, pauses) }
    }
}

package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated layout fixture: no archive, real GPS, sensor connections or network. */
class SpaceCompassCelestialLandscapeLayoutTest {
    @get:Rule val compose = createComposeRule()
    private val width = mutableStateOf(800f)
    private val height = mutableStateOf(360f)
    private val fontScale = mutableStateOf(1f)
    private val night = mutableStateOf(false)
    private val direction = mutableStateOf(LayoutDirection.Ltr)

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(0.5f, fontScale.value),
                LocalLayoutDirection provides direction.value) {
                MaterialTheme(colorScheme = if (night.value) darkColorScheme() else lightColorScheme()) {
                    val colors = MaterialTheme.colorScheme
                    Box(Modifier.requiredSize(width.value.dp, height.value.dp)) {
                        SpaceCompassSunFinderContent(SpaceCompassSunFinderReadings(), 1_800_000_000_000L,
                            colors.onSurface, colors.onSurfaceVariant, colors.background,
                            onDismissRequest = {})
                    }
                }
            }
        }
    }

    private fun navigationFitsBetweenCornerControls() {
        compose.onNodeWithTag("celestial-toolbar").assertDoesNotExist()
        val navigation = compose.onNodeWithTag("celestial-main-navigation").fetchSemanticsNode().boundsInRoot
        val start = compose.onNodeWithTag("celestial-top-start-controls").fetchSemanticsNode().boundsInRoot
        val end = compose.onNodeWithTag("celestial-top-end-controls").fetchSemanticsNode().boundsInRoot
        val left = if (direction.value == LayoutDirection.Rtl) end else start
        val right = if (direction.value == LayoutDirection.Rtl) start else end
        assertTrue(navigation.left >= left.right)
        assertTrue(navigation.right <= right.left)
        assertEquals((left.right + right.left) / 2f, navigation.center.x, 1f)
        val logo = compose.onNodeWithTag("main-info").fetchSemanticsNode().boundsInRoot
        val camera = compose.onNodeWithTag("toggle-camera").fetchSemanticsNode().boundsInRoot
        assertEquals(camera.center.y, logo.center.y, 1f)
        assertEquals(logo.width, camera.width, 1f)
        val menu = compose.onNodeWithTag("app-settings").fetchSemanticsNode().boundsInRoot
        val axis = compose.onNodeWithTag("toggle-pointing-axis").fetchSemanticsNode().boundsInRoot
        val chevron = compose.onNodeWithTag("toggle-main-details").fetchSemanticsNode().boundsInRoot
        assertEquals(camera.width, menu.width, 1f)
        assertEquals(logo.center.y, menu.center.y, 1f)
        assertEquals(axis.center.y, chevron.center.y, 1f)
        assertTrue(menu.bottom < axis.top)
        if (direction.value == LayoutDirection.Rtl) {
            assertTrue(menu.right < logo.left)
            assertTrue(axis.right < chevron.left)
        } else {
            assertTrue(logo.right < menu.left)
            assertTrue(chevron.right < axis.left)
        }
        val reticle = compose.onNodeWithTag("toggle-reticle").fetchSemanticsNode().boundsInRoot
        val references = compose.onNodeWithTag("toggle-sky-references").fetchSemanticsNode().boundsInRoot
        val objects = compose.onNodeWithTag("celestial-select").fetchSemanticsNode().boundsInRoot
        assertEquals(camera.center.x, reticle.center.x, 1f)
        assertEquals(objects.center.x, references.center.x, 1f)
        assertTrue(reticle.top > camera.bottom)
        assertTrue(references.top > objects.bottom)
        assertEquals(reticle.center.y, references.center.y, 1f)
        assertEquals(camera.width, reticle.width, 1f)
    }

    @Test fun landscapeKeepsEqualColumnsAndFloatingNavigationInBothThemesAndDirections() {
        show()
        for (w in listOf(640f, 1200f)) for (dark in listOf(false, true))
            for (rtl in listOf(false, true)) {
                compose.runOnIdle {
                    width.value = w; night.value = dark
                    direction.value = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                }
                navigationFitsBetweenCornerControls()
                val root = compose.onNodeWithTag("sun-finder-content").fetchSemanticsNode().boundsInRoot
                val details = compose.onNodeWithTag("celestial-details-column").fetchSemanticsNode().boundsInRoot
                val island = compose.onNodeWithTag("sun-finder-body-data").fetchSemanticsNode().boundsInRoot
                val sky = compose.onNodeWithTag("celestial-pointing-area").fetchSemanticsNode().boundsInRoot
                val capture = compose.onNodeWithTag("celestial-capture-controls").fetchSemanticsNode().boundsInRoot
                assertEquals(root.width * .5f - 10f, sky.width, 1f)
                assertEquals(root.top, sky.top, 1f)
                assertEquals(root.top + 5f, details.top, 1f)
                assertTrue(capture.top >= details.bottom)
                assertTrue(capture.bottom <= root.bottom - 5f)
                if (rtl) assertTrue(capture.right <= root.center.x + 1f)
                else assertTrue(capture.left >= root.center.x - 1f)
                val innerGap = if (rtl) root.center.x - island.right else island.left - root.center.x
                val outerGap = if (rtl) island.left - root.left else root.right - island.right
                assertEquals(outerGap, innerGap, 1f)
                assertEquals(5f, innerGap, 1f)
            }
    }

    @Test fun portraitAlsoUsesFloatingNavigationAndRotationRestoresTheSplit() {
        show()
        compose.runOnIdle { width.value = 360f; height.value = 640f }
        compose.onNodeWithTag("celestial-landscape-layout").assertDoesNotExist()
        navigationFitsBetweenCornerControls()
        val root = compose.onNodeWithTag("sun-finder-content").fetchSemanticsNode().boundsInRoot
        val sky = compose.onNodeWithTag("celestial-pointing-area").fetchSemanticsNode().boundsInRoot
        assertEquals(root.top, sky.top, 1f)
        compose.runOnIdle { width.value = 640f; height.value = 360f }
        compose.onNodeWithTag("celestial-landscape-layout").assertExists()
        navigationFitsBetweenCornerControls()
        compose.onNodeWithTag("celestial-select").performClick()
        compose.onNodeWithTag("celestial-menu").assertIsDisplayed()
    }

    @Test fun largeTextUsesTheSplitOnlyWhenTheDataColumnHasEnoughWidth() {
        show()
        compose.runOnIdle { width.value = 560f; height.value = 360f; fontScale.value = 2f }
        compose.onNodeWithTag("celestial-landscape-layout").assertDoesNotExist()
        compose.runOnIdle { width.value = 800f }
        compose.onNodeWithTag("celestial-landscape-layout").assertExists()
        navigationFitsBetweenCornerControls()
        compose.onNodeWithTag("sun-finder-details").assertExists()
    }
}

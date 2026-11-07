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

    private fun floatingGroupsFitCurrentLayout() {
        compose.onNodeWithTag("celestial-toolbar").assertDoesNotExist()
        val infoNavigation = compose.onNodeWithTag("celestial-main-info-navigation").fetchSemanticsNode().boundsInRoot
        val menuNavigation = compose.onNodeWithTag("celestial-main-menu-navigation").fetchSemanticsNode().boundsInRoot
        val start = compose.onNodeWithTag("celestial-top-start-controls").fetchSemanticsNode().boundsInRoot
        val end = compose.onNodeWithTag("celestial-top-end-controls").fetchSemanticsNode().boundsInRoot
        assertTrue(infoNavigation.left >= start.left && infoNavigation.right <= start.right)
        assertTrue(menuNavigation.left >= end.left && menuNavigation.right <= end.right)
        val logo = compose.onNodeWithTag("main-info").fetchSemanticsNode().boundsInRoot
        val menu = compose.onNodeWithTag("app-settings").fetchSemanticsNode().boundsInRoot
        val objects = compose.onNodeWithTag("celestial-select").fetchSemanticsNode().boundsInRoot
        val references = compose.onNodeWithTag("toggle-sky-references").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("toggle-reticle").assertDoesNotExist()
        val forward = compose.onNodeWithTag("toggle-forward-path").fetchSemanticsNode().boundsInRoot
        val topCentre = compose.onNodeWithTag("celestial-top-center-controls").fetchSemanticsNode().boundsInRoot
        val sky = compose.onNodeWithTag("sun-finder-sky").fetchSemanticsNode().boundsInRoot
        assertEquals(sky.center.x, topCentre.center.x, 1f)
        assertEquals(logo.center.y, menu.center.y, 1f)
        assertEquals(logo.center.y, references.center.y, 1f)
        assertEquals(objects.center.y, references.center.y, 1f)
        assertEquals(objects.center.y, forward.center.y, 1f)

        if (direction.value == LayoutDirection.Rtl) {
            assertTrue(menu.right < topCentre.left)
            assertTrue(topCentre.right < logo.left)
            assertTrue(end.right < start.left)
            assertTrue(objects.right < references.left)
            assertTrue(references.right < forward.left)

        } else {
            assertTrue(logo.right < topCentre.left)
            assertTrue(topCentre.right < menu.left)
            assertTrue(start.right < end.left)

            assertTrue(references.right < objects.left)
            assertTrue(forward.right < references.left)
        }
        val axis = compose.onNodeWithTag("toggle-pointing-axis").fetchSemanticsNode().boundsInRoot
        val chevron = compose.onNodeWithTag("toggle-main-details").fetchSemanticsNode().boundsInRoot
        val weather = compose.onNodeWithTag("toggle-weather").fetchSemanticsNode().boundsInRoot
        val camera = compose.onNodeWithTag("toggle-camera").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithTag("celestial-center-start-controls").assertDoesNotExist()
        compose.onNodeWithTag("celestial-center-end-controls").assertDoesNotExist()
        compose.onNodeWithTag("celestial-top-end-primary-controls").assertDoesNotExist()
        compose.onNodeWithTag("celestial-top-end-secondary-controls").assertDoesNotExist()
        val centre = compose.onNodeWithTag("celestial-bottom-center-controls").fetchSemanticsNode().boundsInRoot
        val capture = compose.onNodeWithTag("capture-panorama").fetchSemanticsNode().boundsInRoot
        val captureGroup = compose.onNodeWithTag("celestial-capture-controls").fetchSemanticsNode().boundsInRoot
        val zoom = compose.onNodeWithTag("virtual-zoom-value").fetchSemanticsNode().boundsInRoot
        assertEquals(weather.center.y, zoom.center.y, 1f)
        assertEquals(capture.bottom, camera.bottom, 1f)
        assertTrue(camera.left >= captureGroup.left && camera.right <= captureGroup.right)
        assertTrue(capture.left >= captureGroup.left && capture.right <= captureGroup.right)
        assertEquals(sky.center.x, centre.center.x, 1f)
        if (width.value > height.value) {
            assertEquals(menu.center.x, axis.center.x, 1f)
            assertTrue(axis.top > menu.bottom)
        } else assertEquals(chevron.bottom, axis.bottom, 1f)
        assertTrue(topCentre.bottom < centre.top)
        listOf(camera, weather, axis, chevron, capture).forEach { bounds ->
            assertEquals(logo.width, bounds.width, 1f)
        }
        if (direction.value == LayoutDirection.Rtl) {
            assertEquals(logo.right, chevron.right, 1f)
            assertTrue(zoom.right < weather.left)
            assertTrue(axis.right < chevron.left)
            assertTrue(capture.right < camera.left)
        } else {
            assertEquals(logo.left, chevron.left, 1f)
            assertTrue(weather.right < zoom.left)
            assertTrue(chevron.right < axis.left)
            assertTrue(camera.right < capture.left)
        }
    }

    @Test fun landscapeKeepsRequestedColumnsAndFloatingNavigationInBothThemesAndDirections() {
        show()
        for (w in listOf(640f, 1200f)) for (dark in listOf(false, true))
            for (rtl in listOf(false, true)) {
                compose.runOnIdle {
                    width.value = w; night.value = dark
                    direction.value = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                }
                floatingGroupsFitCurrentLayout()
                val root = compose.onNodeWithTag("sun-finder-content").fetchSemanticsNode().boundsInRoot
                val details = compose.onNodeWithTag("celestial-details-column").fetchSemanticsNode().boundsInRoot
                val island = compose.onNodeWithTag("sun-finder-body-data").fetchSemanticsNode().boundsInRoot
                val sky = compose.onNodeWithTag("celestial-pointing-area").fetchSemanticsNode().boundsInRoot
                val capture = compose.onNodeWithTag("celestial-capture-controls").fetchSemanticsNode().boundsInRoot
                assertEquals(root.width * .55f - 10f, sky.width, 1f)
                assertEquals(root.top, sky.top, 1f)
                assertEquals(root.top + 5f, details.top, 1f)
                assertTrue(capture.top >= details.bottom)
                assertTrue(capture.bottom <= root.bottom - 5f)
                val boundary = if (rtl) root.right - root.width * .55f else root.left + root.width * .55f
                if (rtl) assertTrue(capture.right <= boundary + 1f)
                else assertTrue(capture.left >= boundary - 1f)
                val innerGap = if (rtl) boundary - island.right else island.left - boundary
                val outerGap = if (rtl) island.left - root.left else root.right - island.right
                assertEquals(outerGap, innerGap, 1f)
                assertEquals(5f, innerGap, 1f)
            }
    }

    @Test fun portraitAlsoUsesFloatingNavigationAndRotationRestoresTheSplit() {
        show()
        compose.runOnIdle { width.value = 360f; height.value = 640f }
        compose.onNodeWithTag("celestial-landscape-layout").assertDoesNotExist()
        floatingGroupsFitCurrentLayout()
        val root = compose.onNodeWithTag("sun-finder-content").fetchSemanticsNode().boundsInRoot
        val sky = compose.onNodeWithTag("celestial-pointing-area").fetchSemanticsNode().boundsInRoot
        assertEquals(root.top, sky.top, 1f)
        compose.runOnIdle { width.value = 640f; height.value = 360f }
        compose.onNodeWithTag("celestial-landscape-layout").assertExists()
        floatingGroupsFitCurrentLayout()
        compose.onNodeWithTag("celestial-select").performClick()
        compose.onNodeWithTag("celestial-menu").assertIsDisplayed()
    }

    @Test fun largeTextUsesTheSplitOnlyWhenTheDataColumnHasEnoughWidth() {
        show()
        compose.runOnIdle { width.value = 560f; height.value = 360f; fontScale.value = 2f }
        compose.onNodeWithTag("celestial-landscape-layout").assertDoesNotExist()
        compose.runOnIdle { width.value = 800f }
        compose.onNodeWithTag("celestial-landscape-layout").assertExists()
        floatingGroupsFitCurrentLayout()
        compose.onNodeWithTag("sun-finder-details").assertExists()
    }
}

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
            // Fixed fixture density fits both phone and tablet cases on a disposable test device.
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

    @Test fun phoneAndTabletLandscapeHeadersUseTheWiderSkyColumnInBothThemesAndDirections() {
        show()
        for (w in listOf(640f, 1200f)) for (dark in listOf(false, true))
            for (rtl in listOf(false, true)) {
                compose.runOnIdle {
                    width.value = w; night.value = dark
                    direction.value = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                }
                val root = compose.onNodeWithTag("sun-finder-content").fetchSemanticsNode().boundsInRoot
                val toolbar = compose.onNodeWithTag("celestial-toolbar").fetchSemanticsNode().boundsInRoot
                val details = compose.onNodeWithTag("celestial-details-column").fetchSemanticsNode().boundsInRoot
                val island = compose.onNodeWithTag("sun-finder-body-data").fetchSemanticsNode().boundsInRoot
                val sky = compose.onNodeWithTag("celestial-pointing-area").fetchSemanticsNode().boundsInRoot
                assertEquals(root.width * .55f, toolbar.width, 1f)
                assertEquals(root.top, toolbar.top, 1f)
                assertEquals(root.top + 5f, details.top, 1f)
                assertEquals(root.bottom - 5f, details.bottom, 1f)
                assertTrue("Islands start above the bottom of the toolbar", island.top < toolbar.bottom)
                assertEquals("Sky continues beneath its translucent toolbar", toolbar.top, sky.top, 1f)
                if (rtl) assertTrue(details.right <= toolbar.left + 1f)
                else assertTrue(details.left >= toolbar.right - 1f)
                val titleGap = if (rtl) toolbar.left - island.right else island.left - toolbar.right
                val outerGap = if (rtl) island.left - root.left else root.right - island.right
                assertEquals("Title-side and outer-edge island gaps match", outerGap, titleGap, 1f)
                assertEquals("Both landscape gaps remain 10 dp", 5f, titleGap, 1f)
            }
    }

    @Test fun portraitStillHasAFullWidthToolbarAndRotationRestoresTheSplit() {
        show()
        compose.runOnIdle { width.value = 360f; height.value = 640f }
        compose.onNodeWithTag("celestial-landscape-layout").assertDoesNotExist()
        val root = compose.onNodeWithTag("sun-finder-content").fetchSemanticsNode().boundsInRoot
        val toolbar = compose.onNodeWithTag("celestial-toolbar").fetchSemanticsNode().boundsInRoot
        assertEquals(root.width, toolbar.width, 1f)
        val sky = compose.onNodeWithTag("celestial-pointing-area").fetchSemanticsNode().boundsInRoot
        assertEquals("Portrait sky also continues beneath the toolbar", toolbar.top, sky.top, 1f)
        compose.runOnIdle { width.value = 640f; height.value = 360f }
        compose.onNodeWithTag("celestial-landscape-layout").assertExists()
        compose.onNodeWithTag("celestial-select").performClick()
        compose.onNodeWithTag("celestial-menu").assertIsDisplayed()
    }

    @Test fun largeTextUsesTheSplitOnlyWhenTheDataColumnHasEnoughWidth() {
        show()
        compose.runOnIdle { width.value = 560f; height.value = 360f; fontScale.value = 2f }
        compose.onNodeWithTag("celestial-landscape-layout").assertDoesNotExist()
        compose.runOnIdle { width.value = 800f }
        compose.onNodeWithTag("celestial-landscape-layout").assertExists()
        compose.onNodeWithTag("celestial-toolbar").assertExists()
        compose.onNodeWithTag("sun-finder-details").assertExists()
    }
}

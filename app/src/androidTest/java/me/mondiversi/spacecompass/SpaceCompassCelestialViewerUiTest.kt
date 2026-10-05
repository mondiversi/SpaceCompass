package me.mondiversi.spacecompass

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.Locale

/** Compile-only on personal phones. Run in an isolated emulator/test application, never the user's installation. */
class SpaceCompassCelestialViewerUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
        .apply { setLocale(Locale.ITALIAN) }).resources
    private val body = mutableStateOf(SpaceCompassCelestialBody.ISS)
    private val wide = mutableStateOf(false)
    private val dark = mutableStateOf(false)
    private var closed = false
    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources) {
                MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                    Box(if (wide.value) Modifier.size(780.dp,360.dp) else Modifier.fillMaxSize()) {
                        val c = MaterialTheme.colorScheme
                        SpaceCompassCelestialViewerScreen(body.value, Instant.parse("2026-10-04T12:00:00Z").toEpochMilli(),
                            null,null,0.0,SpaceCompassCelestialRemoteData(),c.onSurface,c.onSurfaceVariant,c.background) { closed = true }
                    }
                }
            }
        }
    }
    @Test fun twoTabsAndFactsRemainAvailableWithoutGpsInBothThemes() {
        show()
        for (night in listOf(false,true)) {
            compose.runOnIdle { dark.value = night }
            compose.onNodeWithTag("celestial-view-current").assertIsSelected().assertIsDisplayed()
            compose.onNodeWithTag("celestial-view-information").assertIsDisplayed()
            compose.onNodeWithTag("celestial-view-rotation").performClick().assertIsSelected()
            compose.onNodeWithTag("celestial-model-viewport").performTouchInput { swipeLeft() }
            compose.onNodeWithTag("celestial-model-viewport").performTouchInput { doubleClick() }
            compose.onNodeWithTag("celestial-view-current").performClick()
        }
        assertFalse(closed)
    }
    @Test fun missingLocationNeverPretendsToRenderTheCurrentPlanetFace() {
        body.value = SpaceCompassCelestialBody.MOON
        show()
        compose.onNodeWithText(resources.getString(R.string.celestial_view_location_needed)).assertExists()
        compose.onNodeWithTag("celestial-model-viewport").assertDoesNotExist()
        compose.onNodeWithTag("celestial-view-rotation").performClick()
        compose.onNodeWithTag("celestial-model-viewport").assertExists()
    }
    @Test fun surfaceGravityIncludesEarthGWithoutHidingUnavailableValues() {
        body.value = SpaceCompassCelestialBody.POLARIS
        show()
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark.value = night }
            compose.onNodeWithText(resources.getString(R.string.celestial_view_gravity)).assertIsDisplayed()
            compose.onNodeWithText(formatSpaceCompassCelestialGravity(spaceCompassCelestialFacts(body.value).gravity,
                SpaceCompassNumericFormat.SYSTEM, 2)).assertIsDisplayed()
        }
        compose.runOnIdle { body.value = SpaceCompassCelestialBody.ISS }
        compose.onNodeWithText(resources.getString(R.string.celestial_view_gravity)).assertIsDisplayed()
        compose.onAllNodesWithText("—").assertCountEquals(12)
    }
    @Test fun thermalRowsUseTheCorrectLayerAndFollowBodyChangesInBothThemes() {
        body.value = SpaceCompassCelestialBody.MOON
        show()
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark.value = night; body.value = SpaceCompassCelestialBody.MOON }
            for (value in spaceCompassCelestialTemperatures(SpaceCompassCelestialBody.MOON)) {
                compose.onNodeWithText(resources.getString(value.kind.labelResource)).performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(formatSpaceCompassCelestialTemperature(value, SpaceCompassNumericFormat.SYSTEM)).assertIsDisplayed()
            }
            compose.runOnIdle { body.value = SpaceCompassCelestialBody.JUPITER }
            compose.onNodeWithText(resources.getString(R.string.celestial_temperature_atmosphere)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(resources.getString(R.string.celestial_temperature_day_maximum)).assertDoesNotExist()
            compose.runOnIdle { body.value = SpaceCompassCelestialBody.ISS }
            compose.onNodeWithText(resources.getString(R.string.celestial_temperature)).performScrollTo().assertIsDisplayed()
            compose.onNodeWithText(resources.getString(R.string.celestial_temperature_atmosphere)).assertDoesNotExist()
        }
    }
    @Test fun landscapeKeepsTabsInsideModelAndUsesSeparateInformationHalf() {
        show()
        compose.runOnIdle { wide.value = true }
        val model=compose.onNodeWithTag("celestial-model-viewport").fetchSemanticsNode().boundsInRoot
        val info=compose.onNodeWithTag("celestial-view-information").fetchSemanticsNode().boundsInRoot
        assertTrue(info.left >= model.right)
        compose.onNodeWithTag("celestial-view-current").assertIsDisplayed()
        compose.onNodeWithTag("celestial-view-rotation").assertIsDisplayed()
        val tab = compose.onNodeWithTag("celestial-view-rotation").fetchSemanticsNode().boundsInRoot
        assertTrue(tab.top >= model.top && tab.bottom <= model.bottom)
        val current = compose.onNodeWithTag("celestial-view-current").fetchSemanticsNode().boundsInRoot
        assertTrue("Icon controls stay at the top-right rather than spanning the image", current.left > model.center.x)
        assertTrue("Icon control remains touch accessible", tab.width >= with(compose.density) { 47.dp.toPx() })
        compose.onNodeWithText(resources.getString(R.string.celestial_view_current)).assertDoesNotExist()
        compose.onNodeWithText(resources.getString(R.string.celestial_view_rotation)).assertDoesNotExist()
        compose.onNodeWithText(resources.getString(R.string.celestial_view_information)).assertDoesNotExist()
    }
    @Test fun viewerStartsImmediatelyBelowToolbarInBothLayoutsThemesAndDirections() {
        val width = mutableStateOf(640f)
        val height = mutableStateOf(360f)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources,
                LocalDensity provides Density(0.5f), LocalLayoutDirection provides direction.value) {
                MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                    val c = MaterialTheme.colorScheme
                    Box(Modifier.requiredSize(width.value.dp, height.value.dp)) {
                        SpaceCompassCelestialViewerScreen(SpaceCompassCelestialBody.ISS,
                            Instant.parse("2026-10-04T12:00:00Z").toEpochMilli(),
                            null, null, 0.0, SpaceCompassCelestialRemoteData(), c.onSurface,
                            c.onSurfaceVariant, c.background) {}
                    }
                }
            }
        }
        for ((w, h) in listOf(360f to 640f, 640f to 360f, 800f to 1200f, 1200f to 800f))
            for (night in listOf(false, true))
            for (rtl in listOf(false, true)) {
                compose.runOnIdle {
                    width.value = w; height.value = h; dark.value = night
                    direction.value = if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                }
                val root = compose.onNodeWithTag("celestial-viewer").fetchSemanticsNode().boundsInRoot
                val toolbar = compose.onNodeWithTag("celestial-view-toolbar").fetchSemanticsNode().boundsInRoot
                val model = compose.onNodeWithTag("celestial-view-model").fetchSemanticsNode().boundsInRoot
                val info = compose.onNodeWithTag("celestial-view-information-island").fetchSemanticsNode().boundsInRoot
                assertEquals("No added gap above the image", toolbar.bottom, model.top, 1f)
                if (w > h) assertEquals("Landscape facts start at the same height", model.top, info.top, 1f)
                else assertEquals("Portrait image/facts spacing is unchanged", 5f, info.top - model.bottom, 1f)
                val sideGap = if (rtl) root.right - model.right else root.right - info.right
                assertEquals("Side inset is unchanged", 5f, sideGap, 1f)
                assertEquals("Bottom inset is unchanged", 3f, root.bottom - info.bottom, 1f)
            }
    }
    @Test fun pinchWorksInBothTabsAndDoubleTapStillResumesRotation() {
        show()
        val model = compose.onNodeWithTag("celestial-model-viewport")
        fun pinch() = model.performTouchInput {
            down(0, center - Offset(40f, 0f)); down(1, center + Offset(40f, 0f))
            for (step in 1..10) {
                updatePointerTo(0, center - Offset(40f + step * 6, 0f))
                updatePointerTo(1, center + Offset(40f + step * 6, 0f))
                move()
            }
            up(0); up(1)
        }
        pinch()
        val zoom = model.fetchSemanticsNode().config[SemanticsProperties.StateDescription]
        assertNotEquals("Zoom: 100%", zoom)
        assertTrue("Touch increments must accumulate even within a single UI frame",
            zoom.filter(Char::isDigit).toInt() >= 130)
        compose.onNodeWithTag("celestial-view-rotation").performClick()
        assertEquals(zoom, model.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
        pinch()
        model.performTouchInput { swipeLeft(); doubleClick() }
        compose.onNodeWithTag("celestial-view-rotation").assertIsSelected()
        compose.onNodeWithTag("celestial-view-current").performClick()
        model.performTouchInput { doubleClick() }
        assertEquals("Zoom: 100%", model.fetchSemanticsNode().config[SemanticsProperties.StateDescription])
    }
}

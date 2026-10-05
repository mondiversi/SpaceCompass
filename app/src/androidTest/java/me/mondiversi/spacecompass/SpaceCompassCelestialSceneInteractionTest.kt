package me.mondiversi.spacecompass

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Disposable test device only: synthetic scene, no location, network or user archive. */
class SpaceCompassCelestialSceneInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
        .apply { setLocale(Locale.ITALIAN) }).resources
    private fun facing(position: SpaceCompassSunPosition): SpaceCompassSunOrientation {
        val a = Math.toRadians(position.azimuthDegrees); val e = Math.toRadians(position.elevationDegrees)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a), -sin(a), 0.0),
            SpaceCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            SpaceCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }

    @Test fun onlyTheInspectedBodyHasAnOffscreenDirectionAndSwitchingReplacesIt() {
        val position = SpaceCompassSunPosition(90.0, 15.0)
        val overlays = spaceCompassCelestialCatalogOrder.associateWith { body -> SpaceCompassCelestialOverlay(body,
            SpaceCompassCelestialObservation(position, 1.0), null, null, null) }
        val active = mutableStateOf<SpaceCompassCelestialBody?>(SpaceCompassCelestialBody.SUN)
        compose.setContent {
            MaterialTheme { Box(Modifier.size(340.dp, 400.dp)) {
                SpaceCompassSunPointingViewport(null, facing(SpaceCompassSunPosition(180.0, 0.0)), Modifier.fillMaxSize(), "fixture",
                    rememberSpaceCompassSunDailyPathUiState(), overlays = overlays, showActions = false,
                    offscreenBody = active.value)
            } }
        }
        for (selected in spaceCompassCelestialCatalogOrder) {
            compose.runOnIdle { active.value = selected }
            compose.onAllNodesWithTag("celestial-offscreen-marker", useUnmergedTree = true).assertCountEquals(1)
            spaceCompassCelestialCatalogOrder.forEach { body ->
                val preview = compose.onNodeWithTag("celestial-offscreen-preview-${body.name}", useUnmergedTree = true)
                if (body == selected) preview.assertIsDisplayed() else preview.assertDoesNotExist()
            }
        }
        compose.runOnIdle { active.value = null }
        compose.onAllNodesWithTag("celestial-offscreen-marker", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("sun-daily-path").assertDoesNotExist()
    }

    @Test fun pointingAndTappingAnotherBodiesPathShowsItsNumberAndBothRightAlignedAngleRows() {
        val position = SpaceCompassSunPosition(180.0, 25.0)
        val markers = (0..7).map { index -> SpaceCompassSunPathPoint(1_791_112_320_000L + index * 3_600_000L,
            if (index == 7) position else SpaceCompassSunPosition(0.0, 25.0)) }
        val path = SpaceCompassSunDailyPath(LocalDate.parse("2026-10-04"), ZoneId.of("UTC"), markers, markers, SpaceCompassCelestialBody.MOON)
        val state = SpaceCompassSunDailyPathUiState()
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalSpaceCompassTimeFormat provides SpaceCompassTimeFormat.H24) {
                MaterialTheme { Box(Modifier.size(340.dp, 400.dp)) {
                    SpaceCompassSunPointingViewport(SpaceCompassSunPosition(0.0, 25.0), facing(position), Modifier.fillMaxSize(), "fixture",
                        state, timeMs = markers.last().timeMs, body = SpaceCompassCelestialBody.SUN, showActions = false,
                        primaryText = Color.Black, secondaryText = Color.Gray, backgroundColor = Color.White,
                        overlays = mapOf(path.body to SpaceCompassCelestialOverlay(path.body, null, null, null, path)))
                } }
            }
        }
        val numbered = resources.getString(R.string.celestial_point_number, 8)
        compose.onNodeWithText(numbered, substring = true).assertIsDisplayed()
        compose.onNodeWithTag("celestial-badge-body").assertTextEquals("Luna")
        val azimuth = compose.onNodeWithTag("celestial-badge-azimuth").fetchSemanticsNode().boundsInRoot
        val elevation = compose.onNodeWithTag("celestial-badge-elevation").fetchSemanticsNode().boundsInRoot
        assertEquals(azimuth.right, elevation.right, 0.5f)
        compose.onNodeWithTag("sun-finder-sky").performTouchInput { click(center) }
        compose.onNodeWithTag("sun-path-selected").assertIsDisplayed()
        compose.onNodeWithTag("celestial-selected-caption").assertTextContains(numbered)
        compose.onNodeWithTag("celestial-selected-azimuth").assertIsDisplayed()
        compose.onNodeWithTag("celestial-selected-elevation").assertIsDisplayed()
        compose.runOnIdle { assertEquals(path.body, state.selectedBody); assertEquals(markers.last(), state.selectedIn(path)) }
    }

    @Test fun aPartlyClippedLiveMiniatureBecomesACompleteDirectionalLocator() {
        val edge = SpaceCompassSunPosition(155.0, 0.0)
        compose.setContent {
            MaterialTheme { Box(Modifier.size(340.dp, 400.dp)) {
                SpaceCompassSunPointingViewport(null, facing(SpaceCompassSunPosition(180.0, 0.0)), Modifier.fillMaxSize(), "fixture",
                    rememberSpaceCompassSunDailyPathUiState(), showActions = false, body = SpaceCompassCelestialBody.MARS,
                    overlays = mapOf(SpaceCompassCelestialBody.MARS to SpaceCompassCelestialOverlay(SpaceCompassCelestialBody.MARS,
                        SpaceCompassCelestialObservation(edge, 1.0), null, null, null)))
            } }
        }
        compose.onNodeWithTag("celestial-live-preview-MARS", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("celestial-offscreen-preview-MARS", useUnmergedTree = true).assertIsDisplayed()
    }
}

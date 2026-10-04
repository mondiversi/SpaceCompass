package me.mondiversi.planetcompass

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
class PlanetCompassCelestialSceneInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
        .apply { setLocale(Locale.ITALIAN) }).resources
    private fun facing(position: PlanetCompassSunPosition): PlanetCompassSunOrientation {
        val a = Math.toRadians(position.azimuthDegrees); val e = Math.toRadians(position.elevationDegrees)
        return PlanetCompassSunOrientation(PlanetCompassSunVector(cos(a), -sin(a), 0.0),
            PlanetCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            PlanetCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }

    @Test fun onlyTheInspectedBodyHasAnOffscreenDirectionAndSwitchingReplacesIt() {
        val position = PlanetCompassSunPosition(90.0, 15.0)
        val overlays = planetCompassCelestialCatalogOrder.associateWith { body -> PlanetCompassCelestialOverlay(body,
            PlanetCompassCelestialObservation(position, 1.0), null, null, null) }
        val active = mutableStateOf<PlanetCompassCelestialBody?>(PlanetCompassCelestialBody.SUN)
        compose.setContent {
            MaterialTheme { Box(Modifier.size(340.dp, 400.dp)) {
                PlanetCompassSunPointingViewport(null, facing(PlanetCompassSunPosition(180.0, 0.0)), Modifier.fillMaxSize(), "fixture",
                    rememberPlanetCompassSunDailyPathUiState(), overlays = overlays, showActions = false,
                    offscreenBody = active.value)
            } }
        }
        for (selected in planetCompassCelestialCatalogOrder) {
            compose.runOnIdle { active.value = selected }
            compose.onAllNodesWithTag("celestial-offscreen-marker", useUnmergedTree = true).assertCountEquals(1)
            planetCompassCelestialCatalogOrder.forEach { body ->
                val preview = compose.onNodeWithTag("celestial-offscreen-preview-${body.name}", useUnmergedTree = true)
                if (body == selected) preview.assertIsDisplayed() else preview.assertDoesNotExist()
            }
        }
        compose.runOnIdle { active.value = null }
        compose.onAllNodesWithTag("celestial-offscreen-marker", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("sun-daily-path").assertDoesNotExist()
    }

    @Test fun pointingAndTappingAnotherBodiesPathShowsItsNumberAndBothRightAlignedAngleRows() {
        val position = PlanetCompassSunPosition(180.0, 25.0)
        val markers = (0..7).map { index -> PlanetCompassSunPathPoint(1_791_112_320_000L + index * 3_600_000L,
            if (index == 7) position else PlanetCompassSunPosition(0.0, 25.0)) }
        val path = PlanetCompassSunDailyPath(LocalDate.parse("2026-10-04"), ZoneId.of("UTC"), markers, markers, PlanetCompassCelestialBody.MOON)
        val state = PlanetCompassSunDailyPathUiState()
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalPlanetCompassTimeFormat provides PlanetCompassTimeFormat.H24) {
                MaterialTheme { Box(Modifier.size(340.dp, 400.dp)) {
                    PlanetCompassSunPointingViewport(PlanetCompassSunPosition(0.0, 25.0), facing(position), Modifier.fillMaxSize(), "fixture",
                        state, timeMs = markers.last().timeMs, body = PlanetCompassCelestialBody.SUN, showActions = false,
                        primaryText = Color.Black, secondaryText = Color.Gray, backgroundColor = Color.White,
                        overlays = mapOf(path.body to PlanetCompassCelestialOverlay(path.body, null, null, null, path)))
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
        compose.onNodeWithTag("celestial-selected-caption").assertTextContains("Luna · $numbered")
        compose.onNodeWithTag("celestial-selected-azimuth").assertIsDisplayed()
        compose.onNodeWithTag("celestial-selected-elevation").assertIsDisplayed()
        compose.runOnIdle { assertEquals(path.body, state.selectedBody); assertEquals(markers.last(), state.selectedIn(path)) }
    }

    @Test fun aPartlyClippedLiveMiniatureBecomesACompleteDirectionalLocator() {
        val edge = PlanetCompassSunPosition(155.0, 0.0)
        compose.setContent {
            MaterialTheme { Box(Modifier.size(340.dp, 400.dp)) {
                PlanetCompassSunPointingViewport(null, facing(PlanetCompassSunPosition(180.0, 0.0)), Modifier.fillMaxSize(), "fixture",
                    rememberPlanetCompassSunDailyPathUiState(), showActions = false, body = PlanetCompassCelestialBody.MARS,
                    overlays = mapOf(PlanetCompassCelestialBody.MARS to PlanetCompassCelestialOverlay(PlanetCompassCelestialBody.MARS,
                        PlanetCompassCelestialObservation(edge, 1.0), null, null, null)))
            } }
        }
        compose.onNodeWithTag("celestial-live-preview-MARS", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("celestial-offscreen-preview-MARS", useUnmergedTree = true).assertIsDisplayed()
    }
}

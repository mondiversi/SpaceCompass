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
import java.time.Instant
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

    @Test fun pointingAndTappingAnotherBodiesPathShowsClockCountdownAndBothRightAlignedAngleRows() {
        val position = SpaceCompassSunPosition(180.0, 25.0)
        val markers = (0..7).map { index -> SpaceCompassSunPathPoint(Instant.parse("2026-10-04T00:00:00Z").toEpochMilli() + index * 3_600_000L,
            if (index == 7) position else SpaceCompassSunPosition(0.0, 25.0)) }
        val path = SpaceCompassSunDailyPath(LocalDate.parse("2026-10-04"), ZoneId.of("UTC"), markers, markers, SpaceCompassCelestialBody.MOON)
        val state = SpaceCompassSunDailyPathUiState()
        val offscreenCurrent = SpaceCompassSunPosition(0.0, -15.0)
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalSpaceCompassTimeFormat provides SpaceCompassTimeFormat.H24) {
                MaterialTheme { Box(Modifier.size(340.dp, 400.dp)) {
                    SpaceCompassSunPointingViewport(SpaceCompassSunPosition(0.0, 25.0), facing(position), Modifier.fillMaxSize(), "fixture",
                        state, timeMs = markers.last().timeMs, body = SpaceCompassCelestialBody.SUN, showActions = false,
                        primaryText = Color.Black, secondaryText = Color.Gray, backgroundColor = Color.White,
                        overlays = mapOf(path.body to SpaceCompassCelestialOverlay(path.body,
                            SpaceCompassCelestialObservation(offscreenCurrent, 1.0), null, null, path)))
                }
                SpaceCompassSunDailyPathPage(state, Color.Black, Color.Gray, Color.White)
                }
            }
        }
        val numbered = resources.getString(R.string.celestial_point_number, 7)
        compose.onNodeWithText(numbered, substring = true).assertDoesNotExist()
        compose.onNodeWithTag("celestial-badge-body").assertDoesNotExist()
        compose.onNodeWithTag("celestial-badge-event").assertDoesNotExist()
        compose.onNodeWithTag("celestial-badge-caption").assertTextContains("07:00").assertTextContains("T+00:00")
        val clock = compose.onNodeWithTag("celestial-point-clock", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val countdown = compose.onNodeWithTag("celestial-point-countdown", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(clock.right < countdown.left)
        assertEquals(clock.center.y, countdown.center.y, .5f)
        val azimuth = compose.onNodeWithTag("celestial-badge-azimuth").fetchSemanticsNode().boundsInRoot
        val elevation = compose.onNodeWithTag("celestial-badge-elevation").fetchSemanticsNode().boundsInRoot
        assertEquals(azimuth.right, elevation.right, 0.5f)
        compose.onNodeWithTag("sun-finder-sky").performTouchInput { click(center) }
        compose.onNodeWithTag("sun-path-page").assertDoesNotExist()
        compose.onNodeWithTag("celestial-pinned-badge").assertIsDisplayed()
        compose.runOnIdle {
            assertTrue(state.isSelected(path, markers[7]))
            assertNull(state.menuPath)
            state.openMenu(path, spaceCompassCurrentPathPoint(path.body, markers.last().timeMs, offscreenCurrent))
        }
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-point-7").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
        compose.onNodeWithTag("sun-path-current").assertIsDisplayed().assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(path, state.menuPath); assertEquals("7", state.menuFocusPointId); assertEquals(1, state.selectedPoints.size)
            assertEquals(offscreenCurrent, state.menuCurrentPoint!!.position)
            assertEquals(markers.last().timeMs, state.menuCurrentPoint!!.timeMs)
        }
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

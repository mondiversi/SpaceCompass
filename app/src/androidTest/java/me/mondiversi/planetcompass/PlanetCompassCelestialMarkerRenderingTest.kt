package me.mondiversi.planetcompass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import kotlin.math.*

/** Disposable test device only: synthetic angles, no GPS, network, preferences or archive access. */
class PlanetCompassCelestialMarkerRenderingTest {
    @get:Rule val compose = createComposeRule()

    private fun facing(position: PlanetCompassSunPosition): PlanetCompassSunOrientation {
        val a = Math.toRadians(position.azimuthDegrees)
        val e = Math.toRadians(position.elevationDegrees)
        return PlanetCompassSunOrientation(PlanetCompassSunVector(cos(a), -sin(a), 0.0),
            PlanetCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            PlanetCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }

    @Test fun allTargetsUseTheirOwnEquallySizedPreviewsAboveAndBelowHorizonInBothThemes() {
        val body = mutableStateOf(PlanetCompassCelestialBody.SUN)
        val position = mutableStateOf(PlanetCompassSunPosition(180.0, 25.0))
        val dark = mutableStateOf(false)
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                val state = rememberPlanetCompassSunDailyPathUiState()
                Box(Modifier.size(200.dp).background(Color(0xFF203B5C)).testTag("celestial-dot-fixture")) {
                    PlanetCompassSunPointingViewport(position.value, facing(position.value), Modifier.fillMaxSize(), "fixture",
                        state, body = body.value)
                }
            }
        }
        for (night in listOf(false, true)) for (elevation in listOf(-15.0, 0.0, 25.0, 70.0)) {
            compose.runOnIdle { dark.value = night; position.value = PlanetCompassSunPosition(180.0, elevation) }
            for (candidate in PlanetCompassCelestialBody.entries) {
                compose.runOnIdle { body.value = candidate }
                compose.onNodeWithTag("celestial-live-preview-${candidate.name}", useUnmergedTree = true)
                    .assertIsDisplayed().assertWidthIsEqualTo(32.dp).assertHeightIsEqualTo(32.dp)
            }
        }
    }

    @Test fun tappingTheLivePositionSelectsItsExactInstantWithOrWithoutAnAvailableCurve() {
        val time = 1_791_112_320_000L
        val current = PlanetCompassSunPathPoint(time, PlanetCompassSunPosition(180.0, 25.0))
        // An hourly dot at precisely the same place must not steal the current instant.
        val hourly = current.copy(timeMs = time - 720_000)
        val path = PlanetCompassSunDailyPath(java.time.LocalDate.parse("2026-10-04"), java.time.ZoneId.of("UTC"),
            listOf(hourly), listOf(hourly), PlanetCompassCelestialBody.MARS)
        val state = PlanetCompassSunDailyPathUiState()
        val now = mutableStateOf(time)
        val available = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(340.dp, 400.dp)) {
                    PlanetCompassSunPointingViewport(current.position, facing(current.position), Modifier.fillMaxSize(), "fixture",
                        state, dailyPath = path.takeIf { available.value }, body = path.body, timeMs = now.value)
                }
            }
        }
        for (visible in listOf(true, false)) {
            compose.runOnIdle { state.clearSelection(); now.value = time; available.value = visible }
            compose.onNodeWithTag("celestial-current-point").performTouchInput { click(center) }
            compose.onNodeWithTag("sun-path-selected").assertIsDisplayed()
            compose.runOnIdle { assertEquals(current, state.selectedCurrentFor(path.body)); now.value += 60_000 }
            compose.runOnIdle { assertEquals(current, state.selectedCurrentFor(path.body)) }
        }
    }

    @Test fun voyagerCurrentPointAlsoWorksWithoutInventingADailyPath() {
        val current = PlanetCompassSunPathPoint(1_791_112_320_000L, PlanetCompassSunPosition(180.0, -15.0))
        val state = PlanetCompassSunDailyPathUiState()
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(340.dp, 400.dp)) {
                    PlanetCompassSunPointingViewport(current.position, facing(current.position), Modifier.fillMaxSize(), "fixture",
                        state, body = PlanetCompassCelestialBody.VOYAGER_1, timeMs = current.timeMs)
                }
            }
        }
        compose.onNodeWithTag("celestial-current-point").assertHasClickAction().performClick()
        compose.onNodeWithTag("sun-path-selected").assertIsDisplayed()
        compose.onNodeWithTag("sun-daily-path").assertDoesNotExist()
        compose.runOnIdle { assertEquals(current, state.selectedCurrentFor(PlanetCompassCelestialBody.VOYAGER_1)) }
    }

    @Test fun allTargetsReuseTheirOwnUprightOffscreenPreviewInBothThemes() {
        val body = mutableStateOf(PlanetCompassCelestialBody.SUN)
        val position = mutableStateOf(PlanetCompassSunPosition(90.0, 15.0))
        val dark = mutableStateOf(false)
        val pose = facing(PlanetCompassSunPosition(180.0, 0.0))
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                Box(Modifier.size(200.dp).background(Color(0xFF203B5C)).testTag("celestial-arrow-fixture")) {
                    PlanetCompassSunPointingViewport(position.value, pose, Modifier.fillMaxSize(), "fixture",
                        rememberPlanetCompassSunDailyPathUiState(), body = body.value)
                }
            }
        }
        for (night in listOf(false, true)) for (elevation in listOf(15.0, -15.0)) {
            compose.runOnIdle { dark.value = night; position.value = PlanetCompassSunPosition(90.0, elevation) }
            for (candidate in PlanetCompassCelestialBody.entries) {
                compose.runOnIdle { body.value = candidate }
                compose.onNodeWithTag("celestial-offscreen-marker", useUnmergedTree = true)
                    .assertIsDisplayed().assertWidthIsEqualTo(56.dp).assertHeightIsEqualTo(56.dp)
                compose.onNodeWithTag("celestial-offscreen-preview-${candidate.name}", useUnmergedTree = true)
                    .assertIsDisplayed().assertWidthIsEqualTo(32.dp).assertHeightIsEqualTo(32.dp)
                compose.onNodeWithTag("celestial-offscreen-pointer", useUnmergedTree = true).assertIsDisplayed()
                compose.onNodeWithTag("celestial-live-preview-${candidate.name}", useUnmergedTree = true)
                    .assertDoesNotExist()
            }
        }
    }

    @Test fun liveAndEdgeMoonUseTheSameCurrentPhaseInBothThemesAndAcrossTheHorizon() {
        val now = mutableStateOf(java.time.Instant.parse("2024-04-08T18:21:00Z").toEpochMilli())
        val visible = mutableStateOf(true)
        val dark = mutableStateOf(false)
        val elevation = mutableStateOf(25.0)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                val position = PlanetCompassSunPosition(180.0, elevation.value)
                val orientation = facing(if (visible.value) position else PlanetCompassSunPosition(90.0, 0.0))
                Box(Modifier.size(200.dp).background(Color(0xFF203B5C))) {
                    PlanetCompassSunPointingViewport(position, orientation, Modifier.fillMaxSize(), "fixture",
                        rememberPlanetCompassSunDailyPathUiState(), body = PlanetCompassCelestialBody.MOON, timeMs = now.value)
                }
            }
        }
        for (utc in listOf("2024-04-08T18:21:00Z", "2024-04-23T23:49:00Z"))
            for (night in listOf(false, true)) for (height in listOf(25.0, -15.0))
                for (onscreen in listOf(true, false)) {
                    val instant = java.time.Instant.parse(utc).toEpochMilli()
                    val phase = calculatePlanetCompassMoonPhase(instant)
                    val description = context.getString(R.string.moon_phase_description,
                        context.getString(phase.kind.nameResource),
                        formatPlanetCompassNumber(phase.illuminatedFraction * 100, 1, PlanetCompassNumericFormat.SYSTEM))
                    compose.runOnIdle { now.value = instant; visible.value = onscreen; dark.value = night; elevation.value = height }
                    val tag = if (onscreen) "celestial-live-preview-MOON" else "celestial-offscreen-preview-MOON"
                    compose.waitUntil(10_000) {
                        compose.onAllNodes(hasTestTag(tag) and hasContentDescription(description), useUnmergedTree = true)
                            .fetchSemanticsNodes().isNotEmpty()
                    }
                    compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed()
                        .assertWidthIsEqualTo(32.dp).assertHeightIsEqualTo(32.dp)
                    if (!onscreen) compose.onNodeWithTag("celestial-offscreen-pointer", useUnmergedTree = true).assertIsDisplayed()
                }
    }
}

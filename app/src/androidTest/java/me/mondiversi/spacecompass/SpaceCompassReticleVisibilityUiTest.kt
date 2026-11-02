package me.mondiversi.spacecompass

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated preferences and synthetic pointing only; never read or change the personal app archive. */
class SpaceCompassReticleVisibilityUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun hideAndRestoreReticleAndCaptionWithoutLosingPinsOrChangingExportLabels() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val preferenceName="reticle-fixture-"+UUID.randomUUID()
        val preferences=context.getSharedPreferences(preferenceName,Context.MODE_PRIVATE)
        val now=Instant.parse("2026-10-04T10:00:00Z").toEpochMilli()
        val point=SpaceCompassSunPathPoint(now+3_600_000,SpaceCompassSunPosition(180.0,25.0))
        val path=SpaceCompassSunDailyPath(LocalDate.parse("2026-10-04"),ZoneId.of("UTC"),
            listOf(point),listOf(point),SpaceCompassCelestialBody.MOON)
        val state=SpaceCompassSunDailyPathUiState().apply { select(path,point) }
        val generation=mutableIntStateOf(0)
        preferences.edit().putString(SPACE_COMPASS_PANORAMA_LABEL_MODE_KEY,SpaceCompassPanoramaLabelMode.ALL.key).apply()
        try {
            assertTrue(readSpaceCompassReticleVisible(preferences))
            compose.setContent {
                MaterialTheme {
                    key(generation.intValue) {
                        var visible by remember { mutableStateOf(readSpaceCompassReticleVisible(preferences)) }
                        Column(Modifier.fillMaxSize()) {
                            Box(Modifier.size(340.dp,420.dp)) {
                                SpaceCompassSunPointingViewport(null,facing(point.position),Modifier.fillMaxSize(),
                                    "synthetic sky",state,dailyPath=path,body=path.body,timeMs=now,
                                    showActions=false,showReticle=visible,onToggleReticle={
                                        visible=!visible;saveSpaceCompassReticleVisible(preferences,visible)
                                    })
                            }
                        }
                    }
                }
            }
            compose.onNodeWithTag("toggle-reticle").assertDoesNotExist()
            compose.onNodeWithTag("sun-finder-reticle").assertExists()
            compose.onNodeWithTag("celestial-badge-caption").assertExists()
            tapEmptySky()
            assertFalse(readSpaceCompassReticleVisible(preferences))
            compose.onNodeWithTag("sun-finder-reticle").assertDoesNotExist()
            compose.onNodeWithTag("celestial-badge-caption").assertDoesNotExist()
            compose.onNodeWithTag("celestial-badge-reminder").assertDoesNotExist()
            compose.runOnIdle {
                assertTrue(state.isSelected(path,point))
                assertEquals(SpaceCompassPanoramaLabelMode.ALL.key,
                    preferences.getString(SPACE_COMPASS_PANORAMA_LABEL_MODE_KEY,null))
                generation.intValue++
            }
            compose.onNodeWithTag("toggle-reticle").assertDoesNotExist()
            compose.onNodeWithTag("sun-finder-reticle").assertDoesNotExist()
            tapEmptySky()
            compose.onNodeWithTag("sun-finder-reticle").assertExists()
            compose.onNodeWithTag("celestial-badge-caption").assertExists()
            compose.runOnIdle { assertTrue(state.isSelected(path,point)) }
        } finally { context.deleteSharedPreferences(preferenceName) }
    }

    private fun tapEmptySky() {
        compose.onNodeWithTag("celestial-scene-input").performTouchInput {
            click(androidx.compose.ui.geometry.Offset(width - 8f, height - 8f))
        }
    }

    @Test fun pointTapsAndDragsDoNotToggleReticleAndCoveredAreasAreInert() {
        val visible=mutableStateOf(true)
        val now=1_800_000_000_000L
        val point=SpaceCompassSunPathPoint(now,SpaceCompassSunPosition(180.0,25.0))
        val path=SpaceCompassSunDailyPath(LocalDate.of(2027,1,15),ZoneId.of("UTC"),
            listOf(point),listOf(point),SpaceCompassCelestialBody.MOON)
        val state=SpaceCompassSunDailyPathUiState()
        compose.setContent { MaterialTheme {
            SpaceCompassSunPointingViewport(null,facing(point.position),Modifier.size(340.dp,420.dp),
                "synthetic sky",state,dailyPath=path,body=path.body,timeMs=now,
                showActions=false,showReticle=visible.value,onToggleReticle={visible.value=!visible.value},
                timeBadgeExclusions=listOf(SpaceCompassSunSceneFrame(0.0,0.0,50.0,50.0)))
        } }
        val scene=compose.onNodeWithTag("celestial-scene-input")
        scene.performTouchInput { click(center) }
        compose.runOnIdle { assertTrue(state.isSelected(path,point));assertTrue(visible.value) }
        scene.performTouchInput { click(androidx.compose.ui.geometry.Offset(5f,5f)) }
        compose.runOnIdle { assertTrue(visible.value) }
        scene.performTouchInput { swipe(
            androidx.compose.ui.geometry.Offset(width*.15f,height*.85f),
            androidx.compose.ui.geometry.Offset(width*.85f,height*.85f)) }
        compose.runOnIdle { assertTrue(visible.value) }
        tapEmptySky()
        compose.runOnIdle { assertFalse(visible.value) }
        scene.performTouchInput { click(center) }
        compose.runOnIdle { assertFalse(state.isSelected(path,point));assertFalse(visible.value) }
        tapEmptySky()
        compose.runOnIdle { assertTrue(visible.value) }
    }

    private fun facing(position: SpaceCompassSunPosition): SpaceCompassSunOrientation {
        val a=Math.toRadians(position.azimuthDegrees);val e=Math.toRadians(position.elevationDegrees)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a),-sin(a),0.0),
            SpaceCompassSunVector(-sin(e)*sin(a),-sin(e)*cos(a),cos(e)),
            SpaceCompassSunVector(cos(e)*sin(a),cos(e)*cos(a),sin(e)))
    }
}

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
                            SpaceCompassReticleToggleButton(visible,{ next ->
                                saveSpaceCompassReticleVisible(preferences,next);visible=next
                            },Color.Black,Color.White)
                            Box(Modifier.size(340.dp,420.dp)) {
                                SpaceCompassSunPointingViewport(null,facing(point.position),Modifier.fillMaxSize(),
                                    "synthetic sky",state,dailyPath=path,body=path.body,timeMs=now,
                                    showActions=false,showReticle=visible)
                            }
                        }
                    }
                }
            }
            compose.onNodeWithTag("toggle-reticle").assertIsSelected()
            compose.onNodeWithTag("sun-finder-reticle").assertExists()
            compose.onNodeWithTag("celestial-badge-caption").assertExists()
            compose.onNodeWithTag("toggle-reticle").performClick().assertIsNotSelected()
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
            compose.onNodeWithTag("toggle-reticle").assertIsNotSelected()
            compose.onNodeWithTag("sun-finder-reticle").assertDoesNotExist()
            compose.onNodeWithTag("toggle-reticle").performClick().assertIsSelected()
            compose.onNodeWithTag("sun-finder-reticle").assertExists()
            compose.onNodeWithTag("celestial-badge-caption").assertExists()
            compose.runOnIdle { assertTrue(state.isSelected(path,point)) }
        } finally { context.deleteSharedPreferences(preferenceName) }
    }

    private fun facing(position: SpaceCompassSunPosition): SpaceCompassSunOrientation {
        val a=Math.toRadians(position.azimuthDegrees);val e=Math.toRadians(position.elevationDegrees)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a),-sin(a),0.0),
            SpaceCompassSunVector(-sin(e)*sin(a),-sin(e)*cos(a),cos(e)),
            SpaceCompassSunVector(cos(e)*sin(a),cos(e)*cos(a),sin(e)))
    }
}

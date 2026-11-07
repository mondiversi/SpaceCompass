package me.mondiversi.spacecompass

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.testTag
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

    @Test fun singleTapTogglesReticleAndControlsAndDoubleTapPausesTime() {
        val now=Instant.parse("2026-11-07T10:00:00Z").toEpochMilli()
        val point=SpaceCompassSunPathPoint(now+3_600_000,SpaceCompassSunPosition(180.0,25.0))
        val path=SpaceCompassSunDailyPath(LocalDate.parse("2026-11-07"),ZoneId.of("UTC"),
            listOf(point),listOf(point),SpaceCompassCelestialBody.MOON)
        val selection=SpaceCompassSunDailyPathUiState().apply { select(path,point) }
        val observer=SpaceCompassObserverState(null, elapsedClock = { 0L }).apply { active(true) }
        compose.setContent { MaterialTheme {
            var controlsVisible by remember { mutableStateOf(true) }
            SpaceCompassSunPointingViewport(null,facing(point.position),Modifier.size(340.dp,420.dp),
                "synthetic sky",selection,dailyPath=path,body=path.body,timeMs=now,
                showActions=false,showReticle=controlsVisible,showFloatingControls=controlsVisible,
                timePaused=observer.playbackPaused,onToggleTime={observer.togglePlayback(now,0L)},
                onToggleFloatingControls={controlsVisible=!controlsVisible},
                topStartActions={ Box(it.size(40.dp).testTag("fixture-floating-control")) })
        } }
        tapEmptySky()
        compose.onNodeWithTag("fixture-floating-control").assertDoesNotExist()
        compose.onNodeWithTag("sun-finder-reticle").assertDoesNotExist()
        compose.runOnIdle { assertFalse(observer.playbackPaused);assertTrue(selection.isSelected(path,point)) }
        tapEmptySky(twice=true)
        compose.runOnIdle { assertTrue(observer.playbackPaused) }
        compose.onNodeWithTag("sun-finder-reticle").assertDoesNotExist()
        tapEmptySky()
        compose.onNodeWithTag("fixture-floating-control").assertExists()
        compose.onNodeWithTag("sun-finder-reticle").assertExists()
        compose.runOnIdle { assertTrue(observer.playbackPaused) }
        tapEmptySky(twice=true)
        compose.runOnIdle { assertFalse(observer.playbackPaused);assertTrue(selection.isSelected(path,point)) }
    }

    private fun tapEmptySky(twice: Boolean = false) {
        compose.onNodeWithTag("celestial-scene-input").performTouchInput {
            val point = androidx.compose.ui.geometry.Offset(width - 8f, height - 8f)
            if (twice) doubleClick(point) else {
                click(point)
                advanceEventTime(viewConfiguration.doubleTapTimeoutMillis + 1)
            }
        }
    }

    @Test fun pointTapsAndDragsDoNotToggleTimeAndCoveredAreasAreInert() {
        val paused=mutableStateOf(false)
        val now=1_800_000_000_000L
        val point=SpaceCompassSunPathPoint(now,SpaceCompassSunPosition(180.0,25.0))
        val path=SpaceCompassSunDailyPath(LocalDate.of(2027,1,15),ZoneId.of("UTC"),
            listOf(point),listOf(point),SpaceCompassCelestialBody.MOON)
        val state=SpaceCompassSunDailyPathUiState()
        compose.setContent { MaterialTheme {
            SpaceCompassSunPointingViewport(null,facing(point.position),Modifier.size(340.dp,420.dp),
                "synthetic sky",state,dailyPath=path,body=path.body,timeMs=now,
                showActions=false,showReticle=true,timePaused=paused.value,onToggleTime={paused.value=!paused.value},
                timeBadgeExclusions=listOf(SpaceCompassSunSceneFrame(0.0,0.0,50.0,50.0)))
        } }
        val scene=compose.onNodeWithTag("celestial-scene-input")
        scene.performTouchInput { click(center) }
        compose.runOnIdle { assertTrue(state.isSelected(path,point));assertFalse(paused.value) }
        scene.performTouchInput { click(androidx.compose.ui.geometry.Offset(5f,5f)) }
        compose.runOnIdle { assertFalse(paused.value) }
        scene.performTouchInput { swipe(
            androidx.compose.ui.geometry.Offset(width*.15f,height*.85f),
            androidx.compose.ui.geometry.Offset(width*.85f,height*.85f)) }
        compose.runOnIdle { assertFalse(paused.value) }
        tapEmptySky(twice=true)
        compose.runOnIdle { assertTrue(paused.value) }
        scene.performTouchInput { click(center) }
        compose.runOnIdle { assertFalse(state.isSelected(path,point));assertTrue(paused.value) }
        tapEmptySky(twice=true)
        compose.runOnIdle { assertFalse(paused.value) }
    }

    private fun facing(position: SpaceCompassSunPosition): SpaceCompassSunOrientation {
        val a=Math.toRadians(position.azimuthDegrees);val e=Math.toRadians(position.elevationDegrees)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a),-sin(a),0.0),
            SpaceCompassSunVector(-sin(e)*sin(a),-sin(e)*cos(a),cos(e)),
            SpaceCompassSunVector(cos(e)*sin(a),cos(e)*cos(a),sin(e)))
    }
}

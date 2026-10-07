package me.mondiversi.spacecompass

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic gestures only; no real camera, location, remote data or stored preferences. */
class SpaceCompassCameraPinchZoomUiTest {
    @get:Rule val compose = createComposeRule()

    private fun pinch(start: Float, end: Float) {
        compose.onNodeWithTag("pinch-sky").performTouchInput {
            down(0, center - Offset(start, 0f))
            down(1, center + Offset(start, 0f))
            for (step in 1..12) {
                val span = start + (end - start) * step / 12f
                updatePointerTo(0, center - Offset(span, 0f))
                updatePointerTo(1, center + Offset(span, 0f))
                move(delayMillis = 16)
            }
            up(1)
            up(0)
        }
    }

    @Test fun pinchingContinuesWhileTheCameraHandsOffAndDoesNotActivateASceneTap() {
        val zoom = mutableFloatStateOf(1f)
        val ready = mutableStateOf(true)
        var taps = 0
        var updates = 0
        compose.setContent {
            Box(Modifier.size(300.dp).testTag("pinch-sky").spaceCompassCameraPinchZoom(
                SpaceCompassCameraZoomRange(.5f, 8f), zoom.floatValue, true, ready.value, {
                    zoom.floatValue = it
                    ready.value = false
                    updates++
                })) {
                Box(Modifier.fillMaxSize().clickable { taps++ })
            }
        }
        compose.onNodeWithTag("pinch-sky").performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(1, taps) }
        pinch(30f, 100f)
        compose.runOnIdle {
            assertTrue(zoom.floatValue > 2f)
            assertTrue(updates > 1)
            assertEquals(1, taps)
        }
        compose.runOnIdle { ready.value = true }
        pinch(100f, 30f)
        compose.runOnIdle {
            assertTrue(zoom.floatValue < 1.2f)
            assertEquals(1, taps)
        }
    }

    @Test fun cameraOffOrCaptureBusyDoesNotChangeZoom() {
        val enabled = mutableStateOf(false)
        var updates = 0
        compose.setContent {
            Box(Modifier.size(300.dp).testTag("pinch-sky").spaceCompassCameraPinchZoom(
                SpaceCompassCameraZoomRange(.5f, 8f), 1f, enabled.value, true, { updates++ }))
        }
        pinch(30f, 100f)
        compose.runOnIdle { assertEquals(0, updates) }
        compose.runOnIdle { enabled.value = true }
        pinch(30f, 100f)
        compose.runOnIdle { assertTrue(updates > 0) }
    }
}

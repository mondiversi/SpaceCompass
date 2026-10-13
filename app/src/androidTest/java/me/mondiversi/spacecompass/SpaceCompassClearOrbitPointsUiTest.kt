package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Local synthetic selections only; no GPS, camera, remote requests or stored preferences. */
class SpaceCompassClearOrbitPointsUiTest {
    @get:Rule val compose = createComposeRule()
    private val state = SpaceCompassSunDailyPathUiState()
    private val date = LocalDate.parse("2026-10-12")
    private val zone = ZoneId.of("UTC")
    private val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val markers = listOf(1, 12).map { SpaceCompassSunPathPoint(start + it * 3_600_000,
        SpaceCompassSunPosition(170.0, 20.0)) }
    private val moon = SpaceCompassSunDailyPath(date, zone, markers, markers, SpaceCompassCelestialBody.MOON)
    private val sun = moon.copy(body = SpaceCompassCelestialBody.SUN)
    private val dark = mutableStateOf(false)
    private fun show() {
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme(primary = Color.Cyan)
                else lightColorScheme(primary = Color.Blue)) {
                Box(Modifier.size(120.dp)) {
                    if (state.selectedPoints.isNotEmpty()) SpaceCompassClearOrbitPointsButton(state::clearSelection,
                        MaterialTheme.colorScheme.primary, if (dark.value) Color.Black else Color.White,
                        selectedCount = state.selectedPoints.size)
                }
            }
        }
    }
    @Test fun theCommandAppearsForPinsAndClearsEveryOrbitAndCurrentPointAtOnce() {
        show(); compose.onNodeWithTag("clear-orbit-points").assertDoesNotExist()
        compose.runOnIdle {
            state.select(moon, markers[0]); state.select(sun, markers[1]); state.selectCurrent(sun.body, markers[0], sun)
        }
        compose.onNodeWithTag("clear-orbit-points-count", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("clear-orbit-points").assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "3"))
            .performClick()
        compose.onNodeWithTag("clear-orbit-points").assertDoesNotExist()
        compose.onNodeWithTag("clear-orbit-points-count", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { assertTrue(state.selectedPoints.isEmpty()); assertFalse(state.isSelected(moon, markers[0])) }
    }
    @Test fun theClearPointIconUsesTheBlueOrCyanThemeAccent() {
        state.select(moon, markers[0]); show()
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark.value = night }
            val bitmap = compose.onNodeWithTag("clear-orbit-points").captureToImage().asAndroidBitmap()
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            val foreground = (if (night) Color.Cyan else Color.Blue).toArgb()
            assertTrue("The clear-point icon has visible theme accent ink", pixels.count { it == foreground } > 10)
        }
    }
}

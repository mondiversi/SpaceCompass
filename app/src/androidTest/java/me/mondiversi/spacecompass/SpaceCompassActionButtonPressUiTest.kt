package me.mondiversi.spacecompass

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated buttons only: no app preferences, network actions or saved observations. */
class SpaceCompassActionButtonPressUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun neutralDayPressPreservesItsForeground() = verifyPress(dark = false, primary = false)
    @Test fun neutralNightPressPreservesItsForeground() = verifyPress(dark = true, primary = false)
    @Test fun blueDayPressPreservesItsForeground() = verifyPress(dark = false, primary = true)
    @Test fun blueNightPressPreservesItsForeground() = verifyPress(dark = true, primary = true)
    @Test fun disabledDayButtonDoesNotReact() = verifyPress(dark = false, primary = true, enabled = false)
    @Test fun disabledNightButtonDoesNotReact() = verifyPress(dark = true, primary = false, enabled = false)

    private fun verifyPress(dark: Boolean, primary: Boolean, enabled: Boolean = true) {
        val foreground = if (dark) Color.White else Color(0xFF101418)
        val surface = if (dark) Color(0xFF1C242B) else Color.White
        val accent = if (dark) Color(0xFF55C8DB) else Color(0xFF217E9F)
        var iconColor = Color.Unspecified
        var clicks = 0
        compose.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                MaterialTheme(colorScheme = if (dark)
                    darkColorScheme(primary = accent, onSurface = foreground, surface = surface)
                else lightColorScheme(primary = accent, onSurface = foreground, surface = surface)) {
                    Box(Modifier.fillMaxWidth().background(surface).padding(16.dp)) {
                        SpaceCompassSettingsActionButton("Action", { clicks++ },
                            Modifier.testTag("action"), enabled = enabled, primary = primary) {
                            val color = LocalContentColor.current
                            SideEffect { iconColor = color }
                            SpaceCompassActionIcon(R.drawable.ic_update)
                        }
                    }
                }
            }
        }
        val button = compose.onNodeWithTag("action")
        val before = backgroundSample()
        val originalIcon = iconColor
        val bounds = button.fetchSemanticsNode().boundsInRoot
        if (enabled) assertEquals(if (primary) accent else foreground, originalIcon)
        else button.assertIsNotEnabled()

        compose.mainClock.autoAdvance = false
        button.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(500)
        try {
            // Android's ripple can use the render-thread clock rather than the Compose clock.
            if (enabled) compose.waitUntil(2_000) {
                colorDifference(before, backgroundSample()) > 0.01f
            }
            val held = backgroundSample()
            assertEquals(originalIcon, iconColor)
            assertEquals(bounds, button.fetchSemanticsNode().boundsInRoot)
            // A soft indication must never turn the entire button into an opaque fill.
            assertTrue(colorDifference(before, held) < if (enabled) 0.30f else 0.02f)
        } finally {
            button.performTouchInput { cancel() }
        }
        compose.mainClock.autoAdvance = true
        compose.waitUntil(2_000) { colorDifference(before, backgroundSample()) < 0.02f }
        assertEquals(0, clicks)
        if (!enabled) {
            button.performTouchInput { click(center) }
            assertEquals(0, clicks)
        }
    }

    private fun backgroundSample(): Color {
        val image = compose.onNodeWithTag("action").captureToImage()
        return image.toPixelMap()[(image.width * 0.93f).toInt(), image.height / 2]
    }

    private fun colorDifference(first: Color, second: Color): Float = maxOf(
        abs(first.red - second.red), abs(first.green - second.green), abs(first.blue - second.blue))
}

package me.mondiversi.spacecompass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic scenes only: no GPS, real orientation listeners, settings or archive writes. */
class SpaceCompassSunGroundAppearanceRenderingTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val down = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
        SpaceCompassSunVector(0.0, 1.0, 0.0), SpaceCompassSunVector(0.0, 0.0, -1.0))
    private val level = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, 1.0, 0.0))
    private fun centreColor(): Color {
        val image = compose.onNodeWithTag("ground-fixture").captureToImage()
        return image.toPixelMap()[image.width / 2, image.height / 2]
    }
    private fun assertColor(expected: Color, actual: Color) {
        assertEquals(expected.red, actual.red, 0.01f)
        assertEquals(expected.green, actual.green, 0.01f)
        assertEquals(expected.blue, actual.blue, 0.01f)
    }

    @Test fun allSixPhasesRenderDistinctGroundInBothAppThemesAndDoNotDependOnWeather() {
        compose.mainClock.autoAdvance = false
        val phase = mutableStateOf(SpaceCompassSunSkyPhase.NIGHT)
        val dark = mutableStateOf(false)
        val storm = mutableStateOf(false)
        val lookingDown = mutableStateOf(true)
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                val pixels = with(LocalDensity.current) { 220.dp.toPx().toDouble() }
                Box(Modifier.size(220.dp).background(MaterialTheme.colorScheme.background).testTag("ground-fixture")) {
                    SpaceCompassSunSkyBackdrop(phase.value, SpaceCompassSunWeatherSnapshot(
                        if (storm.value) SpaceCompassSunWeatherKind.STORM else SpaceCompassSunWeatherKind.CLEAR,
                        if (storm.value) 1f else 0f, 1L), Modifier.fillMaxSize())
                    SpaceCompassSunGroundBackdrop(if (lookingDown.value) down else level,
                        SpaceCompassSunSceneFrame(0.0, 0.0, pixels, pixels), phase.value, Modifier.fillMaxSize())
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        for (nightTheme in listOf(false, true)) for (item in SpaceCompassSunSkyPhase.entries) {
            compose.runOnIdle { dark.value = nightTheme; phase.value = item; lookingDown.value = true; storm.value = false }
            compose.mainClock.advanceTimeBy(2_200L)
            assertColor(Color(spaceCompassSunGroundPalette(item).nearArgb), centreColor())
            val clear = centreColor()
            compose.runOnIdle { storm.value = true }
            compose.mainClock.advanceTimeBy(2_200L)
            assertColor(clear, centreColor())
            compose.runOnIdle { lookingDown.value = false; storm.value = false }
            compose.mainClock.advanceTimeBy(2_200L)
            val bitmap = compose.onNodeWithTag("ground-fixture").captureToImage().asAndroidBitmap()
            File(context.externalCacheDir, "ground-${item.name.lowercase()}-${if (nightTheme) "dark" else "light"}.png")
                .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun solarPhaseTransitionsAnimateRatherThanInstantlyChangingTheGround() {
        compose.mainClock.autoAdvance = false
        val phase = mutableStateOf(SpaceCompassSunSkyPhase.MORNING)
        compose.setContent {
            Box(Modifier.size(180.dp).testTag("ground-fixture")) {
                SpaceCompassSunGroundBackdrop(down, SpaceCompassSunSceneFrame(0.0, 0.0, 180.0, 180.0),
                    phase.value, Modifier.fillMaxSize())
            }
        }
        compose.mainClock.advanceTimeByFrame()
        val morning = centreColor()
        compose.runOnIdle { phase.value = SpaceCompassSunSkyPhase.NIGHT }
        compose.mainClock.advanceTimeBy(1_000L)
        val transition = centreColor()
        val night = Color(spaceCompassSunGroundPalette(SpaceCompassSunSkyPhase.NIGHT).nearArgb)
        assertTrue(transition.luminance() < morning.luminance())
        assertTrue(transition.luminance() > night.luminance())
        compose.mainClock.advanceTimeBy(1_300L)
        assertColor(night, centreColor())
    }
}

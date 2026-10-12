package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import kotlin.math.*

/** Shared sky/list selections with synthetic positions; no device data or preferences. */
class SpaceCompassOrbitMultiSelectionUiTest {
    @get:Rule val compose = createComposeRule()
    private val zone = ZoneId.of("UTC")
    private val date = LocalDate.parse("2026-10-12")
    private val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val orientation = SpaceCompassSunOrientation(SpaceCompassSunVector(-1.0, 0.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, -1.0, 0.0))
    private val sun = SpaceCompassSunDailyPath(date, zone, listOf(
        SpaceCompassSunPathPoint(start, SpaceCompassSunPosition(180.0, 0.0)),
        SpaceCompassSunPathPoint(start + 24 * 3_600_000, SpaceCompassSunPosition(180.0, 0.0))), listOf(
        SpaceCompassSunPathPoint(start + 12 * 3_600_000, SpaceCompassSunPosition(165.0, 10.0)),
        SpaceCompassSunPathPoint(start + 13 * 3_600_000, SpaceCompassSunPosition(190.0, 10.0))), SpaceCompassCelestialBody.SUN)
    private val moon = sun.copy(body = SpaceCompassCelestialBody.MOON, markers = listOf(
        sun.markers[0].copy(position = SpaceCompassSunPosition(210.0, -10.0))))
    private val state = SpaceCompassSunDailyPathUiState()
    private val dark = mutableStateOf(false)
    private fun show() {
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme(primary = Color(0xFF55C8DB))
                else lightColorScheme(primary = Color(0xFF176B88))) {
                Box(Modifier.size(340.dp, 420.dp)) {
                    if (state.menuPath != null) SpaceCompassSunDailyPathPage(state,
                        MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurfaceVariant,
                        MaterialTheme.colorScheme.background, start + 11 * 3_600_000)
                    else SpaceCompassSunPointingViewport(null, orientation, Modifier.fillMaxSize(), "synthetic sky", state,
                        timeMs = start + 11 * 3_600_000, showActions = false, offscreenBody = null,
                        primaryText = MaterialTheme.colorScheme.onSurface, secondaryText = MaterialTheme.colorScheme.onSurfaceVariant,
                        backgroundColor = MaterialTheme.colorScheme.background,
                        overlays = listOf(sun, moon).associate { it.body to SpaceCompassCelestialOverlay(it.body, null, null, null, it) })
                }
            }
        }
    }
    private fun tap(point: SpaceCompassSunPathPoint) {
        val frame = compose.onNodeWithTag("sun-finder-sky").fetchSemanticsNode().boundsInRoot
        val projected = projectSpaceCompassSun(point.position, orientation, frame.width.toDouble(), frame.height.toDouble())
        compose.onNodeWithTag("sun-finder-sky").performTouchInput { click(Offset(projected.x.toFloat(), projected.y.toFloat())) }
    }
    @Test fun tapsPinMultiplePointsFromMultipleOrbitsAndNeverOpenTheDailyPage() {
        show(); tap(sun.markers[0]); tap(sun.markers[1]); tap(moon.markers[0])
        compose.onNodeWithTag("sun-path-page").assertDoesNotExist()
        compose.onAllNodesWithTag("celestial-pinned-badge").assertCountEquals(3)
        compose.runOnIdle { assertEquals(3, state.selectedPoints.size) }
        tap(sun.markers[0])
        compose.onAllNodesWithTag("celestial-pinned-badge").assertCountEquals(2)
        compose.runOnIdle { assertTrue(state.isSelected(sun, sun.markers[1])); assertTrue(state.isSelected(moon, moon.markers[0])) }
    }
    @Test fun removingOneDailyRowRemovesOnlyItsPinnedCaptionWhenReturningToTheSky() {
        show(); tap(sun.markers[0]); tap(sun.markers[1]); tap(moon.markers[0])
        compose.runOnIdle { state.openMenu(sun) }
        compose.onNodeWithTag("sun-path-point-0").assertIsSelected().performClick().assertIsNotSelected()
        compose.onNodeWithTag("sun-path-point-1").assertIsSelected()
        compose.runOnIdle { state.closeMenu() }
        compose.onAllNodesWithTag("celestial-pinned-badge").assertCountEquals(2)
        compose.runOnIdle { assertTrue(state.isSelected(moon, moon.markers[0])); state.openMenu(sun) }
        compose.onNodeWithTag("sun-path-point-0").assertIsNotSelected()
        compose.onNodeWithTag("sun-path-point-1").assertIsSelected()
    }
    @Test fun markerCentersUseTheDayAndNightAccentWhilePinnedCaptionsAvoidEachOther() {
        show(); tap(sun.markers[0]); tap(sun.markers[1]); tap(moon.markers[0])
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark.value = night }
            val frame = compose.onNodeWithTag("sun-finder-sky").fetchSemanticsNode().boundsInRoot
            val projected = projectSpaceCompassSun(sun.markers[0].position, orientation, frame.width.toDouble(), frame.height.toDouble())
            val bitmap = compose.onNodeWithTag("sun-finder-sky").captureToImage().asAndroidBitmap()
            val actual = bitmap.getPixel(projected.x.roundToInt(), projected.y.roundToInt())
            assertEquals((if (night) Color(0xFF55C8DB) else Color(0xFF176B88)).toArgb(), actual)
            val boxes = compose.onAllNodesWithTag("celestial-pinned-badge").fetchSemanticsNodes().map { it.boundsInRoot }
            assertEquals(3, boxes.size)
            boxes.forEachIndexed { i, a -> boxes.drop(i + 1).forEach { b -> assertFalse(a.overlaps(b)) } }
        }
    }
}

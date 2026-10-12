package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic snapshots only: no sensor, GPS, camera, remote requests or saved settings. */
class SpaceCompassDailyPointNavigationUiTest {
    @get:Rule val compose = createComposeRule()
    private val zone = ZoneId.of("UTC")
    private val date = LocalDate.parse("2026-10-11")
    private val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val markers = (0..23).map { SpaceCompassSunPathPoint(start + it * 3_600_000L, SpaceCompassSunPosition(180.0, it.toDouble())) }
    private val path = SpaceCompassSunDailyPath(date, zone, listOf(markers.first(), markers.last()), markers)

    private fun show(state: SpaceCompassSunDailyPathUiState, dark: MutableState<Boolean> = mutableStateOf(false)) {
        compose.setContent {
            MaterialTheme(colorScheme = if (dark.value) darkColorScheme(primary = Color(0xFF55C8DB))
                else lightColorScheme(primary = Color(0xFF176B88))) {
                Box(Modifier.fillMaxWidth().height(420.dp)) {
                    SpaceCompassSunDailyPathPage(state, MaterialTheme.colorScheme.onSurface,
                        MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.background,
                        nowMs = start + 12 * 3_600_000L)
                }
            }
        }
    }
    private fun centered(tag: String) {
        compose.waitForIdle()
        val row = compose.onNodeWithTag(tag).assertIsDisplayed().assertIsSelected().fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("sun-path-list-viewport").fetchSemanticsNode().boundsInRoot
        assertEquals(viewport.center.y, row.center.y, 2f)
    }
    @Test fun firstAndLastSceneMarkersStayVisibleWithoutExtraSpaceAndMiddleIsCentered() {
        val state = SpaceCompassSunDailyPathUiState(); state.openPoint(path, markers[0]); show(state)
        for (index in listOf(0, 12, 23)) {
            if (index != 0) compose.runOnIdle { state.closeMenu() }
            if (index != 0) compose.runOnIdle { state.openPoint(path, markers[index]) }
            compose.waitForIdle()
            val name = compose.onNodeWithTag("sun-path-object-name").assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            val date = compose.onNodeWithTag("sun-path-date").assertIsDisplayed().assertTextContains("UTC")
                .fetchSemanticsNode().boundsInRoot
            val azimuth = compose.onNodeWithTag("sun-path-angle-azimuth").assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            val elevation = compose.onNodeWithTag("sun-path-angle-elevation").assertIsDisplayed()
                .fetchSemanticsNode().boundsInRoot
            assertTrue("The date and zone must occupy the line below the object name", name.bottom <= date.top)
            assertEquals(name.left, date.left, 1f)
            assertTrue("Both angle columns must remain to the right of the object and date", date.right < azimuth.left)
            assertTrue(azimuth.right < elevation.left)
            val row = compose.onNodeWithTag("sun-path-point-$index").assertIsDisplayed().assertIsSelected()
                .fetchSemanticsNode().boundsInRoot
            val viewport = compose.onNodeWithTag("sun-path-list-viewport").fetchSemanticsNode().boundsInRoot
            when (index) {
                0 -> assertTrue("The list must start below the toolbar without a centering spacer",
                    row.top - viewport.top in 0f..viewport.height / 4)
                23 -> assertTrue("The last point must stop at the real bottom of the list",
                    viewport.bottom - row.bottom in 0f..viewport.height / 4)
                else -> centered("sun-path-point-$index")
            }
        }
    }
    @Test fun currentHasItsOwnCenteredRowEvenAtAnHourlyCoincidence() {
        val state = SpaceCompassSunDailyPathUiState(); state.openCurrent(path.body, markers[12], path); show(state)
        centered("sun-path-current")
        compose.onNodeWithTag("sun-path-point-12").assertIsNotSelected()
        compose.onNodeWithTag("sun-path-current").performClick().assertIsNotSelected()
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        assertFalse(state.isSelected(path, markers[12], true))
    }
    @Test fun highlightedBackgroundUsesTheThemeAccentAndBackPreservesTheSelection() {
        val state = SpaceCompassSunDailyPathUiState(); val dark = mutableStateOf(false)
        state.openPoint(path, markers[12]); show(state, dark)
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark.value = night }
            centered("sun-path-point-12")
            val bitmap = compose.onNodeWithTag("sun-path-point-12").captureToImage().asAndroidBitmap()
            val scheme = if (night) darkColorScheme(primary = Color(0xFF55C8DB)) else lightColorScheme(primary = Color(0xFF176B88))
            val pageBackground = if (night) scheme.background else Color.White
            val expected = scheme.primary.copy(alpha = .16f).compositeOver(pageBackground).toArgb()
            val actual = bitmap.getPixel(3, bitmap.height / 2)
            for (shift in listOf(0, 8, 16)) assertTrue(kotlin.math.abs(((expected shr shift) and 255) - ((actual shr shift) and 255)) <= 2)
        }
        compose.runOnIdle { state.closeMenu() }
        compose.onNodeWithTag("sun-path-page").assertDoesNotExist()
        compose.runOnIdle { state.openMenu(path) }
        compose.onNodeWithTag("sun-path-point-12").assertIsSelected()
    }
}

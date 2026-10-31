package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Compile on personal devices; execute only in an isolated emulator/test application. */
class SpaceCompassObjectPageScrollUiTest {
    @get:Rule val compose = createComposeRule()
    private val session = SpaceCompassObjectPageScrollSession()
    private val body = mutableStateOf(SpaceCompassCelestialBody.SUN)
    private val page = mutableStateOf(SpaceCompassObjectPage.OBSERVATORY)
    private val visible = mutableStateOf(true)
    private val count = mutableIntStateOf(80)
    private val height = mutableIntStateOf(240)
    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalSpaceCompassObjectPageScrollSession provides session) {
                MaterialTheme {
                    if (visible.value) {
                        val entry = rememberSpaceCompassObjectPageScroll(body.value, page.value).entry
                        Column(Modifier.size(300.dp, height.intValue.dp).verticalScroll(entry.scroll).testTag("session-list")) {
                            repeat(count.intValue) { Text("Row $it", Modifier.height(40.dp)) }
                        }
                    }
                }
            }
        }
    }
    private fun offset() = session.open(body.value, page.value).entry.scroll.value

    @Test fun reopenRestoresEachObjectAndEachPageIndependently() {
        show()
        compose.onNodeWithText("Row 40").performScrollTo()
        var saved = 0
        compose.runOnIdle { saved = offset(); visible.value = false }
        compose.waitForIdle()
        compose.runOnIdle { body.value = SpaceCompassCelestialBody.MOON; visible.value = true }
        compose.runOnIdle { assertEquals(0, offset()) }
        compose.onNodeWithText("Row 20").performScrollTo()
        compose.runOnIdle { page.value = SpaceCompassObjectPage.DAILY_PATH }
        compose.runOnIdle { assertEquals(0, offset()) }
        compose.onNodeWithText("Row 10").performScrollTo()
        compose.runOnIdle { body.value = SpaceCompassCelestialBody.SUN; page.value = SpaceCompassObjectPage.OBSERVATORY }
        compose.waitUntil { offset() == saved }
        compose.onNodeWithText("Row 40").assertIsDisplayed()
    }

    @Test fun asynchronousContentAndResponsiveRemeasurementKeepTheSavedOffset() {
        show()
        compose.onNodeWithText("Row 60").performScrollTo()
        var saved = 0
        compose.runOnIdle { saved = offset(); visible.value = false }
        compose.waitForIdle()
        compose.runOnIdle { count.intValue = 4; visible.value = true }
        compose.runOnIdle { assertEquals(0, offset()) }
        compose.runOnIdle { count.intValue = 80 }
        compose.waitUntil { offset() == saved }
        compose.runOnIdle { height.intValue = 400 }
        compose.runOnIdle { assertEquals(saved, offset()) }
        compose.onNodeWithText("Row 60").assertIsDisplayed()
    }
}

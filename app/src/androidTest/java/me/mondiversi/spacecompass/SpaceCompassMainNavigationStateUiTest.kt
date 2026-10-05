package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso.pressBack
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Navigation fixture owns no GPS/network/preferences: inspect actual composition lifetime. */
class SpaceCompassMainNavigationStateUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun settingsRoundTripsKeepRememberedDataAndDoNotDisposeMainOwner() {
        var starts = 0
        var disposals = 0
        compose.setContent { MaterialTheme {
            SpaceCompassAppPages {
                val navigate = LocalSpaceCompassNavigate.current
                val generation = remember { ++starts }
                DisposableEffect(Unit) { onDispose { disposals++ } }
                Column {
                    Text("Loaded $generation", Modifier.testTag("loaded-main"))
                    Button(onClick = { navigate("info") }, modifier = Modifier.testTag("open-info-fixture")) { Text("Info") }
                }
            }
        } }
        repeat(3) {
            compose.onNodeWithTag("loaded-main").assertTextEquals("Loaded 1")
            compose.onNodeWithTag("open-info-fixture").performClick()
            compose.onNodeWithTag("page-info").assertIsDisplayed()
            compose.onNodeWithTag("loaded-main").assertIsNotDisplayed()
            compose.runOnIdle { assertEquals(1, starts); assertEquals(0, disposals) }
            pressBack()
            compose.onNodeWithTag("loaded-main").assertIsDisplayed().assertTextEquals("Loaded 1")
        }
        compose.runOnIdle { assertEquals(1, starts); assertEquals(0, disposals) }
    }
}

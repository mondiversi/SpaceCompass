package me.mondiversi.spacecompass

import android.content.res.Configuration
import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Live refresh regressions, using synthetic readings without sensor or archive access. */
class SpaceCompassSunDailyPathPopupTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
        .apply { setLocale(Locale.ITALIAN) }).resources
    private val location = mutableStateOf<Location?>(fix())
    private val time = mutableStateOf(Instant.parse("2026-10-03T07:00:00Z").toEpochMilli())
    private val landscape = mutableStateOf(false)
    private val reliable = mutableStateOf(true)
    private val weather = mutableStateOf(SpaceCompassSunWeatherReading())

    private fun fix(latitude: Double = 45.0, altitude: Double = 100.0) = Location("synthetic-popup").apply {
        this.latitude = latitude; longitude = 9.0; this.altitude = altitude; accuracy = 5f
    }

    private fun open() {
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalSpaceCompassTimeFormat provides SpaceCompassTimeFormat.H24) {
                MaterialTheme {
                    val baseDensity = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides if (landscape.value)
                        Density(baseDensity.density * 0.40f, baseDensity.fontScale) else baseDensity) {
                        Box(if (landscape.value) Modifier.size(800.dp, 380.dp) else Modifier.fillMaxSize()) {
                            SpaceCompassSunFinderContent(SpaceCompassSunFinderReadings(location = location.value,
                                locationStatus = if (location.value == null) SpaceCompassSunLocationStatus.SEARCHING
                                    else SpaceCompassSunLocationStatus.READY,
                                compassAvailable = true, compassReliable = reliable.value), time.value,
                                Color.Black, Color.DarkGray, Color.White, weather.value, onDismissRequest = {})
                        }
                    }
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
    }

    @Test fun popupStaysOpenDuringTemporaryLocationLossAndRecovery() {
        open()
        compose.onNodeWithText("12:00", substring = true).performScrollTo()
        val before = compose.onNodeWithText("12:00", substring = true).fetchSemanticsNode().boundsInRoot.top
        compose.runOnIdle { location.value = null }
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        compose.onNodeWithText("12:00", substring = true).assertIsDisplayed()
        assertEquals(before, compose.onNodeWithText("12:00", substring = true).fetchSemanticsNode().boundsInRoot.top, 1f)
        compose.runOnIdle { location.value = fix(45.001, 130.0) }
        compose.waitForIdle()
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        compose.onNodeWithText("12:00", substring = true).assertIsDisplayed()
    }

    @Test fun repeatedGpsAndAltitudeRefreshesPreserveTheConsultedValuesAndScrollPosition() {
        open()
        val row = compose.onNode(hasText("12:00", substring = true) and hasClickAction())
        row.performScrollTo()
        val labels = row.fetchSemanticsNode().config[SemanticsProperties.Text]
        val top = row.fetchSemanticsNode().boundsInRoot.top
        repeat(10) { index ->
            compose.runOnIdle { location.value = fix(45.0002 + index * 0.0002, 110.0 + index * 10) }
            compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
            row.assertIsDisplayed()
            assertEquals(labels, row.fetchSemanticsNode().config[SemanticsProperties.Text])
            assertEquals(top, row.fetchSemanticsNode().boundsInRoot.top, 1f)
        }
    }

    @Test fun clockWeatherAndCompassUpdatesDoNotDismissThePopup() {
        open()
        repeat(20) { index ->
            compose.runOnIdle {
                time.value += 2_000
                reliable.value = index % 2 == 0
                weather.value = SpaceCompassSunWeatherReading(loading = index % 2 != 0)
            }
            compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        }
    }

    @Test fun midnightDoesNotReplaceTheOpenListButReopeningUsesTheNewDay() {
        open()
        val dateRow = compose.onNode(hasText(ZoneId.systemDefault().id, substring = true))
        val originalDate = dateRow.fetchSemanticsNode().config[SemanticsProperties.Text]
        compose.runOnIdle { time.value += 86_400_000 }
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        assertEquals(originalDate, dateRow.fetchSemanticsNode().config[SemanticsProperties.Text])
        compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("sun-path-open").performClick()
        org.junit.Assert.assertNotEquals(originalDate, dateRow.fetchSemanticsNode().config[SemanticsProperties.Text])
    }

    @Test fun switchingPortraitAndLandscapeKeepsThePopupAndVisibilityChoice() {
        open()
        compose.onNode(isToggleable()).performClick().assertIsOff()
        for (wide in listOf(true, false, true, false)) {
            compose.runOnIdle { landscape.value = wide }
            compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
            compose.onNode(isToggleable()).assertIsOff()
        }
        compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNode(isToggleable()).assertIsOff()
    }

    @Test fun closingThePopupRemainsEffectiveAfterGpsAndClockRefreshes() {
        open()
        compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        compose.runOnIdle { location.value = null; time.value += 2_000 }
        compose.onNodeWithTag("sun-path-page").assertDoesNotExist()
        compose.runOnIdle { location.value = fix(45.01, 150.0) }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("sun-path-page").assertDoesNotExist()
    }
}

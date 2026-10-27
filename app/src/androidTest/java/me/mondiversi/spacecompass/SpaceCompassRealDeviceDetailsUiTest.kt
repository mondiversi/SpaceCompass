package me.mondiversi.spacecompass

import android.content.Context
import android.location.Location
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.util.UUID

/** Distinct synthetic scene/device inputs; no real GPS, provider downloads or personal preferences. */
class SpaceCompassRealDeviceDetailsUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferenceName = "test_real_device_" + UUID.randomUUID()
    private val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    private val shown = mutableStateOf(true)
    private val realTime = System.currentTimeMillis()
    private val sceneTime = realTime + 2 * 86_400_000L
    private val realPose = SpaceCompassSunOrientation(SpaceCompassSunVector(0.0, -1.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(1.0, 0.0, 0.0))
    private val scenePose = SpaceCompassSunOrientation(SpaceCompassSunVector(0.0, 1.0, 0.0),
        SpaceCompassSunVector(.707106781, 0.0, .707106781), SpaceCompassSunVector(-.707106781, 0.0, .707106781))

    private fun show(realFix: Location?) {
        val sceneFix = Location("scenario fixture").apply { latitude = 40.7; longitude = -74.0; altitude = 3000.0 }
        val observer = SpaceCompassObserverState(null, elapsedClock = { 0L }).apply {
            apply(spaceCompassObserverPlan(40.7, -74.0, 3000.0, sceneTime, "America/New_York")!!)
        }
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalSpaceCompassPreferences provides preferences,
                LocalSpaceCompassNumericFormat provides SpaceCompassNumericFormat.INTERNATIONAL,
                LocalSpaceCompassUnits provides SpaceCompassUnits(distance = "mkm"),
                LocalSpaceCompassObserver provides observer) {
                if (shown.value) SpaceCompassSunFinderContent(
                    SpaceCompassSunFinderReadings(sceneFix, SpaceCompassSunLocationStatus.READY, scenePose, true, true),
                    sceneTime, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.background,
                    weather = SpaceCompassSunWeatherReading(SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.STORM, 1f, sceneTime)),
                    deviceReadings = SpaceCompassSunFinderReadings(realFix,
                        if (realFix == null) SpaceCompassSunLocationStatus.SEARCHING else SpaceCompassSunLocationStatus.READY,
                        realPose, true, true), deviceTimeMs = realTime,
                    deviceWeather = SpaceCompassSunWeatherReading(SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.CLEAR, 0f, realTime)),
                    resumed = false, onDismissRequest = {})
            }
        } }
    }

    @After fun disposeFixture() {
        compose.runOnIdle { shown.value = false }
        compose.waitForIdle()
        context.deleteSharedPreferences(preferenceName)
    }

    @Test fun compassIslandUsesTheRealAltitudeAndAttitudeDuringAScenario() {
        show(Location("device fixture").apply {
            latitude = 45.0; longitude = 9.0; altitude = 112.0; accuracy = 5f; verticalAccuracyMeters = 8f
        })
        compose.onNodeWithTag("sun-data-altitude", useUnmergedTree = true).assertTextEquals("112 m (±8 m)")
        compose.onNodeWithTag("sun-data-heading", useUnmergedTree = true).assertTextContains("90", substring = true)
        compose.onNodeWithTag("sun-data-tilt", useUnmergedTree = true).assertTextContains("0", substring = true)
    }

    @Test fun missingRealGpsNeverFallsBackToTheSimulatedPositionInDetails() {
        show(null)
        compose.onNodeWithTag("sun-data-altitude", useUnmergedTree = true).assertTextEquals("—")
        compose.onNode(hasClickAction() and hasAnyDescendant(hasTestTag("sun-finder-info")),
            useUnmergedTree = true).performClick()
        for (tag in listOf("sun-info-coordinates-value", "sun-info-altitude-value")) {
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withTagValue(
                org.hamcrest.Matchers.`is`(tag))).check(androidx.test.espresso.assertion.ViewAssertions.matches(
                    androidx.test.espresso.matcher.ViewMatchers.withText("—")))
        }
    }

    @Test fun realCoordinatesAndLiveWeatherRemainIndependentOfAFutureScene() {
        show(Location("device fixture").apply {
            latitude = 45.0; longitude = 9.0; altitude = 112.0; accuracy = 5f; verticalAccuracyMeters = 8f
        })
        compose.onNode(hasClickAction() and hasAnyDescendant(hasTestTag("sun-finder-info")),
            useUnmergedTree = true).performClick()
        val expectedCoordinates = formatSpaceCompassSelectedCoordinates(45.0, 9.0,
            SpaceCompassNumericFormat.INTERNATIONAL, false)!!
        val expectedWeather = context.getString(spaceCompassWeatherValueResource(
            SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.CLEAR, 0f, realTime), false))
        for ((tag, value) in listOf("sun-info-coordinates-value" to expectedCoordinates,
            "sun-info-altitude-value" to "112 m (±8 m)", "celestial-environment-weather-value" to expectedWeather)) {
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withTagValue(
                org.hamcrest.Matchers.`is`(tag))).check(androidx.test.espresso.assertion.ViewAssertions.matches(
                    androidx.test.espresso.matcher.ViewMatchers.withText(value)))
        }
    }
}

package me.mondiversi.spacecompass

import android.content.Context
import android.content.res.Configuration
import android.location.Location
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.ZoneId
import java.util.UUID

/** Effective scene fixtures, independent of device GPS, downloads and personal preferences. */
class SpaceCompassScenarioSummaryUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferenceName = "test_scenario_summary_" + UUID.randomUUID()
    private val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    private val shown = mutableStateOf(true)
    private val moment = System.currentTimeMillis()

    @After fun disposeFixture() {
        compose.runOnIdle { shown.value = false }
        compose.waitForIdle()
        context.deleteSharedPreferences(preferenceName)
    }

    private fun dialog(plan: SpaceCompassObserverPlan? = null,
        weather: State<SpaceCompassSunWeatherReading>, dark: Boolean = false,
        place: State<SpaceCompassEstimatedPlaceReading> = mutableStateOf(SpaceCompassEstimatedPlaceReading()), onCustomize: () -> Unit = {}, onDismiss: () -> Unit = {}) {
        compose.setContent { MaterialTheme {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(LocalConfiguration provides configuration,
                LocalLayoutDirection provides LayoutDirection.Ltr,
                LocalSpaceCompassNumericFormat provides SpaceCompassNumericFormat.INTERNATIONAL,
                LocalSpaceCompassUnits provides SpaceCompassUnits(distance = "mkm")) {
                if (shown.value) SpaceCompassScenarioSummaryDialog(plan, 40.7, -74.0, 112.0,
                    moment, ZoneId.of("America/New_York"), weather.value, placeReading = place.value, deviceTimeMs = moment, deviceZone = ZoneId.of("America/New_York"),
                    onCustomize = onCustomize, onDismiss = onDismiss)
            }
        } }
    }

    @Test fun realDefaultsAreSummarizedEvenWithoutAnActiveOverrideAndActionsStayOrdered() {
        var customized = false
        var dismissed = false
        val weather = mutableStateOf(SpaceCompassSunWeatherReading(
            SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.CLEAR, 0f, moment)))
        dialog(weather = weather, onCustomize = { customized = true }, onDismiss = { dismissed = true })
        for (key in listOf("position", "altitude", "zone", "date", "time", "speed", "weather")) {
            compose.onNodeWithTag("observer-summary-$key-value", useUnmergedTree = true).assertExists()
        }
        compose.onNodeWithTag("observer-summary-altitude-value", useUnmergedTree = true).assertTextEquals("112.00 m")
        compose.onNodeWithTag("observer-summary-weather-value", useUnmergedTree = true).assertTextEquals(
            context.getString(spaceCompassWeatherValueResource(weather.value.snapshot, false)))
        compose.onNodeWithText(context.getString(R.string.scene_scenario_off)).assertDoesNotExist()
        compose.onNodeWithTag("observer-summary-speed-value", useUnmergedTree = true).assertTextEquals(
            context.getString(R.string.scenario_time_forward) + " · 1×")
        val close = compose.onNodeWithTag("scenario-summary-close").fetchSemanticsNode().boundsInRoot
        val customize = compose.onNodeWithTag("scenario-summary-customize").fetchSemanticsNode().boundsInRoot
        assertTrue("Customize precedes Close in the right-aligned action row", customize.right <= close.left)
        compose.onNodeWithTag("scenario-summary-close").performClick()
        compose.runOnIdle { assertTrue(dismissed); assertFalse(customized) }
        compose.onNodeWithTag("scenario-summary-customize").performClick()
        compose.runOnIdle { assertTrue(customized) }
    }

    @Test fun configuredHeightAndScenarioWeatherRemainDistinctFromRealDefaultsAndMissingWeatherIsExplicit() {
        val plan = spaceCompassObserverPlan(40.7, -74.0, 3000.0, moment, "America/New_York")!!
        val weather = mutableStateOf(SpaceCompassSunWeatherReading(loading = true))
        dialog(plan, weather)
        compose.onNodeWithTag("observer-summary-weather-automatic", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("observer-summary-altitude-value", useUnmergedTree = true).assertTextEquals("3000.00 m")
        compose.onNodeWithTag("observer-summary-weather-value", useUnmergedTree = true).assertTextEquals(
            context.getString(spaceCompassWeatherValueResource(null, true)))
        compose.runOnIdle { weather.value = SpaceCompassSunWeatherReading(failed = true) }
        compose.onNodeWithTag("observer-summary-weather-value", useUnmergedTree = true).assertTextEquals(
            context.getString(spaceCompassWeatherValueResource(null, false)))
        val storm = SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.STORM, 1f, moment)
        compose.runOnIdle { weather.value = SpaceCompassSunWeatherReading(storm) }
        compose.onNodeWithTag("observer-summary-weather-value", useUnmergedTree = true).assertTextEquals(
            context.getString(spaceCompassWeatherValueResource(storm, false)))
    }

    @Test fun aLatePlaceNameDoesNotMoveThePositionRowOrFollowingValues() {
        val place = mutableStateOf(SpaceCompassEstimatedPlaceReading(loading = true))
        dialog(weather = mutableStateOf(SpaceCompassSunWeatherReading()), place = place)
        val before = compose.onNodeWithTag("observer-summary-position", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val altitude = compose.onNodeWithTag("observer-summary-altitude", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { place.value = SpaceCompassEstimatedPlaceReading("Morro Jable, Canarias, Spagna") }
        val after = compose.onNodeWithTag("observer-summary-position", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val next = compose.onNodeWithTag("observer-summary-altitude", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(before.height, after.height, 0f)
        assertEquals(altitude.top, next.top, 0f)
    }

    @Test fun inferredHeightAndZoneAreOrangeWhileTheLabelsRemainNeutral() {
        val plan = spaceCompassObserverPlan(40.7, -74.0, 900.0, moment, "America/New_York",
            simulatePosition = true, simulateTime = false, simulateAltitude = false, simulateZone = false,
            automaticAltitudeMeters = 112.0, automaticZoneId = "America/New_York", simulateDate = false)!!
        dialog(plan, mutableStateOf(SpaceCompassSunWeatherReading()))
        for (key in listOf("altitude", "zone")) {
            assertTextColor(key, spaceCompassAttentionColor(false))
            compose.onNodeWithTag("observer-summary-$key-automatic", useUnmergedTree = true).assertExists()
            val layouts = mutableListOf<TextLayoutResult>()
            compose.onNodeWithTag("observer-summary-$key-label", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(Color(0xFF101418).copy(alpha = .72f), layouts.single().layoutInput.style.color)
        }
        compose.onNodeWithTag("observer-summary-altitude-value", useUnmergedTree = true).assertTextEquals("112.00 m")
        compose.runOnIdle { assertFalse(plan.simulateAltitude || plan.simulateZone) }
    }

    private fun assertTextColor(key: String, expected: Color) {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("observer-summary-$key-value", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(expected, results.single().layoutInput.style.color)
    }

    private fun checkOnlyActiveFieldsAreOrange(dark: Boolean) {
        val plan = spaceCompassObserverPlan(40.7, -74.0, 3000.0, moment, "America/New_York",
            simulatePosition = false, simulateTime = false, simulateAltitude = true,
            simulateZone = false, simulateDate = false)!!
        dialog(plan, mutableStateOf(SpaceCompassSunWeatherReading()), dark = dark)
        assertTextColor("altitude", spaceCompassAttentionColor(dark))
        assertTextColor("position", if (dark) Color.White else Color(0xFF101418))
        assertTextColor("speed", if (dark) Color.White else Color(0xFF101418))
    }

    @Test fun enabledFieldsUseTheDaytimeAttentionTone() = checkOnlyActiveFieldsAreOrange(false)
    @Test fun enabledFieldsUseTheNighttimeAttentionTone() = checkOnlyActiveFieldsAreOrange(true)

    @Test fun backwardsTimeShowsItsDirectionAndCommittedRate() {
        val plan = spaceCompassObserverPlan(40.7, -74.0, 3000.0, moment, "America/New_York")!!.also {
            it.scenarioClock = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(enabled = true, rate = -60),
                "summary reverse fixture", moment, moment, 0L, elapsed = { 0L })
        }
        dialog(plan, mutableStateOf(SpaceCompassSunWeatherReading()))
        compose.onNodeWithTag("observer-summary-speed-value", useUnmergedTree = true).assertTextEquals(
            context.getString(R.string.scenario_time_backward) + " · −60×")
        assertTextColor("speed", spaceCompassAttentionColor(false))
    }

    @Test fun pausedTimeIsStillIncludedAndDoesNotPretendToAdvance() {
        val plan = spaceCompassObserverPlan(40.7, -74.0, 3000.0, moment, "America/New_York")!!.also {
            it.scenarioClock = SpaceCompassScenarioClock(SpaceCompassScenarioTimeSettings(enabled = true, rate = 0),
                "summary paused fixture", moment, moment, 0L, elapsed = { 0L })
        }
        dialog(plan, mutableStateOf(SpaceCompassSunWeatherReading()))
        compose.onNodeWithTag("observer-summary-speed-value", useUnmergedTree = true).assertTextEquals(
            context.getString(R.string.scenario_time_paused) + " · 0×")
    }

    @Test fun summaryReappearsAfterEditingAndExplicitCloseDisarmsIt() {
        val route = mutableStateOf<String?>(null)
        val fix = Location("scenario summary fixture").apply { latitude = 45.0; longitude = 9.0; altitude = 112.0 }
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalSpaceCompassPreferences provides preferences,
                LocalSpaceCompassNavigate provides { route.value = it },
                LocalSpaceCompassMainVisible provides (route.value == null)) {
                if (shown.value) SpaceCompassSunFinderContent(
                    SpaceCompassSunFinderReadings(fix, SpaceCompassSunLocationStatus.READY, null, false, false),
                    moment, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurfaceVariant,
                    MaterialTheme.colorScheme.background, resumed = false, onDismissRequest = {})
            }
        } }
        compose.onNodeWithTag("open-scenario").performClick()
        compose.onNodeWithTag("scenario-summary-dialog").assertIsDisplayed()
        compose.runOnIdle { assertNull(route.value) }
        compose.onNodeWithTag("scenario-summary-customize").performClick()
        compose.onNodeWithTag("scenario-summary-dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals("observer", route.value); route.value = null }
        compose.onNodeWithTag("scenario-summary-dialog").assertIsDisplayed()
        compose.onNodeWithTag("scenario-summary-close").performClick()
        compose.onNodeWithTag("scenario-summary-dialog").assertDoesNotExist()
        compose.runOnIdle { route.value = "info" }
        compose.runOnIdle { route.value = null }
        compose.onNodeWithTag("scenario-summary-dialog").assertDoesNotExist()
    }
}

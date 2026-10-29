package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/** Disposable editor: no device preferences, provider requests or real GPS. */
class SpaceCompassObserverAutosaveUiTest {
    @get:Rule val compose = createComposeRule()
    private val initial = spaceCompassObserverPlan(28.35, -14.1, 102.5,
        Instant.parse("2027-04-06T20:00:00Z").toEpochMilli(), "Atlantic/Canary",
        simulatePosition = false, simulateTime = true)!!
    private val observer = SpaceCompassObserverState(null).apply { apply(initial) }
    private val visible = mutableStateOf(true)
    private fun show() {
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalSpaceCompassObserver provides observer) {
                Column(Modifier.fillMaxSize()) { if (visible.value) SpaceCompassObserverPage(Modifier.weight(1f)) }
            }
        } }
        compose.onNodeWithTag("observer-apply").assertDoesNotExist()
    }

    private fun open(section: String) {
        compose.onNodeWithTag("scenario-section-$section").performScrollTo().performClick()
        compose.onNodeWithTag("scenario-export-open").assertDoesNotExist()
        compose.onNodeWithTag("scenario-import-open").assertDoesNotExist()
    }
    private fun menu() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithContentDescription(context.getString(R.string.navigate_back)).performClick()
        compose.onNodeWithTag("scenario-management-menu").assertExists()
    }

    @Test fun openingDoesNotRewriteTheSavedMomentAndOnlyMenuCheckboxesToggleOverrides() {
        show()
        compose.runOnIdle { assertEquals(initial,observer.plan) }
        compose.onNodeWithTag("scenario-toggle-time").performClick()
        compose.runOnIdle { assertTrue(observer.plan!!.simulateDate);assertFalse(observer.plan!!.simulateTime) }
        open("time")
        compose.onNodeWithTag("observer-enabled-time").assertDoesNotExist()
        compose.onNodeWithTag("observer-time").assertIsEnabled()
        menu()
        compose.onNodeWithTag("scenario-toggle-date").performClick()
        compose.runOnIdle { assertNull(observer.plan);assertNotNull(observer.savedPlan) }
        compose.onNodeWithTag("scenario-toggle-time").performClick()
        compose.runOnIdle { assertTrue(observer.plan!!.simulateTime);assertFalse(observer.plan!!.simulateDate) }
    }

    @Test fun completeTextChangeAutosavesAndIncompleteDateRetainsTheLastValidPlan() {
        show(); open("date")
        compose.onNodeWithTag("observer-date").performScrollTo().performTextReplacement("2027-04-07")
        val expected = spaceCompassObserverMoment("2027-04-07", Instant.ofEpochMilli(initial.timeMs)
            .atZone(ZoneId.systemDefault()).toLocalTime().toString().take(5), ZoneId.systemDefault().id)!!
        compose.waitUntil(10_000) { observer.plan?.timeMs == expected }
        compose.onNodeWithTag("observer-date").performScrollTo().performTextReplacement("2027-02-30")
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("observer-validation-error").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { assertEquals(expected, observer.plan!!.timeMs) }
        menu(); open("time"); menu(); open("date")
        compose.onNodeWithTag("observer-date").assertTextContains("2027-02-30")
        compose.runOnIdle { assertEquals(expected, observer.plan!!.timeMs) }
    }

    @Test fun leavingBeforeTheTypingPauseFlushesCompleteValues() {
        show(); open("time")
        compose.onNodeWithTag("observer-time").performScrollTo().performTextReplacement("21:05")
        compose.runOnIdle { visible.value = false }
        compose.runOnIdle {
            val date = Instant.ofEpochMilli(initial.timeMs).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            assertEquals(spaceCompassObserverMoment(date, "21:05", ZoneId.systemDefault().id), observer.plan!!.timeMs)
        }
    }
    @Test fun inactiveDateStaysEditableAndPreparedValuesWaitForTheMenuCheckbox() {
        show()
        compose.onNodeWithTag("scenario-toggle-date").performClick().assertIsOff()
        open("date")
        compose.onNodeWithTag("observer-enabled-date").assertDoesNotExist()
        compose.onNodeWithTag("observer-date").assertIsDisplayed().assertIsEnabled().performTextReplacement("2030-08-12")
        compose.onNodeWithTag("observer-current-date").assertIsEnabled()
        compose.waitUntil(10_000) { observer.savedPlan?.let { Instant.ofEpochMilli(it.timeMs).atZone(ZoneId.of(it.momentZoneId)).toLocalDate().toString() }=="2030-08-12" }
        compose.runOnIdle { assertFalse(observer.plan!!.simulateDate) }
        menu()
        compose.onNodeWithTag("scenario-toggle-date").assertIsOff().performClick().assertIsOn()
        open("date")
        compose.onNodeWithTag("observer-date").assertTextContains("2030-08-12")
    }

    @Test fun childToolbarsHaveNoActivationCheckboxAndResumptionIsAlwaysCheckedAndDisabled() {
        show()
        compose.onNodeWithTag("scenario-toggle-date").assertIsOn().performClick().assertIsOff()
        open("date")
        compose.onNodeWithTag("observer-enabled-date").assertDoesNotExist()
        compose.onNodeWithTag("observer-date").assertIsEnabled()
        menu()
        compose.onNodeWithTag("scenario-toggle-date").performClick().assertIsOn()
        compose.onNodeWithTag("scenario-resume-required").performScrollTo().assertIsOn().assertIsNotEnabled()
        compose.onNodeWithTag("scenario-toggle-resume").assertDoesNotExist()
        open("resume")
        compose.onNodeWithTag("scenario-resume-choices").assertExists()
        compose.onNodeWithTag("scenario-resume-ALWAYS").assertIsEnabled()
    }

}

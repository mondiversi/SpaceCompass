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

    @Test fun openingDoesNotRewriteTheSavedMomentAndCheckboxesSaveWithoutLeaving() {
        show()
        compose.runOnIdle { assertEquals(initial, observer.plan) }
        open("time")
        compose.onNodeWithTag("observer-enabled-time").performClick()
        compose.runOnIdle { assertTrue(observer.plan!!.simulateDate); assertFalse(observer.plan!!.simulateTime) }
        menu(); open("date")
        compose.onNodeWithTag("observer-enabled-date").performClick()
        compose.runOnIdle { assertNull(observer.plan); assertNotNull(observer.savedPlan) }
        menu(); open("time")
        compose.onNodeWithTag("observer-enabled-time").performClick()
        compose.runOnIdle { assertTrue(observer.plan!!.simulateTime); assertFalse(observer.plan!!.simulateDate) }
        compose.onNodeWithTag("observer-page").assertExists()
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
    @Test fun inactiveDateKeepsItsCompleteFormVisibleButDisablesItsActions() {
        show(); open("date")
        compose.onNodeWithTag("observer-enabled-date").performClick().assertIsOff()
        compose.onNodeWithTag("observer-date").assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithTag("observer-current-date").assertExists().assertIsNotEnabled()
        compose.onNodeWithTag("observer-enabled-date").performClick().assertIsOn()
        compose.onNodeWithTag("observer-date").assertIsEnabled()
        compose.onNodeWithTag("observer-current-date").assertIsEnabled()
    }

}

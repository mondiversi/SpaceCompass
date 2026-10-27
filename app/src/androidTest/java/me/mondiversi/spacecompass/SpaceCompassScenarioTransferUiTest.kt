package me.mondiversi.spacecompass

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.util.UUID

/** Isolated preferences; no provider calls, file picker automation or actual personal scenario changes. */
class SpaceCompassScenarioTransferUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val preferenceName = "test_scenario_transfer_" + UUID.randomUUID()
    private val preferences = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE)
    private val shown = mutableStateOf(true)
    private val observer = SpaceCompassObserverState(preferences, elapsedClock = { 0L })
    private val moment = Instant.parse("2027-04-06T20:00:00Z").toEpochMilli()
    private val original = spaceCompassObserverPlan(45.0, 9.0, 112.0, moment, "Europe/Rome",
        simulatePosition = false, simulateTime = false, simulateAltitude = true, simulateZone = false, simulateDate = false)!!

    private fun show() {
        observer.apply(original)
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalSpaceCompassPreferences provides preferences,
                LocalSpaceCompassObserver provides observer,
                LocalSpaceCompassNumericFormat provides SpaceCompassNumericFormat.INTERNATIONAL,
                LocalSpaceCompassUnits provides SpaceCompassUnits(distance = "mkm")) {
                if (shown.value) SpaceCompassObserverPage()
            }
        } }
    }

    @After fun dispose() {
        compose.runOnIdle { shown.value = false }
        compose.waitForIdle(); context.deleteSharedPreferences(preferenceName)
    }

    @Test fun toolbarActionsFollowResumeExportImportAndTheExportDialogOffersThreeActions() {
        show()
        val resume = compose.onNodeWithTag("scenario-resume-open").fetchSemanticsNode().boundsInRoot
        val export = compose.onNodeWithTag("scenario-export-open").fetchSemanticsNode().boundsInRoot
        val load = compose.onNodeWithTag("scenario-import-open").fetchSemanticsNode().boundsInRoot
        assertTrue(resume.right <= export.left && export.right <= load.left)
        compose.onNodeWithTag("scenario-export-open").performClick()
        compose.onNodeWithTag("scenario-export-dialog").assertIsDisplayed()
        compose.onNodeWithTag("scenario-export-name").assertTextContains("112 m", substring = true)
        for (tag in listOf("scenario-export-cancel", "scenario-export-save", "scenario-export-share"))
            compose.onNodeWithTag(tag).assertIsEnabled()
        val actions = listOf("scenario-export-cancel", "scenario-export-save", "scenario-export-share")
            .map { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot }
        assertTrue(actions.zipWithNext().all { (a, b) -> a.top == b.top && a.right <= b.left })
        compose.onNodeWithTag("scenario-export-cancel").performClick()
        compose.onNodeWithTag("scenario-export-dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals(original, observer.savedPlan) }
    }

    @Test fun emptyFileNamesDisableDeliveryWithoutChangingTheScenario() {
        show(); compose.onNodeWithTag("scenario-export-open").performClick()
        compose.onNodeWithTag("scenario-export-name").performTextClearance()
        compose.onNodeWithTag("scenario-export-save").assertIsNotEnabled()
        compose.onNodeWithTag("scenario-export-share").assertIsNotEnabled()
        compose.onNodeWithTag("scenario-export-cancel").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(original, observer.savedPlan) }
    }

    @Test fun restoredArchivesRecreateEditorFieldsAndKeepTheImportedTimeRate() {
        show()
        compose.onNodeWithTag("observer-altitude").assertTextContains("112.00", substring = true)
        compose.runOnIdle {
            observer.importArchive(SpaceCompassScenarioArchive("Imported", original.copy(altitudeMeters = 500.0),
                SpaceCompassScenarioTimeSettings(true, -60, SpaceCompassScenarioResume.ACTIVE)))
        }
        compose.onNodeWithTag("observer-altitude").assertTextContains("500.00", substring = true)
        compose.runOnIdle {
            assertEquals(-60, observer.timeSettings.rate)
            assertEquals(SpaceCompassScenarioResume.ACTIVE, observer.timeSettings.resume)
        }
    }
}

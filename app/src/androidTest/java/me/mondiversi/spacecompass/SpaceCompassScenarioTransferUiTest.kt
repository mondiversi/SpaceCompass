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
    private var retainedExport: SpaceCompassCaptureExportState? = null
    private val observer = SpaceCompassObserverState(preferences, elapsedClock = { 0L })
    private val moment = Instant.parse("2027-04-06T20:00:00Z").toEpochMilli()
    private val original = spaceCompassObserverPlan(45.0, 9.0, 112.0, moment, "Europe/Rome",
        simulatePosition = false, simulateTime = false, simulateAltitude = true, simulateZone = false, simulateDate = false)!!

    private fun show(initial: SpaceCompassObserverPlan? = original) {
        observer.apply(initial)
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalSpaceCompassPreferences provides preferences,
                LocalSpaceCompassObserver provides observer,
                LocalSpaceCompassNumericFormat provides SpaceCompassNumericFormat.INTERNATIONAL,
                LocalSpaceCompassUnits provides SpaceCompassUnits(distance = "mkm")) {
                val export = rememberSpaceCompassDataExport("retained-test-export",emptyList()) { error("No delivery in this test") }
                SideEffect { retainedExport = export }
                if (shown.value) SpaceCompassObserverPage()
            }
        } }
    }

    @After fun dispose() {
        compose.runOnIdle { shown.value = false }
        compose.waitForIdle(); context.deleteSharedPreferences(preferenceName)
    }

    @Test fun toolbarKeepsExportImportWithoutResumeAndTheExportDialogOffersThreeActions() {
        show()
        compose.onNodeWithTag("scenario-resume-open").assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.observer_management_title)).assertExists()
        val export = compose.onNodeWithTag("scenario-export-open").fetchSemanticsNode().boundsInRoot
        val load = compose.onNodeWithTag("scenario-import-open").fetchSemanticsNode().boundsInRoot
        assertTrue(export.right <= load.left)
        compose.onNodeWithTag("scenario-export-open").performClick()
        compose.onNodeWithTag("scenario-export-dialog").assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.panorama_export_description)).assertExists()
        compose.onNodeWithTag("scenario-export-profile-divider").assertExists()
        compose.onNodeWithTag("scenario-export-name").assertTextContains("112m", substring = true)
        for (tag in listOf("scenario-export-cancel", "scenario-export-save", "scenario-export-share"))
            compose.onNodeWithTag(tag).assertIsEnabled()
        val actions = listOf("scenario-export-cancel", "scenario-export-save", "scenario-export-share")
            .map { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot }
        assertTrue(actions.zipWithNext().all { (a, b) -> a.top == b.top && a.right <= b.left })
        compose.onNodeWithTag("scenario-export-cancel").performClick()
        compose.onNodeWithTag("scenario-export-dialog").assertDoesNotExist()
        compose.runOnIdle { assertEquals(original, observer.savedPlan) }
    }

    @Test fun scenarioProfileUpdatesRetainedExportsAndPreservesAnEditedName() {
        preferences.edit().putString(SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY,"international").commit()
        show(); compose.onNodeWithTag("scenario-export-open").performClick()
        compose.onNodeWithTag("panorama-export-international").assertIsSelected()
        compose.onNodeWithTag("panorama-export-selected").performClick()
        compose.runOnIdle {
            assertEquals("selected",preferences.getString(SPACE_COMPASS_PANORAMA_EXPORT_MODE_KEY,null))
            assertEquals(SpaceCompassPanoramaExportMode.SELECTED,retainedExport!!.mode)
        }
        compose.onNodeWithTag("scenario-export-name").performTextReplacement("My-custom-scenario")
        compose.onNodeWithTag("panorama-export-international").performClick()
        compose.onNodeWithTag("scenario-export-name").assertTextContains("My-custom-scenario")
        compose.runOnIdle { assertEquals(SpaceCompassPanoramaExportMode.INTERNATIONAL,retainedExport!!.mode) }
        compose.onNodeWithTag("scenario-export-cancel").performClick()
        compose.onNodeWithTag("scenario-export-open").performClick()
        compose.onNodeWithTag("panorama-export-international").assertIsSelected()
        compose.onNodeWithTag("scenario-export-cancel").performClick()
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
        compose.onNodeWithTag("scenario-section-altitude").performScrollTo().performClick()
        compose.onNodeWithTag("scenario-export-open").assertDoesNotExist()
        compose.onNodeWithTag("observer-altitude").assertTextContains("112.00", substring = true)
        compose.runOnIdle {
            observer.importArchive(SpaceCompassScenarioArchive("Imported", original.copy(altitudeMeters = 500.0),
                SpaceCompassScenarioTimeSettings(true, -60, SpaceCompassScenarioResume.ACTIVE)))
        }
        compose.onNodeWithTag("scenario-management-menu").assertExists()
        compose.onNodeWithTag("scenario-section-altitude").performScrollTo().performClick()
        compose.onNodeWithTag("observer-altitude").assertTextContains("500.00", substring = true)
        compose.runOnIdle {
            assertEquals(-60, observer.timeSettings.rate)
            assertEquals(SpaceCompassScenarioResume.ACTIVE, observer.timeSettings.resume)
        }
    }
    @Test fun liveObjectsExportKeepsAutomaticValuesAndTheChoiceResetsOnReopening() {
        show(initial = null)
        compose.runOnIdle { observer.devicePlace = SpaceCompassDevicePlace(28.35, -14.1, 102.5) }
        compose.onNodeWithTag("scenario-export-open").assertIsEnabled().performClick()
        compose.onNodeWithTag("location-export-objects").assertIsOff()
        compose.onNodeWithTag("scenario-export-save").assertIsEnabled()
        compose.onNodeWithTag("scenario-export-share").assertIsEnabled()
        compose.onNodeWithTag("location-export-objects").performClick().assertIsOn()
        compose.onNodeWithTag("scenario-export-save").assertIsEnabled()
        compose.onNodeWithTag("scenario-export-share").assertIsEnabled()
        compose.runOnIdle { assertNull(observer.plan); assertNull(observer.savedPlan) }
        compose.onNodeWithTag("location-export-objects").performClick().assertIsOff()
        compose.onNodeWithTag("scenario-export-save").assertIsEnabled()
        compose.onNodeWithTag("scenario-export-cancel").performClick()
        compose.onNodeWithTag("scenario-export-open").performClick()
        compose.onNodeWithTag("location-export-objects").assertIsOff()
        compose.onNodeWithTag("scenario-export-share").assertIsEnabled()
        compose.onNodeWithTag("scenario-export-cancel").performClick()
    }

    @Test fun includingObjectsDoesNotRequireOrInventALocationFix() {
        show(initial = null)
        compose.onNodeWithTag("scenario-export-open").performClick()
        compose.onNodeWithTag("location-export-objects").performClick().assertIsOn()
        compose.onNodeWithTag("scenario-export-save").assertIsEnabled()
        compose.onNodeWithTag("scenario-export-share").assertIsEnabled()
        compose.onNodeWithTag("scenario-export-cancel").assertIsEnabled().performClick()
        compose.runOnIdle { assertNull(observer.plan); assertNull(observer.savedPlan) }
    }

}

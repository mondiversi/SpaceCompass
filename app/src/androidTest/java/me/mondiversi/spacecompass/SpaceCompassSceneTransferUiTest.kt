package me.mondiversi.spacecompass

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated synthetic state; never read or change the user's saved Location or celestial selection. */
class SpaceCompassSceneTransferUiTest {
    @get:Rule val compose=createComposeRule()
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val preferenceName="test_scene_transfer_"+UUID.randomUUID()
    private val preferences=context.getSharedPreferences(preferenceName,Context.MODE_PRIVATE)
    private val shown=mutableStateOf(true)
    private lateinit var observer: SpaceCompassObserverState
    private lateinit var navigate: (String)->Unit
    private fun show(dark: Boolean=false,fix: Boolean=true) {
        preferences.edit().putStringSet("celestial_selected",setOf("SUN","MOON")).putString("celestial_active","MOON").commit()
        compose.setContent {
            val system=LocalConfiguration.current
            val themed=remember(system,dark) { Configuration(system).apply {
                uiMode=(uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if(dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            } }
            CompositionLocalProvider(LocalConfiguration provides themed) {
            val palette=spaceCompassSurfacePalette(dark)
            val foreground=if(dark) Color.White else Color(0xFF101418)
            MaterialTheme(colorScheme=if(dark) darkColorScheme(primary=Color(0xFF55C8DB),
                background=palette.background,surface=palette.island,onSurface=foreground,onBackground=foreground)
                else lightColorScheme(primary=Color(0xFF217E9F),background=palette.background,
                    surface=palette.island,onSurface=foreground,onBackground=foreground)) {
            CompositionLocalProvider(LocalSpaceCompassPreferences provides preferences) {
                if(shown.value) SpaceCompassAppPages {
                    val owner=LocalSpaceCompassObserver.current!!
                    val nav=LocalSpaceCompassNavigate.current
                    SideEffect { observer=owner;navigate=nav }
                    Column(Modifier.fillMaxSize()) { SpaceCompassSettingsButton(Color.White,Color.Black) }
                }
            }
        } } }
        compose.runOnIdle { if(fix) observer.devicePlace=SpaceCompassDevicePlace(51.5,-0.12,35.0) }
        compose.waitUntil(10_000) {
            runCatching { compose.onAllNodesWithTag("app-settings").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
    }
    private fun openScene() {
        compose.onNodeWithTag("app-settings").performClick()
        compose.onNodeWithTag("open-scene-export").performClick()
        compose.onNodeWithTag("scene-export-page").assertIsDisplayed()
    }
    @After fun dispose() {
        compose.runOnIdle { shown.value=false }
        compose.waitForIdle();context.deleteSharedPreferences(preferenceName)
    }
    @Test fun mainMenuOrdersUnitsExportImportTutorial() {
        show();compose.onNodeWithTag("app-settings").performClick()
        val keys=listOf("open-units","open-scene-export","open-scene-import","open-tutorial")
        val tops=keys.map { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.top }
        assertEquals(tops.sorted(),tops)
    }
    @Test fun sceneExportIncludesLocationByDefaultWithoutChangingTheObserver() {
        show();openScene()
        compose.onNodeWithTag("scene-export-location").assertIsOn()
        compose.onNodeWithTag("scenario-export-save").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("scene-export-location").performScrollTo().performClick().assertIsOff()
        compose.onNodeWithTag("scenario-export-share").performScrollTo().assertIsEnabled()
        compose.runOnIdle { assertNull(observer.plan);assertEquals("MOON",preferences.getString("celestial_active",null)) }
        compose.onNodeWithContentDescription(context.getString(R.string.navigate_back)).performClick()
        compose.onNodeWithTag("app-settings").assertIsDisplayed()
    }
    @Test fun sceneOnlyExportWorksWithoutGpsAndTheCheckboxWorksInNightTheme() {
        show(dark=true,fix=false);openScene()
        compose.onNodeWithTag("scene-export-location").assertIsOn()
        compose.onNodeWithTag("scenario-export-save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("scene-export-location").performScrollTo().performClick()
        compose.onNodeWithTag("scenario-export-save").performScrollTo().assertIsEnabled()
        compose.runOnIdle { assertNull(observer.plan);assertNull(observer.devicePlace) }
    }
    @Test fun locationExportHasAutomaticValuesInsteadOfCelestialObjects() {
        show();compose.runOnIdle { navigate("observer") }
        compose.onNodeWithTag("scenario-export-open").performClick()
        compose.onNodeWithTag("scenario-export-page").assertIsDisplayed()
        compose.onNodeWithTag("location-export-objects").assertDoesNotExist()
        compose.onNodeWithTag("location-export-automatic").assertIsOn()
        compose.onNodeWithTag("scenario-export-save").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("location-export-automatic").performScrollTo().performClick().assertIsOff()
        compose.onNodeWithTag("scenario-export-save").performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { assertNull(observer.plan);assertEquals("MOON",preferences.getString("celestial_active",null)) }
    }
    @Test fun editedScenarioNameSurvivesProfileAndLocationCheckboxChanges() {
        show();openScene()
        compose.onNodeWithTag("scenario-export-name").performTextReplacement("My-scene")
        compose.onNodeWithTag("panorama-export-international").performScrollTo().performClick()
        compose.onNodeWithTag("scene-export-location").performScrollTo().performClick()
        compose.onNodeWithTag("scenario-export-name").performScrollTo().assertTextContains("My-scene")
        compose.onNodeWithContentDescription(context.getString(R.string.navigate_back)).performClick()
        openScene()
        compose.onNodeWithTag("scene-export-location").assertIsOn()
        compose.onNodeWithTag("panorama-export-international").performScrollTo().assertIsSelected()
    }
    @Test fun sceneAutomaticCompletionIsAlignedDisabledWithNoLocationAndCheckedByDefault() {
        show(fix=false);openScene()
        compose.onNodeWithTag("scene-export-automatic").assertIsOn()
        compose.onNodeWithTag("scenario-export-save").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("scene-export-automatic").performScrollTo().performClick().assertIsOff()
        compose.onNodeWithTag("scenario-export-save").performScrollTo().assertIsEnabled()
        compose.onNodeWithTag("scene-export-location").performScrollTo().performClick()
        compose.onNodeWithTag("scene-export-automatic").assertExists().assertIsNotEnabled().assertIsOff()
        compose.onNodeWithTag("scene-export-automatic").performClick().assertIsOff()
        compose.onNodeWithTag("scene-export-location").performClick()
        compose.onNodeWithTag("scene-export-automatic").assertIsEnabled().assertIsOff()
        compose.onNodeWithContentDescription(context.getString(R.string.navigate_back)).performClick()
        openScene()
        compose.onNodeWithTag("scene-export-automatic").assertIsOn()
        compose.runOnIdle { assertNull(observer.plan);assertNull(observer.devicePlace) }
    }

    @Test fun sceneAndLegacyLocationDocumentsRoundTripThroughTheActualContentProvider() {
        val time=Instant.parse("2027-06-21T12:00:00Z").toEpochMilli()
        val base=SpaceCompassScenarioArchive("Example",SpaceCompassObserverPlan(51.5,-.12,35.0,time,"Europe/London"),
            SpaceCompassScenarioTimeSettings()).withCelestialObjects(SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.SUN),
                SpaceCompassCelestialBody.SUN),emptyList(),emptyList(),null,false,emptyList())
        val scene=SpaceCompassSceneArchive(base,true,SpaceCompassSceneDisplay(true,false,true,false),time)
        val folder=File(context.cacheDir,"scenarios/test-scene-"+UUID.randomUUID()).also { check(it.mkdirs()) }
        try {
            val file=File(folder,spaceCompassSceneFileName(base.name));file.writeText(scene.encode())
            val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
            assertEquals(scene,SpaceCompassSceneArchive.decode(readSpaceCompassDocumentText(context,uri,SPACE_COMPASS_SCENARIO_FILE_LIMIT)))
            file.writeText(base.encode())
            assertEquals(base,readSpaceCompassScenario(context,uri))
            assertEquals(base,SpaceCompassSceneArchive.decode(readSpaceCompassDocumentText(context,uri,SPACE_COMPASS_SCENARIO_FILE_LIMIT)).contents)
            assertNull(readSpaceCompassScenario(context,uri).locationOnly().celestialSelection)
            assertTrue(file.delete())
        } finally { folder.listFiles()?.forEach { it.delete() };folder.delete() }
    }
}

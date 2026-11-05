package me.mondiversi.spacecompass

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.FileProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Deliver controlled picker results, then exercise the actual asynchronous content-resolver I/O. */
class SpaceCompassSceneDeliveryUiTest {
    @get:Rule val compose=createComposeRule()
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val preferenceName="test_scene_delivery_"+UUID.randomUUID()
    private val preferences=context.getSharedPreferences(preferenceName,Context.MODE_PRIVATE)
    private val folder=File(context.cacheDir,"scenarios/test-delivery-"+UUID.randomUUID()).also { check(it.mkdirs()) }
    private val file=File(folder,"fixture.spacecompass-scene").also { it.writeText("") }
    private val uri=FileProvider.getUriForFile(context,context.packageName+".fileprovider",file)
    private val shown=mutableStateOf(true)
    private lateinit var observer: SpaceCompassObserverState
    private lateinit var points: SpaceCompassSunDailyPathUiState
    private lateinit var navigate: (String)->Unit
    private var launchedName: String?=null
    private var launches=0
    private val now=Instant.parse("2027-06-21T12:00:00Z").toEpochMilli()
    private val registry=object:ActivityResultRegistry() {
        override fun <I,O> onLaunch(requestCode: Int,contract: ActivityResultContract<I,O>,input: I,options: ActivityOptionsCompat?) {
            require(contract is ActivityResultContracts.CreateDocument || contract is ActivityResultContracts.OpenDocument)
            if(input is String) launchedName=input
            launches++
            Handler(Looper.getMainLooper()).post { dispatchResult(requestCode,Activity.RESULT_OK,Intent().setData(uri)) }
        }
    }
    private val registryOwner=object:ActivityResultRegistryOwner { override val activityResultRegistry=registry }
    private fun show() {
        preferences.edit().putStringSet("celestial_selected",setOf("SUN","MOON")).putString("celestial_active","MOON").commit()
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalSpaceCompassPreferences provides preferences,
                LocalActivityResultRegistryOwner provides registryOwner) {
                if(shown.value) SpaceCompassAppPages {
                    val o=LocalSpaceCompassObserver.current!!
                    val p=LocalSpaceCompassOrbitSelection.current!!
                    val n=LocalSpaceCompassNavigate.current
                    SideEffect { observer=o;points=p;navigate=n }
                    Column(Modifier.fillMaxSize()) { SpaceCompassSettingsButton(Color.White,Color.Black) }
                }
            }
        } }
        compose.runOnIdle { observer.devicePlace=SpaceCompassDevicePlace(51.5,-.12,35.0) }
        compose.waitUntil(10_000) {
            runCatching { compose.onAllNodesWithTag("app-settings").fetchSemanticsNodes().isNotEmpty() }.getOrDefault(false)
        }
    }
    @After fun dispose() {
        compose.runOnIdle { shown.value=false };compose.waitForIdle()
        context.deleteSharedPreferences(preferenceName)
        folder.listFiles()?.forEach { it.delete() };folder.delete()
    }
    private fun mixedLocation()=SpaceCompassScenarioArchive("Canary",SpaceCompassObserverPlan(28.35,-14.1,102.5,now,"Atlantic/Canary"),
        SpaceCompassScenarioTimeSettings()).withCelestialObjects(SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.MARS),
            SpaceCompassCelestialBody.MARS),emptyList(),emptyList(),null,false,emptyList())
    @Test fun sceneSaveWritesAnImportableFileAndReturnsToTheMainView() {
        show();compose.onNodeWithTag("app-settings").performClick()
        compose.onNodeWithTag("open-scene-export").performClick()
        compose.onNodeWithTag("scenario-export-save").performScrollTo().performClick()
        compose.waitUntil(10_000) { runCatching { SpaceCompassSceneArchive.decode(file.readText()) }.isSuccess }
        val saved=SpaceCompassSceneArchive.decode(file.readText())
        assertEquals(1,launches);assertTrue(launchedName!!.endsWith(SPACE_COMPASS_SCENE_EXTENSION))
        assertTrue(saved.locationIncluded);assertEquals(51.5,saved.contents.configuration.latitude,0.0)
        assertEquals(35.0,saved.contents.configuration.altitudeMeters,0.0)
        assertEquals(setOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON),saved.contents.celestialSelection!!.selected)
        assertEquals(SpaceCompassCelestialBody.MOON,saved.contents.celestialSelection!!.active)
        compose.onNodeWithTag("app-settings").assertIsDisplayed()
        compose.runOnIdle { assertNull(observer.plan) }
    }
    @Test fun locationSaveWritesNoCelestialSectionsAndPreservesTheCurrentScene() {
        show();compose.runOnIdle { navigate("observer") }
        compose.onNodeWithTag("scenario-export-open").performClick()
        compose.onNodeWithTag("scenario-export-save").performScrollTo().performClick()
        compose.waitUntil(10_000) { runCatching { SpaceCompassScenarioArchive.decode(file.readText()) }.isSuccess }
        val saved=SpaceCompassScenarioArchive.decode(file.readText())
        assertTrue(launchedName!!.endsWith(SPACE_COMPASS_SCENARIO_EXTENSION));assertNull(saved.celestialSelection)
        assertNull(saved.comparisons);assertNull(saved.selectedOrbitPoints)
        assertEquals(51.5,saved.configuration.latitude,0.0)
        compose.onNodeWithTag("observer-page").assertIsDisplayed()
        compose.runOnIdle { assertNull(observer.plan);assertEquals("MOON",preferences.getString("celestial_active",null)) }
    }
    @Test fun sceneImportThroughThePickerKeepsTheCurrentLocationWhenItIsExcluded() {
        show()
        val own=SpaceCompassObserverPlan(51.5,-.12,35.0,now,"Europe/London")
        compose.runOnIdle { observer.apply(own) }
        val incoming=SpaceCompassSceneArchive(mixedLocation().withoutLocation(),false,SpaceCompassSceneDisplay(true,true,false,true),now)
        file.writeText(incoming.encode())
        compose.onNodeWithTag("app-settings").performClick();compose.onNodeWithTag("open-scene-import").performClick()
        compose.waitUntil(10_000) { preferences.getString("celestial_active",null)=="MARS" }
        compose.runOnIdle {
            assertEquals(own,observer.savedPlan);assertEquals(SpaceCompassSceneDisplay(true,true,false,true),readSpaceCompassSceneDisplay(preferences))
            assertEquals(setOf(SpaceCompassCelestialBody.MARS),readSpaceCompassCelestialSelection(preferences).selected)
        }
        compose.onNodeWithTag("app-settings").assertIsDisplayed()
    }
    @Test fun legacyMixedLocationImportThroughThePickerCannotReplaceObjectsOrSelectedPoints() {
        show()
        val point=SpaceCompassSunPathPoint(now,SpaceCompassSunPosition(150.0,60.0))
        val pin=SpaceCompassSelectedOrbitPoint(spaceCompassOrbitPointKey(SpaceCompassCelestialBody.MOON,point,ZoneId.of("UTC")),point,null)
        compose.runOnIdle { points.restoreImportedPoints(listOf(pin));points.restoreAntipodes(true) }
        file.writeText(mixedLocation().encode())
        compose.runOnIdle { navigate("observer") }
        compose.onNodeWithTag("scenario-import-open").performClick()
        compose.waitUntil(10_000) { observer.savedPlan?.latitude==28.35 }
        compose.runOnIdle {
            assertEquals("MOON",preferences.getString("celestial_active",null))
            assertEquals(setOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON),readSpaceCompassCelestialSelection(preferences).selected)
            assertEquals(listOf(pin),points.selectedPoints);assertTrue(points.antipodesVisible)
        }
        compose.onNodeWithTag("observer-page").assertIsDisplayed()
    }
}

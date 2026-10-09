package me.mondiversi.spacecompass

import android.content.res.Configuration
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Locale

/** Replace the activity's content with a synthetic fixture; do not alter saved app preferences. */
class SpaceCompassStatusNoticesUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun noticesFitTheSkyCornerAcrossLanguagesSizesAndFontsAndRetryRemainsClickable() {
        val width = mutableStateOf(360f)
        val height = mutableStateOf(500f)
        val language = mutableStateOf("it")
        val scale = mutableStateOf(1f)
        var retries = 0
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            val config = Configuration(activity.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language.value))
            }
            val resources = activity.createConfigurationContext(config).resources
            val direction = if (language.value in setOf("ar", "fa", "he")) LayoutDirection.Rtl else LayoutDirection.Ltr
            CompositionLocalProvider(LocalResources provides resources, LocalConfiguration provides config,
                LocalDensity provides Density(1f, scale.value), LocalLayoutDirection provides direction) {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value.dp, height.value.dp)) {
                        SpaceCompassSunPointingViewport(null, null, Modifier.requiredSize(width.value.dp, height.value.dp),
                            "Synthetic sky", rememberSpaceCompassSunDailyPathUiState(),
                            primaryText = Color.White, secondaryText = Color.LightGray, backgroundColor = Color(0xFF101418),
                            compassWarning = stringResource(R.string.sun_finder_compass_accuracy), showActions = false,
                            statusMessage = stringResource(R.string.celestial_data_unavailable_compact),
                            statusActionLabel = stringResource(R.string.sun_finder_retry), onStatusAction = { retries++ },
                            simulationLabel = stringResource(R.string.observer_simulation_active))
                    }
                }
            }
        } }
        val languages = listOf("en", "it", "de", "fr", "es", "pt", "ru", "el", "tr", "ar", "fa", "he",
            "hi", "ja", "ko", "zh", "sw", "pl", "nl", "id")
        for (w in listOf(360f, 800f)) for (font in listOf(1f, 1.6f)) for (code in languages) {
            compose.runOnIdle { width.value = w; scale.value = font; language.value = code }
            val sky = compose.onNodeWithTag("sun-finder-sky").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithTag("celestial-compass-warning").assertDoesNotExist()
            compose.onNodeWithTag("observer-simulation-banner").assertDoesNotExist()
            val notice = compose.onNodeWithTag("sun-finder-status-island").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithTag("sun-finder-status-action").assertIsDisplayed().fetchSemanticsNode().touchBoundsInRoot
            assertTrue("Notice stays within its sky in $code", notice.left >= sky.left && notice.right <= sky.right)
            assertTrue("Status width stays compact in $code", notice.width <= 280.5f)
            if (font == 1f) assertTrue("Regular-font status has one compact row in $code", notice.height <= 61f)
            assertTrue("Retry keeps a 48 dp touch target in $code", action.width >= 47.5f && action.height >= 47.5f)
            if (code in setOf("ar", "fa", "he")) assertEquals(sky.right - 4f, notice.right, 1f)
            else assertEquals(sky.left + 4f, notice.left, 1f)
        }
        compose.runOnIdle { language.value = "it"; width.value = 360f; scale.value = 1f }
        compose.onNodeWithTag("sun-finder-status-action").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        for ((name, w) in listOf("phone" to 360f, "tablet" to 800f)) {
            compose.runOnIdle { width.value = w }
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val file = File(compose.activity.getExternalFilesDir(null), "compact-status-fixture-$name.png")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun retryAndLoadingTakePriorityUntilRecoveryThenRestoreTheLatestCompassWarning() {
        val warning = mutableStateOf("Reduced precision")
        val message = mutableStateOf<String?>("Offline")
        val action = mutableStateOf<String?>("Retry")
        var retries = 0
        compose.activityRule.scenario.onActivity { activity -> activity.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 1f)) {
                MaterialTheme {
                    Box(Modifier.requiredSize(360.dp, 500.dp)) {
                        SpaceCompassSunStatusNotices(warning.value, message.value, action.value, {
                            retries++
                            message.value = "Loading"
                            action.value = null
                        }, Color(0xFF101418))
                    }
                }
            }
        } }
        compose.onNodeWithTag("sun-finder-status-island").assertIsDisplayed()
        compose.onNodeWithTag("celestial-compass-warning").assertDoesNotExist()
        compose.onNodeWithTag("sun-finder-status-action").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithText("Loading").assertIsDisplayed()
        compose.onNodeWithTag("sun-finder-status-action").assertDoesNotExist()
        compose.onNodeWithTag("celestial-compass-warning").assertDoesNotExist()

        // A failed retry keeps priority even if the sensor state changes in the meantime.
        compose.runOnIdle {
            warning.value = "Move magnetic objects away"
            message.value = "Offline"
            action.value = "Retry"
        }
        compose.onNodeWithTag("sun-finder-status-action").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(2, retries); message.value = null }
        compose.onNodeWithTag("sun-finder-status-island").assertDoesNotExist()
        compose.onNodeWithTag("celestial-compass-warning").assertIsDisplayed()
            .assertTextEquals("Move magnetic objects away")
        compose.runOnIdle { warning.value = "Reduced precision" }
        compose.onNodeWithTag("celestial-compass-warning").assertTextEquals("Reduced precision")
    }
}

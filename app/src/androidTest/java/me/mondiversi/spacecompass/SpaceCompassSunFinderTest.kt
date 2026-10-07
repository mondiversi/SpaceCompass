package me.mondiversi.spacecompass

import android.content.res.Configuration
import android.location.Location
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.util.Locale
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Isolated synthetic GPS/orientation fixtures; no real location or hardware commands. */
class SpaceCompassSunFinderTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val configuration = Configuration(context.resources.configuration).apply { setLocale(Locale.ITALIAN) }
    private val resources = context.createConfigurationContext(configuration).resources
    private fun fix() = Location("synthetic").apply {
        latitude = 39.742476; longitude = -105.1786; altitude = 1830.14
        accuracy = 8f; elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
    }

    @Test fun staleAndInvalidLocationsAreRejected() {
        val location = fix()
        assertTrue(spaceCompassSunLocationUsable(location, location.elapsedRealtimeNanos))
        assertFalse(spaceCompassSunLocationUsable(location,
            location.elapsedRealtimeNanos + SPACE_COMPASS_SUN_LOCATION_MAX_AGE_MS * 1_000_000 + 1))
        assertFalse(spaceCompassSunLocationUsable(location, location.elapsedRealtimeNanos - 1))
        location.latitude = Double.NaN
        assertFalse(spaceCompassSunLocationUsable(location, location.elapsedRealtimeNanos))
    }

    @Test fun sunViewRendersInBothThemesAndLandscapeWithoutRealPositionAccess() {
        val night = mutableStateOf(false)
        val landscape = mutableStateOf(false)
        val rtl = mutableStateOf(false)
        val largeText = mutableStateOf(false)
        val timestamp = Instant.parse("2003-10-17T19:30:30Z").toEpochMilli()
        val position = calculateSpaceCompassSunPosition(timestamp, 39.742476, -105.1786, 1830.14)
        val az = Math.toRadians(position.azimuthDegrees); val el = Math.toRadians(position.elevationDegrees)
        val orientation = SpaceCompassSunOrientation(SpaceCompassSunVector(kotlin.math.cos(az), -kotlin.math.sin(az), 0.0),
            SpaceCompassSunVector(-kotlin.math.sin(el) * kotlin.math.sin(az), -kotlin.math.sin(el) * kotlin.math.cos(az), kotlin.math.cos(el)),
            SpaceCompassSunVector(kotlin.math.cos(el) * kotlin.math.sin(az), kotlin.math.cos(el) * kotlin.math.cos(az), kotlin.math.sin(el)))
        val reading = SpaceCompassSunFinderReadings(fix(), SpaceCompassSunLocationStatus.READY, orientation, true, true)
        compose.setContent {
            val density = LocalDensity.current
            val view = LocalView.current
            DisposableEffect(view) {
                val old = view.keepScreenOn; view.keepScreenOn = true
                onDispose { view.keepScreenOn = old }
            }
            CompositionLocalProvider(LocalResources provides resources,
                LocalConfiguration provides spaceCompassThemeConfiguration(configuration,
                    if (night.value) SpaceCompassAppTheme.DARK else SpaceCompassAppTheme.LIGHT),
                // Render a genuine wide viewport without changing the user's device orientation/settings.
                LocalDensity provides Density(if (landscape.value) density.density * 0.5f else density.density,
                    if (largeText.value) 2f else density.fontScale),
                LocalLayoutDirection provides if (rtl.value) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                MaterialTheme(colorScheme = if (night.value) darkColorScheme() else lightColorScheme()) {
                    val colors = MaterialTheme.colorScheme
                    Box(Modifier.fillMaxSize().background(colors.background).testTag("sun-fixture")) {
                        val size = if (landscape.value) Modifier.fillMaxWidth().height(260.dp)
                            else Modifier.fillMaxSize()
                        Box(size) {
                            SpaceCompassSunFinderContent(reading, timestamp, colors.onSurface, colors.onSurfaceVariant, colors.background,
                                weather = SpaceCompassSunWeatherReading(SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.CLEAR, 0f, timestamp)),
                                onDismissRequest = {})
                        }
                    }
                }
            }
        }
        for (dark in listOf(false, true)) {
            compose.runOnIdle { night.value = dark }
            compose.onNode(hasText(resources.getString(R.string.celestial_orbits_title)) and
                hasAnyAncestor(hasTestTag("celestial-title"))).assertIsDisplayed()
            assertGpsInEnvironmentGroup("sun-data-altitude")
            compose.onNodeWithTag("sun-data-gps").assertDoesNotExist()
            compose.onNodeWithText("Sole allineato").assertDoesNotExist()
            compose.onNodeWithText("Cielo simulato").assertDoesNotExist()
            compose.onNodeWithContentDescription("N, NE, E, SE, S, SW, W, NW").assertIsDisplayed()
            val backdrop = compose.onNodeWithTag("sun-finder-backdrop").fetchSemanticsNode().boundsInRoot
            assertEquals(compose.onNodeWithTag("sun-finder-content").fetchSemanticsNode().boundsInRoot, backdrop)
            capture(if (dark) "night" else "day")
            assertPanelFitsWithoutScrolling()
        }
        compose.runOnIdle { landscape.value = true }
        compose.onNodeWithTag("sun-finder-sky").assertIsDisplayed()
        assertGpsInEnvironmentGroup("sun-data-altitude")
        compose.onNodeWithTag("sun-data-gps").assertDoesNotExist()
        capture("landscape")
        assertPanelFitsWithoutScrolling()
        compose.runOnIdle { rtl.value = true; largeText.value = true }
        compose.onNodeWithTag("sun-finder-sky").assertIsDisplayed()
        capture("large-font-rtl")
    }

    @Test fun unreliableCompassWarningIsCentredOnTheReticleInsteadOfTheDataPanel() {
        val night = mutableStateOf(false)
        val wide = mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources) {
                MaterialTheme(colorScheme = if (night.value) darkColorScheme() else lightColorScheme()) {
                    val c = MaterialTheme.colorScheme
                    val reading = SpaceCompassSunFinderReadings(fix(), SpaceCompassSunLocationStatus.READY,
                        SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0), SpaceCompassSunVector(0.0, 0.0, 1.0),
                            SpaceCompassSunVector(0.0, 1.0, 0.0)), true, false)
                    Box(if (wide.value) Modifier.size(780.dp, 360.dp) else Modifier.fillMaxSize()) {
                        SpaceCompassSunFinderContent(reading, Instant.parse("2003-10-17T19:30:30Z").toEpochMilli(),
                            c.onSurface, c.onSurfaceVariant, c.background)
                    }
                }
            }
        }
        for (dark in listOf(false, true)) for (landscape in listOf(false, true)) {
            compose.runOnIdle { night.value = dark; wide.value = landscape }
            val sky = compose.onNodeWithTag("sun-finder-sky").fetchSemanticsNode().boundsInRoot
            val warning = compose.onNodeWithTag("celestial-compass-warning").assertIsDisplayed()
                .assert(hasAnyAncestor(hasTestTag("sun-finder-sky")))
                .assert(!hasAnyAncestor(hasTestTag("sun-finder-details"))).fetchSemanticsNode().boundsInRoot
            assertEquals(sky.center.x, warning.center.x, 1f)
            assertEquals(sky.center.y, warning.center.y, 1f)
            compose.onNodeWithText(resources.getString(R.string.sun_finder_safety)).assertDoesNotExist()
        }
    }

    @Test fun missingLocationShowsARealErrorStateWithoutAComputedSun() {
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources) {
                MaterialTheme {
                    SpaceCompassSunFinderContent(SpaceCompassSunFinderReadings(), System.currentTimeMillis(),
                        Color.Black, Color.Gray, Color.White)
                }
            }
        }
        compose.onNodeWithText(resources.getString(R.string.sun_finder_location_permission)).assertExists()
        compose.onNodeWithText("Sole allineato").assertDoesNotExist()
        compose.onNodeWithContentDescription("Azimut (Sole): —").assertExists()
    }

    private fun capture(name: String) {
        File(context.externalCacheDir, "sun-finder-$name.png").outputStream().use {
            compose.onNodeWithTag("sun-fixture").captureToImage().asAndroidBitmap()
                .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertPanelFitsWithoutScrolling() {
        val range = compose.onNodeWithTag("sun-finder-details").fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertEquals("Normal data panel must not need a scrollbar", 0f, range.maxValue(), 1f)
    }

    private fun assertGpsInEnvironmentGroup(vararg tags: String) {
        for (tag in tags) compose.onNodeWithTag(tag)
            .assert(hasAnyAncestor(hasTestTag("sun-finder-environment-data")))
            .assert(hasAnyAncestor(hasTestTag("sun-finder-location-data")))
            .assert(!hasAnyAncestor(hasTestTag("sun-finder-pointing-data")))
    }

    @Test fun solarTableFitsAllLanguagesAndInfoRetainsTheModelNotesAndAttribution() {
        val language = mutableStateOf("it")
        val codes = listOf("en", "it", "de", "fr", "es", "pt", "ru", "el", "tr", "ar", "fa", "he",
            "hi", "ja", "ko", "zh", "sw", "pl", "nl", "id")
        compose.setContent {
            val config = Configuration(configuration).apply { setLocale(Locale.forLanguageTag(language.value)) }
            val translated = context.createConfigurationContext(config).resources
            val density = LocalDensity.current
            CompositionLocalProvider(LocalResources provides translated, LocalConfiguration provides config,
                LocalDensity provides Density(density.density, 1f),
                LocalLayoutDirection provides if (language.value in listOf("ar", "fa", "he")) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                MaterialTheme {
                    val rows = listOf(R.string.sun_finder_heading, R.string.sun_finder_tilt,
                        R.string.sun_finder_azimuth, R.string.sun_finder_elevation,
                        R.string.sun_finder_altitude, R.string.sun_finder_location_accuracy).mapIndexed { index, id ->
                        spaceCompassSunDataRow(translated.getString(id, SPACE_COMPASS_SUN_DATA_MARKER),
                            if (index == 4) "1.830 m (±12 m)" else if (index == 5) "8 m" else "194,3°", "table-$index")
                    }
                    SpaceCompassSunFinderDataPanel(rows.take(4), rows.drop(4), SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
                        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, 1.0, 0.0)), true,
                        translated.getString(R.string.sun_weather_estimate, translated.getString(R.string.sun_weather_clear)),
                        true, Color.Black, Color.DarkGray, Color.White, Modifier.width(320.dp).height(300.dp),
                        false)
                }
            }
        }
        for (code in codes) {
            compose.runOnIdle { language.value = code }
            assertPanelFitsWithoutScrolling()
            assertGpsInEnvironmentGroup("table-4", "table-5")
            val density = context.resources.displayMetrics.density
            for (index in 0..5) {
                val bounds = compose.onNodeWithTag("table-$index").fetchSemanticsNode().boundsInRoot
                assertTrue("Single-line numeric table in $code, row $index", bounds.height <= 24f * density)
            }
        }
        compose.runOnIdle { language.value = "it" }
        compose.onNodeWithTag("sun-finder-info").performClick()
        compose.onNodeWithText(resources.getString(R.string.celestial_environment_info_title)).assertIsDisplayed()
        compose.onNodeWithText(resources.getString(R.string.celestial_environment_orientation_note)).assertIsDisplayed()
        compose.onNodeWithText(resources.getString(R.string.sun_finder_terrain_note)).assertIsDisplayed()
        compose.onNodeWithText(resources.getString(R.string.sun_weather_privacy)).assertIsDisplayed()
        compose.onNodeWithText("Open-Meteo · CC BY 4.0").assertIsDisplayed()
        compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        compose.onNodeWithTag("sun-finder-model-info").assertDoesNotExist()
        compose.onNodeWithTag("table-0").assertIsDisplayed()
        assertPanelFitsWithoutScrolling()
    }

    @Test fun weatherParserRejectsUnknownMissingNullAndStaleValues() {
        val now = 1_800_000_000_000L
        fun json(code: String = "61", cover: String = "90", time: Long = now / 1000) =
            """{"current":{"weather_code":$code,"cloud_cover":$cover,"time":$time}}"""
        val rain = parseSpaceCompassSunWeather(json(), now)
        assertEquals(SpaceCompassSunWeatherKind.RAIN, rain.kind)
        assertEquals(0.9f, rain.cloudCover, 1e-6f)
        for (data in listOf("{}", json(code = "null"), json(code = "100"), json(code = "1.5"),
            json(cover = "null"), json(cover = "101"), json(time = now / 1000 - 7200))) {
            assertThrows(Exception::class.java) { parseSpaceCompassSunWeather(data, now) }
        }
    }

    @Test fun allSixSolarSkiesAndWeatherTypesRenderWithoutNetworkOrUserLocation() {
        val phase = mutableStateOf(SpaceCompassSunSkyPhase.NIGHT)
        val kind = mutableStateOf(SpaceCompassSunWeatherKind.CLEAR)
        compose.setContent {
            Box(Modifier.fillMaxSize().testTag("sun-fixture")) {
                SpaceCompassSunSkyBackdrop(phase.value, SpaceCompassSunWeatherSnapshot(kind.value,
                    if (kind.value == SpaceCompassSunWeatherKind.CLEAR) 0f else 0.9f, 1L), Modifier.fillMaxSize())
            }
        }
        for (item in SpaceCompassSunSkyPhase.entries) {
            compose.runOnIdle { phase.value = item }
            compose.onNodeWithTag("sun-finder-backdrop").assertIsDisplayed()
            capture("phase-${item.name.lowercase()}")
        }
        for (item in SpaceCompassSunWeatherKind.entries) {
            compose.runOnIdle { kind.value = item }
            compose.onNodeWithTag("sun-finder-backdrop").assertIsDisplayed()
            capture("weather-${item.name.lowercase()}")
        }
    }

    @Test fun virtualGroundFollowsPitchAndRollAndTheNegativeSunRemainsVisible() {
        val scenario = mutableIntStateOf(0)
        val day = Instant.parse("2003-10-17T19:30:30Z").toEpochMilli()
        val night = Instant.parse("2003-10-18T03:30:30Z").toEpochMilli()
        val names = listOf("ground-level", "ground-roll", "ground-below-sun", "ground-look-up", "ground-look-down")
        compose.setContent {
            val view = LocalView.current
            DisposableEffect(view) {
                val old = view.keepScreenOn; view.keepScreenOn = true
                onDispose { view.keepScreenOn = old }
            }
            CompositionLocalProvider(LocalResources provides resources,
                LocalConfiguration provides spaceCompassThemeConfiguration(configuration, SpaceCompassAppTheme.LIGHT)) {
                MaterialTheme(colorScheme = lightColorScheme()) {
                    val colors = MaterialTheme.colorScheme
                    val time = if (scenario.intValue == 2) night else day
                    val position = calculateSpaceCompassSunPosition(time, 39.742476, -105.1786, 1830.14)
                    val pitch = when (scenario.intValue) {
                        2 -> position.elevationDegrees
                        3 -> 90.0
                        4 -> -90.0
                        else -> 0.0
                    }
                    val az = Math.toRadians(position.azimuthDegrees); val el = Math.toRadians(pitch)
                    val unrolled = SpaceCompassSunOrientation(SpaceCompassSunVector(kotlin.math.cos(az), -kotlin.math.sin(az), 0.0),
                        SpaceCompassSunVector(-kotlin.math.sin(el) * kotlin.math.sin(az), -kotlin.math.sin(el) * kotlin.math.cos(az), kotlin.math.cos(el)),
                        SpaceCompassSunVector(kotlin.math.cos(el) * kotlin.math.sin(az), kotlin.math.cos(el) * kotlin.math.cos(az), kotlin.math.sin(el)))
                    val orientation = if (scenario.intValue != 1) unrolled else {
                        fun rotate(a: SpaceCompassSunVector, b: SpaceCompassSunVector, sign: Double) = SpaceCompassSunVector(
                            (a.east + sign * b.east) / kotlin.math.sqrt(2.0),
                            (a.north + sign * b.north) / kotlin.math.sqrt(2.0),
                            (a.up + sign * b.up) / kotlin.math.sqrt(2.0))
                        unrolled.copy(right = rotate(unrolled.right, unrolled.screenUp, 1.0),
                            screenUp = rotate(unrolled.screenUp, unrolled.right, -1.0))
                    }
                    Box(Modifier.fillMaxSize().testTag("sun-fixture")) {
                        SpaceCompassSunFinderContent(SpaceCompassSunFinderReadings(fix(), SpaceCompassSunLocationStatus.READY,
                            orientation, true, true), time, colors.onSurface, colors.onSurfaceVariant, colors.background,
                            weather = SpaceCompassSunWeatherReading(SpaceCompassSunWeatherSnapshot(SpaceCompassSunWeatherKind.CLEAR, 0f, time)),
                            onDismissRequest = {})
                    }
                }
            }
        }
        for (index in names.indices) {
            compose.runOnIdle { scenario.intValue = index }
            compose.waitForIdle()
            val bounds = compose.onNodeWithTag("sun-finder-content").fetchSemanticsNode().boundsInRoot
            assertEquals(bounds, compose.onNodeWithTag("sun-finder-ground").fetchSemanticsNode().boundsInRoot)
            if (index == 2) {
                val viewport = compose.onNodeWithTag("sun-finder-sky").fetchSemanticsNode().boundsInRoot
                val root = compose.onNodeWithTag("sun-fixture")
                val imageBounds = root.fetchSemanticsNode().boundsInRoot
                val pixels = root.captureToImage().toPixelMap()
                val colour = pixels[(viewport.center.x - imageBounds.left).toInt(), (viewport.center.y - imageBounds.top).toInt()]
                // The violet centre remains visible, not hidden under the ground plane.
                assertTrue(colour.blue > colour.red + 0.03f && colour.blue > colour.green + 0.05f)
                compose.onNodeWithText(resources.getString(R.string.celestial_below, resources.getString(R.string.celestial_sun))).assertExists()
            }
            capture(names[index])
        }
    }
}

package me.mondiversi.planetcompass

import android.content.res.Configuration
import android.location.Location
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic locations and orientation only; no hardware listeners, preferences or archive writes. */
class PlanetCompassSunDailyPathRenderingTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
        .apply { setLocale(Locale.ITALIAN) }).resources
    private val path = calculatePlanetCompassSunDailyPath(LocalDate.parse("2026-10-03"), ZoneId.of("Europe/Rome"), 45.0, 9.0)
    private val hour = path.markers.first {
        it.event == PlanetCompassSunPathEvent.HOUR && Instant.ofEpochMilli(it.timeMs).atZone(path.zone).hour == 9
    }
    private fun pointCaption(number: Int, time: String): String =
        resources.getString(R.string.celestial_point_time,
            resources.getString(R.string.celestial_point_number, number), time)
    private fun currentCaption(time: String): String =
        resources.getString(R.string.celestial_point_time, resources.getString(R.string.celestial_point_current), time)

    private fun facing(position: PlanetCompassSunPosition): PlanetCompassSunOrientation {
        val a = Math.toRadians(position.azimuthDegrees); val e = Math.toRadians(position.elevationDegrees)
        return PlanetCompassSunOrientation(PlanetCompassSunVector(cos(a), -sin(a), 0.0),
            PlanetCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            PlanetCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }
    private fun show(orientation: PlanetCompassSunOrientation? = facing(hour.position), reliable: Boolean = true,
        scene: PlanetCompassSunDailyPath = path) {
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalPlanetCompassTimeFormat provides PlanetCompassTimeFormat.H24) {
                MaterialTheme {
                    val state = rememberPlanetCompassSunDailyPathUiState()
                    Box(Modifier.size(340.dp, 400.dp).background(Color(0xFF247AB1))) {
                        PlanetCompassSunPointingViewport(hour.position, orientation, Modifier.fillMaxSize(), "test sky",
                            dailyPathUiState = state,
                            compassReliable = reliable, dailyPath = scene, primaryText = Color.Black,
                            secondaryText = Color.DarkGray, backgroundColor = Color.White, timeMs = hour.timeMs)
                    }
                    PlanetCompassSunDailyPathDialog(state, Color.Black, Color.DarkGray, Color.White)
                }
            }
        }
    }
    @Test fun tappingTheLiveBodyShowsItsTimeAndAnglesAndArrowButtonsChangeTheSelectedPoint() {
        show()
        compose.onNodeWithTag("sun-daily-path").performTouchInput { click(center) }
        compose.onNodeWithTag("sun-path-selected").assertIsDisplayed()
        compose.onNode(hasText("09:00", substring = true) and hasAnyAncestor(hasTestTag("sun-path-selected"))).assertIsDisplayed()
        compose.onNodeWithText(currentCaption("09:00")).assertIsDisplayed()
        compose.onNodeWithContentDescription(resources.getString(R.string.sun_path_next)).performClick()
        compose.onNodeWithText(pointCaption(11, "10:00")).assertIsDisplayed()
        // Stepping the explicit selection does not move the phone: the reticle still points at 09:00.
        compose.onNodeWithText(currentCaption("09:00")).assertIsDisplayed()
        compose.onNodeWithContentDescription(resources.getString(R.string.sun_path_previous)).performClick()
        compose.onNode(hasText("09:00", substring = true) and hasAnyAncestor(hasTestTag("sun-path-selected"))).assertIsDisplayed()
        val bitmap = compose.onNodeWithTag("sun-finder-sky").captureToImage().asAndroidBitmap()
        File(context.externalCacheDir, "daily-path-selected.png").outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
        compose.onNodeWithText(currentCaption("09:00")).assertIsDisplayed()
    }
    @Test fun tappingOutsideTheOldHitAreaStillSelectsTheNearestHourlyDot() {
        show()
        compose.onNodeWithTag("sun-daily-path").performTouchInput {
            click(center + Offset(0f, 30.dp.toPx()))
        }
        compose.onNodeWithTag("sun-path-selected").assertIsDisplayed()
        compose.onNode(hasText("09:00", substring = true) and hasAnyAncestor(hasTestTag("sun-path-selected"))).assertIsDisplayed()
    }

    @Test fun orientationRefreshBetweenPressAndReleaseDoesNotCancelTheTap() {
        val orientation = mutableStateOf(facing(hour.position))
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalPlanetCompassTimeFormat provides PlanetCompassTimeFormat.H24) {
                MaterialTheme {
                    val state = rememberPlanetCompassSunDailyPathUiState()
                    Box(Modifier.size(340.dp, 400.dp)) {
                        PlanetCompassSunDailyPathLayer(path, orientation.value, state,
                            Color.Black, Color.DarkGray, Color.White)
                    }
                }
            }
        }
        compose.onNodeWithTag("sun-daily-path").performTouchInput { down(center) }
        compose.runOnIdle { orientation.value = facing(hour.position.copy(
            azimuthDegrees = hour.position.azimuthDegrees + 0.2)) }
        compose.onNodeWithTag("sun-daily-path").performTouchInput { up() }
        compose.onNodeWithTag("sun-path-selected").assertIsDisplayed()
        compose.onNode(hasText("09:00", substring = true) and hasAnyAncestor(hasTestTag("sun-path-selected"))).assertIsDisplayed()
    }

    @Test fun dailyListMakesOffscreenHoursAccessibleAndPreservesTheLiveViewport() {
        show()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithTag("sun-path-dialog").assertIsDisplayed()
        val first = path.markers.indexOfFirst { it.event == PlanetCompassSunPathEvent.HOUR }
        compose.onNodeWithTag("sun-path-point-$first").performScrollTo().performClick()
        compose.onNodeWithTag("sun-path-dialog").assertDoesNotExist()
        compose.onNodeWithText(pointCaption(1, "00:00")).assertIsDisplayed()
        compose.onNodeWithTag("sun-finder-sky").assertIsDisplayed()
    }
    @Test fun dailyListHasNoVisibilityToggleAndClosingItKeepsTheCurve() {
        show()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithText(resources.getString(R.string.sun_path_show)).assertDoesNotExist()
        compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        compose.onNodeWithTag("sun-daily-path").assertIsDisplayed()
        compose.onNodeWithTag("sun-finder-sky").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithText(resources.getString(R.string.sun_path_show)).assertDoesNotExist()
        compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        compose.onNodeWithTag("sun-daily-path").assertIsDisplayed()
    }
    @Test fun unreliableCompassDoesNotRenderAFalsePathButStillAllowsReadingDailyEvents() {
        show(reliable = false)
        compose.onNodeWithTag("sun-daily-path").assertDoesNotExist()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithText(resources.getString(R.string.sun_path_note)).assertIsDisplayed()
        val rise = path.markers.indexOfFirst { it.event == PlanetCompassSunPathEvent.SUNRISE }
        compose.onNodeWithTag("sun-path-point-$rise").performScrollTo().assertIsDisplayed()
    }
    @Test fun repeatedDstHourLabelsIncludeTheirUtcOffsets() {
        val autumn = calculatePlanetCompassSunDailyPath(LocalDate.parse("2026-10-25"), path.zone, 45.0, 9.0)
        show(scene = autumn)
        compose.onNodeWithTag("sun-path-open").performClick()
        for (offset in listOf("+02:00", "+01:00")) {
            compose.onNodeWithText("02:00 (UTC$offset)", substring = true).performScrollTo().assertIsDisplayed()
        }
    }
    @Test fun fullPageRendersInBothThemesLandscapeAndBelowGroundWithoutRealLocationAccess() {
        val dark = mutableStateOf(false)
        val landscape = mutableStateOf(false)
        val below = mutableStateOf(false)
        val fix = Location("synthetic-daily-path").apply {
            latitude = 45.0; longitude = 9.0; altitude = 100.0; accuracy = 5f
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        val midnight = path.markers.first { it.event == PlanetCompassSunPathEvent.HOUR }
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalPlanetCompassTimeFormat provides PlanetCompassTimeFormat.H24) {
                MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                    val point = if (below.value) midnight else hour
                    val sun = calculatePlanetCompassSunPosition(point.timeMs, fix.latitude, fix.longitude, fix.altitude)
                    val hostDensity = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides if (landscape.value)
                        Density(hostDensity.density * 0.40f, hostDensity.fontScale) else hostDensity) {
                    Box(if (landscape.value) Modifier.size(800.dp, 380.dp) else Modifier.fillMaxSize()) {
                        PlanetCompassSunFinderContent(PlanetCompassSunFinderReadings(location = fix,
                            locationStatus = PlanetCompassSunLocationStatus.READY, orientation = facing(sun),
                            compassAvailable = true, compassReliable = true), point.timeMs,
                            if (dark.value) Color.White else Color.Black, Color.Gray,
                            if (dark.value) Color(0xFF161616) else Color.White, onDismissRequest = {})
                    } }
                }
            }
        }
        for (night in listOf(false, true)) for (wide in listOf(false, true)) for (underground in listOf(false, true)) {
            compose.runOnIdle { dark.value = night; landscape.value = wide; below.value = underground }
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag("sun-path-open").assertIsDisplayed()
            val bitmap = compose.onNodeWithTag("sun-finder-content").captureToImage().asAndroidBitmap()
            File(context.externalCacheDir, "daily-path-${if (night) "dark" else "light"}-${if (wide) "wide" else "portrait"}-${if (underground) "night" else "day"}.png")
                .outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test fun landscapeMovesTheSelectionAboveDataAndKeepsBodyValuesInTheirOwnIsland() {
        val wide = mutableStateOf(false)
        val dark = mutableStateOf(false)
        val height = mutableStateOf(380.dp)
        val fix = Location("synthetic-layout").apply {
            latitude = 45.0; longitude = 9.0; altitude = 100.0; accuracy = 5f
        }
        val sun = calculatePlanetCompassSunPosition(hour.timeMs, fix.latitude, fix.longitude, fix.altitude)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalResources provides resources, LocalPlanetCompassTimeFormat provides PlanetCompassTimeFormat.H24,
                LocalDensity provides if (wide.value) Density(density.density * 0.4f, 1f) else density) {
                MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                    val colors = MaterialTheme.colorScheme
                    Box(if (wide.value) Modifier.size(800.dp, height.value) else Modifier.fillMaxSize()) {
                        PlanetCompassSunFinderContent(PlanetCompassSunFinderReadings(fix, PlanetCompassSunLocationStatus.READY,
                            facing(sun), true, true), hour.timeMs, colors.onSurface, colors.onSurfaceVariant,
                            colors.background, onDismissRequest = {})
                    }
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("sun-path-open").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithTag("sun-path-point-0").performScrollTo().performClick()
        compose.onNodeWithTag("sun-path-selected").assert(hasAnyAncestor(hasTestTag("sun-finder-sky")))
        compose.onNodeWithText(pointCaption(1, "00:00")).assertIsDisplayed()

        for (night in listOf(false, true)) for (panelHeight in listOf(380.dp, 700.dp)) {
            compose.runOnIdle { wide.value = true; dark.value = night; height.value = panelHeight }
            val selection = compose.onNodeWithTag("sun-path-selected")
                .assertIsDisplayed().assert(hasAnyAncestor(hasTestTag("celestial-details-column")))
                .assert(!hasAnyAncestor(hasTestTag("sun-finder-sky"))).fetchSemanticsNode().boundsInRoot
            val data = compose.onNodeWithTag("sun-finder-details").fetchSemanticsNode().boundsInRoot
            assertTrue("Selection must be above the azimuth/altitude data", selection.bottom <= data.top)
            for (tag in listOf("celestial-distance", "celestial-speed")) {
                val bounds = compose.onNodeWithTag(tag).assertIsDisplayed()
                    .assert(hasAnyAncestor(hasTestTag("sun-finder-body-data"))).fetchSemanticsNode().boundsInRoot
                assertTrue(bounds.left >= data.left && bounds.right <= data.right)
                assertTrue(bounds.top >= data.top && bounds.bottom <= data.bottom)
            }
            assertEquals(1, compose.onAllNodesWithTag("sun-path-selected").fetchSemanticsNodes().size)
            assertEquals(1, compose.onAllNodesWithTag("celestial-distance").fetchSemanticsNodes().size)
            compose.onNodeWithText(pointCaption(1, "00:00")).assertIsDisplayed()
        }
        compose.onNodeWithContentDescription(resources.getString(R.string.sun_path_next)).performClick()
        compose.onNodeWithText(pointCaption(2, "01:00")).assertIsDisplayed()
        compose.runOnIdle { wide.value = false }
        compose.onNodeWithTag("sun-path-selected").assertIsDisplayed()
            .assert(hasAnyAncestor(hasTestTag("sun-finder-sky")))
        compose.onNodeWithText(pointCaption(2, "01:00")).assertIsDisplayed()
    }
}

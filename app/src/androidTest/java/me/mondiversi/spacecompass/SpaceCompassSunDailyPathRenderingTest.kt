package me.mondiversi.spacecompass

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
class SpaceCompassSunDailyPathRenderingTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
        .apply { setLocale(Locale.ITALIAN) }).resources
    private val path = calculateSpaceCompassSunDailyPath(LocalDate.parse("2026-10-03"), ZoneId.of("Europe/Rome"), 45.0, 9.0)
    private val hour = path.markers.first {
        it.event == SpaceCompassSunPathEvent.HOUR && Instant.ofEpochMilli(it.timeMs).atZone(path.zone).hour == 9
    }
    private fun pointCaption(number: Int, time: String): String =
        resources.getString(R.string.celestial_point_time,
            resources.getString(R.string.celestial_point_number, number), time)
    private fun currentCaption(time: String): String =
        resources.getString(R.string.celestial_point_time, resources.getString(R.string.celestial_point_current), time)

    private fun facing(position: SpaceCompassSunPosition): SpaceCompassSunOrientation {
        val a = Math.toRadians(position.azimuthDegrees); val e = Math.toRadians(position.elevationDegrees)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a), -sin(a), 0.0),
            SpaceCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            SpaceCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }
    private fun show(orientation: SpaceCompassSunOrientation? = facing(hour.position), reliable: Boolean = true,
        scene: SpaceCompassSunDailyPath = path) {
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalSpaceCompassTimeFormat provides SpaceCompassTimeFormat.H24) {
                MaterialTheme {
                    val state = rememberSpaceCompassSunDailyPathUiState()
                    Box(Modifier.size(340.dp, 400.dp).background(Color(0xFF247AB1))) {
                        SpaceCompassSunPointingViewport(hour.position, orientation, Modifier.fillMaxSize(), "test sky",
                            dailyPathUiState = state,
                            compassReliable = reliable, dailyPath = scene, primaryText = Color.Black,
                            secondaryText = Color.DarkGray, backgroundColor = Color.White, timeMs = hour.timeMs)
                    }
                    SpaceCompassSunDailyPathPage(state, Color.Black, Color.DarkGray, Color.White)
                }
            }
        }
    }
    @Test fun tappingTheLiveBodyPinsItAndItsDailyHighlightCanBeCleared() {
        show()
        compose.onNodeWithTag("sun-daily-path").performTouchInput { click(center) }
        compose.onNodeWithTag("sun-path-page").assertDoesNotExist()
        compose.onNodeWithTag("celestial-pinned-badge").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-current").assertIsDisplayed().assertIsSelected().performClick()
        compose.onNodeWithTag("sun-path-current").assertIsNotSelected()
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
        compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        compose.onNodeWithTag("sun-path-page").assertDoesNotExist()
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onAllNodes(isSelected()).assertCountEquals(0)
    }
    @Test fun tappingNearTheVisibleDotPinsItWithoutOpeningTheDailyList() {
        show()
        compose.onNodeWithTag("sun-daily-path").performTouchInput { click(center + Offset(0f, 30.dp.toPx())) }
        compose.onNodeWithTag("sun-path-page").assertDoesNotExist()
        compose.onNodeWithTag("celestial-pinned-badge").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        compose.onAllNodes(isSelected()).assertCountEquals(1)
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
    }

    @Test fun orientationRefreshBetweenPressAndReleaseDoesNotCancelTheTap() {
        val orientation = mutableStateOf(facing(hour.position))
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalSpaceCompassTimeFormat provides SpaceCompassTimeFormat.H24) {
                MaterialTheme {
                    val state = rememberSpaceCompassSunDailyPathUiState()
                    if (state.menuPath == null) Box(Modifier.size(340.dp, 400.dp)) {
                        SpaceCompassSunDailyPathLayer(path, orientation.value, state,
                            Color.Black, Color.DarkGray, Color.White)
                    } else SpaceCompassSunDailyPathPage(state, Color.Black, Color.DarkGray, Color.White)
                }
            }
        }
        compose.onNodeWithTag("sun-daily-path").performTouchInput { down(center) }
        compose.runOnIdle { orientation.value = facing(hour.position.copy(
            azimuthDegrees = hour.position.azimuthDegrees + 0.2)) }
        compose.onNodeWithTag("sun-daily-path").performTouchInput { up() }
        compose.onNodeWithTag("sun-path-page").assertDoesNotExist()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        compose.onAllNodes(isSelected()).assertCountEquals(1)
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
    }

    @Test fun dailyListMakesOffscreenHoursAccessibleAndPreservesTheLiveViewport() {
        show()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        val first = path.markers.indexOfFirst { it.event == SpaceCompassSunPathEvent.HOUR }
        compose.onNodeWithTag("sun-path-point-$first").performScrollTo().performClick()
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-point-$first").assertIsSelected()
        compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        compose.onNodeWithTag("sun-finder-sky").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
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
        compose.onNodeWithText(resources.getString(R.string.sun_path_note)).assertDoesNotExist()
        val rise = path.markers.indexOfFirst { it.event == SpaceCompassSunPathEvent.SUNRISE }
        compose.onNodeWithTag("sun-path-point-$rise").performScrollTo().assertIsDisplayed()
    }
    @Test fun repeatedDstHourLabelsIncludeTheirUtcOffsets() {
        val autumn = calculateSpaceCompassSunDailyPath(LocalDate.parse("2026-10-25"), path.zone, 45.0, 9.0)
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
        val midnight = path.markers.first { it.event == SpaceCompassSunPathEvent.HOUR }
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalSpaceCompassTimeFormat provides SpaceCompassTimeFormat.H24) {
                MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                    val point = if (below.value) midnight else hour
                    val sun = calculateSpaceCompassSunPosition(point.timeMs, fix.latitude, fix.longitude, fix.altitude)
                    val hostDensity = LocalDensity.current
                    CompositionLocalProvider(LocalDensity provides if (landscape.value)
                        Density(hostDensity.density * 0.40f, hostDensity.fontScale) else hostDensity) {
                    Box(if (landscape.value) Modifier.size(800.dp, 380.dp) else Modifier.fillMaxSize()) {
                        SpaceCompassSunFinderContent(SpaceCompassSunFinderReadings(location = fix,
                            locationStatus = SpaceCompassSunLocationStatus.READY, orientation = facing(sun),
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

    @Test fun pointNavigationUsesAFullPageInPortraitAndLandscapeAndRetainsPinsOnBack() {
        val wide = mutableStateOf(false)
        val fix = Location("synthetic-layout").apply { latitude = 45.0; longitude = 9.0; altitude = 100.0; accuracy = 5f }
        val sun = calculateSpaceCompassSunPosition(hour.timeMs, fix.latitude, fix.longitude, fix.altitude)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalResources provides resources, LocalSpaceCompassTimeFormat provides SpaceCompassTimeFormat.H24,
                LocalDensity provides if (wide.value) Density(density.density * .4f, 1f) else density) {
                MaterialTheme {
                    Box(if (wide.value) Modifier.size(800.dp, 380.dp) else Modifier.fillMaxSize()) {
                        SpaceCompassSunFinderContent(SpaceCompassSunFinderReadings(fix, SpaceCompassSunLocationStatus.READY,
                            facing(sun), true, true), hour.timeMs, Color.Black, Color.Gray, Color.White, onDismissRequest = {})
                    }
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("celestial-current-point").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasTestTag("celestial-current-point") and hasContentDescription("Sole", substring = true)).performClick()
        compose.onNodeWithTag("sun-path-page").assertDoesNotExist()
        compose.onNodeWithTag("celestial-pinned-badge").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithTag("sun-path-current").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
        compose.runOnIdle { wide.value = true }
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-current").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
        compose.onNodeWithContentDescription(resources.getString(R.string.close)).performClick()
        compose.onNodeWithTag("sun-finder-sky").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithTag("sun-path-current").assertIsSelected()
    }
}

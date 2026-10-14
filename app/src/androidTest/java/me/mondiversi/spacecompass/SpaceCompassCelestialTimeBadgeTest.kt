package me.mondiversi.spacecompass

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.util.Locale
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic scene only. Do not run a connected suite against an unbacked-up personal archive. */
class SpaceCompassCelestialTimeBadgeTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
        .apply { setLocale(Locale.ITALIAN) }).resources
    private val initialTime = Instant.parse("2026-10-03T07:00:00Z").toEpochMilli()
    private val path = calculateSpaceCompassSunDailyPath(java.time.LocalDate.parse("2026-10-03"),
        java.time.ZoneId.of("Europe/Rome"), 45.0, 9.0)
    private val point = path.markers.first { it.event == SpaceCompassSunPathEvent.HOUR && it.timeMs == initialTime }
    private val now = mutableLongStateOf(initialTime)
    private val dark = mutableStateOf(false)
    private val selected = mutableStateOf(false)
    private val pointing = mutableStateOf<SpaceCompassSunOrientation?>(facing(point.position))
    private val showPath = mutableStateOf(true)
    private val reliable = mutableStateOf(true)
    private val hasTime = mutableStateOf(true)
    private fun currentCaption(time: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("celestial-badge-event").assertTextEquals(resources.getString(R.string.celestial_point_current))
        return compose.onNodeWithTag("celestial-badge-caption").assertTextContains(time).assertTextContains("T+0h0m")
    }
    private fun pointCaption(time: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("celestial-badge-event").assertDoesNotExist()
        return compose.onNodeWithTag("celestial-badge-caption").assertTextContains(time)
    }

    private fun facing(position: SpaceCompassSunPosition): SpaceCompassSunOrientation {
        val a = Math.toRadians(position.azimuthDegrees); val e = Math.toRadians(position.elevationDegrees)
        return SpaceCompassSunOrientation(SpaceCompassSunVector(cos(a), -sin(a), 0.0),
            SpaceCompassSunVector(-sin(e) * sin(a), -sin(e) * cos(a), cos(e)),
            SpaceCompassSunVector(cos(e) * sin(a), cos(e) * cos(a), sin(e)))
    }
    private fun open(orientation: SpaceCompassSunOrientation? = facing(point.position), body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.SUN) {
        pointing.value = orientation
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalSpaceCompassTimeFormat provides SpaceCompassTimeFormat.H24) {
                MaterialTheme(colorScheme = if (dark.value) darkColorScheme() else lightColorScheme()) {
                    val state = rememberSpaceCompassSunDailyPathUiState()
                    LaunchedEffect(selected.value) {
                        if (selected.value) state.select(path, point) else state.clearSelection()
                    }
                    Box(Modifier.size(340.dp, 400.dp)) {
                        SpaceCompassSunPointingViewport(point.position, pointing.value, Modifier.fillMaxSize(), "synthetic sky", state,
                            dailyPath = if (body.supportsDailyPath && showPath.value) path else null,
                            primaryText = if (dark.value) Color.White else Color.Black,
                            secondaryText = Color.Gray, backgroundColor = if (dark.value) Color.Black else Color.White,
                            body = body, timeMs = now.longValue.takeIf { hasTime.value }, compassReliable = reliable.value)
                    }
                }
            }
        }
    }
    @Test fun aimingAtTheLiveBodyShowsCurrentThenMovingToAHourlyDotShowsThatPointsOwnTime() {
        open()
        currentCaption("09:00").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
        compose.runOnIdle { now.longValue += 60_000L }
        currentCaption("09:01").assertIsDisplayed()
        val next = path.markers.first { it.event == SpaceCompassSunPathEvent.HOUR && it.timeMs == initialTime + 3_600_000L }
        compose.runOnIdle { pointing.value = facing(next.position) }
        pointCaption("10:00").assertTextContains("T−0h59m").assertIsDisplayed()
        compose.runOnIdle { now.longValue += 2_000L }
        pointCaption("10:00").assertTextContains("T−0h58m").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-selected").assertDoesNotExist()
    }
    @Test fun anExplicitSelectionDoesNotOverrideTheDotThePhoneIsPointingAt() {
        open()
        val next = path.markers.first { it.event == SpaceCompassSunPathEvent.HOUR && it.timeMs == initialTime + 3_600_000L }
        compose.runOnIdle { selected.value = true; pointing.value = facing(next.position) }
        compose.onNodeWithTag("celestial-pinned-caption").assertDoesNotExist()
        pointCaption("10:00").assertIsDisplayed()
    }
    @Test fun lookingAwayHidesTheCaptionButHidingThePathDoesNotHideTheLiveCaption() {
        open()
        compose.onNodeWithTag("celestial-time-badge").assertIsDisplayed()
        compose.runOnIdle { pointing.value = facing(point.position.copy(elevationDegrees = 90.0)) }
        compose.onNodeWithTag("celestial-time-badge").assertDoesNotExist()
        compose.runOnIdle { pointing.value = facing(point.position); showPath.value = false }
        currentCaption("09:00").assertIsDisplayed()
    }
    @Test fun captionStaysInsideTheSkyAndOutsideTheMenuWithoutAPermanentSelectionPanelInBothThemes() {
        open()
        for (night in listOf(false, true)) {
            compose.runOnIdle { dark.value = night; selected.value = true }
            compose.onNodeWithTag("celestial-time-badge").assertIsDisplayed()
            val caption = compose.onNodeWithTag("celestial-time-badge").fetchSemanticsNode().boundsInRoot
            val sky = compose.onNodeWithTag("sun-finder-sky").fetchSemanticsNode().boundsInRoot
            val menu = compose.onNodeWithTag("sun-path-open").fetchSemanticsNode().boundsInRoot
            compose.onNodeWithTag("celestial-pinned-badge").assertDoesNotExist()
            assertTrue(caption.left >= sky.left && caption.right <= sky.right)
            assertTrue(caption.top >= sky.top && caption.bottom <= sky.bottom)
            assertFalse(caption.overlaps(menu))
        }
    }
    @Test fun aProbeWithoutATrajectoryShowsOnlyItsActualLivePointWhenInTheReticle() {
        open(body = SpaceCompassCelestialBody.VOYAGER_1)
        val time = java.time.Instant.ofEpochMilli(initialTime).atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
        currentCaption(time).assertIsDisplayed()
        compose.onNodeWithTag("sun-path-open").assertDoesNotExist()
        compose.runOnIdle { pointing.value = facing(point.position.copy(elevationDegrees = 90.0)) }
        compose.onNodeWithTag("celestial-time-badge").assertDoesNotExist()
    }
    @Test fun aMissingOrientationDoesNotInventAPointedMoment() {
        open(orientation = null)
        compose.onNodeWithTag("celestial-time-badge").assertDoesNotExist()
        compose.onNodeWithTag("sun-daily-path").assertDoesNotExist()
    }

    @Test fun anUnreliableCompassSuppressesTheLiveCaptionUntilOrientationCanBeTrusted() {
        open()
        currentCaption("09:00").assertIsDisplayed()
        compose.runOnIdle { reliable.value = false }
        compose.onNodeWithTag("celestial-time-badge").assertDoesNotExist()
        compose.runOnIdle { reliable.value = true }
        currentCaption("09:00").assertIsDisplayed()
    }

    @Test fun currentRequiresItsActualEphemerisTimestamp() {
        open()
        currentCaption("09:00").assertIsDisplayed()
        compose.runOnIdle { hasTime.value = false }
        compose.onNodeWithTag("celestial-time-badge").assertDoesNotExist()
    }
}

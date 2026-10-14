package me.mondiversi.spacecompass

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.*
import org.junit.Rule
import org.junit.Test

/** Daily rows keep ordinal names; live bubbles show only named events above clock and countdown. */
class SpaceCompassSunPathPointLabelsUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
        .apply { setLocale(Locale.ITALIAN) }).resources
    private val noon = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    private val hour = SpaceCompassSunPathPoint(noon, SpaceCompassSunPosition(180.0, 25.0))
    private val markers = (0 until 24).map { hour.copy(timeMs = noon + (it - 12) * 3_600_000L) } +
        listOf(SpaceCompassSunPathEvent.SUNRISE, SpaceCompassSunPathEvent.CULMINATION, SpaceCompassSunPathEvent.SUNSET, SpaceCompassSunPathEvent.MINIMUM)
            .map { hour.copy(event = it) }
    private val path = SpaceCompassSunDailyPath(LocalDate.parse("2026-10-04"), ZoneId.of("UTC"),
        listOf(markers.first(), markers[23]), markers.sortedBy { it.timeMs })
    private val state = SpaceCompassSunDailyPathUiState()
    private val scene = mutableStateOf(path)

    private fun open() {
        val azimuth = Math.toRadians(hour.position.azimuthDegrees)
        val elevation = Math.toRadians(hour.position.elevationDegrees)
        val orientation = SpaceCompassSunOrientation(SpaceCompassSunVector(cos(azimuth), -sin(azimuth), 0.0),
            SpaceCompassSunVector(-sin(elevation) * sin(azimuth), -sin(elevation) * cos(azimuth), cos(elevation)),
            SpaceCompassSunVector(cos(elevation) * sin(azimuth), cos(elevation) * cos(azimuth), sin(elevation)))
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalSpaceCompassTimeFormat provides SpaceCompassTimeFormat.H24) {
                MaterialTheme {
                    Box(Modifier.size(340.dp, 400.dp)) {
                        SpaceCompassSunDailyPathLayer(scene.value, orientation, state, Color.Black, Color.Gray, Color.White, timeMs = noon,
                            currentPoint = state.selectedPoints.firstOrNull { it.key.isCurrent && it.key.body == scene.value.body }?.point)
                    }
                    SpaceCompassSunDailyPathPage(state, Color.Black, Color.Gray, Color.White)
                }
            }
        }
    }

    @Test fun numberedRowsAndNamedEventsStayDistinctEvenAtTheSameInstant() {
        open()
        compose.onNodeWithTag("sun-path-open").performClick()
        for (caption in listOf("Punto 12", "Sorge", "Culmine", "Tramonta", "Minimo")) {
            compose.onNodeWithText(caption).performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("Punto 23").performScrollTo().assertIsDisplayed()
    }

    @Test fun eventSelectionUsesItsNameNotTheOrdinalOfTheOverlappingRegularPoint() {
        open()
        for ((event, caption) in listOf(SpaceCompassSunPathEvent.SUNRISE to "Sorge",
                SpaceCompassSunPathEvent.CULMINATION to "Culmine", SpaceCompassSunPathEvent.SUNSET to "Tramonta",
                SpaceCompassSunPathEvent.MINIMUM to "Minimo")) {
            compose.runOnIdle {
                scene.value = path.copy(markers = listOf(markers.single { it.event == event }))
                state.select(path, markers.single { it.event == event })
            }
            compose.onNode(hasText(caption, substring = true) and hasAnyAncestor(hasTestTag("celestial-pinned-badge")))
                .assertIsDisplayed()
        }
    }

    @Test fun focusingAnEventShowsItsNameInTheFloatingBadgeAsWell() {
        open()
        for ((event, caption) in listOf(SpaceCompassSunPathEvent.SUNRISE to "Sorge",
                SpaceCompassSunPathEvent.CULMINATION to "Culmine", SpaceCompassSunPathEvent.SUNSET to "Tramonta",
                SpaceCompassSunPathEvent.MINIMUM to "Minimo")) {
            compose.runOnIdle { scene.value = path.copy(markers = listOf(hour.copy(event = event))) }
            compose.onNodeWithTag("celestial-badge-event").assertTextEquals(caption).assertIsDisplayed()
            compose.onNodeWithTag("celestial-badge-caption").assertTextContains("12:00").assertTextContains("T+0h0m")
            compose.onNodeWithTag("celestial-badge-body").assertDoesNotExist()
        }
    }

    @Test fun eventBadgeIsNotMaskedByAnOverlappingHourlyDot() {
        open()
        for ((event, caption) in listOf(SpaceCompassSunPathEvent.SUNRISE to "Sorge",
                SpaceCompassSunPathEvent.CULMINATION to "Culmine", SpaceCompassSunPathEvent.SUNSET to "Tramonta",
                SpaceCompassSunPathEvent.MINIMUM to "Minimo")) {
            compose.runOnIdle { scene.value = path.copy(markers = listOf(hour, hour.copy(event = event))) }
            compose.onNodeWithTag("celestial-badge-event").assertTextEquals(caption).assertIsDisplayed()
            compose.onNodeWithTag("celestial-badge-caption").assertTextContains("12:00").assertTextContains("T+0h0m")
        }
    }

    @Test fun livePositionRemainsExplicitlyCurrentEvenWhenItEqualsPointTwelve() {
        open()
        compose.runOnIdle { state.selectCurrent(path.body, hour, path) }
        compose.onNode(hasText(resources.getString(R.string.celestial_point_current), substring = true) and hasAnyAncestor(hasTestTag("celestial-pinned-badge"))).assertIsDisplayed()
        compose.runOnIdle { state.select(path, hour) }
        compose.onAllNodesWithTag("celestial-pinned-badge").assertCountEquals(1)
        compose.onNodeWithTag("celestial-badge-event").assertTextEquals(resources.getString(R.string.celestial_point_current))
        compose.onAllNodesWithTag("celestial-pinned-caption").assertCountEquals(1)
        compose.runOnIdle { org.junit.Assert.assertEquals(2, state.selectedPoints.size) }
        compose.onNodeWithText("Punto 12", substring = true).assertDoesNotExist()
    }

    @Test fun anotherBodyUsesClockOnlyForRegularPointsAndKeepsEventAndCurrentHeadings() {
        val moon = path.copy(body = SpaceCompassCelestialBody.MOON)
        scene.value = moon.copy(markers = listOf(hour))
        open()
        compose.onNodeWithTag("celestial-badge-body").assertDoesNotExist()
        compose.onNodeWithTag("celestial-badge-event").assertDoesNotExist()
        compose.runOnIdle { state.select(moon, hour) }
        compose.onNodeWithTag("celestial-pinned-caption").assertTextContains("12:00").assertTextContains("T+0h0m")
        compose.runOnIdle { state.clearSelection() }
        for ((point, caption) in listOf(hour.copy(event = SpaceCompassSunPathEvent.CULMINATION) to "Culmine")) {
            compose.runOnIdle { scene.value = moon.copy(markers = listOf(point)); state.select(moon, point) }
            compose.onNode(hasText(caption, substring = true) and hasAnyAncestor(hasTestTag("celestial-pinned-badge"))).assertIsDisplayed()
        }
        compose.runOnIdle { state.selectCurrent(moon.body, hour, moon) }
        compose.onNode(hasText(resources.getString(R.string.celestial_point_current), substring = true) and hasAnyAncestor(hasTestTag("celestial-pinned-badge"))).assertIsDisplayed()
    }

    @Test fun aGenuinelyCoincidentMinimumAndRiseShareOneCaption() {
        open()
        val merged = mergeCoincidentSpaceCompassPathEvents(listOf(hour,
            hour.copy(event = SpaceCompassSunPathEvent.SUNRISE), hour.copy(event = SpaceCompassSunPathEvent.MINIMUM)))
        compose.runOnIdle { scene.value = path.copy(markers = merged) }
        compose.onNodeWithTag("celestial-badge-event").assertTextEquals("Minimo · Sorge").assertIsDisplayed()
        compose.onNodeWithTag("celestial-badge-caption").assertTextContains("12:00").assertTextContains("T+0h0m")
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithText("Minimo · Sorge · 12:00").performScrollTo().assertIsDisplayed()
    }
}

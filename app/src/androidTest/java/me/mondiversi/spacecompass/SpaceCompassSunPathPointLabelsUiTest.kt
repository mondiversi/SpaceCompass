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

/** Synthetic positions only: event/ordinal labels must agree in the list, badge and selected panel. */
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
            compose.runOnIdle { state.select(path, markers.single { it.event == event }) }
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
            compose.onNodeWithTag("celestial-badge-caption").assertTextContains(caption).assertTextContains("12:00")
                .assertIsDisplayed()
            compose.onNodeWithTag("celestial-badge-body").assertTextEquals("Sole")
        }
    }

    @Test fun eventBadgeIsNotMaskedByAnOverlappingHourlyDot() {
        open()
        for ((event, caption) in listOf(SpaceCompassSunPathEvent.SUNRISE to "Sorge",
                SpaceCompassSunPathEvent.CULMINATION to "Culmine", SpaceCompassSunPathEvent.SUNSET to "Tramonta",
                SpaceCompassSunPathEvent.MINIMUM to "Minimo")) {
            compose.runOnIdle { scene.value = path.copy(markers = listOf(hour, hour.copy(event = event))) }
            compose.onNodeWithTag("celestial-badge-caption").assertTextContains(caption).assertTextContains("12:00")
                .assertIsDisplayed()
        }
    }

    @Test fun livePositionRemainsExplicitlyCurrentEvenWhenItEqualsPointTwelve() {
        open()
        compose.runOnIdle { state.selectCurrent(path.body, hour, path) }
        compose.onNode(hasText(resources.getString(R.string.celestial_point_current), substring = true) and hasAnyAncestor(hasTestTag("celestial-pinned-badge"))).assertIsDisplayed()
        compose.runOnIdle { state.select(path, hour) }
        compose.onNode(hasText("Punto 12", substring = true) and hasAnyAncestor(hasTestTag("celestial-pinned-badge")))
            .assertIsDisplayed()
    }

    @Test fun anotherBodyKeepsItsOwnNameInBadgeAndSelectedRegularEventAndCurrentCaptions() {
        val moon = path.copy(body = SpaceCompassCelestialBody.MOON)
        scene.value = moon
        open()
        compose.onNodeWithTag("celestial-badge-body").assertTextEquals("Luna")
        for ((point, caption) in listOf(hour to "Punto 12",
                hour.copy(event = SpaceCompassSunPathEvent.CULMINATION) to "Culmine")) {
            compose.runOnIdle { state.select(moon, point) }
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
        compose.onNodeWithTag("celestial-badge-caption").assertTextContains("Minimo · Sorge").assertTextContains("12:00")
            .assertIsDisplayed()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithText("Minimo · Sorge · 12:00").performScrollTo().assertIsDisplayed()
    }
}

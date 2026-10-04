package me.mondiversi.planetcompass

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
class PlanetCompassSunPathPointLabelsUiTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.createConfigurationContext(Configuration(context.resources.configuration)
        .apply { setLocale(Locale.ITALIAN) }).resources
    private val noon = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    private val hour = PlanetCompassSunPathPoint(noon, PlanetCompassSunPosition(180.0, 25.0))
    private val markers = (0 until 24).map { hour.copy(timeMs = noon + (it - 12) * 3_600_000L) } +
        listOf(PlanetCompassSunPathEvent.SUNRISE, PlanetCompassSunPathEvent.CULMINATION, PlanetCompassSunPathEvent.SUNSET, PlanetCompassSunPathEvent.MINIMUM)
            .map { hour.copy(event = it) }
    private val path = PlanetCompassSunDailyPath(LocalDate.parse("2026-10-04"), ZoneId.of("UTC"),
        listOf(markers.first(), markers[23]), markers.sortedBy { it.timeMs })
    private val state = PlanetCompassSunDailyPathUiState()
    private val scene = mutableStateOf(path)

    private fun open() {
        val azimuth = Math.toRadians(hour.position.azimuthDegrees)
        val elevation = Math.toRadians(hour.position.elevationDegrees)
        val orientation = PlanetCompassSunOrientation(PlanetCompassSunVector(cos(azimuth), -sin(azimuth), 0.0),
            PlanetCompassSunVector(-sin(elevation) * sin(azimuth), -sin(elevation) * cos(azimuth), cos(elevation)),
            PlanetCompassSunVector(cos(elevation) * sin(azimuth), cos(elevation) * cos(azimuth), sin(elevation)))
        compose.setContent {
            CompositionLocalProvider(LocalResources provides resources, LocalPlanetCompassTimeFormat provides PlanetCompassTimeFormat.H24) {
                MaterialTheme {
                    Box(Modifier.size(340.dp, 400.dp)) {
                        PlanetCompassSunDailyPathLayer(scene.value, orientation, state, Color.Black, Color.Gray, Color.White, timeMs = noon)
                    }
                    PlanetCompassSunDailyPathDialog(state, Color.Black, Color.Gray, Color.White)
                }
            }
        }
    }

    @Test fun numberedRowsAndNamedEventsStayDistinctEvenAtTheSameInstant() {
        open()
        compose.onNodeWithTag("sun-path-open").performClick()
        for (caption in listOf("Punto 13", "Sorge", "Culmine", "Tramonta", "Minimo")) {
            compose.onNodeWithText("$caption · 12:00").performScrollTo().assertIsDisplayed()
        }
        compose.onNodeWithText("Punto 24 · 23:00").performScrollTo().assertIsDisplayed()
    }

    @Test fun eventSelectionUsesItsNameNotTheOrdinalOfTheOverlappingRegularPoint() {
        open()
        for ((event, caption) in listOf(PlanetCompassSunPathEvent.SUNRISE to "Sorge",
                PlanetCompassSunPathEvent.CULMINATION to "Culmine", PlanetCompassSunPathEvent.SUNSET to "Tramonta",
                PlanetCompassSunPathEvent.MINIMUM to "Minimo")) {
            compose.runOnIdle { state.select(path, markers.single { it.event == event }) }
            compose.onNode(hasText("Sole · $caption · 12:00") and hasAnyAncestor(hasTestTag("sun-path-selected")))
                .assertIsDisplayed()
        }
    }

    @Test fun focusingAnEventShowsItsNameInTheFloatingBadgeAsWell() {
        open()
        for ((event, caption) in listOf(PlanetCompassSunPathEvent.SUNRISE to "Sorge",
                PlanetCompassSunPathEvent.CULMINATION to "Culmine", PlanetCompassSunPathEvent.SUNSET to "Tramonta",
                PlanetCompassSunPathEvent.MINIMUM to "Minimo")) {
            compose.runOnIdle { scene.value = path.copy(markers = listOf(hour.copy(event = event))) }
            compose.onNode(hasText("$caption · 12:00") and hasAnyAncestor(hasTestTag("celestial-time-badge")))
                .assertIsDisplayed()
            compose.onNodeWithTag("celestial-badge-body").assertTextEquals("Sole")
        }
    }

    @Test fun eventBadgeIsNotMaskedByAnOverlappingHourlyDot() {
        open()
        for ((event, caption) in listOf(PlanetCompassSunPathEvent.SUNRISE to "Sorge",
                PlanetCompassSunPathEvent.CULMINATION to "Culmine", PlanetCompassSunPathEvent.SUNSET to "Tramonta",
                PlanetCompassSunPathEvent.MINIMUM to "Minimo")) {
            compose.runOnIdle { scene.value = path.copy(markers = listOf(hour, hour.copy(event = event))) }
            compose.onNode(hasText("$caption · 12:00") and hasAnyAncestor(hasTestTag("celestial-time-badge")))
                .assertIsDisplayed()
        }
    }

    @Test fun livePositionRemainsExplicitlyCurrentEvenWhenItEqualsPointThirteen() {
        open()
        compose.runOnIdle { state.selectCurrent(path.body, hour) }
        compose.onNodeWithText("Sole · Posizione attuale · 12:00").assertIsDisplayed()
        compose.runOnIdle { state.select(path, hour) }
        compose.onNode(hasText("Sole · Punto 13 · 12:00") and hasAnyAncestor(hasTestTag("sun-path-selected")))
            .assertIsDisplayed()
    }

    @Test fun anotherBodyKeepsItsOwnNameInBadgeAndSelectedRegularEventAndCurrentCaptions() {
        val moon = path.copy(body = PlanetCompassCelestialBody.MOON)
        scene.value = moon
        open()
        compose.onNodeWithTag("celestial-badge-body").assertTextEquals("Luna")
        for ((point, caption) in listOf(hour to "Punto 13",
                hour.copy(event = PlanetCompassSunPathEvent.CULMINATION) to "Culmine")) {
            compose.runOnIdle { state.select(moon, point) }
            compose.onNodeWithTag("celestial-selected-caption").assertTextEquals("Luna · $caption · 12:00")
        }
        compose.runOnIdle { state.selectCurrent(moon.body, hour) }
        compose.onNodeWithTag("celestial-selected-caption").assertTextEquals("Luna · Posizione attuale · 12:00")
    }

    @Test fun aGenuinelyCoincidentMinimumAndRiseShareOneCaption() {
        open()
        val merged = mergeCoincidentPlanetCompassPathEvents(listOf(hour,
            hour.copy(event = PlanetCompassSunPathEvent.SUNRISE), hour.copy(event = PlanetCompassSunPathEvent.MINIMUM)))
        compose.runOnIdle { scene.value = path.copy(markers = merged) }
        compose.onNode(hasText("Minimo · Sorge · 12:00") and hasAnyAncestor(hasTestTag("celestial-time-badge")))
            .assertIsDisplayed()
        compose.onNodeWithTag("sun-path-open").performClick()
        compose.onNodeWithText("Minimo · Sorge · 12:00").performScrollTo().assertIsDisplayed()
    }
}

package me.mondiversi.spacecompass

import android.location.Location
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** Disposable UI fixture only: no archive, settings or real location. */
class SpaceCompassCelestialMultiSelectionUiTest {
    @get:Rule val compose = createComposeRule()
    private val selection = mutableStateOf(SpaceCompassCelestialSelection())
    private fun show() {
        val location = Location("fixture").apply {
            latitude = 45.0; longitude = 9.0; accuracy = 5f
            altitude = 112.0; verticalAccuracyMeters = 8f
        }
        val reading = SpaceCompassSunFinderReadings(location, SpaceCompassSunLocationStatus.READY,
            SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0), SpaceCompassSunVector(0.0, 0.0, 1.0),
                SpaceCompassSunVector(0.0, 1.0, 0.0)), true, true)
        compose.setContent { MaterialTheme {
            val colors = MaterialTheme.colorScheme
            SpaceCompassSunFinderContent(reading, Instant.parse("2026-10-04T12:00:00Z").toEpochMilli(),
                colors.onSurface, colors.onSurfaceVariant, colors.background,
                body = selection.value.active ?: SpaceCompassCelestialBody.SUN,
                selectedBodies = selection.value.selected, onSelectionChange = { selection.value = it },
                onBodyChange = { selection.value = selection.value.copy(active = it) }, onDismissRequest = {})
        } }
    }
    @Test fun checkboxesKeepMenuOpenAndMasterCanSelectAllOrNone() {
        show()
        compose.onNodeWithTag("celestial-selection-count", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("celestial-select").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "2"))
        compose.onNodeWithTag("celestial-select").performClick()
        compose.onNodeWithTag("celestial-body-SUN").assertIsOn()
        compose.onNodeWithTag("celestial-body-MOON").performScrollTo().assertIsOn()
        compose.onNodeWithTag("celestial-body-MOON").performClick().assertIsOff()
        compose.runOnIdle { assertEquals(setOf(SpaceCompassCelestialBody.SUN), selection.value.selected) }
        compose.onNodeWithTag("celestial-body-MOON").performScrollTo().performClick()
        compose.onNodeWithTag("celestial-body-MOON").assertIsOn()
        compose.onNodeWithTag("celestial-menu").assertIsDisplayed()
        compose.runOnIdle { assertEquals(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON), selection.value.selected) }
        compose.onNodeWithTag("celestial-select").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "2"))
        compose.onNodeWithTag("celestial-select-all").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(spaceCompassCelestialCatalogOrder.size, selection.value.selected.size) }
        compose.onNodeWithTag("celestial-select").assert(SemanticsMatcher.expectValue(
            SemanticsProperties.StateDescription, spaceCompassCelestialCatalogOrder.size.toString()))
        compose.onNodeWithTag("celestial-select-all").performClick()
        compose.runOnIdle { assertTrue(selection.value.selected.isEmpty()); assertNull(selection.value.active) }
        compose.onNodeWithTag("celestial-selection-count", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("celestial-select").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "0"))
        compose.onNodeWithTag("celestial-menu").assertIsDisplayed()
    }
    @Test fun threePathsButOnePairOfActionsAndArrowsChangeOnlyTheInspectedObject() {
        selection.value = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON, SpaceCompassCelestialBody.MARS), SpaceCompassCelestialBody.SUN)
        show()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("sun-daily-path").fetchSemanticsNodes().size == 3 }
        compose.onAllNodesWithTag("celestial-visualize").assertCountEquals(1)
        compose.onAllNodesWithTag("sun-path-open").assertCountEquals(1)
        compose.onNodeWithTag("celestial-visualize").assert(hasAnyAncestor(hasTestTag("celestial-body-header")))
        compose.onNodeWithTag("celestial-body-next").performClick()
        compose.runOnIdle { assertEquals(SpaceCompassCelestialBody.MOON, selection.value.active); assertEquals(3, selection.value.selected.size) }
        compose.onNodeWithTag("celestial-select").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "3"))
        compose.onNodeWithTag("celestial-body-previous").performClick()
        compose.runOnIdle { assertEquals(SpaceCompassCelestialBody.SUN, selection.value.active) }
    }

    @Test fun sunOnlyHasNoUncheckedMiniaturesAndClearingSelectionRemovesEverySceneTarget() {
        selection.value = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.SUN))
        show()
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("sun-daily-path").fetchSemanticsNodes().size == 1 }
        fun assertNoMiniaturesExcept(checked: Set<SpaceCompassCelestialBody>) {
            for (candidate in SpaceCompassCelestialBody.entries.filterNot { it in checked }) {
                compose.onNodeWithTag("celestial-live-preview-${candidate.name}", useUnmergedTree = true).assertDoesNotExist()
                compose.onNodeWithTag("celestial-offscreen-preview-${candidate.name}", useUnmergedTree = true).assertDoesNotExist()
            }
        }
        assertNoMiniaturesExcept(setOf(SpaceCompassCelestialBody.SUN))
        compose.runOnIdle { selection.value = selection.value.toggleAll() }
        compose.waitUntil(15_000) { compose.onAllNodesWithTag("sun-daily-path").fetchSemanticsNodes().size >= 3 }
        compose.runOnIdle { selection.value = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.SUN)) }
        assertNoMiniaturesExcept(setOf(SpaceCompassCelestialBody.SUN))
        compose.onAllNodesWithTag("sun-daily-path").assertCountEquals(1)
        compose.runOnIdle { selection.value = SpaceCompassCelestialSelection(emptySet(), null) }
        assertNoMiniaturesExcept(emptySet())
        compose.onAllNodesWithTag("sun-daily-path").assertCountEquals(0)
        compose.onAllNodesWithTag("celestial-current-point").assertCountEquals(0)
        compose.onAllNodesWithTag("celestial-offscreen-marker").assertCountEquals(0)
    }

    @Test fun normalIslandInsetsArePreservedAndOnlyTheTitleBarHasCompactVerticalPadding() {
        selection.value = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON), SpaceCompassCelestialBody.SUN)
        show()
        val island = compose.onNodeWithTag("sun-finder-body-data").fetchSemanticsNode().boundsInRoot
        val header = compose.onNodeWithTag("celestial-body-header").fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("celestial-body-content").fetchSemanticsNode().boundsInRoot
        val data = compose.onNodeWithTag("sun-finder-pointing-data").fetchSemanticsNode().boundsInRoot
        val previous = compose.onNodeWithTag("celestial-body-previous").fetchSemanticsNode().boundsInRoot
        val inset = with(compose.density) { 12.dp.toPx() }
        assertEquals(inset, header.left - island.left, 1f)
        assertEquals(inset, island.right - header.right, 1f)
        assertEquals(inset, data.left - island.left, 1f)
        assertEquals(inset, island.right - data.right, 1f)
        val dataInset = with(compose.density) { 10.dp.toPx() }
        assertEquals(dataInset, data.top - content.top, 1f)
        assertEquals(dataInset, island.bottom - data.bottom, 1f)
        val headerInset = with(compose.density) { 1.dp.toPx() }
        assertEquals(headerInset, previous.top - header.top, 1f)
        assertEquals(headerInset, header.bottom - previous.bottom, 1f)
        assertTrue(previous.height >= with(compose.density) { 48.dp.toPx() })
    }
    @Test fun detailsAreBelowAltitudeAtTheRightEdgeAndWeatherIsOnlyInTheDialog() {
        show()
        val row = compose.onNodeWithTag("sun-finder-environment-content").fetchSemanticsNode().boundsInRoot
        val values = compose.onNodeWithTag("sun-finder-environment-values").fetchSemanticsNode().boundsInRoot
        val compass = compose.onNodeWithTag("sun-finder-compass").fetchSemanticsNode().boundsInRoot
        val info = compose.onNodeWithTag("sun-finder-info", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val column = compose.onNodeWithTag("sun-finder-compass-column").fetchSemanticsNode().boundsInRoot
        assertEquals(maxOf(values.height, column.height), row.height, 1f)
        val altitude = compose.onNodeWithTag("sun-data-altitude", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(info.bottom > altitude.bottom)
        assertEquals(values.right, info.right, 1f)
        assertTrue(info.left >= compass.right)
        assertTrue(values.left >= column.right)
        assertTrue(compass.width >= with(compose.density) { 79.dp.toPx() })
        val action = compose.onNode(hasClickAction() and hasAnyDescendant(hasTestTag("sun-finder-info")),
            useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(action.height >= with(compose.density) { 48.dp.toPx() })
        assertEquals(compass.center.y, values.center.y, 1f)
        assertTrue(values.left - compass.right >= with(compose.density) { 13.dp.toPx() })
        val environment = compose.onNodeWithTag("sun-finder-environment-data").fetchSemanticsNode().boundsInRoot
        assertEquals(with(compose.density) { 10.dp.toPx() }, row.top - environment.top, 1f)
        assertEquals(with(compose.density) { 10.dp.toPx() }, environment.bottom - row.bottom, 1f)
        assertTrue(row.height <= with(compose.density) { 82.dp.toPx() })
        compose.onNodeWithTag("sun-data-gps").assertDoesNotExist()
        compose.onNodeWithTag("sun-data-altitude", useUnmergedTree = true).assertTextEquals("112 m (±8 m)")
        compose.onNodeWithTag("sun-data-accuracy").assertDoesNotExist()
        compose.onNodeWithTag("celestial-environment-weather").assertDoesNotExist()
        compose.onNodeWithTag("sun-finder-info", useUnmergedTree = true).performClick()
        androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText(
            formatSpaceCompassSelectedCoordinates(45.0, 9.0, SpaceCompassNumericFormat.SYSTEM, false)!!))
            .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()))
        fun rowBounds(tag: String): android.graphics.Rect {
            var bounds = android.graphics.Rect()
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withTagValue(org.hamcrest.Matchers.`is`(tag)))
                .check { view, missing ->
                    if (missing != null) throw missing
                    val point = IntArray(2); view.getLocationOnScreen(point)
                    bounds = android.graphics.Rect(point[0], point[1], point[0] + view.width, point[1] + view.height)
                }
            return bounds
        }
        val coordinates = rowBounds("sun-info-coordinates")
        val height = rowBounds("sun-info-altitude")
        val weather = rowBounds("celestial-environment-weather")
        assertTrue(coordinates.bottom <= height.top)
        assertTrue(height.bottom <= weather.top)
        compose.onNodeWithTag("sun-data-gps").assertDoesNotExist()
        // The contextual information contains no solar viewing warning.
        val text = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.sun_finder_safety)
        compose.onNodeWithText(text).assertDoesNotExist()
    }

    @Test fun bodyDistanceSpeedAndAngleRowBaselinesHaveTheSameSpacing() {
        show()
        val distance = compose.onNodeWithTag("celestial-distance").fetchSemanticsNode().boundsInRoot
        val speed = compose.onNodeWithTag("celestial-speed").fetchSemanticsNode().boundsInRoot
        val angles = compose.onNodeWithTag("celestial-body-angles").fetchSemanticsNode().boundsInRoot
        assertEquals(speed.center.y - distance.center.y, angles.center.y - speed.center.y, 1f)
    }
    @Test fun orbitsTitleIsIndependentOfTheActiveObjectAndTheCatalogueActionIsAtTheRightEdge() {
        show()
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNodeWithText(context.getString(R.string.celestial_orbits_title)).assertIsDisplayed()
        val title = compose.onNodeWithTag("celestial-title").fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithTag("celestial-select").fetchSemanticsNode().boundsInRoot
        assertTrue(action.left >= title.right)
        compose.onNodeWithTag("celestial-select").performClick()
        compose.onNodeWithTag("celestial-menu").assertIsDisplayed()
    }
}

package me.mondiversi.spacecompass

import android.location.Location
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Synthetic observer and selection only; no sensor access or saved scene changes. */
class SpaceCompassObjectIslandActionsUiTest {
    @get:Rule val compose = createComposeRule()
    private val selection = mutableStateOf(SpaceCompassCelestialSelection())
    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    private fun show() {
        val fix = Location("object-action-fixture").apply { latitude=45.0; longitude=9.0; altitude=100.0; accuracy=5f }
        compose.setContent { MaterialTheme {
            val colors = MaterialTheme.colorScheme
            SpaceCompassSunFinderContent(SpaceCompassSunFinderReadings(location=fix,
                locationStatus=SpaceCompassSunLocationStatus.READY, compassAvailable=true, compassReliable=true),
                Instant.parse("2026-11-03T12:00:00Z").toEpochMilli(), colors.onSurface,
                colors.onSurfaceVariant, colors.background,
                body=selection.value.active ?: SpaceCompassCelestialBody.SUN,
                selectedBodies=selection.value.selected, onSelectionChange={ selection.value=it },
                onBodyChange={ selection.value=selection.value.copy(active=it) }, onDismissRequest={})
        } }
    }
    @Test fun removalRequiresConfirmationAndCanLeaveAnExplicitlyEmptySelection() {
        show()
        compose.onNodeWithTag("sun-path-open").assertDoesNotExist()
        compose.onNodeWithTag("celestial-body-remove").performClick()
        compose.onNodeWithTag("celestial-remove-cancel").performClick()
        compose.runOnIdle { assertEquals(setOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON),selection.value.selected) }
        compose.onNodeWithTag("celestial-body-remove").performClick()
        val remove=compose.onNodeWithTag("celestial-remove-confirm").fetchSemanticsNode().boundsInRoot
        val cancel=compose.onNodeWithTag("celestial-remove-cancel").fetchSemanticsNode().boundsInRoot
        assertTrue(remove.center.x < cancel.center.x)
        compose.onNodeWithTag("celestial-remove-confirm").performClick()
        compose.runOnIdle { assertEquals(setOf(SpaceCompassCelestialBody.MOON),selection.value.selected) }
        compose.onNodeWithTag("celestial-body-remove").performClick()
        compose.onNodeWithTag("celestial-remove-confirm").performClick()
        compose.runOnIdle { assertTrue(selection.value.selected.isEmpty()); assertNull(selection.value.active) }
        compose.onNodeWithTag("celestial-body-remove").assertDoesNotExist()
    }
    @Test fun observatoryOrdersMenuItemsAndReturnsFromPathToTheSameObject() {
        show()
        compose.onNodeWithTag("celestial-body-island-title").performClick()
        val inToolbar = hasAnyAncestor(hasTestTag("celestial-view-toolbar"))
        val favorite = compose.onNodeWithTag("observatory-favorite").assert(inToolbar).assertIsDisplayed()
        val menu = compose.onNodeWithTag("observatory-menu").assert(inToolbar).assertIsDisplayed()
        assertTrue(favorite.fetchSemanticsNode().boundsInRoot.center.x < menu.fetchSemanticsNode().boundsInRoot.center.x)
        compose.onNodeWithTag("observatory-path").assertDoesNotExist()
        compose.onNodeWithTag("observatory-export").assertDoesNotExist()
        menu.performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("observatory-path") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        val items = listOf("observatory-path" to R.string.celestial_daily_path,
            "observatory-heights" to R.string.observation_heights_title,
            "observatory-events" to R.string.key_events_title, "observatory-personal-moments" to R.string.personal_moments_title,
            "observatory-export" to R.string.panorama_export)
            .map { (tag, label) -> compose.onNodeWithTag(tag).assertIsDisplayed()
                .assertTextEquals(resources.getString(label)).fetchSemanticsNode().boundsInRoot }
        assertTrue(items.zipWithNext().all { (first, second) -> first.center.y < second.center.y })
        compose.onNodeWithTag("observatory-path").performClick()
        compose.onNodeWithTag("sun-path-page").assertIsDisplayed()
        compose.onNodeWithTag("sun-path-object-name").assertTextEquals(resources.getString(SpaceCompassCelestialBody.SUN.nameResource))
        compose.onNodeWithContentDescription(resources.getString(R.string.navigate_back)).performClick()
        compose.onNodeWithTag("celestial-viewer").assertIsDisplayed()
        compose.onNodeWithTag("observatory-menu").assertIsDisplayed()
        compose.runOnIdle { assertEquals(SpaceCompassCelestialBody.SUN,selection.value.active) }
        compose.onNodeWithContentDescription(resources.getString(R.string.navigate_back)).performClick()
        compose.onNodeWithTag("celestial-body-remove").assertIsDisplayed()
    }
}

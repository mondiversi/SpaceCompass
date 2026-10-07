package me.mondiversi.spacecompass

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.time.Instant

/** Disposable catalog fixture: no provider downloads, saved preferences or real GPS. */
class SpaceCompassCatalogRefreshUiTest {
    @get:Rule val compose = createComposeRule()
    private val start = Instant.parse("2026-10-05T12:00:00Z").toEpochMilli()
    private val time = mutableLongStateOf(start)
    private val remote = mutableStateOf(SpaceCompassCelestialRemoteData())
    private var selectionActions = 0
    private var refreshActions = 0
    private val checked = setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON)
    private val tag = "celestial-body-HALLEY"

    private fun motion(): SpaceCompassHorizonsMotion {
        // Synthetic radial motion, intentionally large enough to change the rounded distance within ten minutes.
        val vx = 240.0 / SPACE_COMPASS_AU_DAY_TO_KM_SECOND
        return SpaceCompassHorizonsMotion(SpaceCompassCelestialBody.HALLEY, listOf(
            SpaceCompassHorizonsStateSample(start, 10.0, 0.0, 0.0, vx, 0.0, 0.0),
            SpaceCompassHorizonsStateSample(start + 1_200_000L, 10.0 + vx * 1200.0 / 86_400.0,
                0.0, 0.0, vx, 0.0, 0.0)))
    }
    private fun show() {
        compose.setContent { MaterialTheme {
            CompositionLocalProvider(LocalSpaceCompassUnits provides SpaceCompassUnits(distance = "mkm"),
                LocalSpaceCompassNumericFormat provides SpaceCompassNumericFormat.INTERNATIONAL) {
                SpaceCompassCelestialCatalogPage(time.longValue, remote.value, checked,
                    MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.background,
                    onBack = {}, onToggleAll = { selectionActions++ }, onSelect = { selectionActions++ },
                    onRefresh = { refreshActions++ })
            }
        } }
        compose.onNodeWithTag("celestial-catalog-scroll").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).assertIsOff()
    }
    private fun distance(): String? = compose.onAllNodes(
        hasText("Mkm", substring = true) and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)
        .fetchSemanticsNodes().singleOrNull()?.config?.getOrNull(SemanticsProperties.Text)
        ?.joinToString { it.text }

    @Test fun arrivingDataUpdatesAnUncheckedCometWithoutOpeningItOrMovingTheList() {
        show()
        compose.onNode(hasText("—") and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true).assertExists()
        val before = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { remote.value = SpaceCompassCelestialRemoteData(
            motions = mapOf(SpaceCompassCelestialBody.HALLEY to motion())) }
        compose.waitUntil(10_000) { distance() != null }
        compose.onNodeWithTag(tag).assertIsDisplayed().assertIsOff()
        val after = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertEquals(before.top, after.top, 1f)
        compose.runOnIdle { assertEquals(0, selectionActions); assertEquals(2, checked.size) }
    }

    @Test fun theOpenCatalogKeepsDistancesStableUntilTenMinutesPass() {
        remote.value = SpaceCompassCelestialRemoteData(motions = mapOf(SpaceCompassCelestialBody.HALLEY to motion()))
        show()
        compose.waitUntil(10_000) { distance() != null }
        val before = distance()
        compose.runOnIdle { time.longValue = start + 60_000L }
        compose.waitForIdle()
        assertEquals(before, distance())
        compose.runOnIdle { time.longValue = start + 599_999L }
        compose.waitForIdle()
        assertEquals(before, distance())
        compose.runOnIdle { time.longValue = start + 600_000L }
        compose.waitUntil(10_000) { distance() != null && distance() != before }
        compose.onNodeWithTag(tag).assertIsDisplayed().assertIsOff()
        compose.runOnIdle { assertEquals(0, selectionActions) }
    }

    @Test fun manualRefreshRecalculatesWithinTheIntervalWithoutMovingOrSelectingRows() {
        remote.value = SpaceCompassCelestialRemoteData(motions = mapOf(SpaceCompassCelestialBody.HALLEY to motion()))
        show()
        compose.waitUntil(10_000) { distance() != null }
        val before = distance()
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { time.longValue = start + 120_000L }
        compose.waitForIdle()
        assertEquals(before, distance())
        compose.onNodeWithTag("catalog-refresh").assertIsEnabled().performClick()
        compose.waitUntil(10_000) { distance() != null && distance() != before }
        compose.onNodeWithTag(tag).assertIsDisplayed().assertIsOff()
        assertEquals(bounds.top, compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top, 1f)
        compose.runOnIdle { assertEquals(1, refreshActions); assertEquals(0, selectionActions) }
    }

    @Test fun anActiveRemoteBatchDisablesDuplicateManualRequests() {
        remote.value = SpaceCompassCelestialRemoteData(refreshing = true)
        show()
        compose.onNodeWithTag("catalog-refresh").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(0, refreshActions) }
        compose.runOnIdle { remote.value = remote.value.copy(refreshing = false) }
        compose.onNodeWithTag("catalog-refresh").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, refreshActions); assertEquals(0, selectionActions) }
    }

    @Test fun changingSortChangesHeaderAndValuesWithoutChangingCheckedObjects() {
        show()
        compose.onNodeWithTag("catalog-sort-toggle").performClick()
        compose.onNodeWithTag("catalog-sort-MASS_DESC").performScrollTo().performClick()
        val resources = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.resources
        compose.onNodeWithTag("catalog-value-header").assertTextEquals(resources.getString(R.string.celestial_view_mass))
        compose.onNodeWithTag("celestial-catalog-scroll").performScrollToNode(hasTestTag("celestial-body-SUN"))
        compose.onNode(hasText("kg", substring = true) and hasAnyAncestor(hasTestTag("celestial-body-SUN")),
            useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("celestial-body-SUN").assertIsOn()
        compose.onNodeWithTag("catalog-sort-toggle").performClick()
        compose.onNodeWithTag("catalog-sort-DAY_TEMPERATURE_DESC").performScrollTo().performClick()
        compose.onNodeWithTag("catalog-value-header").assertTextEquals(resources.getString(R.string.catalog_sort_day_temperature))
        compose.onNodeWithTag("celestial-catalog-scroll").performScrollToNode(hasTestTag("celestial-body-MOON"))
        compose.onNodeWithTag("catalog-value-MOON", useUnmergedTree = true).assertTextEquals("≈127 °C")
        compose.onNodeWithTag("celestial-body-MOON").assertIsOn()
        compose.runOnIdle { assertEquals(0, selectionActions) }
    }
}

package me.mondiversi.spacecompass

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Compile-only on personal devices; execute in an isolated emulator/test installation. */
class SpaceCompassKeyEventsUiTest {
    @get:Rule val compose = createComposeRule()
    private val instant = Instant.parse("2026-07-01T12:34:56Z").toEpochMilli()
    private val state = SpaceCompassObserverState(null,elapsedClock={0L})
    private val visible = mutableStateOf(true)
    private val comparisons = SpaceCompassTemporalComparisonState(SpaceCompassSunDailyPathUiState())
    private val prepared = SpaceCompassObserverPlan(28.3,-14.1,1000.0,instant,"Atlantic/Canary",
        simulatePosition=true,simulateAltitude=true,simulateZone=true)

    private fun show(complete:Boolean,night:Boolean) {
        state.apply(prepared)
        val request=SpaceCompassObservationRequest(
            SpaceCompassObservationCalendar(SpaceCompassCelestialBody.MOON,2026,ZoneId.of("Atlantic/Canary")),
            SpaceCompassObservationPlace(28.3,-14.1,1000.0,true,true),null,null)
        val events = (0..2).map { index ->
            SpaceCompassKeyEvent(SpaceCompassKeyEventKind.FULL_MOON, instant + index * 86_400_000L, 42.0)
        }
        val result=SpaceCompassKeyEventsUiState(events,complete,true,request)
        compose.setContent {
            CompositionLocalProvider(LocalSpaceCompassObserver provides state, LocalSpaceCompassComparisons provides comparisons) {
                MaterialTheme(colorScheme=if(night)darkColorScheme() else lightColorScheme()) {
                    if (visible.value) SpaceCompassKeyEventsRows(SpaceCompassCelestialBody.MOON,result,MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
    @Test fun completedCheckboxAddsAndRemovesAComparisonWithoutChangingLocation() {
        show(true,false)
        compose.onNodeWithTag("key-event-comparison-0").assertIsEnabled().assertIsOff().performClick()
        compose.onNodeWithTag("key-event-comparison-0").assertIsOn()
        compose.runOnIdle {
            assertEquals(prepared,state.plan)
            assertEquals(1,comparisons.entries.size)
            assertEquals(instant,comparisons.entries.single().timeMs)
        }
        compose.onNodeWithTag("key-event-comparison-0").performClick().assertIsOff()
        compose.runOnIdle { assertTrue(comparisons.entries.isEmpty()) }
    }
    @Test fun eventChecksMoveToTheTopOnlyWhenThePageIsReopened() {
        show(true, false)
        fun checkbox(index: Int) = compose.onNodeWithTag("key-event-comparison-$index")
        fun rowY(index: Int) = checkbox(index).fetchSemanticsNode().boundsInRoot.center.y
        fun reopen() {
            compose.runOnIdle { visible.value = false }
            compose.waitForIdle()
            compose.runOnIdle { visible.value = true }
        }
        checkbox(2).performClick().assertIsOn()
        checkbox(1).performClick().assertIsOn()
        assertTrue(rowY(0) < rowY(1) && rowY(1) < rowY(2))
        reopen()
        assertTrue(rowY(1) < rowY(2) && rowY(2) < rowY(0))
        checkbox(1).performClick().assertIsOff()
        assertTrue(rowY(1) < rowY(2))
        reopen()
        assertTrue(rowY(2) < rowY(0) && rowY(0) < rowY(1))
        checkbox(2).assertIsOn()
    }
    @Test fun aLongListRetainsItsScrollPositionAcrossAPlaceholder() {
        state.apply(prepared)
        val body = SpaceCompassCelestialBody.MOON
        val events = List(80) { index ->
            SpaceCompassKeyEvent(SpaceCompassKeyEventKind.FULL_MOON, instant + index * 86_400_000L, 42.0)
        }
        val loaded = SpaceCompassKeyEventsUiState(events, complete = true, hasLocation = true)
        val result = mutableStateOf(loaded)
        compose.setContent {
            CompositionLocalProvider(LocalSpaceCompassObserver provides state) {
                MaterialTheme {
                    SpaceCompassKeyEventsPage(body, instant, 28.3, -14.1, 1000.0,
                        SpaceCompassCelestialRemoteData(), result.value, onBack = {})
                }
            }
        }

        val target = "key-event-row-55"
        compose.onNodeWithTag(target).assertDoesNotExist()
        compose.onNodeWithTag("key-events-list").performScrollToNode(hasTestTag(target))
        val row = compose.onNodeWithTag(target).assertIsDisplayed()
        val position = row.fetchSemanticsNode().boundsInRoot.top
        compose.runOnIdle { result.value = loaded.copy(events = null, complete = false) }
        compose.onNodeWithTag("key-events-loading").assertIsDisplayed()
        compose.runOnIdle { result.value = loaded }
        assertEquals(position, row.assertIsDisplayed().fetchSemanticsNode().boundsInRoot.top, .5f)
    }

    @Test fun staleEventsKeepTheirValuesButCannotAddComparisonsWhileRefreshing() {
        show(false,true)
        compose.onNodeWithTag("key-events-loading").assertIsDisplayed()
        compose.onNodeWithTag("key-event-date-0").assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithTag("key-event-comparison-0").assertIsNotEnabled()
    }
}

package me.mondiversi.spacecompass

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
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
    private val prepared = SpaceCompassObserverPlan(28.3,-14.1,1000.0,instant,"Atlantic/Canary",
        simulatePosition=true,simulateAltitude=true,simulateZone=true)

    private fun show(complete:Boolean,night:Boolean) {
        state.apply(prepared)
        val request=SpaceCompassObservationRequest(
            SpaceCompassObservationCalendar(SpaceCompassCelestialBody.MOON,2026,ZoneId.of("Atlantic/Canary")),
            SpaceCompassObservationPlace(28.3,-14.1,1000.0,true,true),null,null)
        val result=SpaceCompassKeyEventsUiState(listOf(SpaceCompassKeyEvent(
            SpaceCompassKeyEventKind.FULL_MOON,instant,42.0)),complete,true,request)
        compose.setContent {
            CompositionLocalProvider(LocalSpaceCompassObserver provides state) {
                MaterialTheme(colorScheme=if(night)darkColorScheme() else lightColorScheme()) {
                    SpaceCompassKeyEventsRows(SpaceCompassCelestialBody.MOON,result,MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
    @Test fun completedDateTapAppliesTheInstantAndKeepsTheObserversSpatialOverrides() {
        show(true,false)
        compose.onNodeWithTag("key-event-date-0").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(prepared.position,state.plan!!.position)
            assertEquals(prepared.altitudeMeters,state.plan!!.altitudeMeters,0.0)
            assertEquals(0,state.timeSettings.multiplier)
            assertEquals(instant,state.plan!!.observationTimeMs(instant+3_600_000,ZoneId.of("UTC")))
        }
    }
    @Test fun datesFromAReplacedLocationStayVisibleButCannotBeAppliedWhileRefreshing() {
        show(false,true)
        compose.onNodeWithTag("key-events-loading").assertIsDisplayed()
        compose.onNodeWithTag("key-event-date-0").assertIsDisplayed().assertIsNotEnabled()
    }
}

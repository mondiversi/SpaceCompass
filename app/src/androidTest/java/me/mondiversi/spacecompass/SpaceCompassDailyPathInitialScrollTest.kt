package me.mondiversi.spacecompass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Synthetic daily rows: no GPS, sensors or personal preferences. */
class SpaceCompassDailyPathInitialScrollTest {
    @get:Rule val compose = createComposeRule()
    private val zone = ZoneId.of("UTC")
    private val date = LocalDate.of(2026, 6, 21)
    private val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun point(hour: Double) = SpaceCompassSunPathPoint(start + (hour * 3_600_000).toLong(),
        SpaceCompassSunPosition(180.0, 30.0))
    private val path = SpaceCompassSunDailyPath(date, zone, listOf(point(0.0), point(24.0)),
        (0..23).map { point(it.toDouble()) }, SpaceCompassCelestialBody.SUN)
    private val state = SpaceCompassSunDailyPathUiState()
    private val session = SpaceCompassObjectPageScrollSession()
    private val visible = mutableStateOf(true)

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalSpaceCompassObjectPageScrollSession provides session) {
                MaterialTheme {
                    if (visible.value) Box(Modifier.size(340.dp, 440.dp)) {
                        SpaceCompassSunDailyPathPage(state, Color.Black, Color.DarkGray, Color.White,
                            nowMs = point(13.5).timeMs)
                    }
                }
            }
        }
    }

    private fun waitForCurrentAtCenter() {
        compose.waitUntil(5_000) {
            val row = compose.onAllNodesWithTag("sun-path-current").fetchSemanticsNodes().singleOrNull()
            val viewport = compose.onAllNodesWithTag("sun-path-list-viewport").fetchSemanticsNodes().singleOrNull()
            row != null && viewport != null && row.boundsInRoot.height > 0 &&
                abs(row.boundsInRoot.center.y - viewport.boundsInRoot.center.y) < 2f
        }
    }

    @Test fun firstOpeningCentersCurrentInsteadOfTheMostRecentPinnedPoint() {
        state.togglePoint(path, path.markers[4])
        state.openMenu(path, point(13.5))
        show()
        waitForCurrentAtCenter()
    }

    @Test fun aLateCurrentSnapshotCentersOnceAndReturningKeepsManualScrolling() {
        state.openMenu(path)
        show()
        compose.runOnIdle { state.refreshMenu(path, point(13.5)) }
        waitForCurrentAtCenter()
        compose.onNodeWithTag("sun-path-point-20").performScrollTo()
        var saved = 0
        compose.runOnIdle {
            saved = session.open(path.body, SpaceCompassObjectPage.DAILY_PATH).entry.scroll.value
            visible.value = false
        }
        compose.waitForIdle()
        compose.runOnIdle { visible.value = true }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(saved, session.open(path.body, SpaceCompassObjectPage.DAILY_PATH).entry.scroll.value)
        }
        compose.onNodeWithTag("sun-path-point-20").assertIsDisplayed()
    }
}

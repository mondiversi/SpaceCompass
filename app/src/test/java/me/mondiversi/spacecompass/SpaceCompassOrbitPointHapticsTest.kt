package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassOrbitPointHapticsTest {
    private val zone = ZoneId.of("UTC")
    private val date = LocalDate.parse("2026-10-13")
    private val point = SpaceCompassSunPathPoint(date.atStartOfDay(zone).toInstant().toEpochMilli(),
        SpaceCompassSunPosition(140.0, 20.0))
    private fun path(body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.MOON) =
        SpaceCompassSunDailyPath(date, zone, listOf(point), listOf(point), body)

    @Test fun skyAndDailyRowsEachEmitOneFeedbackWithTheResultingSelection() {
        val feedback = mutableListOf<Boolean>()
        val state = SpaceCompassSunDailyPathUiState(feedback::add)
        val moon = path()
        val target = SpaceCompassCelestialSceneTarget(moon.body, point, SpaceCompassSunScenePoint(100.0, 100.0), moon)
        state.toggleScenePoint(target, zone)
        assertEquals(listOf(true), feedback)
        assertTrue(state.isSelected(moon, point))
        state.openMenu(moon)
        state.toggleMenuPoint("0")
        assertEquals(listOf(true, false), feedback)
        assertFalse(state.isSelected(moon, point))
        val mars = path(SpaceCompassCelestialBody.MARS)
        state.togglePoint(mars, point)
        assertEquals(listOf(true, false, true), feedback)
        assertTrue(state.isSelected(mars, point))
    }

    @Test fun currentScenePointAndCurrentRowShareOneToggleFeedback() {
        val feedback = mutableListOf<Boolean>()
        val state = SpaceCompassSunDailyPathUiState(feedback::add)
        val moon = path()
        state.toggleScenePoint(SpaceCompassCelestialSceneTarget(moon.body, point,
            SpaceCompassSunScenePoint(100.0, 100.0), moon, isCurrent = true), zone)
        state.openMenu(moon, point.copy(timeMs = point.timeMs + 60_000))
        state.toggleMenuPoint("current")
        assertEquals(listOf(true, false), feedback)
        assertTrue(state.selectedPoints.isEmpty())
        state.toggleCurrent(moon.body, point, moon)
        assertEquals(listOf(true, false, true), feedback)
    }

    @Test fun automaticClearingRefreshAndInvalidTargetsDoNotCreateTouchFeedback() {
        val feedback = mutableListOf<Boolean>()
        val state = SpaceCompassSunDailyPathUiState(feedback::add)
        val moon = path()
        state.select(moon, point)
        state.select(moon, point.copy(position = SpaceCompassSunPosition(141.0, 21.0)))
        state.openMenu(moon, point)
        state.closeMenu()
        state.clearSelection()
        state.togglePoint(moon, point.copy(position = SpaceCompassSunPosition(Double.NaN, 20.0)))
        state.toggleCurrent(moon.body, point.copy(position = SpaceCompassSunPosition(140.0, 100.0)), moon)
        state.toggleScenePoint(SpaceCompassCelestialSceneTarget(moon.body, point,
            SpaceCompassSunScenePoint(100.0, 100.0)), zone)
        state.toggleMenuPoint("missing")
        assertTrue(feedback.isEmpty())
        assertTrue(state.selectedPoints.isEmpty())
    }
}

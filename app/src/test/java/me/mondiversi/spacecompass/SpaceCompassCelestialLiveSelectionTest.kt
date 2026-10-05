package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SpaceCompassCelestialLiveSelectionTest {
    private val point = SpaceCompassSunPathPoint(1_791_114_900_000L, SpaceCompassSunPosition(150.0, 20.0))
    private val path = SpaceCompassSunDailyPath(LocalDate.parse("2026-10-04"), ZoneId.of("Europe/Rome"),
        listOf(point), listOf(point.copy(timeMs = point.timeMs - 900_000)), SpaceCompassCelestialBody.MARS)

    @Test fun liveSelectionKeepsItsExactInstantAndBodyAcrossRefreshesWithoutClosingTheMenu() {
        val state = SpaceCompassSunDailyPathUiState()
        state.openMenu(path)
        state.selectCurrent(path.body, point)
        assertEquals(point, state.selectedIn(path.copy(markers = emptyList())))
        assertEquals(point, state.selectedCurrentFor(path.body))
        assertEquals(path.body, state.selectedBody)
        assertSame(path, state.menuPath)
        assertNull(state.selectedCurrentFor(SpaceCompassCelestialBody.VOYAGER_1))
        assertNull(state.selectedIn(path.copy(body = SpaceCompassCelestialBody.MOON)))
    }
    @Test fun selectingAnHourlyPointOrClosingThePanelClearsTheLiveSnapshot() {
        val state = SpaceCompassSunDailyPathUiState()
        state.selectCurrent(path.body, point)
        state.select(path, path.markers.single())
        assertNull(state.selectedCurrentFor(path.body))
        assertEquals(path.markers.single(), state.selectedIn(path))
        state.selectCurrent(SpaceCompassCelestialBody.VOYAGER_1, point)
        assertEquals(SpaceCompassCelestialBody.VOYAGER_1, state.selectedBody)
        assertNull(state.selectedIn(path))
        state.clearSelection()
        assertNull(state.selectedBody)
        assertNull(state.selectedCurrentFor(SpaceCompassCelestialBody.VOYAGER_1))
        assertNull(state.selectedIn(path))
    }
    @Test fun aFrozenPopupSurvivesSelectingAPointFromAnotherBody() {
        val state = SpaceCompassSunDailyPathUiState()
        state.openMenu(path)
        state.selectCurrent(SpaceCompassCelestialBody.MOON, point)
        assertSame(path, state.menuPath)
        assertEquals(SpaceCompassCelestialBody.MOON, state.selectedBody)
        state.closeMenu()
        assertNull(state.menuPath)
        assertEquals(point, state.selectedCurrentFor(SpaceCompassCelestialBody.MOON))
    }
}

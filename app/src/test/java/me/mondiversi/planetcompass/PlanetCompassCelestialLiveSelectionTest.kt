package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PlanetCompassCelestialLiveSelectionTest {
    private val point = PlanetCompassSunPathPoint(1_791_114_900_000L, PlanetCompassSunPosition(150.0, 20.0))
    private val path = PlanetCompassSunDailyPath(LocalDate.parse("2026-10-04"), ZoneId.of("Europe/Rome"),
        listOf(point), listOf(point.copy(timeMs = point.timeMs - 900_000)), PlanetCompassCelestialBody.MARS)

    @Test fun liveSelectionKeepsItsExactInstantAndBodyAcrossRefreshesWithoutClosingTheMenu() {
        val state = PlanetCompassSunDailyPathUiState()
        state.openMenu(path)
        state.selectCurrent(path.body, point)
        assertEquals(point, state.selectedIn(path.copy(markers = emptyList())))
        assertEquals(point, state.selectedCurrentFor(path.body))
        assertEquals(path.body, state.selectedBody)
        assertSame(path, state.menuPath)
        assertNull(state.selectedCurrentFor(PlanetCompassCelestialBody.VOYAGER_1))
        assertNull(state.selectedIn(path.copy(body = PlanetCompassCelestialBody.MOON)))
    }
    @Test fun selectingAnHourlyPointOrClosingThePanelClearsTheLiveSnapshot() {
        val state = PlanetCompassSunDailyPathUiState()
        state.selectCurrent(path.body, point)
        state.select(path, path.markers.single())
        assertNull(state.selectedCurrentFor(path.body))
        assertEquals(path.markers.single(), state.selectedIn(path))
        state.selectCurrent(PlanetCompassCelestialBody.VOYAGER_1, point)
        assertEquals(PlanetCompassCelestialBody.VOYAGER_1, state.selectedBody)
        assertNull(state.selectedIn(path))
        state.clearSelection()
        assertNull(state.selectedBody)
        assertNull(state.selectedCurrentFor(PlanetCompassCelestialBody.VOYAGER_1))
        assertNull(state.selectedIn(path))
    }
    @Test fun aFrozenPopupSurvivesSelectingAPointFromAnotherBody() {
        val state = PlanetCompassSunDailyPathUiState()
        state.openMenu(path)
        state.selectCurrent(PlanetCompassCelestialBody.MOON, point)
        assertSame(path, state.menuPath)
        assertEquals(PlanetCompassCelestialBody.MOON, state.selectedBody)
        state.closeMenu()
        assertNull(state.menuPath)
        assertEquals(point, state.selectedCurrentFor(PlanetCompassCelestialBody.MOON))
    }
}

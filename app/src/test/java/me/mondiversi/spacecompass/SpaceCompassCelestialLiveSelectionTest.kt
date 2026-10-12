package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class SpaceCompassCelestialLiveSelectionTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val point = SpaceCompassSunPathPoint(1_791_114_900_000L, SpaceCompassSunPosition(150.0, 20.0))
    private val path = SpaceCompassSunDailyPath(LocalDate.parse("2026-10-04"), zone,
        listOf(point), listOf(point.copy(timeMs = point.timeMs - 900_000)), SpaceCompassCelestialBody.MARS)
    private val facing = SpaceCompassSunOrientation(SpaceCompassSunVector(-1.0, 0.0, 0.0),
        SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, -1.0, 0.0))

    @Test fun liveAndHourlySelectionsCoexistAcrossObjectsAndPageNavigation() {
        val state = SpaceCompassSunDailyPathUiState(); state.openMenu(path)
        state.selectCurrent(path.body, point, path); state.select(path, path.markers.single())
        state.selectCurrent(SpaceCompassCelestialBody.VOYAGER_1, point, zone = zone)
        assertEquals(3, state.selectedPoints.size); assertSame(path, state.menuPath)
        state.closeMenu(); assertEquals(3, state.selectedPoints.size)
        state.clearSelection(); assertTrue(state.selectedPoints.isEmpty())
    }
    @Test fun projectionUsesRefreshedLiveCoordinatesAndHidesAnUnselectedObject() {
        val state = SpaceCompassSunDailyPathUiState(); state.selectCurrent(path.body, point, path)
        val updated = point.copy(timeMs = point.timeMs + 60_000, position = SpaceCompassSunPosition(180.0, 0.0))
        val projected = projectSpaceCompassSelectedOrbitTargets(state.selectedPoints, mapOf(path.body to path),
            mapOf(path.body to updated), facing, 340.0, 400.0)
        assertEquals(updated, projected.single().point); assertEquals(170.0, projected.single().projection.x, 0.000001); assertEquals(200.0, projected.single().projection.y, 0.000001)
        assertTrue(projectSpaceCompassSelectedOrbitTargets(state.selectedPoints, emptyMap(), emptyMap(), facing, 340.0, 400.0).isEmpty())
    }
    @Test fun renewingASatelliteTimeWindowRetainsTheSelectedHistoricalPointAndItsOriginalLabelPath() {
        val historical = point.copy(position = SpaceCompassSunPosition(180.0, 0.0))
        val satellitePath = path.copy(body = SpaceCompassCelestialBody.ISS, markers = listOf(historical))
        val state = SpaceCompassSunDailyPathUiState(); state.select(satellitePath, historical)
        val newerWindow = satellitePath.copy(markers = listOf(historical.copy(timeMs = historical.timeMs + 300_000)))
        val projected = projectSpaceCompassSelectedOrbitTargets(state.selectedPoints,
            mapOf(satellitePath.body to newerWindow), emptyMap(), facing, 340.0, 400.0)
        assertEquals(historical, projected.single().point); assertSame(satellitePath, projected.single().path)
        val entries = spaceCompassSelectedDailyPathEntries(newerWindow, null, state.selectedPoints)
        val retained = entries.single { it.sourcePath != null }
        assertFalse(retained.isCurrent); assertEquals(historical, retained.point)
        assertEquals("saved-${historical.timeMs}-${historical.event.name}", retained.pointId)
        state.openMenu(newerWindow); assertEquals(retained.pointId, state.menuFocusPointId)
        state.toggleScenePoint(projected.single(), zone)
        assertTrue(state.selectedPoints.isEmpty())
    }
    @Test fun selectedMarkersProjectTheirRefreshedPositionWithoutOverwritingOtherPins() {
        val state = SpaceCompassSunDailyPathUiState(); state.select(path, path.markers.single())
        val updated = path.markers.single().copy(position = SpaceCompassSunPosition(180.0, 0.0))
        val projected = projectSpaceCompassSelectedOrbitTargets(state.selectedPoints, mapOf(path.body to path.copy(markers = listOf(updated))),
            emptyMap(), facing, 340.0, 400.0)
        assertEquals(updated, projected.single().point)
        assertTrue(projectSpaceCompassSelectedOrbitTargets(state.selectedPoints, mapOf(path.body to path), emptyMap(), null, 340.0, 400.0).isEmpty())
        assertEquals(1, state.selectedPoints.size)
    }
}

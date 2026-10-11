package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassDailyPointNavigationTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val date = LocalDate.parse("2026-10-11")
    private val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private fun point(hour: Int) = SpaceCompassSunPathPoint(start + hour * 3_600_000L, SpaceCompassSunPosition(170.0, 20.0))
    private fun path(body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.MOON) = SpaceCompassSunDailyPath(
        date, zone, listOf(point(0), point(24)), listOf(point(0), point(12), point(23)), body)

    @Test fun tappingAnHourlyPointOpensItsOwnFrozenListInsteadOfASkyIsland() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState(); val current = point(13)
        state.openPoint(p, p.markers[1], current)
        assertSame(p, state.menuPath); assertSame(current, state.menuCurrentPoint)
        assertEquals("1", state.menuHighlightedPointId)
        assertNull(state.selectedBody); assertNull(state.selectedIn(p))
    }
    @Test fun currentAndACoincidentHourKeepDifferentHighlightedIdentities() {
        val p = path(SpaceCompassCelestialBody.MARS); val state = SpaceCompassSunDailyPathUiState()
        state.openCurrent(p.body, p.markers[1], p)
        assertEquals("current", state.menuHighlightedPointId)
        val rows = spaceCompassDailyPathListEntries(state.menuPath!!, state.menuCurrentPoint)
        assertEquals(2, rows.count { it.point.timeMs == p.markers[1].timeMs })
        state.toggleMenuPoint("1")
        assertEquals("1", state.menuHighlightedPointId)
        assertSame(p, state.menuPath)
    }
    @Test fun tappingTheHighlightedRowAgainClearsOnlyTheHighlight() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState()
        state.openPoint(p, p.markers[2])
        state.toggleMenuPoint("2")
        assertNull(state.menuHighlightedPointId); assertSame(p, state.menuPath)
        state.toggleMenuPoint("0")
        assertEquals("0", state.menuHighlightedPointId)
    }
    @Test fun backAndOpeningTheNormalPathActionNeverRetainTheClickedPoint() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState()
        state.openPoint(p, p.markers[2], point(13)); state.closeMenu()
        assertNull(state.menuPath); assertNull(state.menuCurrentPoint); assertNull(state.menuHighlightedPointId)
        assertNull(state.selectedBody)
        state.openMenu(p, point(14))
        assertNull(state.menuHighlightedPointId)
        assertEquals(point(14), state.menuCurrentPoint)
    }
    @Test fun aMissingOrStaleCurveShowsOnlyTheRealTappedCurrentObservation() {
        val state = SpaceCompassSunDailyPathUiState(); val current = point(13)
        state.openCurrent(SpaceCompassCelestialBody.VOYAGER_1, current, zone = zone)
        assertEquals(SpaceCompassCelestialBody.VOYAGER_1, state.menuPath!!.body)
        assertTrue(state.menuPath!!.markers.isEmpty())
        assertEquals(listOf(current), spaceCompassDailyPathListEntries(state.menuPath!!, state.menuCurrentPoint).map { it.point })
        state.openCurrent(SpaceCompassCelestialBody.MARS, point(25), path(SpaceCompassCelestialBody.MARS))
        assertTrue(state.menuPath!!.markers.isEmpty())
        assertEquals(point(25), state.menuCurrentPoint)
    }
    @Test fun aLunarTapIncludesItsPhaseAndDoesNotFollowLaterLivePositions() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState(); val tapped = point(13)
        state.openCurrent(p.body, tapped, p)
        val snapshot = state.menuCurrentPoint!!
        assertEquals(calculateSpaceCompassMoonPhase(tapped.timeMs), snapshot.moonPhase)
        val refreshed = p.copy(markers = p.markers + point(15))
        assertNotEquals(refreshed, state.menuPath); assertSame(snapshot, state.menuCurrentPoint)
        assertEquals(tapped.timeMs, snapshot.timeMs)
    }
    @Test fun mergedNamedEventsNavigateToTheOriginalMarkerIndex() {
        val event = point(12).copy(event = SpaceCompassSunPathEvent.CULMINATION,
            coincidentEvents = setOf(SpaceCompassSunPathEvent.SUNRISE))
        val p = path().copy(markers = listOf(point(0), event)); val state = SpaceCompassSunDailyPathUiState()
        state.openPoint(p, event.copy(moonPhase = calculateSpaceCompassMoonPhase(event.timeMs)))
        assertEquals("1", state.menuHighlightedPointId)
        assertEquals(event, state.menuPath!!.markers[1])
    }
    @Test fun invalidCurrentPositionsCannotOpenAFictionalRoute() {
        val state = SpaceCompassSunDailyPathUiState()
        state.openCurrent(SpaceCompassCelestialBody.SUN, point(13).copy(position = SpaceCompassSunPosition(0.0, Double.NaN)))
        assertNull(state.menuPath); assertNull(state.menuHighlightedPointId)
    }
    @Test fun aVisibleSceneDotKeepsCurrentForItsBodyEvenWhenTheLiveObjectIsBehindTheViewer() {
        val orientation = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0, 0.0, 0.0),
            SpaceCompassSunVector(0.0, 0.0, 1.0), SpaceCompassSunVector(0.0, 1.0, 0.0))
        for (body in listOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON,
                SpaceCompassCelestialBody.MARS, SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.STARLINK_V3)) {
            val marker = point(12).copy(position = SpaceCompassSunPosition(0.0, 0.0))
            val p = path(body).copy(markers = listOf(marker))
            val current = point(13).copy(position = SpaceCompassSunPosition(180.0, -20.0))
            val observations = mapOf(body to current)
            val targets = projectSpaceCompassCelestialSceneTargets(mapOf(body to p), observations,
                orientation, 340.0, 400.0)
            assertFalse(targets.any { it.isCurrent })
            val target = tappedSpaceCompassCelestialSceneTarget(targets, SpaceCompassSunScenePoint(170.0, 200.0), 32.0)!!
            val state = SpaceCompassSunDailyPathUiState()
            state.openScenePoint(target, observations, zone)
            val row = spaceCompassDailyPathListEntries(state.menuPath!!, state.menuCurrentPoint).single { it.isCurrent }
            assertEquals(current.timeMs, row.point.timeMs); assertEquals(current.position, row.point.position)
            assertEquals("0", state.menuHighlightedPointId)
            if (body == SpaceCompassCelestialBody.MOON) assertEquals(calculateSpaceCompassMoonPhase(current.timeMs), row.point.moonPhase)
            state.toggleMenuPoint("0")
            assertEquals(row, spaceCompassDailyPathListEntries(state.menuPath!!, state.menuCurrentPoint).single { it.isCurrent })
        }
    }
    @Test fun anotherObjectsObservationCannotBeUsedAsCurrentForTheTappedOrbit() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState()
        val target = SpaceCompassCelestialSceneTarget(p.body, p.markers[1], SpaceCompassSunScenePoint(170.0, 200.0), p)
        state.openScenePoint(target, mapOf(SpaceCompassCelestialBody.SUN to point(13)), zone)
        assertSame(p, state.menuPath); assertEquals("1", state.menuHighlightedPointId)
        assertNull(state.menuCurrentPoint)
        assertFalse(spaceCompassDailyPathListEntries(p, state.menuCurrentPoint).any { it.isCurrent })
    }
    @Test fun centeringUsesTheListViewportRatherThanTheFullScreenOrRowTop() {
        assertEquals(700, spaceCompassDailyPathCenterScroll(1_000, 600, 2_000))
        assertEquals(500, spaceCompassDailyPathCenterScroll(1_000, 1_000, 2_000))
        assertEquals(700, spaceCompassDailyPathCenterScroll(1_000, 601, 2_000))
    }
    @Test fun centeringStaysInsideTheAvailableScrollExtentIncludingAnEmptyList() {
        assertEquals(0, spaceCompassDailyPathCenterScroll(100, 600, 2_000))
        assertEquals(2_000, spaceCompassDailyPathCenterScroll(3_000, 600, 2_000))
        assertEquals(0, spaceCompassDailyPathCenterScroll(1_000, 600, 0))
    }
}

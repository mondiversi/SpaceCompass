package me.mondiversi.spacecompass

import java.time.Instant
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
    private fun target(p: SpaceCompassSunDailyPath, index: Int) = SpaceCompassCelestialSceneTarget(p.body,
        p.markers[index], SpaceCompassSunScenePoint(170.0, 200.0), p)

    @Test fun tappingSeveralSkyPointsNeverOpensAPageAndKeepsEverySelection() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState()
        state.toggleScenePoint(target(p, 0), zone); state.toggleScenePoint(target(p, 2), zone)
        assertNull(state.menuPath); assertEquals(2, state.selectedPoints.size)
        assertTrue(state.isSelected(p, p.markers[0])); assertTrue(state.isSelected(p, p.markers[2]))
    }
    @Test fun selectionsFromDifferentOrbitsDoNotReplaceEachOther() {
        val moon = path(); val mars = path(SpaceCompassCelestialBody.MARS); val state = SpaceCompassSunDailyPathUiState()
        state.toggleScenePoint(target(moon, 1), zone); state.toggleScenePoint(target(mars, 1), zone)
        assertEquals(setOf(moon.body, mars.body), state.selectedPoints.map { it.key.body }.toSet())
        state.toggleScenePoint(target(moon, 1), zone)
        assertFalse(state.isSelected(moon, moon.markers[1])); assertTrue(state.isSelected(mars, mars.markers[1]))
    }
    @Test fun pageOpeningAndClosingRetainsPinsAndCentersTheMostRecentSelection() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState()
        state.togglePoint(p, p.markers[0]); state.togglePoint(p, p.markers[1]); state.openMenu(p, point(13))
        assertEquals("1", state.menuFocusPointId); state.closeMenu()
        assertNull(state.menuPath); assertNull(state.menuCurrentPoint); assertEquals(2, state.selectedPoints.size)
        state.openMenu(p, point(14)); assertEquals("1", state.menuFocusPointId)
    }
    @Test fun deselectingOneDailyRowDoesNotClearOtherPointsOrOtherOrbits() {
        val p = path(); val mars = path(SpaceCompassCelestialBody.MARS); val state = SpaceCompassSunDailyPathUiState()
        state.select(p, p.markers[0]); state.select(p, p.markers[1]); state.select(mars, mars.markers[2])
        state.openMenu(p); state.toggleMenuPoint("1")
        assertFalse(state.isSelected(p, p.markers[1])); assertTrue(state.isSelected(p, p.markers[0]))
        assertTrue(state.isSelected(mars, mars.markers[2])); assertSame(p, state.menuPath)
    }
    @Test fun selectingDailyRowsAddsPinsThatRemainAfterReturningToTheSky() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState(); state.openMenu(p)
        state.toggleMenuPoint("0"); state.toggleMenuPoint("2"); state.closeMenu()
        assertTrue(state.isSelected(p, p.markers[0])); assertTrue(state.isSelected(p, p.markers[2]))
    }
    @Test fun currentAndCoincidentHourAndNamedEventHaveDifferentSelectionIdentities() {
        val p = path().copy(markers = listOf(point(12), point(12).copy(event = SpaceCompassSunPathEvent.CULMINATION)))
        val state = SpaceCompassSunDailyPathUiState(); state.openMenu(p, point(12))
        state.toggleMenuPoint("current"); state.toggleMenuPoint("0"); state.toggleMenuPoint("1")
        assertEquals(3, state.selectedPoints.size); state.toggleMenuPoint("current")
        assertFalse(state.isSelected(p, point(12), true)); assertTrue(state.isSelected(p, p.markers[0]))
        assertTrue(state.isSelected(p, p.markers[1]))
    }
    @Test fun liveSelectionStillTogglesOffAfterItsTimestampAndPositionAdvance() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState()
        state.toggleCurrent(p.body, point(13), p)
        assertTrue(state.isCurrentSelected(p.body, point(14), zone))
        state.toggleCurrent(p.body, point(14), p)
        assertTrue(state.selectedPoints.isEmpty())
    }
    @Test fun refreshedMarkerCoordinatesAndMoonPhaseDoNotLoseTheirSelection() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState(); state.select(p, p.markers[1])
        val updated = p.markers[1].copy(position = SpaceCompassSunPosition(165.0, 18.0),
            moonPhase = calculateSpaceCompassMoonPhase(p.markers[1].timeMs))
        assertTrue(state.isSelected(p.copy(markers = listOf(updated)), updated))
    }
    @Test fun namedEventsSurviveSubMinuteRefinementButTheNextDayDoesNotMatch() {
        val p = path().copy(markers = listOf(point(12).copy(event = SpaceCompassSunPathEvent.CULMINATION)))
        val state = SpaceCompassSunDailyPathUiState(); state.select(p, p.markers[0])
        assertTrue(state.isSelected(p, p.markers[0].copy(timeMs = p.markers[0].timeMs + 5_000)))
        assertFalse(state.isSelected(p.copy(date = date.plusDays(1)), p.markers[0]))
    }
    @Test fun aRefinedNamedEventCanStillBeDeselectedFromTheDailyList() {
        val original = point(12).copy(event = SpaceCompassSunPathEvent.CULMINATION)
        val p = path().copy(markers = listOf(original)); val state = SpaceCompassSunDailyPathUiState()
        state.select(p, original)
        val refreshed = original.copy(timeMs = original.timeMs + 65_000)
        val updated = p.copy(markers = listOf(refreshed))
        assertTrue(state.isSelected(updated, refreshed))
        state.openMenu(updated); state.toggleMenuPoint("0")
        assertTrue(state.selectedPoints.isEmpty())
    }
    @Test fun repeatedDaylightSavingHoursHaveDistinctAbsoluteKeys() {
        val early = SpaceCompassSunPathPoint(Instant.parse("2026-10-25T00:30:00Z").toEpochMilli(), point(0).position)
        val late = early.copy(timeMs = early.timeMs + 3_600_000)
        assertEquals(Instant.ofEpochMilli(early.timeMs).atZone(zone).hour, Instant.ofEpochMilli(late.timeMs).atZone(zone).hour)
        assertNotEquals(spaceCompassOrbitPointKey(path().body, early, zone), spaceCompassOrbitPointKey(path().body, late, zone))
    }
    @Test fun aLiveProbeWithoutACurveCanBePinnedWithoutInventingAnOrbit() {
        val state = SpaceCompassSunDailyPathUiState(); val current = point(13)
        state.toggleScenePoint(SpaceCompassCelestialSceneTarget(SpaceCompassCelestialBody.VOYAGER_1,
            current, SpaceCompassSunScenePoint(170.0, 200.0), isCurrent = true), zone)
        assertNull(state.menuPath); assertNull(state.selectedPoints.single().path)
        assertTrue(state.selectedPoints.single().key.isCurrent)
    }
    @Test fun currentLunarSelectionPreservesTheActualPhase() {
        val state = SpaceCompassSunDailyPathUiState(); val p = path(); state.selectCurrent(p.body, point(13), p)
        assertEquals(calculateSpaceCompassMoonPhase(point(13).timeMs), state.selectedPoints.single().point.moonPhase)
    }
    @Test fun invalidPositionsAndInvalidRowIdsCannotCreatePins() {
        val state = SpaceCompassSunDailyPathUiState(); val p = path()
        state.toggleCurrent(p.body, point(13).copy(position = SpaceCompassSunPosition(0.0, Double.NaN)), p)
        state.openMenu(p); state.toggleMenuPoint("-1"); state.toggleMenuPoint("999"); state.toggleMenuPoint("unknown")
        assertTrue(state.selectedPoints.isEmpty())
    }
    @Test fun clearingAnInvalidatedScenarioRemovesPinsFromAllObjects() {
        val state = SpaceCompassSunDailyPathUiState(); val p = path(); val mars = path(SpaceCompassCelestialBody.MARS)
        state.select(p, p.markers[0]); state.select(mars, mars.markers[1]); state.clearSelection()
        assertTrue(state.selectedPoints.isEmpty())
    }
    @Test fun aSelectedDotCanKeepTheCurrentRowEvenWhenTheLiveObjectIsOffscreen() {
        val p = path(); val state = SpaceCompassSunDailyPathUiState(); state.toggleScenePoint(target(p, 1), zone)
        val current = point(13).copy(position = SpaceCompassSunPosition(180.0, -20.0))
        state.openMenu(p, current)
        assertEquals(current, spaceCompassDailyPathListEntries(p, state.menuCurrentPoint).single { it.isCurrent }.point)
        assertTrue(state.isSelected(p, p.markers[1]))
    }
    @Test fun centeringUsesTheViewportAndClampsToTheActualScrollExtent() {
        assertEquals(700, spaceCompassDailyPathCenterScroll(1_000, 600, 2_000))
        assertEquals(500, spaceCompassDailyPathCenterScroll(1_000, 1_000, 2_000))
        assertEquals(0, spaceCompassDailyPathCenterScroll(100, 600, 2_000))
        assertEquals(2_000, spaceCompassDailyPathCenterScroll(3_000, 600, 2_000))
        assertEquals(0, spaceCompassDailyPathCenterScroll(1_000, 600, 0))
    }
}

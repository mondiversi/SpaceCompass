package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPanoramaSelectionTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val date = LocalDate.parse("2026-10-12")
    private val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val time = start + 12 * 3_600_000
    private fun point(hour: Int, event: SpaceCompassSunPathEvent = SpaceCompassSunPathEvent.HOUR) =
        SpaceCompassSunPathPoint(start + hour * 3_600_000, SpaceCompassSunPosition(123.45, 20.75), event)
    private fun path(body: SpaceCompassCelestialBody = SpaceCompassCelestialBody.MOON) =
        SpaceCompassSunDailyPath(date, zone, listOf(point(0), point(24)), listOf(point(1), point(12), point(23)), body)
    private fun snapshot(paths: List<SpaceCompassSunDailyPath>, state: SpaceCompassSunDailyPathUiState) =
        SpaceCompassPanoramaSnapshot(time, 45.0, 9.0, 100.0, SpaceCompassSunSkyPhase.AFTERNOON, null,
            paths.map { SpaceCompassPanoramaObject(it.body, it.body.name, it) }, SpaceCompassCelestialRemoteData(),
            "", emptyList(), emptyList(), selectedPoints = state.selectedPoints.toList(), zoneId = zone.id)

    @Test fun captureKeepsSeveralOrbitsEvenWhenTheLiveSelectionIsLaterCleared() {
        val moon = path(); val mars = path(SpaceCompassCelestialBody.MARS)
        val state = SpaceCompassSunDailyPathUiState()
        state.select(moon, point(1)); state.select(moon, point(23)); state.select(mars, point(12))
        val captured = snapshot(listOf(moon, mars), state)
        state.clearSelection()
        val resolved = resolveSpaceCompassPanoramaSelections(captured, emptyMap())
        assertEquals(3, resolved.size)
        assertEquals(setOf(moon.body, mars.body), resolved.map { it.body }.toSet())
    }

    @Test fun currentPinsUseTheFrozenCapturePositionAndTimeAndStayDistinctFromHourlyDots() {
        val moon = path(); val state = SpaceCompassSunDailyPathUiState()
        state.selectCurrent(moon.body, point(1), moon); state.select(moon, point(12))
        val captured = snapshot(listOf(moon), state)
        val position = SpaceCompassSunPosition(220.0, -10.0)
        val resolved = resolveSpaceCompassPanoramaSelections(captured, mapOf(moon.body to position))
        assertEquals(2, resolved.size)
        val current = resolved.single { it.isCurrent }
        assertEquals(time, current.point.timeMs); assertEquals(position, current.point.position)
        assertTrue(spaceCompassPanoramaPointSelected(resolved, moon.body, point(12)))
    }

    @Test fun missingCurrentDataCannotInventAFrozenLivePoint() {
        val moon = path(); val state = SpaceCompassSunDailyPathUiState()
        state.selectCurrent(moon.body, point(1), moon)
        assertTrue(resolveSpaceCompassPanoramaSelections(snapshot(listOf(moon), state), emptyMap()).isEmpty())
    }

    @Test fun hiddenBodiesAndDifferentObservationDaysDoNotLeakIntoCapture() {
        val moon = path(); val mars = path(SpaceCompassCelestialBody.MARS)
        val state = SpaceCompassSunDailyPathUiState()
        state.select(moon, point(1)); state.select(mars, point(12))
        assertEquals(listOf(moon.body), resolveSpaceCompassPanoramaSelections(snapshot(listOf(moon), state), emptyMap()).map { it.body })
        val nextDay = moon.copy(date = date.plusDays(1))
        assertTrue(resolveSpaceCompassPanoramaSelections(snapshot(listOf(nextDay), state), emptyMap()).isEmpty())
    }

    @Test fun refinedEventsUseTheCapturedRefinementAndNotTheOlderSavedPosition() {
        val initial = path().copy(markers = listOf(point(12, SpaceCompassSunPathEvent.CULMINATION)))
        val state = SpaceCompassSunDailyPathUiState(); state.select(initial, initial.markers.single())
        val refreshedPoint = initial.markers.single().copy(timeMs = time + 60_000,
            position = SpaceCompassSunPosition(170.0, 33.0))
        val refreshed = initial.copy(markers = listOf(refreshedPoint))
        val resolved = resolveSpaceCompassPanoramaSelections(snapshot(listOf(refreshed), state), emptyMap()).single()
        assertEquals(refreshedPoint, resolved.point)
        assertTrue(spaceCompassPanoramaPointSelected(listOf(resolved), initial.body, refreshedPoint))
    }

    @Test fun rollingSatelliteWindowsRetainThePreviouslyPinnedMarkerWithoutAddingAnHourlyName() {
        val initial = path(SpaceCompassCelestialBody.ISS)
        val state = SpaceCompassSunDailyPathUiState(); state.select(initial, point(1))
        val refreshed = initial.copy(markers = listOf(point(12), point(23)))
        val captured = snapshot(listOf(refreshed), state)
        val resolved = resolveSpaceCompassPanoramaSelections(captured, emptyMap()).single()
        assertEquals(point(1), resolved.point); assertEquals(initial, resolved.path)
        assertEquals("01:00 · T+11h0m\n↔ 123.5° · ↕ +20.8°", formatSpaceCompassPanoramaSelection(captured, resolved))
    }

    @Test fun scientificAndUserPresentationsKeepPinsWhileFormattingTheirOwnCaptions() {
        val moon = path(); val state = SpaceCompassSunDailyPathUiState(); state.select(moon, point(23))
        val captured = snapshot(listOf(moon), state)
        val resolved = resolveSpaceCompassPanoramaSelections(captured, emptyMap()).single()
        val italian = captured.copy(objects = captured.objects.map { it.copy(name = "Luna") },
            formatting = SpaceCompassPanoramaFormatting(locale = Locale.ITALIAN, numeric = SpaceCompassNumericFormat.SYSTEM,
                deviceLocale = Locale.ITALIAN))
        assertEquals("23:00 · T−11h0m\n↔ 123,5° · ↕ +20,8°", formatSpaceCompassPanoramaSelection(italian, resolved))
        assertEquals("23:00 · T−11h0m\n↔ 123.5° · ↕ +20.8°", formatSpaceCompassPanoramaSelection(captured, resolved))
        assertEquals(captured.selectedPoints, italian.selectedPoints)
        assertEquals(captured.selectionTint, italian.selectionTint)
    }

    @Test fun restrictedAndScientificVariantsRetainTheCapturedActiveBodyAndAllPins() {
        val moon = path(); val mars = path(SpaceCompassCelestialBody.MARS)
        val state = SpaceCompassSunDailyPathUiState(); state.select(mars, point(1))
        val captured = snapshot(listOf(moon, mars), state).copy(activeBody = moon.body,
            labelMode = SpaceCompassPanoramaLabelMode.OBJECTS_SELECTED)
        state.clearSelection()
        for (disclosure in SpaceCompassPanoramaPosition.entries) {
            val variant = spaceCompassSnapshotForDisclosure(captured, disclosure)
                .copy(formatting = spaceCompassPanoramaInternationalFormatting,
                    center = SpaceCompassPanoramaCenter.NORTH)
            assertEquals(moon.body, variant.activeBody)
            assertEquals(captured.selectedPoints, variant.selectedPoints)
            assertTrue(variant.labelMode.showsPoint(false, moon.body == variant.activeBody))
            assertFalse(variant.labelMode.showsPoint(false, mars.body == variant.activeBody))
        }
    }

    @Test fun labelVisibilityAndPanoramaCenterChangesDoNotEraseSelection() {
        val moon = path(); val state = SpaceCompassSunDailyPathUiState(); state.select(moon, point(1))
        val captured = snapshot(listOf(moon), state)
        val modified = captured.copy(labelMode = SpaceCompassPanoramaLabelMode.NONE, center = SpaceCompassPanoramaCenter.NORTH)
        assertEquals(resolveSpaceCompassPanoramaSelections(captured, emptyMap()),
            resolveSpaceCompassPanoramaSelections(modified, emptyMap()))
    }

    @Test fun selectedAndOrdinaryHourlyPointsShareTheSameTwoLinesWithBothAngles() {
        val moon = path(); val state = SpaceCompassSunDailyPathUiState(); state.select(moon, point(1))
        val captured = snapshot(listOf(moon), state)
        val resolved = resolveSpaceCompassPanoramaSelections(captured, emptyMap()).single()
        assertEquals(formatSpaceCompassPanoramaPathPointLabel("01:00", resolved.point, captured.formatting,
            captured.pointEventNames, captured.timeMs), formatSpaceCompassPanoramaSelection(captured, resolved))
    }

    @Test fun selectedCurrentAndNamedEventsKeepTheirSpecialNameOnItsOwnTitleLine() {
        val moon = path().copy(markers = listOf(point(12, SpaceCompassSunPathEvent.CULMINATION)))
        val state = SpaceCompassSunDailyPathUiState()
        state.select(moon, moon.markers.single()); state.selectCurrent(moon.body, point(1), moon)
        val captured = snapshot(listOf(moon), state).copy(currentPointName = "Attuale",
            pointEventNames = mapOf(SpaceCompassSunPathEvent.CULMINATION to "Culmine"))
        val selected = resolveSpaceCompassPanoramaSelections(captured,
            mapOf(moon.body to SpaceCompassSunPosition(180.0, -10.0)))
        assertEquals("Culmine\n12:00 · T+0h0m\n↔ 123.5° · ↕ +20.8°",
            formatSpaceCompassPanoramaSelection(captured, selected.single { !it.isCurrent }))
        assertEquals("Attuale\n12:00 · T+0h0m\n↔ 180.0° · ↕ -10.0°",
            formatSpaceCompassPanoramaSelection(captured, selected.single { it.isCurrent }))
    }
}

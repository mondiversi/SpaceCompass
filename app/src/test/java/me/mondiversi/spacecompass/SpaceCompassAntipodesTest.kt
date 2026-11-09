package me.mondiversi.spacecompass

import androidx.compose.runtime.mutableStateOf
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassAntipodesTest {
    private val zone = ZoneId.of("UTC")
    private val date = LocalDate.parse("2026-11-02")
    private val time = date.atStartOfDay(zone).toInstant().toEpochMilli() + 12 * 3_600_000
    private val body = SpaceCompassCelestialBody.MOON
    private val point = SpaceCompassSunPathPoint(time, SpaceCompassSunPosition(30.0, 20.0))
    private val path = SpaceCompassSunDailyPath(date, zone, listOf(point), listOf(point), body)
    private fun state() = SpaceCompassSunDailyPathUiState().also { it.select(path, point) }
    private fun snapshot(state: SpaceCompassSunDailyPathUiState, enabled: Boolean = true) =
        SpaceCompassPanoramaSnapshot(time, 0.0, 0.0, 0.0, SpaceCompassSunSkyPhase.NIGHT, null,
            listOf(SpaceCompassPanoramaObject(body, "Moon", path)), SpaceCompassCelestialRemoteData(), "",
            emptyList(), emptyList(), selectedPoints = state.selectedPoints.toList(), zoneId = zone.id,
            showAntipodes = enabled)

    @Test fun antipodesAreExactlyOppositeAcrossTheSphere() {
        for (az in listOf(-360.0, -10.0, 0.0, 90.0, 179.0, 359.99, 720.0)) {
            for (el in listOf(-90.0, -67.0, 0.0, 34.0, 90.0)) {
                val source = SpaceCompassSunPathPoint(time, SpaceCompassSunPosition(az, el))
                val opposite = source.antipodalDirection()
                assertEquals(time, opposite.timeMs)
                assertEquals(-source.direction.east, opposite.direction.east, 1e-12)
                assertEquals(-source.direction.north, opposite.direction.north, 1e-12)
                assertEquals(-source.direction.up, opposite.direction.up, 1e-12)
                assertTrue(opposite.position.azimuthDegrees in 0.0..<360.0)
                val twice = opposite.antipodalDirection()
                assertEquals(source.direction.east, twice.direction.east, 1e-12)
                assertEquals(source.direction.north, twice.direction.north, 1e-12)
                assertEquals(source.direction.up, twice.direction.up, 1e-12)
            }
        }
    }
    @Test fun invalidDirectionsAreNeverFabricated() {
        for (bad in listOf(SpaceCompassSunPosition(Double.NaN, 0.0), SpaceCompassSunPosition(0.0, 91.0),
            SpaceCompassSunPosition(Double.POSITIVE_INFINITY, 20.0))) {
            assertThrows(IllegalArgumentException::class.java) { spaceCompassAntipode(bad) }
        }
    }
    @Test fun mirrorOfAnOffscreenSourceIsStillProjectedAndKeepsItsProvenance() {
        val source = point.copy(position = SpaceCompassSunPosition(0.0, 0.0))
        val orbit = path.copy(markers = listOf(source))
        val selection = SpaceCompassSunDailyPathUiState().also { it.select(orbit, source) }
        val south = SpaceCompassSunOrientation(SpaceCompassSunVector(-1.0,0.0,0.0),
            SpaceCompassSunVector(0.0,0.0,1.0), SpaceCompassSunVector(0.0,-1.0,0.0))
        assertTrue(projectSpaceCompassSelectedOrbitTargets(selection.selectedPoints, mapOf(body to orbit), emptyMap(),
            south, 400.0, 300.0).isEmpty())
        val mirrors = projectSpaceCompassSelectedOrbitTargets(selection.selectedPoints, mapOf(body to orbit), emptyMap(),
            south, 400.0, 300.0, antipodes = true)
        assertEquals(1, mirrors.size)
        assertTrue(mirrors.single().isAntipode)
        assertEquals(source, mirrors.single().antipodeOf)
        selection.toggleScenePoint(mirrors.single(), zone)
        assertTrue(selection.selectedPoints.isEmpty())
    }
    @Test fun captureRetainsBothDirectionsWhenTheMainSelectionIsLaterCleared() {
        val selection = state()
        val captured = snapshot(selection)
        selection.clearSelection()
        val pins = resolveSpaceCompassPanoramaSelections(captured, emptyMap())
        assertEquals(2, pins.size)
        assertEquals(point, pins.single { !it.isAntipode }.point)
        assertEquals(point.antipodalDirection(), pins.single { it.isAntipode }.point)
        assertEquals(1, captured.selectedPoints.size)
    }
    @Test fun hiddenAntipodesKeepOnlyTheOriginalPin() {
        val pins = resolveSpaceCompassPanoramaSelections(snapshot(state(), false), emptyMap())
        assertEquals(1, pins.size); assertFalse(pins.single().isAntipode)
    }
    @Test fun liveAntipodeUsesTheFrozenShutterCoordinates() {
        val selection = state().also { it.selectCurrent(body, point, path) }
        val captured = snapshot(selection)
        val now = SpaceCompassSunPosition(350.0, -34.0)
        val pins = resolveSpaceCompassPanoramaSelections(captured, mapOf(body to now))
        assertEquals(4, pins.size)
        assertEquals(SpaceCompassSunPosition(170.0, 34.0), pins.single { it.isCurrent && it.isAntipode }.point.position)
        assertEquals(time, pins.single { it.isCurrent && it.isAntipode }.point.timeMs)
    }
    @Test fun dailyPathExportsNeverInheritTheMainAntipodeChoice() {
        val selection = state()
        val daily = spaceCompassSingleOrbitExportSnapshot(path, point, selection.selectedPoints, time)
        assertFalse(daily.showAntipodes)
        assertTrue(spaceCompassSingleOrbitExportTable(daily, path, point).rows.none {
            it.kind == SpaceCompassOrbitExportRowKind.ANTIPODE })
        assertEquals(1, selection.selectedPoints.size)
    }
    @Test fun captureDataContainsDistinctChronologicalAntipodalRowsWithTheirAngles() {
        val captured = snapshot(state()).copy(orbitExportVocabulary = SpaceCompassOrbitExportVocabulary(antipode = "Antipodo"))
        val table = captureSpaceCompassOrbitTable(captured, emptyMap())
        val mirror = table.rows.single { it.kind == SpaceCompassOrbitExportRowKind.ANTIPODE }
        assertEquals(SpaceCompassSunPosition(210.0, -20.0), mirror.cells[body]!!.point.position)
        assertTrue(mirror.cells[body]!!.selected)
        val times = table.rows.mapNotNull { it.cells.values.firstOrNull()?.point?.timeMs }
        assertEquals(times.sorted(), times)
        val csv = renderSpaceCompassOrbitCsv(table, captured, time, "")
        val txt = renderSpaceCompassOrbitTxt(table, captured, time, "")
        assertTrue(csv.contains("Antipodo")); assertTrue(txt.contains("Antipodo"))
        assertTrue(csv.contains("210.0")); assertTrue(csv.contains("-20.0"))
    }
    @Test fun capturedLabelsAndAllPrivacyVariantsKeepTheOppositeDirection() {
        val captured = snapshot(state()).copy(orbitExportVocabulary = SpaceCompassOrbitExportVocabulary(antipode = "Antipodo"))
        for (disclosure in SpaceCompassPanoramaPosition.entries) {
            val variant = spaceCompassSnapshotForDisclosure(captured, disclosure)
            assertTrue(variant.showAntipodes)
            val pin = resolveSpaceCompassPanoramaSelections(variant, emptyMap()).single { it.isAntipode }
            val label = formatSpaceCompassPanoramaSelection(variant, pin)
            assertTrue(label.startsWith("Antipodo\n"))
            assertTrue(label.contains("210.0°")); assertTrue(label.contains("-20.0°"))
        }
    }
    @Test fun tutorialAntipodesOnlyExplainAndNeverChangeTheActualSceneOrCameraDemonstration() {
        for (visible in listOf(false, true)) {
            val selected = state()
            if (visible) selected.toggleAntipodes()
            val before = selected.selectedPoints
            val guide = SpaceCompassTutorialState(mutableStateOf(false), mutableStateOf(false), mutableStateOf(false))
            guide.start()
            repeat(3) { guide.explore(SpaceCompassTutorialFeature.ANTIPODES) }
            assertFalse(guide.cameraPreview)
            assertEquals(visible, selected.antipodesVisible)
            assertEquals(before, selected.selectedPoints)
            guide.explore(SpaceCompassTutorialFeature.CAMERA)
            assertTrue(guide.modeMenuOpen)
            assertFalse(guide.cameraPreview)
            guide.chooseMode(SpaceCompassViewMode.CAMERA)
            guide.explore(SpaceCompassTutorialFeature.ANTIPODES)
            assertTrue(guide.cameraPreview)
            assertFalse(spaceCompassTutorialCameraEnabled(true, guide.active))
            assertEquals(visible, selected.antipodesVisible)
            guide.close()
            assertEquals(before, selected.selectedPoints)
            assertEquals(visible, selected.antipodesVisible)
        }
    }
    @Test fun unavailableRefreshedCoordinatesDoNotCrashOrInventAnAntipode() {
        val selection = state().also { it.selectCurrent(body, point, path) }
        val bad = point.copy(position = SpaceCompassSunPosition(Double.NaN, 0.0))
        val stale = path.copy(markers = listOf(bad))
        val south = SpaceCompassSunOrientation(SpaceCompassSunVector(-1.0,0.0,0.0),
            SpaceCompassSunVector(0.0,0.0,1.0), SpaceCompassSunVector(0.0,-1.0,0.0))
        for (antipodes in listOf(false,true)) assertTrue(projectSpaceCompassSelectedOrbitTargets(
            selection.selectedPoints, mapOf(body to stale), mapOf(body to bad), south, 400.0,300.0,
            antipodes = antipodes).isEmpty())
        assertEquals(2, selection.selectedPoints.size)
    }
}

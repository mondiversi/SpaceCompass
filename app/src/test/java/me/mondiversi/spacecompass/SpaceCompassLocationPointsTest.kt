package me.mondiversi.spacecompass

import java.io.StringReader
import java.io.StringWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.Properties
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassLocationPointsTest {
    private val date = LocalDate.parse("2026-11-02")
    private val zone = ZoneId.of("Europe/Rome")
    private val time = date.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
    private val moon = SpaceCompassCelestialBody.MOON
    private val sun = SpaceCompassCelestialBody.SUN
    private val point = SpaceCompassSunPathPoint(time, SpaceCompassSunPosition(30.0, 20.0),
        moonPhase = SpaceCompassMoonPhase(90.0, .5))
    private val path = SpaceCompassSunDailyPath(date, zone, listOf(point), listOf(point), moon)
    private val objects = SpaceCompassCelestialSelection(setOf(sun, moon), moon)
    private val base = SpaceCompassScenarioArchive("Selected points",
        spaceCompassObserverPlan(45.0, 9.0, 80.0, time, zone.id)!!, SpaceCompassScenarioTimeSettings())
    private fun saved(p: SpaceCompassSunPathPoint = point, body: SpaceCompassCelestialBody = moon,
        current: Boolean = false) = SpaceCompassSelectedOrbitPoint(
            spaceCompassOrbitPointKey(body, p, zone, date, current), p, path.copy(body = body))
    private fun archive(points: List<SpaceCompassSelectedOrbitPoint> = listOf(saved())) = base.withCelestialObjects(objects, points)
    private fun change(text: String = archive().encode(), block: Properties.() -> Unit): String {
        val p = Properties().apply { load(StringReader(text)); block() }
        return StringWriter().also { p.store(it, null) }.toString()
    }
    private fun changeField(field: Int, value: String, text: String = archive().encode()) = change(text) {
        val f = getProperty("point.0").split(',').toMutableList(); f[field] = value
        setProperty("point.0", f.joinToString(","))
    }
    private fun rejects(text: String) = assertThrows(Exception::class.java) { SpaceCompassScenarioArchive.decode(text) }

    @Test fun ordinaryCurrentKeyAndApproachPointsRetainAllPortableMetadata() {
        val rise = point.copy(timeMs = time - 3_600_000, event = SpaceCompassSunPathEvent.SUNRISE,
            coincidentEvents = setOf(SpaceCompassSunPathEvent.CULMINATION))
        val approach = point.copy(timeMs = time + 3_600_000, event = SpaceCompassSunPathEvent.APPROACH,
            approach = SpaceCompassApproach(sun, .75))
        val source = archive(listOf(saved(), saved(current = true), saved(rise, sun), saved(approach)))
        val restored = SpaceCompassScenarioArchive.decode(source.encode())
        assertEquals(source, restored)
        assertEquals(4, restored.selectedOrbitPoints!!.size)
        assertTrue(restored.selectedOrbitPoints.all { it.path == null })
        assertEquals(point.moonPhase, restored.selectedOrbitPoints.first().point.moonPhase)
        assertEquals(objects, restored.celestialSelection)
    }

    @Test fun uncheckedObjectsAreOmittedAndNothingExportsDerivedAntipodes() {
        val state = SpaceCompassSunDailyPathUiState()
        state.select(path, point); state.select(path.copy(body = sun), point)
        state.toggleAntipodes()
        val source = base.withCelestialObjects(objects.copy(selected = setOf(moon)), state.selectedPoints)
        assertEquals(1, source.selectedOrbitPoints!!.size)
        assertEquals(point.position, source.selectedOrbitPoints.single().point.position)
        assertTrue(state.antipodesVisible)
        assertFalse(source.encode().contains("antipod", ignoreCase = true))
    }

    @Test fun oldFilesLeaveExistingPinsAndAntipodeChoiceUntouched() {
        val state = SpaceCompassSunDailyPathUiState()
        state.select(path, point); state.toggleAntipodes()
        val old = SpaceCompassScenarioArchive.decode(base.copy(celestialSelection = objects).encode())
        assertNull(old.selectedOrbitPoints)
        state.restoreImportedPoints(old.selectedOrbitPoints)
        assertEquals(1, state.selectedPoints.size)
        assertTrue(state.antipodesVisible)
    }

    @Test fun anExplicitEmptyListClearsPinsAndAntipodesWithoutFeedback() {
        var feedback = 0
        val state = SpaceCompassSunDailyPathUiState { feedback++ }
        state.select(path, point); state.toggleAntipodes()
        state.restoreImportedPoints(SpaceCompassScenarioArchive.decode(archive(emptyList()).encode()).selectedOrbitPoints)
        assertTrue(state.selectedPoints.isEmpty())
        assertFalse(state.antipodesVisible)
        assertEquals(0, feedback)
    }

    @Test fun importedPinsReplaceRatherThanMergeAndRemainIndividuallyDeselectable() {
        val state = SpaceCompassSunDailyPathUiState()
        state.select(path, point.copy(timeMs = time - 3_600_000)); state.toggleAntipodes()
        val restored = SpaceCompassScenarioArchive.decode(archive().encode())
        state.restoreImportedPoints(restored.selectedOrbitPoints)
        assertEquals(restored.selectedOrbitPoints, state.selectedPoints)
        assertFalse(state.antipodesVisible)
        state.togglePoint(path, point)
        assertTrue(state.selectedPoints.isEmpty())
    }

    @Test fun importedLivePositionsFollowRefreshedDataAndFallbackPinsKeepTheirTouchPath() {
        val north = SpaceCompassSunOrientation(SpaceCompassSunVector(1.0,0.0,0.0),
            SpaceCompassSunVector(0.0,0.0,1.0), SpaceCompassSunVector(0.0,1.0,0.0))
        val current = point.copy(timeMs = time + 60_000, position = SpaceCompassSunPosition(31.0,21.0))
        val state = SpaceCompassSunDailyPathUiState()
        state.restoreImportedPoints(SpaceCompassScenarioArchive.decode(archive(listOf(saved(current = true))).encode()).selectedOrbitPoints)
        val targets = projectSpaceCompassSelectedOrbitTargets(state.selectedPoints, mapOf(moon to path),
            mapOf(moon to current), north, 800.0, 600.0)
        assertEquals(current.position, targets.single().point.position)
        state.restoreImportedPoints(SpaceCompassScenarioArchive.decode(archive().encode()).selectedOrbitPoints)
        val refreshedPath = path.copy(markers = emptyList())
        val rows = spaceCompassSelectedDailyPathEntries(refreshedPath, null, state.selectedPoints)
        assertEquals(point, rows.single().point)
        assertNotNull(rows.single().sourcePath)
        val fallback = projectSpaceCompassSelectedOrbitTargets(state.selectedPoints,
            mapOf(moon to refreshedPath), emptyMap(), north, 800.0, 600.0).single()
        assertNotNull(fallback.path)
        state.toggleScenePoint(fallback, zone)
        assertTrue(state.selectedPoints.isEmpty())
    }

    @Test fun repeatedDstHoursRetainDistinctAbsoluteInstants() {
        val day = LocalDate.parse("2026-10-25")
        val points = listOf("2026-10-25T00:30:00Z", "2026-10-25T01:30:00Z").map {
            val p = point.copy(timeMs = Instant.parse(it).toEpochMilli())
            SpaceCompassSelectedOrbitPoint(spaceCompassOrbitPointKey(moon, p, zone, day), p, null)
        }
        assertEquals(points, SpaceCompassScenarioArchive.decode(archive(points).encode()).selectedOrbitPoints)
    }

    @Test fun bothExportProfilesPreserveCanonicalPointsWithoutChangingLocationOrClockChoices() {
        val source = archive().copy(configuration = base.configuration.copy(simulateAltitude = false, simulateTime = false))
        for (mode in SpaceCompassPanoramaExportMode.entries) {
            val format = if (mode == SpaceCompassPanoramaExportMode.SELECTED)
                SpaceCompassPanoramaFormatting(Locale.ITALIAN, SpaceCompassNumericFormat.EUROPEAN,
                    SpaceCompassDateFormat.EUROPEAN, SpaceCompassTimeFormat.H24, deviceLocale = Locale.ITALY)
                else spaceCompassPanoramaInternationalFormatting
            val profile = SpaceCompassDataExportProfile(format, SpaceCompassUnits(), SpaceCompassTextResources { _, _ -> "Label" })
            assertEquals(source, SpaceCompassScenarioArchive.decode(spaceCompassScenarioExportText(source, mode, profile)))
        }
    }

    @Test fun unknownIdsAndPointsForUncheckedObjectsDoNotAddAnyObjectOrPin() {
        for (body in listOf("FUTURE_OBJECT", SpaceCompassCelestialBody.MARS.name)) {
            val restored = SpaceCompassScenarioArchive.decode(changeField(0, body))
            assertEquals(objects, restored.celestialSelection)
            assertTrue(restored.selectedOrbitPoints!!.isEmpty())
        }
    }

    @Test fun malformedFlagsCountsVersionsAndMissingRowsAreRejected() {
        for ((key,value) in listOf("pointsIncluded" to "1", "pointsVersion" to "2", "pointsCount" to "-1",
            "pointsCount" to "10001", "pointsCount" to "2")) rejects(change { setProperty(key,value) })
        rejects(change { remove("point.0") })
        rejects(change { remove("objectsIncluded"); remove("objects"); remove("activeObject") })
        rejects(change { setProperty("pointsCount","2"); setProperty("point.1",getProperty("point.0")) })
    }

    @Test fun invalidGeometryTimeAndMetadataAreRejectedBeforeReplacingAnySelection() {
        for ((field,value) in listOf(1 to "1900-99-99",2 to "Not/AZone",3 to Long.MAX_VALUE.toString(),
            4 to "NaN",4 to "361",5 to "91",6 to "TRUE",7 to "UNKNOWN",9 to "NaN",10 to "2"))
            rejects(changeField(field,value))
        val state = SpaceCompassSunDailyPathUiState()
        state.select(path,point);state.toggleAntipodes()
        assertThrows(IllegalArgumentException::class.java) {
            state.restoreImportedPoints(listOf(saved().copy(point = point.copy(position = SpaceCompassSunPosition(0.0,91.0)))))
        }
        assertEquals(1,state.selectedPoints.size); assertTrue(state.antipodesVisible)
        val observer = SpaceCompassObserverState(null,elapsedClock={0L})
        observer.apply(base.configuration)
        val original = observer.savedPlan
        assertThrows(IllegalArgumentException::class.java) {
            observer.importArchive(archive().copy(selectedOrbitPoints=listOf(saved().copy(point=point.copy(
                position=SpaceCompassSunPosition(Double.NaN,0.0))))))
        }
        assertEquals(original,observer.savedPlan)
    }

    @Test fun oldReadersCanStillReadTheUnchangedBaseAndArchiveSizeIsBounded() {
        val text = archive().encode()
        val legacy = text.lines().filterNot { it.startsWith("point.") || it.startsWith("points") }.joinToString("\n")
        assertEquals(base.copy(celestialSelection=objects),SpaceCompassScenarioArchive.decode(legacy))
        assertTrue(text.toByteArray(Charsets.UTF_8).size < SPACE_COMPASS_SCENARIO_FILE_LIMIT)
        assertFalse(text.contains("samples=")); assertFalse(text.contains("markers="))
    }
}

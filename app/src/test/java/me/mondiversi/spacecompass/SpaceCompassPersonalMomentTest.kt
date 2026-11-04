package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPersonalMomentTest {
    private val sun = SpaceCompassCelestialBody.SUN
    private val date = LocalDate.of(2026, 6, 21)
    private val rome = ZoneId.of("Europe/Rome")
    private val london = ZoneId.of("Europe/London")
    private val timed = SpaceCompassPersonalMoment(sun, "Solstizio | prova", date, LocalTime.of(13, 30))
    private val untimed = SpaceCompassPersonalMoment(sun, "Percorso del giorno", date.minusDays(1), null)

    @Test fun localDateAndTimeFollowZoneRulesWithoutChangingVariantIdentity() {
        val a = timed.resolve(rome)
        val b = timed.resolve(london)
        assertEquals(a.id, b.id)
        assertEquals(date, a.date)
        assertEquals(date, b.date)
        assertEquals(3_600_000L, b.timeMs - a.timeMs)
        assertTrue(a.hasReference)
        assertEquals(timed.time, Instant.ofEpochMilli(b.timeMs).atZone(london).toLocalTime())
        assertFalse(untimed.resolve(rome).hasReference)
        assertEquals(untimed.date, untimed.resolve(london).date)
    }

    @Test fun savingIsUncheckedAndDisablingAnActiveMomentKeepsItsDefinition() {
        val parents = mutableListOf<SpaceCompassCelestialBody>()
        val state = SpaceCompassTemporalComparisonState(SpaceCompassSunDailyPathUiState()) { parents.add(it) }
        state.addPersonalMoment(timed.copy(enabled = true))
        assertEquals(listOf(timed), state.personalMoments)
        assertTrue(state.sceneEntries(rome).isEmpty())
        state.togglePersonalMoment(timed.id)
        assertEquals(timed.id, state.activeId)
        assertEquals(listOf(timed.resolve(rome)), state.sceneEntries(rome))
        state.remove(timed.id)
        assertEquals(listOf(timed), state.personalMoments)
        assertEquals(listOf(sun), parents)
        assertNull(state.activeId)
        assertTrue(state.selectComparison(timed.resolve(rome)))
        assertEquals(listOf(timed.resolve(rome)), state.sceneEntries(rome))
        state.deletePersonalMoment(timed.id)
        assertFalse(state.selectComparison(timed.resolve(rome)))
        assertTrue(state.personalMoments.isEmpty())
    }

    @Test fun bulkToggleClearsPartialSelectionOrEnablesAllMomentsOfOnlyTheDisplayedBody() {
        val parents = mutableListOf<SpaceCompassCelestialBody>()
        val state = SpaceCompassTemporalComparisonState(SpaceCompassSunDailyPathUiState()) { parents.add(it) }
        val moon = timed.copy(body = SpaceCompassCelestialBody.MOON, enabled = true)
        state.restore(null, timed.id, listOf(untimed, timed.copy(enabled = true), moon))

        state.toggleAllPersonalMoments(sun)
        assertEquals(listOf(untimed, timed, moon), state.personalMoments)
        assertEquals(listOf(sun), parents)
        assertNull(state.activeId)
        state.toggleAllPersonalMoments(sun)
        assertTrue(state.personalMoments.all { it.enabled })
        assertEquals(timed.id, state.activeId)
        state.toggleAllPersonalMoments(sun)
        assertEquals(listOf(untimed, timed, moon), state.personalMoments)
        assertEquals(listOf(sun, sun), parents)
    }

    @Test fun dedicatedFilePreservesCheckedAndUncheckedMomentsAndRejectsLocation() {
        val file = SpaceCompassPersonalMomentsFile(sun, listOf(timed.copy(enabled = true), untimed))
        val text = file.encode()
        assertEquals(file, SpaceCompassPersonalMomentsFile.decode(text))
        assertFalse(text.contains("personalPointsCount"))
        assertThrows(IllegalArgumentException::class.java) { SpaceCompassScenarioArchive.decode(text) }
        assertThrows(IllegalArgumentException::class.java) {
            SpaceCompassPersonalMomentsFile.decode(text.replace("space-compass-personal-moments", "space-compass-scenario"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpaceCompassPersonalMomentsFile.decode(text.replace("object=SUN", "object=MOON"))
        }
    }

    @Test fun exportScopesKeepTheChosenBodiesAndChecks() {
        val checked = timed.copy(enabled = true)
        val moon = untimed.copy(body = SpaceCompassCelestialBody.MOON, enabled = true)
        val all = listOf(checked, untimed, moon)
        assertEquals(all, SpaceCompassPersonalMomentsExportScope.ALL.momentsFor(sun, all))
        assertEquals(listOf(checked, untimed), SpaceCompassPersonalMomentsExportScope.OBJECT.momentsFor(sun, all))
        assertEquals(listOf(checked, moon), SpaceCompassPersonalMomentsExportScope.ALL.momentsFor(sun, all, onlySelected = true))
        assertEquals(listOf(checked), SpaceCompassPersonalMomentsExportScope.OBJECT.momentsFor(sun, all, onlySelected = true))
        assertTrue(SpaceCompassPersonalMomentsExportScope.OBJECT.momentsFor(SpaceCompassCelestialBody.MARS, all).isEmpty())
        assertTrue(SpaceCompassPersonalMomentsExportScope.ALL.momentsFor(sun, listOf(untimed), onlySelected = true).isEmpty())
    }

    @Test fun csvKeepsCivilDatesAndBlankTimesAndEscapesUserInput() {
        val labels = mapOf(R.string.personal_moments_object_column to "Object", R.string.celestial_view_name to "Name",
            R.string.observer_date to "Date", R.string.observer_time to "Time", R.string.capture_table_selected to "Selected",
            sun.nameResource to "Sun", R.string.celestial_view_yes to "Yes", R.string.celestial_view_no to "No")
        val profile = SpaceCompassDataExportProfile(spaceCompassPanoramaInternationalFormatting, SpaceCompassUnits(),
            SpaceCompassTextResources { id, _ -> labels.getValue(id) })
        val csv = renderSpaceCompassPersonalMomentsCsv(listOf(timed.copy(name = "=1+1", enabled = true), untimed), profile)
        val lines = csv.removePrefix("\uFEFF").trimEnd().lines()
        assertEquals("\"Object\",\"Name\",\"Date\",\"Time\",\"Selected\"", lines[0])
        assertEquals("\"Sun\",\"'=1+1\",\"2026-06-21\",\"13:30\",\"Yes\"", lines[1])
        assertEquals("\"Sun\",\"Percorso del giorno\",\"2026-06-20\",\"\",\"No\"", lines[2])
    }

    @Test fun collectionFileRoundTripsMultipleObjectsAndImportsWithoutReplacingExistingChecks() {
        val moon = untimed.copy(body = SpaceCompassCelestialBody.MOON, enabled = true)
        val file = SpaceCompassPersonalMomentsFile(null, listOf(timed.copy(enabled = true), untimed, moon))
        val text = file.encode()
        assertEquals(file, SpaceCompassPersonalMomentsFile.decode(text))
        assertThrows(IllegalArgumentException::class.java) {
            SpaceCompassPersonalMomentsFile.decode(text.replace("scope=collection", "scope=collection\nobject=SUN"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SpaceCompassPersonalMomentsFile.decode(text.replace("scope=collection", ""))
        }
        val state = SpaceCompassTemporalComparisonState(SpaceCompassSunDailyPathUiState())
        state.addPersonalMoment(timed)
        assertEquals(2, state.importPersonalMoments(SpaceCompassPersonalMomentsFile.decode(text).moments))
        assertEquals(listOf(timed, untimed, moon), state.personalMoments)
        assertEquals(0, state.importPersonalMoments(file.moments))
    }

    @Test fun dedicatedImportKeepsExistingChecksAndDoesNotSelectAnotherObject() {
        val state = SpaceCompassTemporalComparisonState(SpaceCompassSunDailyPathUiState())
        val moon = timed.copy(body = SpaceCompassCelestialBody.MOON, enabled = true)
        state.restore(null, moon.id, listOf(moon, timed))
        val incoming = listOf(timed.copy(enabled = true), untimed.copy(enabled = true))
        assertEquals(1, state.importPersonalMoments(incoming))
        assertEquals(listOf(moon, timed, untimed.copy(enabled = true)), state.personalMoments)
        assertEquals(moon.id, state.activeId)
        assertEquals(0, state.importPersonalMoments(incoming))
    }

    @Test fun locationRoundTripIncludesOnlyEnabledMomentsAndTheirPins() {
        val enabled = timed.copy(enabled = true)
        val comparison = enabled.resolve(london)
        val path = calculateSpaceCompassSunDailyPath(date, london, 45.0, 9.0, 100.0).copy(comparison = comparison)
        val points = SpaceCompassSunDailyPathUiState()
        points.select(path, path.markers.first())
        val base = SpaceCompassScenarioArchive("Example", spaceCompassObserverPlan(45.0, 9.0, 100.0,
            comparison.timeMs, rome.id)!!, SpaceCompassScenarioTimeSettings())
        val archive = base.withCelestialObjects(SpaceCompassCelestialSelection(setOf(sun), sun), points.selectedPoints,
            emptyList(), timed.id, personalMoments = listOf(enabled, untimed))
        val decoded = SpaceCompassScenarioArchive.decode(archive.encode())
        assertEquals(archive, decoded)
        assertEquals(listOf(enabled), decoded.personalMoments)
        assertNull(SpaceCompassScenarioArchive.decode(base.encode()).personalMoments)
        assertEquals(listOf(timed, untimed), mergeSpaceCompassPersonalMoments(listOf(timed, untimed), decoded.personalMoments))
        val restored = SpaceCompassTemporalComparisonState(SpaceCompassSunDailyPathUiState())
        restored.restore(decoded.comparisons, decoded.activeComparisonId, decoded.personalMoments)
        assertEquals(timed.id, restored.activeId)
        assertEquals(listOf(timed.resolve(rome)), restored.sceneEntries(rome))
        restored.restore(decoded.comparisons, decoded.activeComparisonId, decoded.personalMoments)
        assertEquals(listOf(enabled), restored.personalMoments)
    }
}

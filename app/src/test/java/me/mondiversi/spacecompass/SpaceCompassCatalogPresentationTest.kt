package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassCatalogPresentationTest {
    private val sun = SpaceCompassCelestialBody.SUN
    private val moon = SpaceCompassCelestialBody.MOON
    private val mars = SpaceCompassCelestialBody.MARS
    private val comet = SpaceCompassCelestialBody.HALLEY
    private val bodies = listOf(sun, moon, mars, comet)
    private val names = mapOf(sun to "Sun", moon to "Moon", mars to "Mars", comet to "Halley")
    private val distances = SpaceCompassCatalogDistances(emptyMap(), emptyMap(), emptyMap())
    private fun request(preferences: SpaceCompassCatalogPreferences = SpaceCompassCatalogPreferences(),
        search: String = "", selected: Set<SpaceCompassCelestialBody> = setOf(moon, mars),
        favorites: Set<SpaceCompassCelestialBody> = emptySet(),
        comparisons: List<SpaceCompassTemporalComparison> = emptyList()) = SpaceCompassCatalogQuery(
        preferences, search, selected, setOf(moon), favorites, emptyMap(), distances, comparisons)
    private suspend fun presenter() = SpaceCompassCatalogPresenter.create(bodies, Locale.ENGLISH,
        { names.getValue(it) }, { it.name })

    @Test fun namesAndAliasesArePreparedAwayFromTheCallingUiThread() = runBlocking {
        val caller = Thread.currentThread()
        var resolved = 0
        val calculator = SpaceCompassCatalogPresenter.create(bodies, Locale.ENGLISH, {
            assertNotSame(caller, Thread.currentThread())
            resolved++
            names.getValue(it)
        }, { it.name })
        assertEquals(bodies.size, resolved)
        val result = calculator.present(request(search = "unmatched-987654321xyz", selected = emptySet()), false)
        assertTrue(result.visible.isEmpty())
        val scientific = calculator.present(request(search = "HALLEY", selected = emptySet()), false)
        assertEquals(listOf(comet), scientific.visible)
    }

    @Test fun replacementQueriesKeepVisitPriorityAndFreshChecksWithoutReorderingTheCheckedGroup() = runBlocking {
        val calculator = presenter()
        val alphabetical = SpaceCompassCatalogPreferences(sort = SpaceCompassCatalogSort.NAME_ASC)
        val query = request(alphabetical, search = "Halley")
        val result = calculator.present(query, false)
        assertSame(query, result.query)
        assertEquals(listOf(moon, comet, mars), result.visible)
        assertEquals(2, result.checkedCount)
        val newer = calculator.present(request(alphabetical, "no match", setOf(moon)), false)
        assertEquals(listOf(moon), newer.visible)
        assertEquals(1, newer.checkedCount)
        assertEquals(listOf(moon, comet, mars), result.visible) // Previously published snapshots remain immutable.
    }

    @Test fun datedRowsFollowTheirSelectedParentInChronologicalOrder() = runBlocking {
        val calculator = presenter()
        fun dated(body: SpaceCompassCelestialBody, date: String) = SpaceCompassTemporalComparison.create(
            body, Instant.parse("${date}T12:00:00Z").toEpochMilli(), ZoneOffset.UTC,
            SpaceCompassComparisonSource.HEIGHT, "MONTHLY:$date")
        val early = dated(sun, "2026-06-21")
        val late = dated(sun, "2026-12-21")
        val moonDate = dated(moon, "2026-08-21")
        val comparisons = listOf(late, moonDate, early)
        val alphabetical = SpaceCompassCatalogPreferences(sort = SpaceCompassCatalogSort.NAME_ASC)
        val chosen = calculator.present(request(alphabetical, search = "no match", selected = setOf(sun, moon),
            comparisons = comparisons), false)
        assertEquals(listOf(moon, moon, sun, sun, sun), chosen.rows.map { it.body })
        assertEquals(listOf(null, moonDate, null, early, late), chosen.rows.map { it.comparison })
        assertEquals(chosen.rows.size, chosen.rows.map { it.key }.toSet().size)
        val hidden = calculator.present(request(alphabetical, selected = setOf(moon), comparisons = comparisons), false)
        assertTrue(sun in hidden.visible)
        assertEquals(listOf(moonDate), hidden.rows.mapNotNull { it.comparison })
        val removed = calculator.present(request(alphabetical, selected = setOf(sun, moon),
            comparisons = comparisons - early), false)
        assertEquals(listOf(moonDate, late), removed.rows.mapNotNull { it.comparison })
    }

    @Test fun closedFiltersHaveNoFacetWorkAndOpeningThemUsesTheFavoriteScopeAndCheckedExceptions() = runBlocking {
        val calculator = presenter()
        val query = request(SpaceCompassCatalogPreferences(favoritesOnly = true), search = "no match",
            selected = setOf(sun, moon), favorites = setOf(sun, comet))
        val closed = calculator.present(query, false)
        assertNull(closed.counts)
        assertTrue(closed.typeOrder.isEmpty())
        assertEquals(listOf(sun), closed.visible)
        val opened = calculator.present(query, true)
        assertEquals(closed.visible, opened.visible)
        assertEquals(1, opened.counts!!.allTypes)
        assertEquals(1, opened.counts.types.getValue(SpaceCompassCatalogType.NATURAL_SATELLITE))
        assertEquals(1, opened.typeOrder.single { it.type == SpaceCompassCatalogType.STAR }.count)
        assertEquals(0, opened.typeOrder.single { it.type == SpaceCompassCatalogType.NATURAL_SATELLITE }.count)
        assertNull(calculator.present(query, false).counts)
    }

    @Test fun changedDistanceModelsReorderInBothDirectionsAndUnknownValuesStayLast() = runBlocking {
        val calculator = presenter()
        val query = SpaceCompassCatalogQuery(
            SpaceCompassCatalogPreferences(sort = SpaceCompassCatalogSort.EARTH_DISTANCE_ASC), "",
            emptySet(), emptySet(), emptySet(), emptyMap(),
            SpaceCompassCatalogDistances(emptyMap(), emptyMap(), mapOf(sun to 100.0, moon to 2.0, mars to 30.0)))
        assertEquals(listOf(moon, mars, sun, comet), calculator.present(query, false).visible)
        val changed = SpaceCompassCatalogQuery(query.preferences, "", emptySet(), emptySet(), emptySet(),
            emptyMap(), SpaceCompassCatalogDistances(emptyMap(), emptyMap(), mapOf(sun to 1.0, moon to 50.0, mars to 30.0)))
        assertEquals(listOf(sun, mars, moon, comet), calculator.present(changed, false).visible)
        val descending = SpaceCompassCatalogQuery(
            query.preferences.copy(sort = SpaceCompassCatalogSort.EARTH_DISTANCE_DESC), "", emptySet(),
            emptySet(), emptySet(), emptyMap(), changed.distances)
        assertEquals(listOf(moon, mars, sun, comet), calculator.present(descending, false).visible)
    }
}

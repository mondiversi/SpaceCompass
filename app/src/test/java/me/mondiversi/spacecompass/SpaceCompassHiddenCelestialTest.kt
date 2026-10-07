package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class SpaceCompassHiddenCelestialTest {
    private val hidden = SpaceCompassCelestialBody.LV_426

    @Test fun defaultCatalogAndMasterSelectionDoNotExposeHiddenObjects() {
        val initial = SpaceCompassCelestialSelection()
        assertEquals(30, spaceCompassAvailableCelestialCatalog(initial.selected).size)
        assertFalse(hidden in spaceCompassCelestialCatalogOrder)
        assertFalse(hidden in initial.toggleAll().selected)
        assertEquals(spaceCompassCelestialCatalogOrder.toSet() + hidden, SpaceCompassCelestialBody.entries.filterNot { it == SpaceCompassCelestialBody.EARTH_CENTER }.toSet())
    }

    @Test fun revealChecksTheObjectOnceAndKeepsTheInspectedObject() {
        val original = SpaceCompassCelestialSelection(setOf(SpaceCompassCelestialBody.MOON), SpaceCompassCelestialBody.MOON)
        val revealed = original.revealHiddenObject()
        assertEquals(original.selected + hidden, revealed.selected)
        assertEquals(original.active, revealed.active)
        assertEquals(revealed, revealed.revealHiddenObject())
        assertEquals(31, spaceCompassAvailableCelestialCatalog(revealed.selected).size)
        assertEquals(hidden, SpaceCompassCelestialSelection(emptySet(), null).revealHiddenObject().active)
    }

    @Test fun deselectionRemovesTheCatalogEntryAndKeepsNavigationValid() {
        val revealed = SpaceCompassCelestialSelection().revealHiddenObject()
        val selected = revealed.copy(active = hidden)
        assertEquals(hidden, selected.ordered.last())
        assertEquals(SpaceCompassCelestialBody.SUN, selected.step(1).active)
        val removed = selected.toggle(hidden)
        assertFalse(hidden in removed.selected)
        assertFalse(hidden in spaceCompassAvailableCelestialCatalog(removed.selected))
        assertEquals(SpaceCompassCelestialBody.SUN, removed.active)
        assertTrue(hidden in removed.revealHiddenObject().selected)
    }

    @Test fun savedSelectionRetainsTheRevealOnlyUntilDeselected() {
        val selected = SpaceCompassCelestialSelection().revealHiddenObject().copy(active = hidden)
        assertEquals(selected, restoreSpaceCompassCelestialSelection(selected.ordered.map { it.name }.toSet(), hidden.name))
        val removed = selected.toggle(hidden)
        assertEquals(removed, restoreSpaceCompassCelestialSelection(removed.ordered.map { it.name }.toSet(), removed.active?.name))
        assertFalse(hidden in restoreSpaceCompassCelestialSelection(null, hidden.name).selected)
    }

    @Test fun selectingAllAfterRevealIncludesItButClearingAllHidesItAgain() {
        val revealed = SpaceCompassCelestialSelection().revealHiddenObject()
        val all = revealed.toggleAll()
        assertEquals(spaceCompassAllCelestialOrder.toSet(), all.selected)
        assertEquals(SpaceCompassCelestialBody.SUN, all.active)
        val cleared = all.toggleVisible(all.selected)
        assertTrue(cleared.selected.isEmpty())
        assertFalse(hidden in spaceCompassAvailableCelestialCatalog(cleared.selected))
        assertFalse(hidden in cleared.toggleAll().selected)
    }

    @Test fun revealedObjectParticipatesInFiltersAndStableNumericSorting() {
        val catalog = spaceCompassAvailableCelestialCatalog(setOf(hidden))
        assertEquals(SpaceCompassCatalogType.NATURAL_SATELLITE, hidden.catalogType)
        val filtered = spaceCompassFilterCatalog(setOf(SpaceCompassCatalogType.NATURAL_SATELLITE),
            SpaceCompassCatalogVisibility.ABOVE, mapOf(hidden to 5.0), catalog)
        assertEquals(listOf(hidden), filtered)
        val sorted = spaceCompassSortCatalog(catalog, SpaceCompassCatalogSort.MASS_DESC, emptyMap(), emptyMap(), Locale.ENGLISH)
        assertTrue(hidden in sorted)
        assertNull(spaceCompassCatalogPhysicalSortValue(hidden, SpaceCompassCatalogSortField.MASS))
        assertEquals("—", formatSpaceCompassCatalogPhysicalValue(hidden, SpaceCompassCatalogSortField.MASS,
            SpaceCompassNumericFormat.INTERNATIONAL, SpaceCompassUnits()) { "reference" })
    }

    @Test fun fictionalFactsDoNotInventMassPressureOrRealSurfaceGeometry() {
        val facts = spaceCompassCelestialFacts(hidden)
        assertEquals(1200.0, facts.diameterKm!!, 0.0)
        assertTrue(facts.diameterEstimated)
        assertEquals(.86 * SPACE_COMPASS_STANDARD_GRAVITY_M_S2, facts.gravity!!, 1e-10)
        assertEquals(2.0, facts.rotationHours!!, 0.0)
        assertEquals("Calpamos", facts.parentName)
        assertNull(facts.massKg); assertNull(facts.density); assertNull(facts.revolutionDays)
        assertNull(spaceCompassAtmosphericPressure(hidden))
        assertNull(calculateSpaceCompassCelestialSpeed(hidden, Instant.parse("2026-10-06T12:00:00Z").toEpochMilli()))
        assertEquals(10.0, spaceCompassCelestialTemperatures(hidden).single().celsius, 0.0)
        assertEquals("lv426.webp", hidden.viewerTexture)
        assertFalse(hidden.usesDeepSkySymbol)
        assertFalse(hidden.hasPhysicalFace)
        assertFalse(hidden.usesHorizons)
    }

    @Test fun scenicHostDirectionAndDailyPathWorkWithoutNetworkInFutureScenarios() {
        val time = Instant.parse("2027-04-06T12:00:00Z").toEpochMilli()
        val observation = calculateSpaceCompassCelestialObservation(hidden, time, -33.0, 151.0)!!
        assertTrue(observation.position.azimuthDegrees in 0.0..360.0)
        assertTrue(observation.position.elevationDegrees in -90.0..90.0)
        assertEquals(39.3, observation.distanceKm / SPACE_COMPASS_LIGHT_YEAR_KM, .001)
        val path = calculateSpaceCompassCelestialPath(hidden, LocalDate.parse("2027-04-06"), ZoneId.of("Australia/Sydney"),
            time, -33.0, 151.0, 0.0, SpaceCompassCelestialRemoteData())!!
        assertEquals(hidden, path.body)
        assertTrue(path.samples.isNotEmpty())
        assertTrue(path.markers.isNotEmpty())
    }
}

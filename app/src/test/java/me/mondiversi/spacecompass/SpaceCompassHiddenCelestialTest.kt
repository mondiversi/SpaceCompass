package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class SpaceCompassHiddenCelestialTest {
    private val hidden = SpaceCompassCelestialBody.LV_426

    @Test fun publicCatalogAndMasterSelectionIncludeLv426WithoutChangingDefaults() {
        val initial = SpaceCompassCelestialSelection()
        assertEquals(setOf(SpaceCompassCelestialBody.SUN, SpaceCompassCelestialBody.MOON), initial.selected)
        assertEquals(999, spaceCompassAvailableCelestialCatalog(initial.selected).size)
        assertTrue(hidden in spaceCompassCelestialCatalogOrder)
        assertTrue(hidden in initial.toggleAll().selected)
        assertEquals(spaceCompassCelestialCatalogOrder.toSet(),
            SpaceCompassCelestialBody.entries.filterNot { it == SpaceCompassCelestialBody.EARTH_CENTER }.toSet())
    }
    @Test fun deselectionNeverHidesThePublicCatalogEntryAndSelectionSurvivesRestore() {
        val selected = SpaceCompassCelestialSelection().toggle(hidden).copy(active = hidden)
        assertEquals(selected, restoreSpaceCompassCelestialSelection(selected.ordered.map { it.name }.toSet(), hidden.name))
        val removed = selected.toggle(hidden)
        assertFalse(hidden in removed.selected)
        assertTrue(hidden in spaceCompassAvailableCelestialCatalog(removed.selected))
        assertEquals(999, removed.toggleAll().selected.size)
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

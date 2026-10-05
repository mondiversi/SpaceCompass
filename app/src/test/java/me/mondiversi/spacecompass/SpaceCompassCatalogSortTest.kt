package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
import java.time.Instant

class SpaceCompassCatalogSortTest {
    private val sun = SpaceCompassCelestialBody.SUN
    private val moon = SpaceCompassCelestialBody.MOON
    private val mars = SpaceCompassCelestialBody.MARS
    private val venus = SpaceCompassCelestialBody.VENUS
    @Test fun localizedNamesSortInBothDirectionsWithoutChangingTheFilteredSet() {
        val bodies = listOf(sun, moon, mars)
        val names = mapOf(sun to "Sole", moon to "Luna", mars to "Marte")
        assertEquals(listOf(moon, mars, sun), spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.NAME_ASC, names, emptyMap(), Locale.ITALIAN))
        assertEquals(listOf(sun, mars, moon), spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.NAME_DESC, names, emptyMap(), Locale.ITALIAN))
        assertEquals(bodies, spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.CATALOG, names, emptyMap(), Locale.ITALIAN))
        val accented = mapOf(sun to "Étoile", moon to "Lune", mars to "Mars")
        assertEquals(listOf(sun, moon, mars), spaceCompassSortCatalog(bodies.reversed(), SpaceCompassCatalogSort.NAME_ASC, accented, emptyMap(), Locale.FRENCH))
    }
    @Test fun commonDistanceUnitsSortNumericallyWithUnavailableValuesLastAndStableTies() {
        val bodies = listOf(mars, moon, venus, sun)
        val distances = mapOf(sun to 0.0, moon to 1.0, mars to 1.0, venus to Double.NaN)
        assertEquals(listOf(sun, moon, mars, venus), spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.DISTANCE_ASC, emptyMap(), distances, Locale.ROOT))
        assertEquals(listOf(moon, mars, sun, venus), spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.DISTANCE_DESC, emptyMap(), distances, Locale.ROOT))
        assertEquals(listOf(sun, venus), spaceCompassSortCatalog(listOf(venus, sun), SpaceCompassCatalogSort.DISTANCE_DESC, emptyMap(), mapOf(sun to 0.0), Locale.ROOT))
    }
    @Test fun nearEarthDisplayDistancesNeverBecomeHeliocentricSortKeys() {
        val now = Instant.parse("2026-10-05T12:00:00Z").toEpochMilli()
        val bodies = listOf(SpaceCompassCelestialBody.EARTH_CENTER, moon, SpaceCompassCelestialBody.ISS)
        bodies.forEach { assertTrue(spaceCompassCelestialCatalogDistanceAu(it, now)!! in .95..1.05) }
        assertEquals(0.0, spaceCompassCelestialCatalogDistanceAu(sun, now)!!, 0.0)
        assertTrue(spaceCompassCelestialCatalogDistanceAu(SpaceCompassCelestialBody.PROXIMA_CENTAURI, now)!! > 100000)
    }
}

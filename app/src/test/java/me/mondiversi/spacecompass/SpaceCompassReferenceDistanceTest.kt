package me.mondiversi.spacecompass

import java.time.Instant
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassReferenceDistanceTest {
    private val now = Instant.parse("2026-10-27T12:00:00Z").toEpochMilli()
    @Test fun acheronUsesTheRealReticuliRangeWithoutCreatingMeasuredFictionalEphemerides() {
        val body = SpaceCompassCelestialBody.LV_426
        val estimate = spaceCompassReferenceDistanceKm(body, now)!!
        assertEquals(39.3, estimate / SPACE_COMPASS_LIGHT_YEAR_KM, 1e-4)
        assertTrue(calculateSpaceCompassCelestialObservation(body, now, 45.0, 9.0)!!.distanceKm.isNaN())
        assertFalse(body.deepSkyReference!!.hasMeasuredDistance)
        assertNull(spaceCompassCelestialCatalogDistanceAu(body, now))
    }
    @Test fun everyFictionalEntryHasAQualifiedFiniteEstimateAndTheReferenceIsUsedAtTheChosenDate() {
        for (body in spaceCompassFictionalReferences.map { it.body }) for (offset in listOf(0L, 150L*86400000)) {
            val time = now + offset
            assertTrue(spaceCompassReferenceDistanceKm(body, time, latitude=45.0, longitude=9.0)!!.isFinite())
            assertTrue(spaceCompassCatalogDisplaySolarDistanceAu(body, time)!! > 0)
            assertTrue(spaceCompassCatalogDisplayEarthDistanceKm(body, time)!! > 0)
        }
        val ship = SpaceCompassCelestialBody.DISCOVERY_ONE
        assertEquals(spaceCompassCelestialCatalogEarthDistanceKm(SpaceCompassCelestialBody.JUPITER, now)!!,
            spaceCompassCatalogDisplayEarthDistanceKm(ship, now)!!, 1e-6)
        val observed = calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.JUPITER, now, -30.0, 150.0, 100.0)!!
        assertEquals(observed.distanceKm, spaceCompassReferenceDistanceKm(ship, now,
            latitude=-30.0, longitude=150.0, altitude=100.0)!!, 1e-6)
    }
    @Test fun unavailableRealRangesAndDirectionOnlyObjectsDoNotGainFictionalFallbacks() {
        for (body in listOf(SpaceCompassCelestialBody.ISS, SpaceCompassCelestialBody.VOYAGER_1,
            SpaceCompassCelestialBody.SOUTHERN_CROSS)) {
            assertNull(spaceCompassReferenceDistanceKm(body, now))
            assertNull(spaceCompassDisplayObserverDistanceKm(body, Double.NaN, now))
        }
        assertEquals(123.0, spaceCompassDisplayObserverDistanceKm(SpaceCompassCelestialBody.MOON, 123.0, now)!!, 0.0)
    }
    @Test fun displaySortingUsesEstimatedSolarAndEarthRangesInBothDirections() {
        val bodies = listOf(SpaceCompassCelestialBody.TRAPPIST_1_E,
            SpaceCompassCelestialBody.LV_426, SpaceCompassCelestialBody.ALPHA_CENTAURI)
        val solar = bodies.associateWith { spaceCompassCatalogDisplaySolarDistanceAu(it, now) }
        val earth = bodies.associateWith { spaceCompassCatalogDisplayEarthDistanceKm(it, now) }
        val ascending = listOf(bodies[2], bodies[1], bodies[0])
        assertEquals(ascending, spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.DISTANCE_ASC,
            emptyMap(), solar, Locale.ROOT, earth))
        assertEquals(ascending.reversed(), spaceCompassSortCatalog(bodies, SpaceCompassCatalogSort.EARTH_DISTANCE_DESC,
            emptyMap(), solar, Locale.ROOT, earth))
    }
    @Test fun everyDistancePresentationQualifiesTheReferenceExactlyOnce() {
        val body = SpaceCompassCelestialBody.LV_426
        val estimate = spaceCompassReferenceDistanceKm(body, now)!!
        for (unit in listOf("mkm", "mmi")) {
            val text = formatSpaceCompassSelectedDistance(body, estimate, SpaceCompassNumericFormat.INTERNATIONAL, unit)!!
            assertTrue(text.startsWith("≈ ") && text.endsWith(" ly"))
            assertEquals(text, spaceCompassReferenceDistanceText(body, text))
            val catalog = formatSpaceCompassCatalogDisplayDistance(body,
                spaceCompassCatalogDisplaySolarDistanceAu(body, now), SpaceCompassNumericFormat.INTERNATIONAL, unit)
            assertTrue(catalog.startsWith("≈ ") && catalog.endsWith(" ly"))
        }
        assertEquals("—", spaceCompassReferenceDistanceText(body, "—"))
        assertNull(spaceCompassReferenceDistanceText(body, null))
        assertEquals("12 km", spaceCompassReferenceDistanceText(SpaceCompassCelestialBody.MOON, "12 km"))
    }
    @Test fun travelRequiresExplicitEstimateProvenanceAndKeepsOrdinaryMissingDataUnknown() {
        val body = SpaceCompassCelestialBody.LV_426
        val estimate = spaceCompassReferenceDistanceKm(body, now)!!
        assertNull(spaceCompassTravelDistanceKm(body, estimate))
        assertEquals(estimate, spaceCompassTravelDistanceKm(body, null, referenceEstimateKm=estimate)!!, 0.0)
        val time = spaceCompassTravelSeconds(estimate, spaceCompassTravelSpeeds.single { it.key == "light" })!!
        assertEquals(39.3, time / SPACE_COMPASS_TRAVEL_YEAR_SECONDS, 1e-4)
        assertNull(spaceCompassTravelDistanceKm(SpaceCompassCelestialBody.SOUTHERN_CROSS, null, referenceEstimateKm=estimate))
    }
}

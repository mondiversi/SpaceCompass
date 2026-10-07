package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassEstimatedPlaceTest {
    private fun key(latitude: Double = 41.90283, language: String = "it-IT") =
        requireNotNull(spaceCompassPlaceKey(latitude, 12.4964, language))

    @Test fun formatsAvailableCityRegionAndCountryAsReadableSeparateLines() {
        assertEquals("Roma, Lazio, Italia", formatSpaceCompassEstimatedPlace(
            SpaceCompassPlaceParts(locality = "Roma", adminArea = "Lazio", country = "Italia")))
    }
    @Test fun omitsMissingNamesWithoutInventingZeroOrAnAddress() {
        assertNull(formatSpaceCompassEstimatedPlace(SpaceCompassPlaceParts()))
        assertEquals("Italia", formatSpaceCompassEstimatedPlace(SpaceCompassPlaceParts(country = "Italia")))
        assertEquals("Lazio", formatSpaceCompassEstimatedPlace(SpaceCompassPlaceParts(adminArea = "Lazio")))
    }
    @Test fun fallsBackToAvailableCountyThenDistrictWhenCityIsMissing() {
        assertEquals("County, Region", formatSpaceCompassEstimatedPlace(
            SpaceCompassPlaceParts(locality = " ", subLocality = "District", subAdminArea = "County", adminArea = "Region")))
        assertEquals("District", formatSpaceCompassEstimatedPlace(SpaceCompassPlaceParts(subLocality = "District")))
    }
    @Test fun removesAdministrativeDuplicatesWithoutLosingTheOriginalSpelling() {
        assertEquals("Singapore", formatSpaceCompassEstimatedPlace(
            SpaceCompassPlaceParts(locality = "Singapore", adminArea = "singapore", country = "SINGAPORE")))
    }
    @Test fun normalizesProviderWhitespaceAndPreservesUnicodePlaceNames() {
        assertEquals("São Paulo, Brasil", formatSpaceCompassEstimatedPlace(
            SpaceCompassPlaceParts(locality = "  São\u00a0 Paulo\n", country = "\tBrasil\u202f")))
        assertEquals("القاهرة, مصر", formatSpaceCompassEstimatedPlace(
            SpaceCompassPlaceParts(locality = "القاهرة", country = "مصر")))
    }
    @Test fun rejectsMissingNonfiniteAndOutOfRangeCoordinates() {
        for (bad in listOf(null, Double.NaN, Double.POSITIVE_INFINITY, -90.001, 90.001))
            assertNull(spaceCompassPlaceKey(bad, 12.0, "it"))
        for (bad in listOf(null, Double.NaN, Double.NEGATIVE_INFINITY, -180.001, 180.001))
            assertNull(spaceCompassPlaceKey(42.0, bad, "it"))
    }
    @Test fun coalescesGpsJitterAndNormalizesNegativeZeroWhileKeepingBoundaryCoordinates() {
        assertEquals(key(41.90283), key(41.90281))
        assertEquals(SpaceCompassPlaceKey(0.0, 0.0, "en"), spaceCompassPlaceKey(-0.0001, -0.0, "en"))
        assertEquals(SpaceCompassPlaceKey(-90.0, 180.0, "en"), spaceCompassPlaceKey(-90.0, 180.0, "en"))
    }
    @Test fun cachesSuccessfulLookupsForReopeningTheSamePageAndExpiresThem() {
        val cache = SpaceCompassPlaceCache()
        val entry = cache.put(key(), "Roma", 1_000L)
        assertEquals("Roma", cache.get(key(), entry.expiresElapsed - 1)?.text)
        assertNull(cache.get(key(), entry.expiresElapsed))
    }
    @Test fun unavailableResultsAreCachedForOnlyTheRetryInterval() {
        val cache = SpaceCompassPlaceCache()
        val entry = cache.put(key(), null, 1_000L)
        assertEquals(61_000L, entry.expiresElapsed)
        assertNotNull(cache.get(key(), 60_999L))
        assertNull(cache.get(key(), 61_000L))
    }
    @Test fun cacheNeverUsesAnotherLocationOrAnotherLanguagesLabel() {
        val cache = SpaceCompassPlaceCache()
        cache.put(key(), "Roma", 0)
        assertNull(cache.get(key(45.0), 1))
        assertNull(cache.get(key(language = "en-US"), 1))
        assertEquals("Roma", cache.get(key(), 1)?.text)
    }
    @Test fun boundedCacheEvictsLeastRecentlyUsedPlaces() {
        val cache = SpaceCompassPlaceCache(2)
        cache.put(key(40.0), "A", 0)
        cache.put(key(41.0), "B", 0)
        assertNotNull(cache.get(key(40.0), 1))
        cache.put(key(42.0), "C", 1)
        assertNull(cache.get(key(41.0), 1))
        assertEquals("A", cache.get(key(40.0), 1)?.text)
        assertEquals("C", cache.get(key(42.0), 1)?.text)
    }
    @Test fun anEmptyProviderResultCannotBecomeAnAvailablePlace() {
        val cache = SpaceCompassPlaceCache()
        assertNull(cache.put(key(), "   ", 0).text)
        assertEquals(SPACE_COMPASS_PLACE_RETRY_MS, cache.get(key(), 1)?.expiresElapsed)
    }
}

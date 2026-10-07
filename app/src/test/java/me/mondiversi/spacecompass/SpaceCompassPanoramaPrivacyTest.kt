package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test

class SpaceCompassPanoramaPrivacyTest {
    private fun row(tag: String, value: String, label: String = "") = SpaceCompassSunDataRow(label, value, "", tag)
    private val data = SpaceCompassPanoramaCaptionData("Space Compass · 2026-10-06 · 05:43 (UTC+02:00)",
        "Zinasco, Lombardia, Italy", "Lombardia, Italy", listOf(
            row("sun-info-coordinates", "45.106896° N\n8.999512° E"), row("sun-info-accuracy", "±15 m"),
            row("sun-info-altitude", "99 m (±14 m)", "GPS altitude"),
            row("celestial-environment-weather", "Cloudy")), "100%")

    @Test fun completePreservesTheExistingScientificCaption() {
        assertEquals(formatSpaceCompassPanoramaCaption(data.timestamp, data.completePlace, data.rows, data.cloudPercent),
            spaceCompassPanoramaCaptionForPosition(data, SpaceCompassPanoramaPosition.COMPLETE))
    }
    @Test fun areaOmitsCityCoordinatesAltitudeAndBothAccuracies() {
        val text = spaceCompassPanoramaCaptionForPosition(data, SpaceCompassPanoramaPosition.AREA)
        assertEquals(data.timestamp + " · Lombardia, Italy · Cloudy (100%)", text)
        for (value in listOf("Zinasco", "45.106896", "8.999512", "15 m", "99 m", "14 m", "GPS altitude"))
            assertFalse(value, text.contains(value))
    }
    @Test fun hiddenOmitsEveryExplicitLocationFieldButKeepsTheCaptureAndWeather() {
        assertEquals(data.timestamp + " · Cloudy (100%)",
            spaceCompassPanoramaCaptionForPosition(data, SpaceCompassPanoramaPosition.HIDDEN))
    }
    @Test fun coarsePlaceNeverFallsBackToCityDistrictOrCounty() {
        assertNull(formatSpaceCompassPanoramaArea(SpaceCompassPlaceParts(locality = "Town", subLocality = "Street", subAdminArea = "County")))
        assertNull(formatSpaceCompassPanoramaArea(null))
        assertEquals("Country", formatSpaceCompassPanoramaArea(SpaceCompassPlaceParts(locality = "Town", country = "Country")))
    }
    @Test fun coarsePlacePreservesUnicodeAndNormalizesDuplicateAdministrativeNames() {
        assertEquals("São Paulo, Brasil", formatSpaceCompassPanoramaArea(
            SpaceCompassPlaceParts(locality = "Private city", adminArea = " São\u00a0 Paulo\n", country = "Brasil")))
        assertEquals("Singapore", formatSpaceCompassPanoramaArea(SpaceCompassPlaceParts(adminArea = "Singapore", country = "SINGAPORE")))
    }
    @Test fun corruptStoredDisclosureFailsClosed() {
        for (value in listOf("", "unknown", "COMPLETE", "full"))
            assertEquals(SpaceCompassPanoramaPosition.HIDDEN, SpaceCompassPanoramaPosition.fromStored(value))
    }
    @Test fun allChoicesRoundTripAndAnExistingInstallationRetainsItsInitialCompleteMode() {
        for (position in SpaceCompassPanoramaPosition.entries)
            assertEquals(position, SpaceCompassPanoramaPosition.fromStored(position.key))
        assertEquals(SpaceCompassPanoramaPosition.AREA, SpaceCompassPanoramaPosition.fromStored(null))
    }
    @Test fun profileSwitchesCannotMutateTheFrozenRowsOrLoseTheOriginalCompleteCaption() {
        val before = data.copy(rows = data.rows.toList())
        val complete = spaceCompassPanoramaCaptionForPosition(data, SpaceCompassPanoramaPosition.COMPLETE)
        SpaceCompassPanoramaPosition.entries.reversed().forEach { spaceCompassPanoramaCaptionForPosition(data, it) }
        assertEquals(before, data)
        assertEquals(complete, spaceCompassPanoramaCaptionForPosition(data, SpaceCompassPanoramaPosition.COMPLETE))
    }
    @Test fun structuredCachedAreaStaysBoundToItsLocationAndLanguage() {
        val cache = SpaceCompassPlaceCache()
        val key = requireNotNull(spaceCompassPlaceKey(45.1, 8.9, "en"))
        val parts = SpaceCompassPlaceParts(locality = "Private town", adminArea = "Region", country = "Country")
        cache.put(key, formatSpaceCompassEstimatedPlace(parts), 0, parts)
        assertEquals("Region, Country", formatSpaceCompassPanoramaArea(cache.get(key, 1)?.parts))
        assertNull(cache.get(key.copy(languageTag = "it"), 1))
        assertNull(cache.get(key.copy(latitude = 46.0), 1))
    }
}

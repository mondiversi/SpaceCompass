package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class SpaceCompassPanoramaCaptionTest {
    private fun row(tag: String, value: String, label: String = "") = SpaceCompassSunDataRow(label, value, "", tag)

    @Test fun requestedValuesShareOneLineAndAccuracyFollowsBothCoordinates() {
        val rows = listOf(row("sun-info-coordinates", "45,106887° N\n8,999557° E"),
            row("sun-info-accuracy", "±20 m"), row("sun-info-altitude", "118 m (±11 m)", "Quota GPS"),
            row("celestial-environment-weather", "Parzialmente nuvoloso"), row("sun-data-heading", "123°", "Direzione"))
        assertEquals("Space Compass · 05/10/26 · 20:32 CEST · Zinasco, Lombardia, Italia · " +
            "45,106887° N · 8,999557° E (±20 m) · Quota GPS 118 m · Parzialmente nuvoloso (53%)",
            formatSpaceCompassPanoramaCaption("Space Compass · 05/10/26 · 20:32 CEST",
                "Zinasco, Lombardia, Italia", rows, "53%"))
    }

    @Test fun unavailableAccuracyAndCloudsNeverBecomeFabricatedNumbers() {
        val rows = listOf(row("sun-info-coordinates", "1° N\n2° E"), row("sun-info-accuracy", "—"),
            row("sun-info-altitude", "—", "GPS altitude"), row("celestial-environment-weather", "Weather unavailable"))
        val value = formatSpaceCompassPanoramaCaption("Space Compass", "Place unavailable", rows, null)
        assertFalse(value.contains("±")); assertFalse(value.contains("%")); assertFalse(value.contains('\n'))
        assertTrue(value.endsWith("GPS altitude — · Weather unavailable"))
    }

    @Test fun captionJoiningPreservesValuesWithoutAttemptingToReconvertThem() {
        val rows = listOf(row("sun-info-coordinates", "33° 51′ 00″ S\n151° 12′ 00″ W"),
            row("sun-info-accuracy", "66 ft"), row("sun-info-altitude", "1\u202f234 ft (±10 ft)", "GPS altitude"),
            row("celestial-environment-weather", "Clear"))
        val value = formatSpaceCompassPanoramaCaption("Space Compass", "Fixture", rows, "0%")
        assertTrue(value.contains("33° 51′ 00″ S · 151° 12′ 00″ W (±66 ft)"))
        assertTrue(value.contains("GPS altitude 1\u202f234 ft")); assertFalse(value.contains("±10"))
    }

    @Test fun timezoneOffsetFollowsTheSelectedZoneAndDaylightSavingAtCapture() {
        val summer = Instant.parse("2026-10-05T18:32:00Z").toEpochMilli()
        val winter = Instant.parse("2026-01-05T18:32:00Z").toEpochMilli()
        for ((id, names) in listOf("Europe/Rome" to listOf("+02:00", "+01:00"),
                "America/New_York" to listOf("-04:00", "-05:00"), "America/Los_Angeles" to listOf("-07:00", "-08:00"))) {
            val zone = TimeZone.getTimeZone(id)
            assertTrue(formatSpaceCompassPanoramaExportTimestamp(summer, zone).endsWith("(UTC${names[0]})"))
            assertTrue(formatSpaceCompassPanoramaExportTimestamp(winter, zone).endsWith("(UTC${names[1]})"))
        }
    }

    @Test fun translatedAndMultilinePlacesRemainWholeOnASingleLine() {
        val value = formatSpaceCompassPanoramaCaption("Space Compass\t· 20:32",
            "المدينة\nالمنطقة، البلد", listOf(row("celestial-environment-weather", "غائم جزئياً")), "53%")
        assertTrue(value.contains("المدينة المنطقة، البلد")); assertTrue(value.endsWith("غائم جزئياً (53%)"))
        assertFalse(value.contains('\n')); assertFalse(value.contains('\t'))
    }
}

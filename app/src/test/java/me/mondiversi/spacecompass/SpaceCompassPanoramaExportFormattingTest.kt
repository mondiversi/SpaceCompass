package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class SpaceCompassPanoramaExportFormattingTest {
    private val captureTime = Instant.parse("2026-10-05T18:32:47.123Z").toEpochMilli()
    private fun rows(latitude: Double? = 45.106887, longitude: Double? = 8.999557,
        altitude: Double? = 1234.0, accuracy: Double? = 20.0) =
        spaceCompassPanoramaExportRows(latitude, longitude, altitude, accuracy,
            "GPS altitude: $SPACE_COMPASS_SUN_DATA_MARKER", "Partly cloudy")

    @Test fun rawGpsUsesMetricLengthsDecimalCoordinatesAndInternationalNumbers() {
        val value = formatSpaceCompassPanoramaCaption("Space Compass", "Zinasco, Lombardy, Italy",
            rows(), formatSpaceCompassPanoramaExportCloudCover(.53f))
        assertEquals("Space Compass · Zinasco, Lombardy, Italy · 45.106887° N · 8.999557° E (±20 m) · " +
            "GPS altitude 1\u202f234 m · Partly cloudy (53%)", value)
        assertFalse(value.contains("ft")); assertFalse(value.contains('\n'))
    }

    @Test fun southernWesternCoordinatesAndBelowSeaLevelAltitudeRetainTheirMeaning() {
        val value = formatSpaceCompassPanoramaCaption("Space Compass", null,
            rows(-33.85, -151.2, -52.6, 6.4), null)
        assertTrue(value.contains("33.850000° S · 151.200000° W (±6 m)"))
        assertTrue(value.contains("GPS altitude -53 m"))
    }

    @Test fun missingAndInvalidMeasurementsNeverBecomeSeaLevelOrZeroAccuracy() {
        for (invalid in listOf(null, Double.NaN, Double.POSITIVE_INFINITY)) {
            val value = formatSpaceCompassPanoramaCaption("Space Compass", null,
                rows(altitude = invalid, accuracy = invalid), null)
            assertTrue(value.contains("GPS altitude —")); assertFalse(value.contains("±"))
        }
        assertEquals("—", rows(latitude = 91.0, accuracy = -1.0)[0].value)
        assertEquals("—", rows(longitude = 181.0)[0].value)
        assertEquals("—", rows(accuracy = -1.0)[1].value)
    }

    @Test fun cloudPercentagePreservesZeroAndFullCoverWithoutFabricatingInvalidData() {
        assertEquals("0%", formatSpaceCompassPanoramaExportCloudCover(0f))
        assertEquals("100%", formatSpaceCompassPanoramaExportCloudCover(1f))
        assertEquals("53%", formatSpaceCompassPanoramaExportCloudCover(.53f))
        for (invalid in listOf(null, Float.NaN, Float.POSITIVE_INFINITY, -.1f, 1.1f))
            assertNull(formatSpaceCompassPanoramaExportCloudCover(invalid))
    }

    @Test fun timestampUsesCaptureInstantIsoDateAndSeasonalPhoneOffset() {
        val rome = TimeZone.getTimeZone("Europe/Rome")
        assertEquals("Space Compass · 2026-10-05 · 20:32 (UTC+02:00)",
            formatSpaceCompassPanoramaExportTimestamp(captureTime, rome))
        val winter = Instant.parse("2026-01-05T18:32:47Z").toEpochMilli()
        assertEquals("Space Compass · 2026-01-05 · 19:32 (UTC+01:00)",
            formatSpaceCompassPanoramaExportTimestamp(winter, rome))
        assertEquals("Space Compass · 2026-10-05 · 14:32 (UTC-04:00)",
            formatSpaceCompassPanoramaExportTimestamp(captureTime, TimeZone.getTimeZone("America/New_York")))
        assertEquals("Space Compass · 2026-10-05 · 18:32 (UTC+00:00)",
            formatSpaceCompassPanoramaExportTimestamp(captureTime, TimeZone.getTimeZone("UTC")))
    }

    @Test fun fractionalZonesAndLocalDateBoundariesAreExplicit() {
        val nepal = formatSpaceCompassPanoramaExportTimestamp(captureTime, TimeZone.getTimeZone("Asia/Kathmandu"))
        assertTrue(nepal.contains("2026-10-06 · 00:17")); assertTrue(nepal.endsWith("(UTC+05:45)"))
        val adelaide = formatSpaceCompassPanoramaExportTimestamp(captureTime, TimeZone.getTimeZone("Australia/Adelaide"))
        assertTrue(adelaide.contains("2026-10-06 · 05:02")); assertTrue(adelaide.endsWith("(UTC+10:30)"))
        val newYork = formatSpaceCompassPanoramaExportTimestamp(Instant.parse("2026-10-05T01:32:00Z").toEpochMilli(),
            TimeZone.getTimeZone("America/New_York"))
        assertTrue(newYork.contains("2026-10-04 · 21:32")); assertTrue(newYork.endsWith("(UTC-04:00)"))
    }

    @Test fun frozenHeaderAndMarkerTimesIgnoreLaterSystemLocaleAndZoneChanges() {
        val localeBefore = Locale.getDefault()
        val zoneBefore = TimeZone.getDefault()
        val capturedZone = TimeZone.getTimeZone("Europe/Rome")
        val header = formatSpaceCompassPanoramaExportTimestamp(captureTime, capturedZone)
        val markerTime = captureTime + 3_600_000L
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            for (locale in listOf(Locale.GERMANY, Locale.US, Locale.forLanguageTag("ar-EG"))) {
                Locale.setDefault(locale)
                assertEquals(header, formatSpaceCompassPanoramaExportTimestamp(captureTime, capturedZone))
                assertEquals("21:32", formatSpaceCompassPanoramaExportTime(markerTime, capturedZone))
                assertEquals("45.106887° N\n8.999557° E", rows()[0].value)
                assertEquals("1\u202f234 m", rows()[2].value)
            }
        } finally {
            Locale.setDefault(localeBefore); TimeZone.setDefault(zoneBefore)
        }
    }
}

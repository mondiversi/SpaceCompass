package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class SpaceCompassPanoramaExportProfileTest {
    private val time = Instant.parse("2026-10-05T18:32:47Z").toEpochMilli()
    private val selected = SpaceCompassPanoramaFormatting(Locale.ITALIAN, SpaceCompassNumericFormat.EUROPEAN,
        SpaceCompassDateFormat.EUROPEAN, SpaceCompassTimeFormat.H12, feet = true, dms = true, deviceLocale = Locale.ITALY)
    private fun rows(format: SpaceCompassPanoramaFormatting) = spaceCompassPanoramaExportRows(
        45.106887, 8.999557, 1234.0, 20.0, "Quota GPS: $SPACE_COMPASS_SUN_DATA_MARKER", "Nuvoloso", format)

    @Test fun missingOrInvalidStoredProfileRetainsInternationalDefault() {
        assertEquals(SpaceCompassPanoramaExportMode.INTERNATIONAL, SpaceCompassPanoramaExportMode.fromStored(null))
        assertEquals(SpaceCompassPanoramaExportMode.INTERNATIONAL, SpaceCompassPanoramaExportMode.fromStored("unexpected"))
        assertEquals(SpaceCompassPanoramaExportMode.SELECTED, SpaceCompassPanoramaExportMode.fromStored("selected"))
    }

    @Test fun selectedAndInternationalProfilesConvertTheSameRawMeasurementsOnce() {
        val imperial = rows(selected)
        assertEquals("4.049 ft", imperial.first { it.tag == "sun-info-altitude" }.value)
        assertEquals("66 ft", imperial.first { it.tag == "sun-info-accuracy" }.value)
        assertTrue(imperial[0].value.contains('′')); assertTrue(imperial[0].value.contains("24,8″ N"))
        val metric = rows(spaceCompassPanoramaInternationalFormatting)
        assertEquals("1\u202f234 m", metric.first { it.tag == "sun-info-altitude" }.value)
        assertEquals("20 m", metric.first { it.tag == "sun-info-accuracy" }.value)
        assertEquals("45.106887° N\n8.999557° E", metric[0].value)
    }

    @Test fun selectedDateAndClockFormatsKeepTheSameInstantAndExplicitObservationZone() {
        val zone = TimeZone.getTimeZone("Europe/Rome")
        val header = formatSpaceCompassPanoramaExportTimestamp(time, zone, selected)
        assertTrue(header.contains("05/10/2026")); assertTrue(header.contains("8:32"))
        assertTrue(header.endsWith("(UTC+02:00)"))
        assertEquals("Space Compass · 2026-10-05 · 20:32 (UTC+02:00)",
            formatSpaceCompassPanoramaExportTimestamp(time, zone))
        assertTrue(formatSpaceCompassPanoramaExportTime(time + 3_600_000, zone, selected).startsWith("9:32"))
    }

    @Test fun frozenSystemNumbersDoNotFollowLaterProcessLocaleChanges() {
        val before = Locale.getDefault()
        val format = selected.copy(numeric = SpaceCompassNumericFormat.SYSTEM, dms = false)
        val expected = rows(format)
        try {
            Locale.setDefault(Locale.US)
            assertEquals(expected, rows(format))
            assertTrue(rows(format)[0].value.startsWith("45,106887°"))
            assertEquals("23,4", formatSpaceCompassPanoramaElevation(23.4, format))
            assertEquals("+23,4°", formatSpaceCompassSkyReferenceDegrees(23.4, format.numeric, format.deviceLocale))
        } finally { Locale.setDefault(before) }
    }

    @Test fun allProfileDisclosureAndLabelCombinationsHaveDistinctCacheIdentities() {
        val keys = SpaceCompassPanoramaExportMode.entries.flatMap { mode ->
            SpaceCompassPanoramaPosition.entries.flatMap { position ->
                listOf(true, false).map { spaceCompassPanoramaVariantKey(position, it, mode) }
            }
        }
        assertEquals(12, keys.toSet().size)
    }

    @Test fun neitherProfileCanBroadenLocationDisclosure() {
        for (format in listOf(selected, spaceCompassPanoramaInternationalFormatting)) {
            val data = SpaceCompassPanoramaCaptionData("Frozen timestamp", "Private city, Region, Country",
                "Region, Country", rows(format), "53%")
            val complete = spaceCompassPanoramaCaptionForPosition(data, SpaceCompassPanoramaPosition.COMPLETE)
            val area = spaceCompassPanoramaCaptionForPosition(data, SpaceCompassPanoramaPosition.AREA)
            val hidden = spaceCompassPanoramaCaptionForPosition(data, SpaceCompassPanoramaPosition.HIDDEN)
            assertTrue(complete.contains("Private city")); assertTrue(complete.contains("±"))
            assertTrue(area.contains("Region, Country")); assertFalse(area.contains("Private city"))
            for (value in listOf(area, hidden)) {
                assertFalse(value.contains("Quota")); assertFalse(value.contains("±")); assertFalse(value.contains('°'))
            }
            assertFalse(hidden.contains("Country")); assertTrue(hidden.contains("Frozen timestamp"))
        }
    }

    @Test fun selectedAngularLabelsNormalizeNegativeZeroAndKeepMissingMeasurementsMissing() {
        assertEquals("0,0", formatSpaceCompassPanoramaElevation(-.001, selected))
        val invalid = spaceCompassPanoramaExportRows(null, null, Double.NaN, -1.0,
            "Quota: $SPACE_COMPASS_SUN_DATA_MARKER", "—", selected)
        assertEquals("—", invalid[0].value); assertEquals("—", invalid[1].value); assertEquals("—", invalid[2].value)
    }

    @Test fun legacyGallerySavesBothProfilesWithoutOverwritingTheFirstImage() {
        val directory = java.nio.file.Files.createTempDirectory("panorama-profiles-").toFile()
        try {
            val first = createUniqueSpaceCompassPanoramaFile(directory, "SpaceCompass_capture.jpg")
            first.writeText("original image")
            val second = createUniqueSpaceCompassPanoramaFile(directory, "SpaceCompass_capture.jpg")
            val third = createUniqueSpaceCompassPanoramaFile(directory, "SpaceCompass_capture.jpg")
            assertEquals("SpaceCompass_capture (1).jpg", second.name)
            assertEquals("SpaceCompass_capture (2).jpg", third.name)
            assertEquals("original image", first.readText())
        } finally { directory.deleteRecursively() }
    }

    @Test fun legacyGalleryRejectsPathsOutsideTheRequestedDirectory() {
        val directory = java.nio.file.Files.createTempDirectory("panorama-profiles-").toFile()
        try {
            try {
                createUniqueSpaceCompassPanoramaFile(directory, "../outside.jpg")
                fail("Directory traversal must be rejected")
            } catch (_: IllegalArgumentException) { }
            assertTrue(directory.listFiles()!!.isEmpty())
        } finally { directory.deleteRecursively() }
    }
}

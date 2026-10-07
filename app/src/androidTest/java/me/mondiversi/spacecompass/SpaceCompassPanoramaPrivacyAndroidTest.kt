package me.mondiversi.spacecompass

import android.graphics.Bitmap
import androidx.exifinterface.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.filters.SdkSuppress
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.UUID

/** Synthetic private-cache JPEGs only; never writes or removes any gallery photograph. */
@SdkSuppress(minSdkVersion = 29)
class SpaceCompassPanoramaPrivacyAndroidTest {
    private fun fixture(root: File, withPath: Boolean = false): SpaceCompassPanoramaPreviewData {
        val time = Instant.parse("2026-10-06T03:43:00Z").toEpochMilli()
        val caption = SpaceCompassPanoramaCaptionData("Space Compass · 2026-10-06 · 05:43 (UTC+02:00)",
            "Private town, Region, Country", "Region, Country", listOf(
                SpaceCompassSunDataRow("Coordinates", "45.1° N\n8.9° E", "", "sun-info-coordinates")), null)
        val snapshot = SpaceCompassPanoramaSnapshot(time, 45.1, 8.9, 99.0, SpaceCompassSunSkyPhase.NIGHT,
            null, listOf(SpaceCompassPanoramaObject(SpaceCompassCelestialBody.SUN, "Sun", if (withPath)
                calculateSpaceCompassSunDailyPath(java.time.LocalDate.of(2026, 10, 6), java.time.ZoneId.of("UTC"),
                    45.1, 8.9, 99.0) else null)),
            SpaceCompassCelestialRemoteData(), spaceCompassPanoramaCaptionForPosition(caption, SpaceCompassPanoramaPosition.COMPLETE),
            listOf("North", "East", "South", "West"), listOf("+90°", "+60°", "+30°", "0°", "−30°", "−60°", "−90°"))
        val file = File(root, spaceCompassPanoramaFileName(time))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = renderSpaceCompassPanorama(snapshot, 512, context)
        try { file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 95, it)) } }
        finally { image.recycle() }
        stampSpaceCompassPanoramaFile(file, time, "Fixture credit")
        ExifInterface(file.absolutePath).apply {
            setAttribute(ExifInterface.TAG_GPS_LATITUDE, "45/1,6/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "8/1,54/1,0/1")
            setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E")
            setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "99/1")
            setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, "0")
            saveAttributes()
        }
        return SpaceCompassPanoramaPreviewData(file, time, SpaceCompassPanoramaPosition.COMPLETE, snapshot, caption, "Fixture credit")
    }
    @Test fun privateVariantsDoNotInheritGpsAndRetainFrozenTimestampAndCredits() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "panorama-privacy-test-" + UUID.randomUUID()).apply { check(mkdirs()) }
        try {
            val data = fixture(root)
            for (position in listOf(SpaceCompassPanoramaPosition.AREA, SpaceCompassPanoramaPosition.HIDDEN)) {
                val file = prepareSpaceCompassPanoramaVariant(context, data, position)
                assertNotEquals(data.file, file)
                val exif = ExifInterface(file.absolutePath)
                assertFalse(exif.getLatLong(FloatArray(2)))
                assertNull(exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE))
                assertEquals("2026:10:06 03:43:00", exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
                assertEquals("Fixture credit", exif.getAttribute(ExifInterface.TAG_ARTIST))
                assertEquals("Fixture credit", exif.getAttribute(ExifInterface.TAG_COPYRIGHT))
            }
            assertTrue(ExifInterface(data.file.absolutePath).getLatLong(FloatArray(2)))
        } finally { root.deleteRecursively() }
    }
    @Test fun privacyCacheNeverReusesTheCompleteFileAndKeepsEachVariantStable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "panorama-privacy-test-" + UUID.randomUUID()).apply { check(mkdirs()) }
        try {
            val data = fixture(root)
            val hidden = prepareSpaceCompassPanoramaVariant(context, data, SpaceCompassPanoramaPosition.HIDDEN)
            val before = hidden.readBytes()
            val area = prepareSpaceCompassPanoramaVariant(context, data, SpaceCompassPanoramaPosition.AREA)
            assertNotEquals(hidden, area)
            assertEquals(data.file, prepareSpaceCompassPanoramaVariant(context, data, SpaceCompassPanoramaPosition.COMPLETE))
            assertEquals(hidden, prepareSpaceCompassPanoramaVariant(context, data, SpaceCompassPanoramaPosition.HIDDEN))
            assertArrayEquals(before, hidden.readBytes())
        } finally { root.deleteRecursively() }
    }

    @Test fun labelAndDisclosureCombinationsKeepSeparateStableExports() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "panorama-options-test-" + UUID.randomUUID()).apply { check(mkdirs()) }
        try {
            val data = fixture(root, withPath = true)
            val original = data.file.readBytes()
            val files = mutableMapOf<Pair<SpaceCompassPanoramaPosition, Boolean>, File>()
            for (position in SpaceCompassPanoramaPosition.entries) for (labels in listOf(true, false)) {
                val file = prepareSpaceCompassPanoramaVariant(context, data, position, labels)
                files[position to labels] = file
                val before = file.readBytes()
                assertEquals(file, prepareSpaceCompassPanoramaVariant(context, data, position, labels))
                assertArrayEquals(before, file.readBytes())
                if (file != data.file) {
                    val exif = ExifInterface(file.absolutePath)
                    assertFalse(exif.getLatLong(FloatArray(2)))
                    assertNull(exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE))
                    assertEquals("2026:10:06 03:43:00", exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
                    assertEquals("Fixture credit", exif.getAttribute(ExifInterface.TAG_COPYRIGHT))
                }
            }
            assertEquals(6, files.values.toSet().size)
            assertArrayEquals(original, data.file.readBytes())
            val shown = android.graphics.BitmapFactory.decodeFile(files.getValue(SpaceCompassPanoramaPosition.HIDDEN to true).absolutePath)!!
            val hidden = android.graphics.BitmapFactory.decodeFile(files.getValue(SpaceCompassPanoramaPosition.HIDDEN to false).absolutePath)!!
            try {
                assertEquals(shown.width, hidden.width)
                assertEquals(shown.height, hidden.height)
                // Verify actual rendered pixels differ, not just cache filenames or metadata.
                var differences = 0
                for (y in 0 until shown.height) for (x in 0 until shown.width)
                    if (shown.getPixel(x, y) != hidden.getPixel(x, y)) differences++
                assertTrue("Switching labels changes the exported scene", differences > 100)
            } finally { shown.recycle(); hidden.recycle() }
        } finally { root.deleteRecursively() }
    }

    @Test fun exportProfilesHaveIndependentPrivacySafeJpegsWithFrozenGeometryAndTime() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "panorama-profiles-test-" + UUID.randomUUID()).apply { check(mkdirs()) }
        try {
            val original = fixture(root, withPath = true)
            val resources = context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply {
                setLocale(java.util.Locale.ITALIAN)
            }).resources
            val formatting = SpaceCompassPanoramaFormatting(java.util.Locale.ITALIAN, SpaceCompassNumericFormat.EUROPEAN,
                SpaceCompassDateFormat.EUROPEAN, SpaceCompassTimeFormat.H12, feet = true, dms = true)
            val selected = spaceCompassPanoramaPresentation(requireNotNull(original.snapshot), resources, formatting,
                java.util.TimeZone.getTimeZone("Europe/Rome"), SpaceCompassPlaceParts(locality = "Private town", adminArea = "Region", country = "Country"),
                99.0, 20.0, simulated = false, simulatedAltitude = false)
            val data = original.copy(selectedPresentation = selected)
            assertEquals(original.snapshot!!.timeMs, selected.snapshot.timeMs)
            assertEquals(original.snapshot.latitude, selected.snapshot.latitude, 0.0)
            assertEquals(original.snapshot.objects.first().path, selected.snapshot.objects.first().path)
            assertEquals("Sole", selected.snapshot.objects.first().name)
            assertTrue(selected.caption.rows.first { it.tag == "sun-info-altitude" }.value.endsWith("ft"))
            val files = mutableSetOf<File>()
            for (mode in SpaceCompassPanoramaExportMode.entries) for (position in SpaceCompassPanoramaPosition.entries)
                for (labels in listOf(true, false)) {
                    val file = prepareSpaceCompassPanoramaVariant(context, data, position, labels, mode)
                    files += file
                    if (file != original.file) {
                        val exif = ExifInterface(file.absolutePath)
                        assertFalse(exif.getLatLong(FloatArray(2)))
                        assertNull(exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE))
                        assertEquals("2026:10:06 03:43:00", exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
                    }
                }
            assertEquals(12, files.size)
            val selectedFile = prepareSpaceCompassPanoramaVariant(context, data, SpaceCompassPanoramaPosition.HIDDEN, true,
                SpaceCompassPanoramaExportMode.SELECTED)
            val internationalFile = prepareSpaceCompassPanoramaVariant(context, data, SpaceCompassPanoramaPosition.HIDDEN, true)
            assertFalse(selectedFile.readBytes().contentEquals(internationalFile.readBytes()))
        } finally { root.deleteRecursively() }
    }

    @Test fun aUserFormattedBasePreviewCannotBeMistakenForTheScientificExport() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "panorama-selected-base-test-" + UUID.randomUUID()).apply { check(mkdirs()) }
        try {
            val original = fixture(root, withPath = true)
            val resources = context.createConfigurationContext(android.content.res.Configuration(context.resources.configuration).apply {
                setLocale(java.util.Locale.ITALIAN)
            }).resources
            val selected = spaceCompassPanoramaPresentation(requireNotNull(original.snapshot), resources,
                SpaceCompassPanoramaFormatting(java.util.Locale.ITALIAN, SpaceCompassNumericFormat.EUROPEAN,
                    SpaceCompassDateFormat.EUROPEAN, feet = true), java.util.TimeZone.getTimeZone("Europe/Rome"),
                SpaceCompassPlaceParts(locality = "Private town", adminArea = "Region", country = "Country"),
                99.0, 20.0, simulated = false, simulatedAltitude = false)
            val snapshot = selected.snapshot.copy(caption = spaceCompassPanoramaCaptionForPosition(selected.caption, original.position))
            val image = renderSpaceCompassPanorama(snapshot, 512, context)
            try { original.file.outputStream().use { assertTrue(image.compress(Bitmap.CompressFormat.JPEG, 95, it)) } }
            finally { image.recycle() }
            val data = original.copy(selectedPresentation = selected, fileExportMode = SpaceCompassPanoramaExportMode.SELECTED)
            val preview = original.file.readBytes()
            assertEquals(data.file, prepareSpaceCompassPanoramaVariant(context, data, data.position,
                exportMode = SpaceCompassPanoramaExportMode.SELECTED))
            val scientific = prepareSpaceCompassPanoramaVariant(context, data, data.position,
                exportMode = SpaceCompassPanoramaExportMode.INTERNATIONAL)
            assertNotEquals(data.file, scientific)
            assertFalse(preview.contentEquals(scientific.readBytes()))
            assertArrayEquals(preview, data.file.readBytes())
            assertEquals(scientific, prepareSpaceCompassPanoramaVariant(context, data, data.position))
            assertEquals(data.file, prepareSpaceCompassPanoramaVariant(context, data, data.position,
                exportMode = SpaceCompassPanoramaExportMode.SELECTED))
        } finally { root.deleteRecursively() }
    }
}

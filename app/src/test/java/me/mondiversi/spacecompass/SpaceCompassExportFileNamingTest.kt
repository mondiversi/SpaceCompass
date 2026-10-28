package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class SpaceCompassExportFileNamingTest {
    private val moment=Instant.parse("2026-10-28T18:32:47.123Z").toEpochMilli()

    @Test fun internationalNamesHaveIsoDates24HourTimeAndAnExplicitFractionalZone() {
        assertEquals("SpaceCompass_2026-10-29_00-17-47.123_UTC+05.45_Moon_Daily-path",
            spaceCompassExportFileStem(moment,spaceCompassPanoramaInternationalFormatting,"Asia/Kathmandu",listOf("Moon","Daily path")))
    }
    @Test fun configuredNamesRespectDatesTwelveHourTimeNumbersAndLocalizedObjectNames() {
        val formatting=SpaceCompassPanoramaFormatting(Locale.ITALIAN,SpaceCompassNumericFormat.EUROPEAN,
            SpaceCompassDateFormat.EUROPEAN,SpaceCompassTimeFormat.H12,deviceLocale=Locale.ITALY)
        val name=spaceCompassExportFileStem(moment,formatting,"Europe/Rome",listOf("Luna","Percorso giornaliero"))
        assertTrue(name.startsWith("SpaceCompass_28-10-2026_7-32-47-PM,123_UTC+01.00_"))
        assertTrue(name.endsWith("_Luna_Percorso-giornaliero"))
    }
    @Test fun systemDateUsesTheDeviceRegionAndCapturedZoneInsteadOfCurrentDefaults() {
        val locale=Locale.getDefault(); val zone=TimeZone.getDefault()
        try {
            val formatting=SpaceCompassPanoramaFormatting(Locale.ENGLISH,SpaceCompassNumericFormat.INTERNATIONAL,
                SpaceCompassDateFormat.SYSTEM,SpaceCompassTimeFormat.H24,deviceLocale=Locale.US)
            val original=spaceCompassExportFileStem(moment,formatting,"America/New_York",listOf("Photo"))
            Locale.setDefault(Locale.JAPAN); TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"))
            assertEquals(original,spaceCompassExportFileStem(moment,formatting,"America/New_York",listOf("Photo")))
            assertTrue(original.contains("_10-28-26_14-32-47.123_UTC-04.00_"))
        } finally { Locale.setDefault(locale);TimeZone.setDefault(zone) }
    }
    @Test fun separatorsCannotBecomePathsOrInjectFieldsAndUnicodeNamesSurvive() {
        assertEquals("São-Tomé-Príncipe",spaceCompassExportFilePart("  São Tomé / Príncipe  "))
        val part=spaceCompassExportFilePart("../Moon_foo\\bar:\u202Enight\n*?<>|",80)
        assertFalse(part.contains('_')); assertFalse(part.contains('/')); assertFalse(part.contains('\\'))
        assertFalse(part.contains('\u202E')); assertFalse(part.contains('\n'))
    }
    @Test fun longUnicodeNamesKeepCompleteCharactersAndLeaveRoomForAnImageSuffix() {
        val stem=spaceCompassExportFileStem(moment,spaceCompassPanoramaInternationalFormatting,"UTC",listOf("天😀".repeat(100)))
        assertTrue(stem.length<=180);assertTrue(stem.toByteArray(Charsets.UTF_8).size<=220)
        assertFalse(Character.isHighSurrogate(stem.last()))
        assertTrue((stem+"_"+spaceCompassExportFilePart("現在の画像".repeat(10),32,28)+".jpg").toByteArray(Charsets.UTF_8).size<256)
    }
}

package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScreensaverWeekdayTest {
    @Test fun italianWeekdaysHaveThreeLettersAndAnInitialCapital() {
        val names = (0L..6L).map { day ->
            val timestamp = LocalDate.of(2026, 10, 5).plusDays(day).atTime(12, 0)
                .toInstant(ZoneOffset.UTC).toEpochMilli()
            formatSpaceCompassScreensaverWeekday(timestamp, Locale.ITALIAN, TimeZone.getTimeZone("UTC"))
        }
        assertEquals(listOf("Lun", "Mar", "Mer", "Gio", "Ven", "Sab", "Dom"), names)
    }

    @Test fun weekdayChangesWithTheLocalDateAcrossMidnight() {
        val timestamp = Instant.parse("2026-10-12T00:30:00Z").toEpochMilli()
        assertEquals("Lun", formatSpaceCompassScreensaverWeekday(timestamp, Locale.ITALIAN, TimeZone.getTimeZone("UTC")))
        assertEquals("Dom", formatSpaceCompassScreensaverWeekday(timestamp, Locale.ITALIAN, TimeZone.getTimeZone("America/Los_Angeles")))
    }

    @Test fun weekdayUsesTheRequestedLanguageAndOmitsAnAbbreviationPeriod() {
        val timestamp = Instant.parse("2026-10-11T12:00:00Z").toEpochMilli()
        val zone = TimeZone.getTimeZone("UTC")
        assertEquals("Sun", formatSpaceCompassScreensaverWeekday(timestamp, Locale.ENGLISH, zone))
        assertEquals("Dim", formatSpaceCompassScreensaverWeekday(timestamp, Locale.FRENCH, zone))
        for (language in listOf("en", "it", "es", "fr", "de", "pt", "ru", "tr", "ar", "he", "hi", "zh-CN", "ja", "ko", "sw", "pl", "nl", "id", "fa", "el")) {
            val name = formatSpaceCompassScreensaverWeekday(timestamp, Locale.forLanguageTag(language), zone)
            assertTrue(language, name.isNotBlank())
            assertFalse(language, name.endsWith('.'))
        }
    }
}

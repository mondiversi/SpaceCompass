package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.Locale

class SpaceCompassPointCountdownTest {
    @Test fun countdownUsesWholeMinutesAndKeepsTheCorrectSignAcrossTheTargetInstant() {
        assertEquals("T−1h1m", formatSpaceCompassPointCountdown(3661000, 0))
        assertEquals("T−0h0m", formatSpaceCompassPointCountdown(1000, 1))
        assertEquals("T+0h0m", formatSpaceCompassPointCountdown(1000, 1000))
        assertEquals("T+0h0m", formatSpaceCompassPointCountdown(1000, 1999))
        assertEquals("T+1h1m", formatSpaceCompassPointCountdown(0, 3661000))
        assertEquals("T−49h0m", formatSpaceCompassPointCountdown(49 * 3600000L, 0))
    }
    @Test fun durationIsIndependentOfRepeatedLocalHoursAndLocalizedDigitsWork() {
        val first = Instant.parse("2026-10-25T00:30:00Z").toEpochMilli()
        val second = Instant.parse("2026-10-25T01:30:00Z").toEpochMilli()
        assertEquals("T−1h0m", formatSpaceCompassPointCountdown(second, first))
        assertEquals("T+1h0m", formatSpaceCompassPointCountdown(first, second))
        val arabic = formatSpaceCompassPointCountdown(3661000, 0, Locale.forLanguageTag("ar-u-nu-arab"))
        assertTrue(arabic.startsWith("T−"))
        assertFalse(arabic.contains("01"))
        assertTrue(formatSpaceCompassPointCountdown(Long.MAX_VALUE, Long.MIN_VALUE).startsWith("T−"))
    }
}

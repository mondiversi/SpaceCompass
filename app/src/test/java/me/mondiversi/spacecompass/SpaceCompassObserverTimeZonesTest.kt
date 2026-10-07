package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObserverTimeZonesTest {
    private fun choices(iso: String, selected: String = "Europe/Rome") =
        spaceCompassObserverTimeZoneChoices(Instant.parse(iso).toEpochMilli(), selected,
            setOf("Europe/Rome", "America/New_York", "Asia/Kathmandu", "Pacific/Chatham", "Etc/GMT+1"))
            .associateBy { it.id }

    @Test fun geographicChoicesKeepDstRulesAndShowTheScenarioDateOffset() {
        val winter = choices("2027-01-07T12:00:00Z")
        val summer = choices("2027-07-07T12:00:00Z")
        assertEquals("UTC+01:00", formatSpaceCompassObserverUtcOffset(winter.getValue("Europe/Rome").offset))
        assertEquals("UTC+02:00", formatSpaceCompassObserverUtcOffset(summer.getValue("Europe/Rome").offset))
        assertEquals("UTC-05:00", formatSpaceCompassObserverUtcOffset(winter.getValue("America/New_York").offset))
        assertEquals("UTC-04:00", formatSpaceCompassObserverUtcOffset(summer.getValue("America/New_York").offset))
        assertEquals(winter.getValue("Europe/Rome").id, summer.getValue("Europe/Rome").id)
    }

    @Test fun quarterHourZonesAndSouthernHemisphereDstStayExact() {
        val winter = choices("2027-01-07T12:00:00Z")
        val summer = choices("2027-07-07T12:00:00Z")
        assertEquals("UTC+05:45", formatSpaceCompassObserverUtcOffset(winter.getValue("Asia/Kathmandu").offset))
        assertEquals("UTC+13:45", formatSpaceCompassObserverUtcOffset(winter.getValue("Pacific/Chatham").offset))
        assertEquals("UTC+12:45", formatSpaceCompassObserverUtcOffset(summer.getValue("Pacific/Chatham").offset))
    }

    @Test fun fixedOffsetsAndLegacyAliasesRemainSelectableWithoutChangingTheirIdentity() {
        for (selected in listOf("+05:30", "US/Eastern", "Etc/GMT+1", "UTC")) {
            assertTrue(choices("2027-01-07T12:00:00Z", selected).containsKey(selected))
        }
        val standard = choices("2027-01-07T12:00:00Z")
        assertFalse(standard.containsKey("Etc/GMT+1"))
        assertTrue(standard.containsKey("UTC"))
        assertFalse(choices("2027-01-07T12:00:00Z", "bad/zone").containsKey("bad/zone"))
    }

    @Test fun choicesAreUniqueAndOrderedByActualOffsetAtTheRequestedInstant() {
        val result = spaceCompassObserverTimeZoneChoices(Instant.parse("2027-07-07T12:00:00Z").toEpochMilli(), "Europe/Rome")
        assertEquals(result.size, result.map { it.id }.distinct().size)
        assertTrue(result.size > 300)
        result.zipWithNext().forEach { (a, b) -> assertTrue(a.offset.totalSeconds <= b.offset.totalSeconds) }
    }

    @Test fun utcAndHistoricalSubMinuteOffsetsAreUnambiguous() {
        assertEquals("UTC+00:00", formatSpaceCompassObserverUtcOffset(ZoneOffset.UTC))
        assertEquals("UTC+00:19:32", formatSpaceCompassObserverUtcOffset(ZoneOffset.ofHoursMinutesSeconds(0, 19, 32)))
        assertEquals("UTC-03:30", formatSpaceCompassObserverUtcOffset(ZoneOffset.ofHoursMinutes(-3, -30)))
    }
}

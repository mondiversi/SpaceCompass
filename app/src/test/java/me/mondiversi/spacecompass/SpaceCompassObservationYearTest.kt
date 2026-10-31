package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassObservationYearTest {
    private val zone = ZoneId.of("Europe/Rome")
    @Test fun editableYearsAcceptLocalizedDigitsAndRejectIncompleteOrUnsupportedInput() {
        for (text in listOf("2026"," 2026 ","٢٠٢٦","२०२६")) assertEquals(2026,parseSpaceCompassObservationYear(text))
        assertEquals(1900,parseSpaceCompassObservationYear("1900"))
        assertEquals(2100,parseSpaceCompassObservationYear("2100"))
        for (text in listOf("","202","1899","2101","20260","2O26","-026","2026.0"))
            assertNull(text,parseSpaceCompassObservationYear(text))
    }
    @Test fun consultationKeepsLocalCalendarMeaningAcrossLeapDaysDstAndYearBoundaries() {
        val leap = Instant.parse("2028-02-29T12:00:00Z").toEpochMilli()
        val previous = Instant.ofEpochMilli(spaceCompassObservationYearReference(leap,2027,zone)).atZone(zone)
        assertEquals(LocalDate.parse("2027-02-28"),previous.toLocalDate())
        val beforeGap = Instant.parse("2025-03-29T01:30:00Z").toEpochMilli()
        val gap = Instant.ofEpochMilli(spaceCompassObservationYearReference(beforeGap,2026,zone)).atZone(zone)
        assertEquals(LocalDate.parse("2026-03-29"),gap.toLocalDate());assertEquals(3,gap.hour)
        val island = ZoneId.of("Pacific/Kiritimati")
        val boundary = Instant.parse("2026-12-31T12:00:00Z").toEpochMilli()
        assertEquals(LocalDate.parse("2028-01-01"),Instant.ofEpochMilli(
            spaceCompassObservationYearReference(boundary,2028,island)).atZone(island).toLocalDate())
    }
    @Test fun actualAnnualSearchAndItsFrozenExportUseTheChosenYearRatherThanTheLocationYear() {
        val location = Instant.parse("2026-10-31T12:00:00Z").toEpochMilli()
        val reference = spaceCompassObservationYearReference(location,2028,zone)
        val values = calculateSpaceCompassObservationMoments(SpaceCompassCelestialBody.SUN,
            reference,zone,45.0,9.0,100.0,SpaceCompassCelestialRemoteData())!!
        assertEquals(4,values.size)
        assertTrue(values.all { Instant.ofEpochMilli(it.point.timeMs).atZone(zone).year==2028 })
        val request = SpaceCompassObservationRequest(SpaceCompassObservationCalendar(SpaceCompassCelestialBody.SUN,2028,zone),
            SpaceCompassObservationPlace(45.0,9.0,100.0),null,null)
        val snapshot = captureSpaceCompassSkyHeights(SpaceCompassObservationMomentsUiState(values,true,true,request),reference,location)
        assertEquals(2028,snapshot.calendar.year)
        assertEquals(reference,snapshot.observationTimeMs)
        assertEquals(location,snapshot.capturedAtMs)
        assertEquals(2026,Instant.ofEpochMilli(location).atZone(zone).year)
    }
    @Test fun anUnsupportedYearCannotBeUsedAsAHiddenReplacementReference() {
        for (year in listOf(1899,2101)) {
            try { spaceCompassObservationYearReference(0L,year,zone);fail("Unsupported year accepted") }
            catch (_: IllegalArgumentException) { }
        }
    }
}

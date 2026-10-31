package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class SpaceCompassSkyHeightsExportTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val observed = Instant.parse("2026-10-31T12:00:00Z").toEpochMilli()
    private val strings = SpaceCompassTextResources { id, args ->
        val value = when (id) {
            R.string.observation_height_maximum -> "Annual maximum"
            R.string.observation_height_minimum -> "Annual minimum"
            R.string.observation_height_cycle_maximum -> "Cycle maximum %s"
            R.string.observation_height_cycle_minimum -> "Cycle minimum %s"
            R.string.observation_height_cycle_maximum_single -> "Cycle maximum"
            R.string.observation_height_cycle_minimum_single -> "Cycle minimum"
            R.string.observation_height_mid_rising -> "Mid-height increasing %s"
            R.string.observation_height_mid_setting -> "Mid-height decreasing %s"
            R.string.observation_height_mid_rising_single -> "Mid-height increasing"
            R.string.observation_height_mid_setting_single -> "Mid-height decreasing"
            R.string.observation_heights_title -> "Key heights"
            R.string.sun_finder_altitude -> "Altitude: %s"
            R.string.capture_table_events -> "Events"
            R.string.format_date_time_heading -> "Date"
            R.string.format_time_heading -> "Time"
            R.string.observer_zone -> "Time zone"
            R.string.capture_table_elevation -> "Elevation"
            else -> "field-$id"
        }
        if (args.isEmpty()) value else String.format(Locale.ROOT, value, *args)
    }
    private fun profile(format: SpaceCompassPanoramaFormatting = spaceCompassPanoramaInternationalFormatting) =
        SpaceCompassDataExportProfile(format, SpaceCompassUnits(feet = format.feet, dms = format.dms), strings)
    private fun event(kind: SpaceCompassObservationMomentKind, date: String, elevation: Double) =
        SpaceCompassObservationMoment(kind, SpaceCompassSunPathPoint(Instant.parse(date).toEpochMilli(),
            SpaceCompassSunPosition(180.0, elevation), SpaceCompassSunPathEvent.CULMINATION))
    private fun moments() = mutableListOf(
        event(SpaceCompassObservationMomentKind.MAXIMUM, "2026-06-21T11:25:17Z", 68.25),
        event(SpaceCompassObservationMomentKind.MINIMUM, "2026-12-21T11:22:13Z", 21.45),
        event(SpaceCompassObservationMomentKind.MID_RISING, "2026-03-20T11:31:11Z", 44.85),
        event(SpaceCompassObservationMomentKind.MID_SETTING, "2026-09-23T11:16:19Z", 44.75))
    private fun state(values: List<SpaceCompassObservationMoment> = moments()) = SpaceCompassObservationMomentsUiState(
        values, true, true, SpaceCompassObservationRequest(
            SpaceCompassObservationCalendar(SpaceCompassCelestialBody.SUN, 2026, zone),
            SpaceCompassObservationPlace(45.106887, 8.999557, 1234.0), null, null))
    private fun snapshot() = captureSpaceCompassSkyHeights(state(), observed, observed + 1000)
    private val place = SpaceCompassPlaceParts(locality="Private town", subLocality="Private street",
        adminArea="Public region", country="Country")

    @Test fun snapshotFreezesEveryEventAndTheAcceptedCalculationReference() {
        val values = moments()
        val state = state(values)
        val snapshot = captureSpaceCompassSkyHeights(state, observed, observed + 1000)
        values.clear()
        assertEquals(4, snapshot.moments.size)
        assertEquals(state.calculationRequest!!.place, snapshot.place)
        assertEquals(2026, snapshot.calendar.year)
        assertEquals(observed, snapshot.observationTimeMs)
        assertEquals(observed + 1000, snapshot.capturedAtMs)
    }

    @Test fun structuredRowsMatchTheScreenNamesAndKeepIndividualSeasonalOffsets() {
        val doc = spaceCompassSkyHeightsDocument(snapshot(), profile(), place, SpaceCompassPanoramaPosition.AREA)
        assertEquals(listOf("Events","Date","Time","Time zone","Elevation (°)"), doc.columns)
        assertTrue(doc.title.endsWith("2026"))
        assertEquals(listOf("Annual maximum","Annual minimum","Mid-height increasing","Mid-height decreasing"),doc.rows.map { it[0] })
        assertTrue(doc.rows.all { it.size == 5 })
        assertEquals(listOf("UTC+02:00","UTC+01:00","UTC+01:00","UTC+02:00"),doc.rows.map { it[3] })
        assertTrue(doc.rows[0][2].contains("17")) // Preserve the actual culmination's seconds.
        assertEquals("68.3",doc.rows[0][4])
        val csv = renderSpaceCompassSkyHeightsCsv(doc)
        val txt = renderSpaceCompassSkyHeightsTxt(doc)
        assertTrue(csv.startsWith("\uFEFF")); assertTrue(csv.contains("\"Annual maximum\",\"2026-06-21\""))
        assertTrue(txt.contains("Events")); assertTrue(txt.contains("68.3")); assertTrue(txt.contains("UTC+01:00"))
    }

    @Test fun selectedFormattingUsesItsOwnDecimalsDatesClocksAndPhysicalUnits() {
        val selected = profile(SpaceCompassPanoramaFormatting(Locale.ITALIAN, SpaceCompassNumericFormat.EUROPEAN,
            SpaceCompassDateFormat.EUROPEAN, SpaceCompassTimeFormat.H12, feet=true, dms=true))
        val doc = spaceCompassSkyHeightsDocument(snapshot(), selected, place, SpaceCompassPanoramaPosition.COMPLETE)
        assertTrue(doc.decimalComma)
        assertEquals("68,3",doc.rows[0][4])
        assertTrue(doc.rows[0][1].startsWith("21/06/"))
        val csv = renderSpaceCompassSkyHeightsCsv(doc)
        assertTrue(csv.contains("\"Annual maximum\";"))
        assertTrue(doc.metadata.last().second.contains("ft"))
        assertTrue(doc.metadata.last().second.contains("Private town"))
        val international = spaceCompassSkyHeightsDocument(snapshot(), profile(), place, SpaceCompassPanoramaPosition.COMPLETE)
        assertFalse(international.decimalComma)
        assertTrue(international.metadata.last().second.contains("m"))
    }

    @Test fun bothFilesHonorAreaAndHiddenDisclosureWithoutFallbackToPreciseLocation() {
        for (disclosure in listOf(SpaceCompassPanoramaPosition.AREA,SpaceCompassPanoramaPosition.HIDDEN)) {
            val doc = spaceCompassSkyHeightsDocument(snapshot(), profile(), place, disclosure)
            for (text in listOf(renderSpaceCompassSkyHeightsCsv(doc),renderSpaceCompassSkyHeightsTxt(doc))) {
                assertFalse(text.contains("Private town")); assertFalse(text.contains("Private street"))
                assertFalse(text.contains("45.106887")); assertFalse(text.contains("1234"))
                if (disclosure == SpaceCompassPanoramaPosition.AREA) assertTrue(text.contains("Public region"))
                else { assertFalse(text.contains("Public region")); assertFalse(text.contains("Country")) }
            }
            if (disclosure == SpaceCompassPanoramaPosition.HIDDEN)
                assertTrue(doc.rows.all { it[3] == "UTC+00:00" })
        }
    }

    @Test fun incompleteUnavailableAndWrongYearResultsCannotProduceAnExport() {
        val valid = state()
        for (bad in listOf(valid.copy(complete=false), valid.copy(moments=null), valid.copy(calculationRequest=null),
            valid.copy(moments=listOf(event(SpaceCompassObservationMomentKind.MAXIMUM,"2027-01-01T12:00:00Z",60.0))))) {
            try { captureSpaceCompassSkyHeights(bad, observed, observed); fail("An invalid annual result was exported") }
            catch (_: IllegalArgumentException) { }
        }
    }
}

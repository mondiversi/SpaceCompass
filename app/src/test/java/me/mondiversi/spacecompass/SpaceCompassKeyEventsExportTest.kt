package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassKeyEventsExportTest {
    private val zone=ZoneId.of("Europe/Rome")
    private val time=Instant.parse("2026-07-01T12:34:56Z").toEpochMilli()
    private val request=SpaceCompassObservationRequest(
        SpaceCompassObservationCalendar(SpaceCompassCelestialBody.MOON,2026,zone),
        SpaceCompassObservationPlace(45.106887,8.999557,1234.0,true,true),null,null)
    private val event=SpaceCompassKeyEvent(SpaceCompassKeyEventKind.PERIGEE,time,40.0,distanceKm=360_000.0,azimuth=125.4)
    private val strings=SpaceCompassTextResources { id,args ->
        val value=when(id) {
            R.string.key_events_title -> "Key events"
            R.string.key_events_value -> "Value"
            R.string.key_event_perigee -> "Perigee"
            R.string.capture_table_events -> "Event"
            R.string.format_date_time_heading -> "Date"
            R.string.format_time_heading -> "Time"
            R.string.observer_zone -> "Zone"
            R.string.capture_table_azimuth -> "Azimuth"
            R.string.capture_table_elevation -> "Elevation"
            R.string.sun_finder_altitude -> "Altitude: %s"
            R.string.key_events_global_note -> "Distances are centre-to-centre"
            else -> "field-$id"
        }
        if(args.isEmpty())value else String.format(Locale.ROOT,value,*args)
    }
    private fun profile(feet:Boolean=false)=SpaceCompassDataExportProfile(
        spaceCompassPanoramaInternationalFormatting.copy(feet=feet),SpaceCompassUnits(feet=feet),strings)
    private fun state(events:List<SpaceCompassKeyEvent> = listOf(event))=
        SpaceCompassKeyEventsUiState(events,true,true,request)
    private fun snapshot()=captureSpaceCompassKeyEvents(state(),time,time+1000)
    private val place=SpaceCompassPlaceParts(locality="Private town",subLocality="Private street",
        adminArea="Public region",country="Country")

    @Test fun exportedSnapshotFreezesEventsAndTheirActualCalculationLocation() {
        val source=mutableListOf(event)
        val snapshot=captureSpaceCompassKeyEvents(state(source),time,time+1000)
        source.clear()
        assertEquals(listOf(event),snapshot.events)
        assertEquals(request.place,snapshot.place)
        assertEquals(request.calendar,snapshot.calendar)
    }
    @Test fun csvAndReadableTableShareChronologyValuesUnitsAndTheSelectedTimeZone() {
        val document=spaceCompassKeyEventsDocument(snapshot(),profile(),place,SpaceCompassPanoramaPosition.COMPLETE)
        assertEquals(listOf("Event","Date","Time","Zone","Azimuth (°)","Elevation (°)","Value"),document.columns)
        assertEquals("125.4",document.rows.single()[4])
        assertEquals("40.0",document.rows.single()[5])
        assertEquals("Perigee",document.rows.single()[0])
        assertTrue(document.rows.single()[2].startsWith("14:34:56"))
        assertTrue(document.rows.single()[3].contains("02"))
        assertTrue(document.rows.single()[6].contains("km"))
        val csv=renderSpaceCompassSkyHeightsCsv(document)
        val txt=renderSpaceCompassSkyHeightsTxt(document)
        assertTrue(csv.startsWith("\uFEFF"));assertTrue(csv.contains("Perigee"));assertTrue(txt.contains("Perigee"))
        val imperial=spaceCompassKeyEventsDocument(snapshot(),profile(true),place,SpaceCompassPanoramaPosition.COMPLETE)
        assertTrue(imperial.rows.single()[6].contains("mi"))
    }
    @Test fun hiddenPositionRemovesCoordinatesAreaAndLocalZoneAndUsesUtc() {
        val document=spaceCompassKeyEventsDocument(snapshot(),profile(),place,SpaceCompassPanoramaPosition.HIDDEN)
        val text=renderSpaceCompassSkyHeightsCsv(document)
        assertFalse(text.contains("Private"));assertFalse(text.contains("Public region"))
        assertFalse(text.contains("45.106887"));assertFalse(text.contains("1234"))
        assertTrue(document.rows.single()[2].startsWith("12:34:56"))
        assertEquals(2,document.metadata.size)
    }
    @Test fun areaDisclosureIncludesOnlyThePublicArea() {
        val document=spaceCompassKeyEventsDocument(snapshot(),profile(),place,SpaceCompassPanoramaPosition.AREA)
        val text=renderSpaceCompassSkyHeightsTxt(document)
        assertTrue(text.contains("Public region"));assertTrue(text.contains("Country"))
        assertFalse(text.contains("Private town"));assertFalse(text.contains("Private street"))
        assertFalse(text.contains("45.106887"))
    }
    @Test fun missingAzimuthRemainsUnavailableInsteadOfInventingNorth() {
        val snapshot=captureSpaceCompassKeyEvents(state(listOf(event.copy(azimuth=null))),time,time+1000)
        val document=spaceCompassKeyEventsDocument(snapshot,profile(),place,SpaceCompassPanoramaPosition.AREA)
        assertEquals("—",document.rows.single()[4])
        assertEquals("40.0",document.rows.single()[5])
    }
    @Test(expected=IllegalArgumentException::class) fun staleCalculationsCannotBeExported() {
        captureSpaceCompassKeyEvents(state().copy(complete=false),time,time)
    }
    @Test(expected=IllegalArgumentException::class) fun exportsCannotMixEventsFromAnotherYear() {
        captureSpaceCompassKeyEvents(state(listOf(event.copy(timeMs=Instant.parse("2027-01-02T00:00:00Z").toEpochMilli()))),time,time)
    }
}

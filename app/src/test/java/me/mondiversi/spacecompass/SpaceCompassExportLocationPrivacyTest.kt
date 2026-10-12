package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassExportLocationPrivacyTest {
    private val time=Instant.parse("2026-10-12T00:12:00Z").toEpochMilli()
    private val zone=ZoneId.of("Asia/Kathmandu")
    private val place=SpaceCompassPlaceParts(locality="Private town",subLocality="Private street",
        adminArea="Region",country="Country")
    private val location=SpaceCompassExportLocation(45.106887,8.999557,1234.0,20.0)
    private val strings=SpaceCompassTextResources { _,args -> "GPS altitude: "+args.joinToString() }
    private fun profile(format: SpaceCompassPanoramaFormatting=spaceCompassPanoramaInternationalFormatting)=
        SpaceCompassDataExportProfile(format,SpaceCompassUnits(),strings)
    private fun snapshot() : SpaceCompassPanoramaSnapshot {
        val day=LocalDate.parse("2026-10-12")
        fun point(t: Long,event: SpaceCompassSunPathEvent=SpaceCompassSunPathEvent.HOUR)=
            SpaceCompassSunPathPoint(t,SpaceCompassSunPosition(123.4,56.7),event=event)
        val points=(0..23).map { point(time+it*3_600_000L) }
        val path=SpaceCompassSunDailyPath(day,zone,points,points+point(time+123_000L,SpaceCompassSunPathEvent.SUNRISE),
            SpaceCompassCelestialBody.MOON)
        val state=SpaceCompassSunDailyPathUiState()
        state.select(path,points[1]);state.selectCurrent(path.body,points[0],path,zone)
        return spaceCompassSingleOrbitExportSnapshot(path,points[0],state.selectedPoints,time).copy(
            latitude=location.latitude,longitude=location.longitude,altitude=1234.0,
            objects=listOf(SpaceCompassPanoramaObject(path.body,"Moon",path)),
            currentTime="05:57",markerTimes=points.associate { it.timeMs to "local clock" })
    }

    @Test fun bothTableFormatsRespectAllDisclosureLevelsInBothFormattingProfiles() {
        val original=snapshot();val table=spaceCompassSingleOrbitExportTable(original,original.objects.single().path!!,
            original.objects.single().path!!.samples.first())
        val selected=SpaceCompassPanoramaFormatting(Locale.ITALIAN,SpaceCompassNumericFormat.EUROPEAN,
            SpaceCompassDateFormat.EUROPEAN,SpaceCompassTimeFormat.H12,feet=true,dms=true)
        for (format in listOf(spaceCompassPanoramaInternationalFormatting,selected)) {
            val profile=profile(format)
            for (position in SpaceCompassPanoramaPosition.entries) {
                val snap=spaceCompassSnapshotForDisclosure(original.copy(formatting=format),position)
                val caption=spaceCompassDailyExportCaption(snap,profile,location,place,position)
                val csv=renderSpaceCompassOrbitCsv(table,snap,time,caption)
                val txt=renderSpaceCompassOrbitTxt(table,snap,time,caption)
                for (value in listOf(csv,txt)) {
                    assertEquals(position==SpaceCompassPanoramaPosition.COMPLETE,value.contains("Private town"))
                    assertEquals(position!=SpaceCompassPanoramaPosition.HIDDEN,value.contains("Region, Country"))
                    assertEquals(position==SpaceCompassPanoramaPosition.COMPLETE,value.contains("GPS altitude"))
                    assertEquals(position==SpaceCompassPanoramaPosition.HIDDEN,value.contains("UTC+00:00"))
                    assertEquals(position!=SpaceCompassPanoramaPosition.HIDDEN,value.contains("UTC+05:45"))
                    if (position!=SpaceCompassPanoramaPosition.COMPLETE) {
                        for (secret in listOf("Private street","45.106887","8.999557","±20","1\u202f234 m"))
                            assertFalse(secret,value.contains(secret))
                    }
                }
            }
        }
    }

    @Test fun hiddenTimesKeepAbsoluteInstantsAcrossDateBoundariesFractionalOffsetsAndDst() {
        val original=snapshot();val path=original.objects.single().path!!
        val table=spaceCompassSingleOrbitExportTable(original,path,path.samples.first())
        for (zoneId in listOf("Asia/Kathmandu","America/New_York","Australia/Lord_Howe","Europe/Rome")) {
            for (moment in listOf(time,Instant.parse("2026-10-25T00:45:00Z").toEpochMilli(),
                Instant.parse("2026-10-25T01:45:00Z").toEpochMilli())) {
                val snapshot=original.copy(timeMs=moment,zoneId=zoneId)
                val frozenTable=table.copy(observationTimeMs=moment,zoneId=zoneId)
                val hidden=spaceCompassSnapshotForDisclosure(snapshot,SpaceCompassPanoramaPosition.HIDDEN)
                val grid=spaceCompassOrbitExportGrid(frozenTable,hidden,moment,"No location")
                val observed=grid[2][1]
                val match=Regex("(\\d{4}-\\d{2}-\\d{2}) · (\\d{2}:\\d{2}) ").find(observed)!!
                assertEquals(moment/60_000L,Instant.parse(match.groupValues[1]+"T"+match.groupValues[2]+":00Z").toEpochMilli()/60_000L)
                assertTrue(observed.endsWith("UTC+00:00)"))
                for (row in grid.drop(5)) if (row[1]!="—") assertTrue(row[1].endsWith("UTC+00:00)"))
                assertEquals(formatSpaceCompassPanoramaExportTime(moment,TimeZone.getTimeZone("UTC")),hidden.currentTime)
            }
        }
    }

    @Test fun privacyChangesOnlyPresentationNeverOrbitGeometrySelectionKeysOrCountdowns() {
        val original=snapshot();val path=original.objects.single().path!!
        val table=spaceCompassSingleOrbitExportTable(original,path,path.samples.first())
        val before=spaceCompassOrbitExportGrid(table,original,time,"Original")
        val hidden=spaceCompassSnapshotForDisclosure(original,SpaceCompassPanoramaPosition.HIDDEN)
        val after=spaceCompassOrbitExportGrid(table,hidden,time,"Hidden")
        assertEquals(original.zoneId,hidden.zoneId);assertEquals(original.selectedPoints,hidden.selectedPoints)
        assertEquals(original.objects,hidden.objects);assertEquals(original.latitude,hidden.latitude,0.0)
        assertEquals(original.longitude,hidden.longitude,0.0);assertEquals(original.altitude,hidden.altitude,0.0)
        assertEquals(original.timeMs,hidden.timeMs);assertEquals(before.drop(5).map { it.drop(2) },after.drop(5).map { it.drop(2) })
        val current=resolveSpaceCompassPanoramaSelections(hidden,mapOf(path.body to path.samples.first().position))
        assertTrue(current.any { it.isCurrent });assertTrue(current.any { !it.isCurrent })
        assertEquals("05:57",original.currentTime);assertEquals("00:12",hidden.currentTime)
        assertEquals(original.currentTime,spaceCompassSnapshotForDisclosure(hidden,SpaceCompassPanoramaPosition.AREA).currentTime)
    }

    @Test fun unavailableGeocodingNeverFallsBackToPreciseLocationInCoarseExports() {
        val snapshot=snapshot()
        for (parts in listOf(null,SpaceCompassPlaceParts(locality="Private town",subAdminArea="Private county"))) {
            val area=spaceCompassDailyExportCaption(snapshot,profile(),location,parts,SpaceCompassPanoramaPosition.AREA)
            assertFalse(area.contains("Private"));assertFalse(area.contains("45.106887"));assertFalse(area.contains("GPS altitude"))
        }
    }

    @Test fun privateCaptionUsesUtcButStillKeepsWeatherAndCameraPointing() {
        val data=SpaceCompassPanoramaCaptionData("Local time UTC+05:45","Private town","Region, Country",
            listOf(SpaceCompassSunDataRow("","Cloudy","","celestial-environment-weather")),"53%",
            cameraPointing="Heading 123°",cameraFieldOfView="Field of view 60°",hiddenTimestamp="Frozen UTC+00:00")
        val value=spaceCompassPanoramaCaptionForPosition(data,SpaceCompassPanoramaPosition.HIDDEN)
        assertEquals("Frozen UTC+00:00 · Cloudy (53%) · Heading 123° · Field of view 60°",value)
        assertFalse(value.contains("Region"));assertFalse(value.contains("Private"));assertFalse(value.contains("UTC+05:45"))
    }
}

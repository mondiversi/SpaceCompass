package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassForwardCaptureTest {
    private val captured=Instant.parse("2026-06-21T22:27:35Z").toEpochMilli()
    private val next=Instant.parse("2026-06-22T00:00:00Z").toEpochMilli()
    private val zone=TimeZone.getTimeZone("Europe/London")
    private val formatting=spaceCompassPanoramaInternationalFormatting

    @Test fun nextDayCaptureLabelsIncludeThePointDateAndCountdown() {
        val point=SpaceCompassSunPathPoint(next,SpaceCompassSunPosition(10.0,-12.0))
        val moment=formatSpaceCompassPanoramaMarkerTime(next,captured,zone,formatting)
        assertTrue(moment.contains("2026-06-22"))
        assertTrue(moment.contains("01:00"))
        val label=formatSpaceCompassPanoramaPathPointLabel(moment,point,formatting,
            spaceCompassPanoramaDefaultEventNames,captured)
        assertTrue(label.contains("T−1h32m"))
        assertEquals("23:27",formatSpaceCompassPanoramaMarkerTime(captured,captured,zone,formatting))
    }

    @Test fun privacyAndSelectedFormatsPreserveTheWindowWhileReformattingPointDates() {
        val italian=formatting.copy(locale=Locale.ITALIAN,date=SpaceCompassDateFormat.EUROPEAN,
            numeric=SpaceCompassNumericFormat.SYSTEM,deviceLocale=Locale.ITALIAN)
        val moment=formatSpaceCompassPanoramaMarkerTime(next,captured,zone,italian)
        assertTrue(moment.contains("22/06/2026"))
        val p=requireNotNull(calculateSpaceCompassCelestialPath(SpaceCompassCelestialBody.SUN,
            Instant.ofEpochMilli(captured).atZone(zone.toZoneId()).toLocalDate(),zone.toZoneId(),captured,
            51.5074,-.1278,35.0,SpaceCompassCelestialRemoteData(),true))
        val snapshot=spaceCompassSingleOrbitExportSnapshot(p,p.samples.first(),emptyList(),captured)
        val hidden=spaceCompassSnapshotForDisclosure(snapshot,SpaceCompassPanoramaPosition.HIDDEN)
        assertTrue(hidden.forward24Hours)
        assertEquals(snapshot.objects,hidden.objects)
        assertEquals("UTC",hidden.displayZoneId)
        assertEquals(formatSpaceCompassPanoramaMarkerTime(next,captured,TimeZone.getTimeZone("UTC"),formatting),
            formatSpaceCompassPanoramaMarkerTime(next,hidden.timeMs,hidden.displayTimeZone,hidden.formatting))
    }

    @Test fun shutterReplacesOldCivilDayPathsAndFreezesOneFutureWindowForPhotoAndPanorama() {
        val z=ZoneId.of("Europe/London")
        val date=Instant.ofEpochMilli(captured).atZone(z).toLocalDate()
        val remote=SpaceCompassCelestialRemoteData()
        val bodies=listOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON)
        val objects=bodies.map { body -> SpaceCompassPanoramaObject(body,body.name,
            calculateSpaceCompassCelestialPath(body,date,z,captured,51.5074,-.1278,35.0,remote)) }
        val snapshot=SpaceCompassPanoramaSnapshot(captured,51.5074,-.1278,35.0,SpaceCompassSunSkyPhase.NIGHT,
            null,objects,remote,"",emptyList(),emptyList(),zoneId=z.id,forward24Hours=true)
        val frozen=freezeSpaceCompassApproachSnapshot(snapshot)
        assertTrue(frozen.forward24Hours)
        for (item in frozen.objects) {
            val path=requireNotNull(item.path)
            assertTrue(path.forward24Hours)
            assertEquals(captured,path.samples.first().timeMs)
            assertEquals(captured+86_400_000L,path.samples.last().timeMs)
            assertTrue(path.markers.all { it.timeMs>captured && it.timeMs<=captured+86_400_000L })
        }
        val table=captureSpaceCompassOrbitTable(frozen)
        assertTrue(table.rows.flatMap { it.cells.values }.all { it.point.timeMs in captured..captured+86_400_000L })
        assertEquals(bodies,table.bodies)
    }

    @Test fun csvAndTxtContainTheFrozenFuturePointsAndBothDates() {
        val z=zone.toZoneId()
        val path=requireNotNull(calculateSpaceCompassCelestialPath(SpaceCompassCelestialBody.SUN,
            Instant.ofEpochMilli(captured).atZone(z).toLocalDate(),z,captured,51.5074,-.1278,35.0,
            SpaceCompassCelestialRemoteData(),true))
        val snapshot=spaceCompassSingleOrbitExportSnapshot(path,path.samples.first(),emptyList(),captured)
        val table=spaceCompassSingleOrbitExportTable(snapshot,path,path.samples.first())
        val csv=renderSpaceCompassOrbitCsv(table,snapshot,captured,"demo")
        val txt=renderSpaceCompassOrbitTxt(table,snapshot,captured,"demo")
        for (text in listOf(csv,txt)) {
            assertTrue(text.contains("2026-06-21"))
            assertTrue(text.contains("2026-06-22"))
            assertTrue(text.contains("T−"))
            assertFalse(text.contains("T+1h"))
        }
        assertTrue(table.rows.all { it.cells.isNotEmpty() })
    }
}

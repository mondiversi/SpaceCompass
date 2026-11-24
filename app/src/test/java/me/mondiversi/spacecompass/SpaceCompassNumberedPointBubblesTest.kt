package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassNumberedPointBubblesTest {
    private val zone=ZoneId.of("Europe/Rome")
    private val date=LocalDate.parse("2026-10-25")
    private fun snapshot(path: SpaceCompassSunDailyPath)=SpaceCompassPanoramaSnapshot(
        path.samples.first().timeMs,45.0,9.0,100.0,SpaceCompassSunSkyPhase.MORNING,null,
        listOf(SpaceCompassPanoramaObject(path.body,"Sole",path)),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList(),
        zoneId=zone.id,orbitExportVocabulary=SpaceCompassOrbitExportVocabulary(point="Punto %s",antipode="Antipodo"),
        currentPointName="Attuale",pointEventNames=spaceCompassPanoramaDefaultEventNames+
            (SpaceCompassSunPathEvent.CULMINATION to "Culmine"))

    @Test fun ordinarySolarLabelsMatchDailyPathThroughRepeatedDstHours() {
        val path=calculateSpaceCompassSunDailyPath(date,zone,45.0,9.0)
        val snapshot=snapshot(path)
        val names=spaceCompassPanoramaPathPointNames(snapshot,path)
        val dots=path.markers.filter { it.event==SpaceCompassSunPathEvent.HOUR }
        assertEquals(25,dots.size)
        assertEquals(listOf(0,1,2,2)+(3..23),dots.map { java.time.Instant.ofEpochMilli(it.timeMs).atZone(zone).hour })
        dots.forEach { point ->
            val hour=java.time.Instant.ofEpochMilli(point.timeMs).atZone(zone).hour
            val label=formatSpaceCompassPanoramaPathPointLabel("02:00",point,snapshot.formatting,
                snapshot.pointEventNames,snapshot.timeMs,pointNames=names)
            assertEquals("Punto $hour",label.lineSequence().first())
        }
    }

    @Test fun satelliteSamplesUseIndicesWhileForward24HoursUsesLocalClockHours() {
        val start=date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dots=listOf(600_000L,30_000L,300_000L).map { SpaceCompassSunPathPoint(start+it,SpaceCompassSunPosition(20.0,15.0)) }
        val path=SpaceCompassSunDailyPath(date,zone,dots.sortedBy { it.timeMs },dots,SpaceCompassCelestialBody.ISS)
        assertEquals(listOf("Punto 2","Punto 0","Punto 1"),dots.map(spaceCompassPanoramaPathPointNames(snapshot(path),path)::name))
        val forward=path.copy(forward24Hours=true)
        assertEquals(listOf("Punto 0","Punto 0","Punto 0"),dots.map(spaceCompassPanoramaPathPointNames(snapshot(forward),forward)::name))
    }

    @Test fun selectedAndAntipodalLabelsKeepTheOriginalNumberButCurrentAndKeyPointsKeepTheirNames() {
        val path=calculateSpaceCompassSunDailyPath(date,zone,45.0,9.0)
        val snapshot=snapshot(path)
        val point=path.markers.first { it.event==SpaceCompassSunPathEvent.HOUR }
        val pin=SpaceCompassPanoramaSelectedPoint(path.body,point,path,false)
        assertTrue(formatSpaceCompassPanoramaSelection(snapshot,pin).startsWith("Punto 0\n"))
        assertTrue(formatSpaceCompassPanoramaSelection(snapshot,pin.copy(point=point.antipodalDirection(),isAntipode=true))
            .startsWith("Antipodo · Punto 0\n"))
        assertTrue(formatSpaceCompassPanoramaSelection(snapshot,pin.copy(isCurrent=true)).startsWith("Attuale\n"))
        val key=path.markers.first { it.event==SpaceCompassSunPathEvent.CULMINATION }
        assertTrue(formatSpaceCompassPanoramaSelection(snapshot,pin.copy(point=key)).startsWith("Culmine\n"))
    }

    @Test fun namesFollowExportLanguageAndDeviceNumericProfileWithoutChangingTheFrozenPoint() {
        val path=calculateSpaceCompassSunDailyPath(date,zone,45.0,9.0)
        val snapshot=snapshot(path)
        val point=path.markers.first { it.event==SpaceCompassSunPathEvent.HOUR }
        val translated=snapshot.copy(formatting=snapshot.formatting.copy(locale=Locale.forLanguageTag("ar"),
            numeric=SpaceCompassNumericFormat.SYSTEM,deviceLocale=Locale.forLanguageTag("ar")),
            orbitExportVocabulary=snapshot.orbitExportVocabulary.copy(point="نقطة %s"))
        val name=spaceCompassPanoramaPathPointNames(translated,path).name(point)
        assertEquals("نقطة "+formatSpaceCompassNumber(0.0,0,SpaceCompassNumericFormat.SYSTEM,grouping=false,
            systemLocale=translated.formatting.deviceLocale),name)
        assertEquals(point,path.markers.first { it.event==SpaceCompassSunPathEvent.HOUR })
    }
}

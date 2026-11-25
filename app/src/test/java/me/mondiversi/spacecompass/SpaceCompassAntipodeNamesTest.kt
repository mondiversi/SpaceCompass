package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassAntipodeNamesTest {
    private val zone = ZoneId.of("UTC")
    private val date = LocalDate.parse("2026-11-25")
    private val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val body = SpaceCompassCelestialBody.MOON
    private val hour = SpaceCompassSunPathPoint(start + 12 * 3_600_000, SpaceCompassSunPosition(30.0,20.0))
    private val peak = hour.copy(timeMs = start + 13 * 3_600_000, event = SpaceCompassSunPathEvent.CULMINATION)
    private val now = start + 14 * 3_600_000
    private val path = SpaceCompassSunDailyPath(date,zone,listOf(hour,peak),listOf(hour,peak),body)
    private fun snapshot() = SpaceCompassPanoramaSnapshot(now,0.0,0.0,0.0,SpaceCompassSunSkyPhase.NIGHT,null,
        listOf(SpaceCompassPanoramaObject(body,"Luna",path)),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList(),
        zoneId=zone.id,currentPointName="Attuale",orbitExportVocabulary=SpaceCompassOrbitExportVocabulary(point="Punto %s",antipode="Antipodo"),
        pointEventNames=spaceCompassPanoramaDefaultEventNames+(SpaceCompassSunPathEvent.CULMINATION to "Culmine"))

    @Test fun capturedAndAdaptiveNamesKeepObjectPointAndLocalizedSuffix() {
        val snapshot=snapshot()
        for((point,current,name) in listOf(Triple(hour,false,"Punto 12"),Triple(peak,false,"Culmine"),Triple(hour,true,"Attuale"))) {
            val pin=SpaceCompassPanoramaSelectedPoint(body,point.antipodalDirection(),path,current,true)
            val nameWithSuffix="$name (Antipodo)"
            val title="Luna · $nameWithSuffix"
            assertEquals(nameWithSuffix,spaceCompassPanoramaSelectionName(snapshot,pin))
            val caption=formatSpaceCompassPanoramaSelection(snapshot,pin)
            assertEquals(title,caption.lineSequence().first())
            val visual=formatSpaceCompassPanoramaSelection(snapshot,pin,visualAntipode=true)
            assertEquals("Luna · $name $SPACE_COMPASS_ANTIPODE_CAPTION_ICON",visual.lineSequence().first())
            assertEquals(caption.lines().drop(1),visual.lines().drop(1))
            assertFalse(visual.contains("(Antipodo)"))
            assertTrue(caption.contains("210.0°"));assertTrue(caption.contains("-20.0°"))
            val adaptive=formatSpaceCompassGlobeMainLabelDetails("Luna",nameWithSuffix,"12:00",pin.point.position,snapshot.formatting,keepBodyName=true)
            for(label in listOf(adaptive.full,adaptive.compact,adaptive.minimal)) assertTrue(label.startsWith(title))
        }
    }

    @Test fun frozenTablesDistinguishCurrentHourlyAndKeyAntipodesInCsvAndText() {
        val state=SpaceCompassSunDailyPathUiState()
        state.select(path,hour);state.select(path,peak)
        state.selectCurrent(body,hour.copy(timeMs=now),path)
        val snapshot=snapshot().copy(selectedPoints=state.selectedPoints,showAntipodes=true)
        val table=captureSpaceCompassOrbitTable(snapshot,mapOf(body to hour.position))
        val mirrors=table.rows.filter { it.kind==SpaceCompassOrbitExportRowKind.ANTIPODE }
        assertEquals(3,mirrors.size)
        assertEquals(1,mirrors.count { it.cells[body]!!.antipodeSource!!.isCurrent })
        for(text in listOf(renderSpaceCompassOrbitCsv(table,snapshot,now,""),renderSpaceCompassOrbitTxt(table,snapshot,now,""))) {
            for(name in listOf("Punto 12","Culmine","Attuale")) assertTrue(text.contains("Luna · $name (Antipodo)"))
            assertTrue(text.contains("210.0"));assertTrue(text.contains("-20.0"))
        }
    }

    @Test fun fixedDateVariantsRetainTheirInstanceNameAndEventWithoutDuplicateBodyNames() {
        val comparison=SpaceCompassTemporalComparison.create(body,peak.timeMs,zone,SpaceCompassComparisonSource.HEIGHT,"MONTHLY:2026:11")
        val variant=path.copy(comparison=comparison)
        val snapshot=snapshot().copy(objects=listOf(SpaceCompassPanoramaObject(body,"Luna · 21-11-2026",variant,comparison=comparison)))
        val pin=SpaceCompassPanoramaSelectedPoint(body,peak.antipodalDirection(),variant,true,true)
        assertEquals("Luna · 21-11-2026 · Culmine (Antipodo)",formatSpaceCompassPanoramaSelection(snapshot,pin).lineSequence().first())
        val english=snapshot.copy(orbitExportVocabulary=snapshot.orbitExportVocabulary.copy(antipode="Antipode"),
            pointEventNames=snapshot.pointEventNames+(SpaceCompassSunPathEvent.CULMINATION to "Culmination"))
        assertEquals("Culmination (Antipode)",spaceCompassPanoramaSelectionName(english,pin))
    }
}

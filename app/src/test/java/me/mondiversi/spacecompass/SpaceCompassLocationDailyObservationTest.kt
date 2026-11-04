package me.mondiversi.spacecompass

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassLocationDailyObservationTest {
    private val body=SpaceCompassCelestialBody.SUN
    private val zone=ZoneId.of("Europe/Rome")
    private val now=Instant.parse("2026-11-03T17:00:00Z").toEpochMilli()
    private val reference=Instant.parse("2026-06-21T11:25:00Z").toEpochMilli()
    private val comparison=SpaceCompassTemporalComparison.create(body,reference,zone,
        SpaceCompassComparisonSource.HEIGHT,"MAXIMUM:2026-06-21")
    private val remote=SpaceCompassCelestialRemoteData()
    private fun path(time:Long=now,z:ZoneId=zone)=calculateSpaceCompassSunDailyPath(
        Instant.ofEpochMilli(time).atZone(z).toLocalDate(),z,45.0,9.0,100.0)
    private fun frozen()=path(reference).copy(comparison=comparison)
    private fun snapshot(items:List<SpaceCompassPanoramaObject>,selected:List<SpaceCompassSelectedOrbitPoint> = emptyList(),antipodes:Boolean=false)=
        SpaceCompassPanoramaSnapshot(now,45.0,9.0,100.0,SpaceCompassSunSkyPhase.NIGHT,null,items,remote,
            "",emptyList(),emptyList(),zoneId=zone.id,currentPointName="Attuale",referencePointName="Riferimento",
            selectedPoints=selected,showAntipodes=antipodes,comparisonPointNames=mapOf(comparison.id to "Massimo annuale"))

    @Test fun neitherProvidedNorRetainedComparisonCanFillTheLocationDailyTable() {
        assertNull(spaceCompassLocationDailyPath(body,now,zone,false,frozen(),frozen()))
        assertNull(spaceCompassLocationDailyOverlay(body,SpaceCompassCelestialOverlay(body,null,null,null,frozen(),reference)))
    }
    @Test fun aNormalLocationPathWinsOverTheRetainedComparison() {
        val live=path()
        assertSame(live,spaceCompassLocationDailyPath(body,now,zone,false,live,frozen()))
        assertSame(live,spaceCompassLocationDailyPath(body,now,zone,false,null,live))
    }
    @Test fun oldDaysOtherBodiesAndDifferentZonesCannotLeakIntoTheDailyTable() {
        val live=path()
        assertNull(spaceCompassLocationDailyPath(body,now,zone,false,null,path(now-86_400_000)))
        assertNull(spaceCompassLocationDailyPath(body,now,zone,false,live.copy(body=SpaceCompassCelestialBody.MOON),null))
        assertNull(spaceCompassLocationDailyPath(body,now,zone,false,path(now,ZoneId.of("UTC")),null))
    }
    @Test fun locationCanBeSimulatedWithoutAdoptingTheComparisonsDateOrZone() {
        val configured=Instant.parse("2027-01-01T01:00:00Z").toEpochMilli()
        val tokyo=ZoneId.of("Asia/Tokyo")
        val context=SpaceCompassLocationDailyObservation(configured,tokyo)
        val expected=path(configured,tokyo)
        assertEquals(LocalDate.of(2027,1,1),spaceCompassLocationDailyPath(body,context.timeMs,context.zone,false,expected,frozen())!!.date)
        assertEquals(tokyo,context.zone)
        assertNotEquals(comparison.timeMs,context.timeMs)
    }
    @Test fun forwardModeRequiresItsOwnWindowAndRejectsExpiredOrFrozenBuffers() {
        val window=SpaceCompassSunDailyPath(Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),zone,
            listOf(SpaceCompassSunPathPoint(now,SpaceCompassSunPosition(0.0,0.0)),
                SpaceCompassSunPathPoint(now+86_400_000,SpaceCompassSunPosition(0.0,0.0))),emptyList(),body,forward24Hours=true)
        assertSame(window,spaceCompassLocationDailyPath(body,now,zone,true,window,null))
        assertNull(spaceCompassLocationDailyPath(body,now+86_400_001,zone,true,window,null))
        assertNull(spaceCompassLocationDailyPath(body,now,zone,false,window,null))
        assertNull(spaceCompassLocationDailyPath(body,now,zone,true,window.copy(comparison=comparison),null))
    }
    @Test fun openingTheLocationDailyTableKeepsComparisonPinsButDoesNotAddThemToItsRows() {
        val saved=frozen();val live=path();val point=saved.markers.first()
        val state=SpaceCompassSunDailyPathUiState();state.select(saved,point)
        val before=state.selectedPoints
        state.openMenu(live,spaceCompassCurrentPathPoint(body,now,calculateSpaceCompassSunPosition(now,45.0,9.0,100.0)))
        val rows=spaceCompassSelectedDailyPathEntries(live,state.menuCurrentPoint,state.selectedPoints)
        assertEquals(before,state.selectedPoints)
        assertTrue(rows.none { it.sourcePath?.comparison!=null })
        assertTrue(rows.none { state.isSelected(live,it.point,it.isCurrent) })
        state.closeMenu();assertEquals(before,state.selectedPoints)
    }
    @Test fun currentImageLabelsRemainCurrentOnlyForLiveObjects() {
        val snap=snapshot(listOf(SpaceCompassPanoramaObject(body,"Sun",path())))
        assertSame(snap,snap.forCurrentInstance(null))
        val frozen=snap.forCurrentInstance(comparison)
        assertEquals("Attuale",snap.currentPointName)
        assertEquals("Massimo annuale",frozen.currentPointName)
        assertEquals(reference,frozen.timeMs)
        assertEquals(zone.id,frozen.displayZoneId)
    }
    @Test fun selectedReferenceAndItsAntipodeUseTheReferenceLabelInCaptureImages() {
        val path=frozen();val point=SpaceCompassSunPathPoint(reference,calculateSpaceCompassSunPosition(reference,45.0,9.0,100.0))
        val state=SpaceCompassSunDailyPathUiState();state.selectCurrent(body,point,path);state.toggleAntipodes()
        val snap=snapshot(listOf(SpaceCompassPanoramaObject(body,"Sun",path)),state.selectedPoints,true)
        val points=resolveSpaceCompassPanoramaSelections(snap,emptyMap())
        assertEquals(2,points.size)
        points.forEach { val text=formatSpaceCompassPanoramaSelection(snap,it)
            assertTrue(text.contains("Massimo annuale"));assertFalse(text.contains("Attuale")) }
    }
    @Test fun referenceRowsAndCellsAreAlsoNamedCorrectlyInCsvAndReadableTables() {
        val snap=snapshot(listOf(SpaceCompassPanoramaObject(body,"Sun reference",frozen())))
        val table=captureSpaceCompassOrbitTable(snap)
        val rows=spaceCompassOrbitExportGrid(table,snap,now,"")
        val current=rows.single { it.first().contains("Massimo annuale") }
        assertFalse(current.first().contains("Attuale"))
        assertTrue(current.any { it=="Massimo annuale" })
        assertTrue(renderSpaceCompassOrbitTxt(table,snap,now,"").contains("Massimo annuale"))
    }
}

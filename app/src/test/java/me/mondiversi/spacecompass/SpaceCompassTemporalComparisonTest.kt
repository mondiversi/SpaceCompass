package me.mondiversi.spacecompass

import java.io.StringReader
import java.io.StringWriter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Properties
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassTemporalComparisonTest {
    private val sun = SpaceCompassCelestialBody.SUN
    private val zone = ZoneId.of("Europe/Rome")
    private val now = Instant.parse("2026-11-03T12:00:00Z").toEpochMilli()
    private val remote = SpaceCompassCelestialRemoteData()
    private fun comparison(day: String = "2026-06-21", source: SpaceCompassComparisonSource = SpaceCompassComparisonSource.HEIGHT) =
        SpaceCompassTemporalComparison.create(sun, LocalDate.parse(day).atTime(13, 0).atZone(zone).toInstant().toEpochMilli(),
            zone, source, "MAXIMUM:$day")
    private fun path(c: SpaceCompassTemporalComparison) = calculateSpaceCompassSunDailyPath(
        Instant.ofEpochMilli(c.timeMs).atZone(c.zone).toLocalDate(), c.zone, 45.0, 9.0, 100.0).copy(comparison = c)
    private fun point(c: SpaceCompassTemporalComparison) = SpaceCompassSunPathPoint(c.timeMs,
        calculateSpaceCompassSunPosition(c.timeMs, 45.0, 9.0, 100.0))
    private fun overlay(c: SpaceCompassTemporalComparison) = SpaceCompassComparisonOverlay(c,
        SpaceCompassCelestialOverlay(sun, calculateSpaceCompassCelestialObservation(sun,c.timeMs,45.0,9.0,100.0,remote),
            null, null, path(c), c.timeMs))
    private fun snapshot(comparisons: List<SpaceCompassTemporalComparison>, state: SpaceCompassSunDailyPathUiState = SpaceCompassSunDailyPathUiState()) =
        SpaceCompassPanoramaSnapshot(now,45.0,9.0,100.0,SpaceCompassSunSkyPhase.AFTERNOON,null,
            listOf(SpaceCompassPanoramaObject(sun,"Sun",calculateSpaceCompassSunDailyPath(
                Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),zone,45.0,9.0,100.0))) +
                comparisons.map { SpaceCompassPanoramaObject(sun,it.id,path(it),it) }, remote,"",emptyList(),emptyList(),
            zoneId=zone.id,selectedPoints=state.selectedPoints,showAntipodes=state.antipodesVisible)
    private fun archive(c: List<SpaceCompassTemporalComparison>, state: SpaceCompassSunDailyPathUiState) =
        SpaceCompassScenarioArchive("Comparison",spaceCompassObserverPlan(45.0,9.0,100.0,now,zone.id)!!,
            SpaceCompassScenarioTimeSettings()).withCelestialObjects(SpaceCompassCelestialSelection(emptySet(),null),
                state.selectedPoints,c,c.firstOrNull()?.id,state.antipodesVisible)

    @Test fun provenanceIsStableAcrossRefinedInstantsAndSeparateAcrossPages() {
        val c=comparison()
        assertEquals(c.id,c.copy(timeMs=c.timeMs+1000).id)
        assertNotEquals(c.id,comparison(source=SpaceCompassComparisonSource.EVENT).id)
        assertThrows(IllegalArgumentException::class.java) { c.copy(id="fake") }
    }
    @Test fun invalidEpochAndZoneCannotBecomeComparisons() {
        assertThrows(Exception::class.java) { comparison().copy(timeMs=Long.MAX_VALUE) }
        assertThrows(Exception::class.java) { comparison().copy(zoneId="Invalid/Zone") }
    }
    @Test fun monthlySolarReferencesHaveTwelveRealTransitsAndCorrectDstOffsets() {
        val result=calculateSpaceCompassObservationMoments(sun,now,zone,45.0,9.0,100.0,remote,includeMonthlySolar=true)!!
        val monthly=result.filter { it.kind==SpaceCompassObservationMomentKind.MONTHLY }
        assertEquals(12,monthly.size)
        assertEquals((1..12).toList(),monthly.map { Instant.ofEpochMilli(it.point.timeMs).atZone(zone).monthValue })
        monthly.forEach { m ->
            assertEquals(21,Instant.ofEpochMilli(m.point.timeMs).atZone(zone).dayOfMonth)
            assertTrue(abs(spaceCompassObservationHourAngle(m.point.position,45.0)!!)<.00002)
        }
        assertEquals(12,Instant.ofEpochMilli(monthly[0].point.timeMs).atZone(zone).hour)
        assertEquals(13,Instant.ofEpochMilli(monthly[5].point.timeMs).atZone(zone).hour)
        assertEquals(4,result.count { it.kind!=SpaceCompassObservationMomentKind.MONTHLY })
    }
    @Test fun monthlyDatesFollowTheObserverZoneAndLeapYear() {
        val z=ZoneId.of("Pacific/Auckland")
        val values=calculateSpaceCompassObservationMoments(sun,Instant.parse("2028-01-01T12:00:00Z").toEpochMilli(),
            z,-36.85,174.76,20.0,remote,includeMonthlySolar=true)!!.filter { it.kind==SpaceCompassObservationMomentKind.MONTHLY }
        assertEquals(12,values.size)
        values.forEach { assertEquals(21,Instant.ofEpochMilli(it.point.timeMs).atZone(z).dayOfMonth) }
        assertEquals(13*3600,Instant.ofEpochMilli(values[0].point.timeMs).atZone(z).offset.totalSeconds)
        assertEquals(12*3600,Instant.ofEpochMilli(values[5].point.timeMs).atZone(z).offset.totalSeconds)
    }
    @Test fun countsBelongToTheirBodyAndSourceAndRemovingSyncsThem() {
        val state=SpaceCompassTemporalComparisonState(SpaceCompassSunDailyPathUiState())
        val c=comparison(); val e=comparison(source=SpaceCompassComparisonSource.EVENT)
        state.toggle(sun,c.timeMs,zone,c.source,c.origin); state.toggle(sun,e.timeMs,zone,e.source,e.origin)
        assertEquals(1,state.count(sun,SpaceCompassComparisonSource.HEIGHT))
        assertEquals(1,state.count(sun,SpaceCompassComparisonSource.EVENT))
        assertEquals(0,state.count(SpaceCompassCelestialBody.MOON,SpaceCompassComparisonSource.HEIGHT))
        state.remove(c.id)
        assertEquals(0,state.count(sun,SpaceCompassComparisonSource.HEIGHT))
        assertEquals(e.id,state.activeId)
    }
    @Test fun removingTheActiveDateReturnsToItsParentWithoutReactingToInactiveDates() {
        val parents = mutableListOf<SpaceCompassCelestialBody>()
        val state = SpaceCompassTemporalComparisonState(SpaceCompassSunDailyPathUiState()) { parents.add(it) }
        val active = comparison()
        val inactive = comparison(source = SpaceCompassComparisonSource.EVENT)
        state.restore(listOf(active, inactive), active.id)
        state.remove(inactive.id)
        assertEquals(active.id, state.activeId)
        assertTrue(parents.isEmpty())
        state.toggle(sun, active.timeMs, zone, active.source, active.origin)
        assertNull(state.activeId)
        assertEquals(listOf(sun), parents)
    }

    @Test fun navigationVisitsEachParentsDatedVariantsBeforeTheNextObject() {
        val moon = SpaceCompassCelestialBody.MOON
        val early = comparison("2026-06-21")
        val late = comparison("2026-12-21", SpaceCompassComparisonSource.EVENT)
        val lunar = SpaceCompassTemporalComparison.create(moon, early.timeMs, zone, early.source, early.origin)
        val personal = SpaceCompassPersonalMoment(sun, "Test", LocalDate.of(2026, 1, 1), null).resolve(zone)
        val dates = listOf(lunar, late, personal, early)
        val order = spaceCompassCatalogDisplayRows(listOf(sun, moon), setOf(sun, moon), dates)
        assertEquals(listOf(sun to null, sun to early.id, sun to late.id, sun to personal.id, moon to null, moon to lunar.id),
            order.map { it.body to it.comparison?.id })
        assertEquals(order.take(4), spaceCompassCatalogDisplayRows(listOf(sun), setOf(sun), dates))
    }

    @Test fun liveAndTwoCopiesOfTheSamePointHaveIndependentSelectionKeys() {
        val c=comparison(); val d=comparison(source=SpaceCompassComparisonSource.EVENT)
        val state=SpaceCompassSunDailyPathUiState()
        state.toggleCurrent(sun,point(c),path(c)); state.toggleCurrent(sun,point(d),path(d))
        state.toggleCurrent(sun,point(c),path(c).copy(comparison=null))
        assertEquals(3,state.selectedPoints.size)
        state.toggleCurrent(sun,point(c),path(c))
        assertEquals(2,state.selectedPoints.size)
        assertTrue(state.isCurrentSelected(sun,point(d),zone,d.id))
        assertTrue(state.isCurrentSelected(sun,point(c),zone))
    }
    @Test fun removingOneCopyKeepsOtherPinsAndAntipodesUntilTheLastPin() {
        val c=comparison(); val d=comparison("2026-12-21")
        val state=SpaceCompassSunDailyPathUiState()
        state.select(path(c),path(c).markers.first()); state.select(path(d),path(d).markers.first())
        state.toggleAntipodes(); state.removeComparison(c.id)
        assertEquals(1,state.selectedPoints.size); assertTrue(state.antipodesVisible)
        state.removeComparison(d.id); assertFalse(state.antipodesVisible)
    }
    @Test fun locationRoundTripKeepsCopiesWhenTheLiveBodyIsUncheckedAndRestoresAntipodes() {
        val c=comparison(); val d=comparison("2026-12-21")
        val state=SpaceCompassSunDailyPathUiState()
        state.selectCurrent(sun,point(c),path(c)); state.select(path(d),path(d).markers.first()); state.toggleAntipodes()
        val original=archive(listOf(c,d),state)
        val restored=SpaceCompassScenarioArchive.decode(original.encode())
        assertEquals(original,restored)
        assertTrue(restored.celestialSelection!!.selected.isEmpty())
        assertEquals(c.id,restored.activeComparisonId); assertEquals(true,restored.antipodesVisible)
        val imported=SpaceCompassSunDailyPathUiState()
        imported.restoreImportedPoints(restored.selectedOrbitPoints); imported.restoreAntipodes(restored.antipodesVisible==true)
        assertTrue(imported.antipodesVisible)
        val saved = SpaceCompassTemporalComparisonState(imported)
        saved.restore(restored.comparisons, restored.activeComparisonId)
        assertTrue(saved.visibleEntries(restored.celestialSelection.selected).isEmpty())
        val hidden = saved.visibleEntries(setOf(SpaceCompassCelestialBody.MOON)).map(::overlay)
        assertTrue(hidden.isEmpty())
        assertNull(hidden.activeSpaceCompassComparison(saved.activeId))
        assertEquals(listOf(c, d), saved.entries)
        assertEquals(2, imported.selectedPoints.size)
        val visibleAgain = saved.visibleEntries(setOf(sun)).map(::overlay)
        assertEquals(listOf(c, d), visibleAgain.map { it.comparison })
        assertEquals(c, visibleAgain.activeSpaceCompassComparison(saved.activeId))
        imported.toggleCurrent(sun,point(c),path(c)); assertEquals(1,imported.selectedPoints.size)
    }
    @Test fun missingSceneExtensionDoesNotInventCopiesOrAntipodeFlags() {
        val old=SpaceCompassScenarioArchive("Old",spaceCompassObserverPlan(45.0,9.0,100.0,now,zone.id)!!,SpaceCompassScenarioTimeSettings())
        val restored=SpaceCompassScenarioArchive.decode(old.encode())
        assertNull(restored.comparisons); assertNull(restored.activeComparisonId); assertNull(restored.antipodesVisible)
    }
    @Test fun orphanedPointAndActiveCopyIdsAreRejected() {
        val c=comparison(); val state=SpaceCompassSunDailyPathUiState(); state.selectCurrent(sun,point(c),path(c))
        val original=archive(listOf(c),state).encode()
        fun changed(key:String,value:String):String {
            val p=Properties().apply { load(StringReader(original)); setProperty(key,value) }
            return StringWriter().also { p.store(it,null) }.toString()
        }
        assertThrows(Exception::class.java) { SpaceCompassScenarioArchive.decode(changed("activeComparison","missing")) }
        assertThrows(Exception::class.java) { SpaceCompassScenarioArchive.decode(changed("comparisonPoint.0","missing|bad")) }
    }
    @Test fun comparisonPersistencePayloadRetainsPinsWithoutCalculatedCurves() {
        val c=comparison(); val state=SpaceCompassSunDailyPathUiState(); state.selectCurrent(sun,point(c),path(c))
        val p=Properties().apply { putSpaceCompassComparisons(listOf(c),state.selectedPoints,c.id,null) }
        assertEquals(listOf(c),p.readSpaceCompassComparisons())
        val points=p.readSpaceCompassComparisonPoints(listOf(c))
        assertEquals(state.selectedPoints.map { it.copy(path=null) },points)
    }
    @Test fun reticleTargetsKeepTheirComparisonIdentityWithoutAliasingLiveTargets() {
        val c=comparison(); val orientation=SpaceCompassSunOrientation(SpaceCompassSunVector(1.0,0.0,0.0),
            SpaceCompassSunVector(0.0,0.0,1.0),SpaceCompassSunVector(0.0,1.0,0.0))
        val projected=projectSpaceCompassComparisonTargets(listOf(overlay(c)),orientation,1000.0,1000.0,null)
        assertTrue(projected.isNotEmpty()); assertTrue(projected.all { it.comparison==c && it.path?.comparison==c })
    }
    @Test fun frozenPointAndItsAntipodeDoNotAdvanceWithTheLiveClock() {
        val c=comparison(); val state=SpaceCompassSunDailyPathUiState(); state.selectCurrent(sun,point(c),path(c));state.toggleAntipodes()
        val a=resolveSpaceCompassPanoramaSelections(snapshot(listOf(c),state),mapOf(sun to SpaceCompassSunPosition(0.0,0.0)))
        val b=resolveSpaceCompassPanoramaSelections(snapshot(listOf(c),state).copy(timeMs=now+86_400_000),mapOf(sun to SpaceCompassSunPosition(180.0,-20.0)))
        assertEquals(a,b);assertEquals(2,a.size)
        assertEquals(c.timeMs,a.first().point.timeMs)
        assertEquals(-a.first().point.position.elevationDegrees,a.last().point.position.elevationDegrees,1e-9)
    }
    @Test fun captureTablesContainSeparateColumnsAndExactInstantsForThreeSuns() {
        val c=comparison();val d=comparison("2026-12-21")
        val snap=snapshot(listOf(c,d));val table=captureSpaceCompassOrbitTable(snap)
        assertEquals(listOf(sun,sun,sun),table.bodies)
        val current=table.rows.filter { it.kind==SpaceCompassOrbitExportRowKind.CURRENT }
        assertEquals(setOf(now,c.timeMs,d.timeMs),current.flatMap { it.allCells }.map { it.point.timeMs }.toSet())
        assertEquals(setOf(c.id,d.id),current.flatMap { it.comparisonCells.keys }.toSet())
        val grid=spaceCompassOrbitExportGrid(table,snap,now,"")
        assertEquals(22,grid.first().size)
        assertTrue(grid.first().any { it.contains(c.id) });assertTrue(grid.first().any { it.contains(d.id) })
    }
    @Test fun simultaneousCopiesDoNotOverwriteEachOtherInTheSameExportRow() {
        val c=comparison();val d=comparison(source=SpaceCompassComparisonSource.EVENT)
        val table=captureSpaceCompassOrbitTable(snapshot(listOf(c,d)))
        val current=table.rows.single { it.kind==SpaceCompassOrbitExportRowKind.CURRENT && it.comparisonCells.isNotEmpty() }
        assertEquals(2,current.comparisonCells.size)
    }
    @Test fun capturedRowsRemainChronologicalIncludingComparisonAntipodes() {
        val c=comparison();val state=SpaceCompassSunDailyPathUiState();state.selectCurrent(sun,point(c),path(c));state.toggleAntipodes()
        val table=captureSpaceCompassOrbitTable(snapshot(listOf(c),state))
        val times=table.rows.mapNotNull { it.allCells.firstOrNull()?.point?.timeMs }
        assertEquals(times.sorted(),times)
        assertTrue(table.rows.any { it.kind==SpaceCompassOrbitExportRowKind.ANTIPODE && it.comparisonCells[c.id]?.selected==true })
    }
    @Test fun dailyPathExportsOnlyItsOwnInstanceAndExcludesDerivedAntipodes() {
        val c=comparison();val d=comparison(source=SpaceCompassComparisonSource.EVENT)
        val state=SpaceCompassSunDailyPathUiState();state.selectCurrent(sun,point(c),path(c));state.selectCurrent(sun,point(d),path(d));state.toggleAntipodes()
        val snap=spaceCompassSingleOrbitExportSnapshot(path(c),point(c),state.selectedPoints,c.timeMs)
        assertEquals(listOf(c.id),snap.selectedPoints.map { it.key.comparisonId });assertFalse(snap.showAntipodes)
        assertEquals(c, snap.objects.single().comparison)
    }
    @Test fun comparingAnotherObserverRecomputesGeometryWithoutChangingTheFrozenEpoch() {
        val c=comparison()
        val north=calculateSpaceCompassSunPosition(c.timeMs,45.0,9.0,100.0)
        val south=calculateSpaceCompassSunPosition(c.timeMs,-35.0,18.0,100.0)
        assertTrue(abs(north.elevationDegrees-south.elevationDegrees)>20)
        assertEquals(c.timeMs,comparison().timeMs)
        assertFalse(spaceCompassPathAcceptsSelection(path(c).copy(comparison=null),
            SpaceCompassSelectedOrbitPoint(spaceCompassOrbitPointKey(sun,point(c),zone,current=true,comparisonId=c.id),point(c),path(c))))
    }
    @Test fun aCurrentPinWithADifferentFrozenEpochCannotEnterALocationFile() {
        val c=comparison();val state=SpaceCompassSunDailyPathUiState()
        state.selectCurrent(sun,point(c).copy(timeMs=c.timeMs+60_000),path(c))
        assertThrows(Exception::class.java) { archive(listOf(c),state).encode() }
    }
    @Test fun hiddenPositionKeepsEpochsButFormatsComparisonNamesAndTimesInUtc() {
        val c=comparison();val snap=snapshot(listOf(c))
        val hidden=spaceCompassSnapshotForDisclosure(snap,SpaceCompassPanoramaPosition.HIDDEN)
        assertTrue(hidden.positionHidden)
        assertEquals("UTC",hidden.comparisonDisplayZoneId(c))
        assertEquals(c,hidden.objects.last().comparison)
        assertTrue(hidden.objects.last().name.contains("UTC+00:00"))
        assertEquals(c.timeMs,hidden.objects.last().referenceTime(hidden.timeMs))
    }

    @Test fun currentComparisonCaptionsUseTheirDisplayZoneRatherThanAnImplicitUtcClock() {
        val c=comparison()
        val snap=snapshot(listOf(c)).copy(timeMs=c.timeMs,zoneId=c.zoneId,displayZoneId=c.zoneId,currentTime=null)
        val label=spaceCompassPanoramaCurrentLabel(snap,point(c).position,SpaceCompassSunScenePoint(0.0,0.0),0,38f)
        val local=formatSpaceCompassPanoramaExportTime(c.timeMs,java.util.TimeZone.getTimeZone(c.zone),snap.formatting)
        val utc=formatSpaceCompassPanoramaExportTime(c.timeMs,java.util.TimeZone.getTimeZone("UTC"),snap.formatting)
        assertTrue(label.text.contains(local));assertFalse(label.text.contains(utc))
    }

}

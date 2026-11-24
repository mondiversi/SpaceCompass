package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassLogicalPointSelectionTest {
    private val date=LocalDate.parse("2026-11-24");private val zone=ZoneId.of("UTC")
    private fun point(day: LocalDate,hour: Int,event: SpaceCompassSunPathEvent=SpaceCompassSunPathEvent.HOUR)=
        SpaceCompassSunPathPoint(day.atTime(hour,0).atZone(zone).toInstant().toEpochMilli(),SpaceCompassSunPosition(hour*10.0,day.dayOfMonth.toDouble()),event)
    private fun path(day: LocalDate=date,body: SpaceCompassCelestialBody=SpaceCompassCelestialBody.SUN,
        markers: List<SpaceCompassSunPathPoint> = listOf(point(day,0),point(day,6,SpaceCompassSunPathEvent.SUNRISE),point(day,12)))=
        SpaceCompassSunDailyPath(day,zone,listOf(point(day,0),point(day.plusDays(1),0)),markers,body)

    @Test fun pointZeroTracksTheRefreshedSatelliteWindowAndKeepsItsNumber() {
        val a=path(body=SpaceCompassCelestialBody.ISS);val state=SpaceCompassSunDailyPathUiState();state.select(a,a.markers.first())
        val next=path(body=a.body,markers=listOf(point(date,12),point(date,13)))
        assertTrue(state.isSelected(next,next.markers.first()))
        assertEquals(next.markers.first(),spaceCompassResolveOrbitSelection(state.selectedPoints.single(),next)!!.point)
    }
    @Test fun recurringEventsRetainTheirOccurrenceAndDoNotAliasOtherPasses() {
        val a=path(body=SpaceCompassCelestialBody.ISS,markers=listOf(point(date,5,SpaceCompassSunPathEvent.CULMINATION),point(date,7,SpaceCompassSunPathEvent.CULMINATION)))
        val state=SpaceCompassSunDailyPathUiState();state.select(a,a.markers[1])
        val d=date.plusDays(1);val b=path(d,a.body,listOf(point(d,4,SpaceCompassSunPathEvent.CULMINATION),point(d,9,SpaceCompassSunPathEvent.CULMINATION)))
        assertFalse(state.isSelected(b,b.markers[0]));assertTrue(state.isSelected(b,b.markers[1]))
    }
    @Test fun aCoincidentEventRetainsItsOriginalRoleAndCanBeDeselectedOnTheMergedMarker() {
        val a=path(markers=listOf(point(date,6,SpaceCompassSunPathEvent.CULMINATION)))
        val state=SpaceCompassSunDailyPathUiState();state.select(a,a.markers.single())
        val d=date.plusDays(1);val merged=point(d,6,SpaceCompassSunPathEvent.SUNRISE).copy(coincidentEvents=setOf(SpaceCompassSunPathEvent.CULMINATION))
        val b=path(d,markers=listOf(merged))
        assertTrue(state.isSelected(b,merged));state.togglePoint(b,merged);assertTrue(state.selectedPoints.isEmpty())
    }
    @Test fun dailyListAndGlobeUseTheNewGeometryWithoutDuplicateHistoricalDots() {
        val a=path();val state=SpaceCompassSunDailyPathUiState();state.select(a,a.markers.first());state.toggleAntipodes()
        val b=path(date.plusDays(1));val snapshot=SpaceCompassPanoramaSnapshot(b.samples.first().timeMs,45.0,9.0,0.0,
            SpaceCompassSunSkyPhase.MORNING,null,listOf(SpaceCompassPanoramaObject(b.body,"Sun",b)),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList(),
            selectedPoints=state.selectedPoints,showAntipodes=true)
        assertEquals(b.markers.size,spaceCompassSelectedDailyPathEntries(b,null,state.selectedPoints).size)
        val track=buildSpaceCompassGlobeModel(snapshot,sky=true).tracks.single()
        assertEquals(b.markers.first(),track.dots.single { it.selected && !it.antipode && !it.current }.point)
        assertEquals(b.markers.first().antipodalDirection(),track.dots.single { it.antipode && !it.current }.point)
    }
    @Test fun sceneRefreshUpdatesPortableDateCoordinatesAndLeavesMissingEventsStored() {
        val a=path();val state=SpaceCompassSunDailyPathUiState();state.select(a,a.markers.first())
        val b=path(date.plusDays(30));state.refreshObservationDate(b.date)
        state.refreshLivePaths(mapOf(b.body to SpaceCompassCelestialOverlay(b.body,null,null,null,b)),zone)
        val updated=state.selectedPoints.single();assertEquals(b.markers.first(),updated.point);assertEquals(b.date,updated.key.date)
        spaceCompassCheckLocationPoint(updated)
    }
    @Test fun archiveIdentityExtensionRejectsInvalidNumbersAndKeepsLegacyPayloadReadable() {
        val p=path();val key=spaceCompassOrbitPointKey(p,p.markers.first())
        spaceCompassCheckLocationPoint(SpaceCompassSelectedOrbitPoint(key,p.markers.first(),p))
        assertThrows(IllegalArgumentException::class.java) {
            spaceCompassCheckLocationPoint(SpaceCompassSelectedOrbitPoint(key.copy(daily=key.daily!!.copy(number=-1)),p.markers.first(),p))
        }
        val legacy=SpaceCompassSelectedOrbitPoint(key.copy(daily=null),p.markers.first(),null)
        assertEquals(path(date.plusDays(1)).markers.first(),spaceCompassResolveOrbitSelection(legacy,path(date.plusDays(1)))!!.point)
    }
    @Test fun realSolarPointZeroUsesEachDatesCalculatedPositionAtTheEffectiveLocation() {
        val z=ZoneId.of("Europe/Rome")
        val first=calculateSpaceCompassSunDailyPath(LocalDate.of(2026,3,21),z,45.0,9.0,125.0)
        val zero=first.markers.first { it.event==SpaceCompassSunPathEvent.HOUR && java.time.Instant.ofEpochMilli(it.timeMs).atZone(z).hour==0 }
        val state=SpaceCompassSunDailyPathUiState();state.select(first,zero);state.toggleAntipodes()
        for(day in listOf(LocalDate.of(2026,6,21),LocalDate.of(2025,12,21),LocalDate.of(2027,1,1))) {
            val next=calculateSpaceCompassSunDailyPath(day,z,45.0,9.0,125.0)
            val resolved=spaceCompassResolveOrbitSelection(state.selectedPoints.single(),next)!!
            assertEquals(day,resolved.key.date);assertEquals(0,resolved.key.daily!!.number)
            assertEquals(calculateSpaceCompassSunPosition(resolved.point.timeMs,45.0,9.0,125.0),resolved.point.position)
            assertTrue(state.antipodesVisible)
        }
    }
    @Test fun rebindingACoincidentEventRetainsItsRoleThroughPortableSceneMetadata() {
        val first=path(markers=listOf(point(date,6,SpaceCompassSunPathEvent.CULMINATION)))
        val state=SpaceCompassSunDailyPathUiState();state.select(first,first.markers.single())
        val day=date.plusDays(1);val merged=point(day,6,SpaceCompassSunPathEvent.SUNRISE).copy(coincidentEvents=setOf(SpaceCompassSunPathEvent.CULMINATION))
        val second=path(day,markers=listOf(merged));state.refreshObservationDate(day)
        state.refreshLivePaths(mapOf(second.body to SpaceCompassCelestialOverlay(second.body,null,null,null,second)),zone)
        val rebound=state.selectedPoints.single();spaceCompassCheckLocationPoint(rebound)
        assertEquals(SpaceCompassSunPathEvent.CULMINATION,rebound.key.daily!!.event)
        val properties=java.util.Properties().apply { putSpaceCompassLocationPoints(listOf(rebound),SpaceCompassCelestialSelection(setOf(second.body),second.body)) }
        val decoded=properties.readSpaceCompassLocationPoints(SpaceCompassCelestialSelection(setOf(second.body),second.body))!!.single()
        val next=path(day.plusDays(1),markers=listOf(point(day.plusDays(1),13,SpaceCompassSunPathEvent.CULMINATION)))
        assertEquals(next.markers.single(),spaceCompassResolveOrbitSelection(decoded,next)!!.point)
    }
    @Test fun currentSelectionRebasesItsPortableDayWithoutSwitchingOffAntipodes() {
        val first=path();val state=SpaceCompassSunDailyPathUiState();state.selectCurrent(first.body,first.markers.first(),first);state.toggleAntipodes()
        val second=path(date.plusDays(10));val now=second.markers.last().timeMs
        val observation=calculateSpaceCompassCelestialObservation(second.body,now,45.0,9.0,0.0,SpaceCompassCelestialRemoteData())!!
        state.refreshObservationDate(second.date)
        state.refreshLivePaths(mapOf(second.body to SpaceCompassCelestialOverlay(second.body,observation,null,null,second,now)),zone)
        val saved=state.selectedPoints.single();assertTrue(saved.key.isCurrent)
        assertEquals(second.date,saved.key.date);assertEquals(now,saved.point.timeMs)
        assertEquals(observation.position,saved.point.position);spaceCompassCheckLocationPoint(saved)
        assertTrue(state.antipodesVisible)
    }
}

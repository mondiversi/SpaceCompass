package me.mondiversi.spacecompass

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassDailySelectionRolloverTest {
    private val zone=ZoneId.of("UTC")
    private val date=LocalDate.parse("2026-11-07")
    private fun path(day: LocalDate=date,body: SpaceCompassCelestialBody=SpaceCompassCelestialBody.MOON,
        z: ZoneId=zone,forward: Boolean=false): SpaceCompassSunDailyPath {
        fun point(hour: Int,event: SpaceCompassSunPathEvent=SpaceCompassSunPathEvent.HOUR)=SpaceCompassSunPathPoint(
            day.atStartOfDay(z).plusHours(hour.toLong()).toInstant().toEpochMilli(),
            SpaceCompassSunPosition((hour*13.0+day.dayOfYear)%360,20.0+hour/10.0),event)
        return SpaceCompassSunDailyPath(day,z,listOf(point(0),point(24)),listOf(point(0),point(6,SpaceCompassSunPathEvent.SUNRISE),
            point(12),point(13,SpaceCompassSunPathEvent.CULMINATION),point(18,SpaceCompassSunPathEvent.SUNSET)),body,forward24Hours=forward)
    }
    private fun snapshot(path: SpaceCompassSunDailyPath,state: SpaceCompassSunDailyPathUiState)=SpaceCompassPanoramaSnapshot(
        path.samples.first().timeMs,45.0,9.0,0.0,SpaceCompassSunSkyPhase.MORNING,null,
        listOf(SpaceCompassPanoramaObject(path.body,"Object",path)),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList(),
        selectedPoints=state.selectedPoints,showAntipodes=state.antipodesVisible,zoneId=path.zone.id)

    @Test fun forwardAndRewindKeepNumbersEventsCurrentAndAntipodesWithoutFeedback() {
        var feedback=0;val state=SpaceCompassSunDailyPathUiState { feedback++ };val initial=path()
        state.refreshObservationDate(date)
        state.select(initial,initial.markers.first());state.select(initial,initial.markers[1]);state.selectCurrent(initial.body,initial.markers[2],initial)
        state.toggleAntipodes();val before=feedback
        for(offset in listOf(1L,30L,-90L,0L,365L)) {
            val p=path(date.plusDays(offset));state.refreshObservationDate(p.date)
            assertEquals(3,state.selectedPoints.size);assertTrue(state.antipodesVisible)
            assertTrue(state.isSelected(p,p.markers.first()));assertTrue(state.isSelected(p,p.markers[1]))
            assertTrue(state.isCurrentSelected(p.body,p.markers[2],p.zone))
            val resolved=resolveSpaceCompassPanoramaSelections(snapshot(p,state),mapOf(p.body to p.markers[2].position))
            assertEquals(6,resolved.size)
            for(original in resolved.filterNot { it.isAntipode }) {
                val opposite=resolved.single { it.isAntipode && it.point.timeMs==original.point.timeMs && it.isCurrent==original.isCurrent }
                assertEquals(spaceCompassAntipode(original.point.position),opposite.point.position)
            }
        }
        assertEquals(before,feedback)
    }

    @Test fun fixedHeightEventAndPersonalPinsKeepTheirOwnInstants() {
        val state=SpaceCompassSunDailyPathUiState();val p=path()
        for(source in SpaceCompassComparisonSource.entries) {
            val c=SpaceCompassTemporalComparison.create(p.body,p.markers[2].timeMs,zone,source,"fixed-${source.name}",
                personalName="Personal".takeIf { source==SpaceCompassComparisonSource.PERSONAL },
                personalDate=date.takeIf { source==SpaceCompassComparisonSource.PERSONAL })
            state.select(p.copy(comparison=c),p.markers.first());state.selectCurrent(p.body,p.markers[2],p.copy(comparison=c))
        }
        val saved=state.selectedPoints.toSet();state.toggleAntipodes();state.refreshObservationDate(date.plusDays(500))
        assertEquals(saved,state.selectedPoints.toSet());assertTrue(state.antipodesVisible)
        assertTrue(saved.all { it.key.daily==null })
    }

    @Test fun missingEventsAreDormantAndReappearWithoutStaleGeometry() {
        val state=SpaceCompassSunDailyPathUiState();val p=path();state.select(p,p.markers[1]);state.toggleAntipodes()
        val missing=path(date.plusDays(1)).let { it.copy(markers=it.markers.filterNot { m -> m.event==SpaceCompassSunPathEvent.SUNRISE }) }
        state.refreshObservationDate(missing.date)
        assertEquals(1,state.selectedPoints.size);assertTrue(state.antipodesVisible)
        assertTrue(resolveSpaceCompassPanoramaSelections(snapshot(missing,state),emptyMap()).isEmpty())
        assertEquals(2,resolveSpaceCompassPanoramaSelections(snapshot(path(date.plusDays(2)),state),emptyMap()).size)
    }

    @Test fun forward24HoursKeepsTheSameClockNumberWhenItsOriginalTimestampExpires() {
        val state=SpaceCompassSunDailyPathUiState();val first=path(forward=true)
        state.select(first,first.markers.first());state.toggleAntipodes()
        val next=path(date.plusDays(1),forward=true);state.refreshObservationDate(next.date)
        val resolved=resolveSpaceCompassPanoramaSelections(snapshot(next,state),emptyMap())
        assertEquals(next.markers.first(),resolved.single { !it.isAntipode }.point)
        assertTrue(state.isSelected(next,next.markers.first()))
        state.togglePoint(next,next.markers.first());assertTrue(state.selectedPoints.isEmpty());assertFalse(state.antipodesVisible)
    }

    @Test fun locationZoneChangesAndRepeatedDstHoursRemainDistinct() {
        val z=ZoneId.of("Europe/Rome");val d=LocalDate.parse("2026-10-25");val start=d.atStartOfDay(z)
        val two=SpaceCompassSunPathPoint(start.plusHours(2).toInstant().toEpochMilli(),SpaceCompassSunPosition(10.0,20.0))
        val repeat=two.copy(timeMs=start.plusHours(3).toInstant().toEpochMilli())
        val p=SpaceCompassSunDailyPath(d,z,listOf(two,repeat),listOf(two,repeat),SpaceCompassCelestialBody.SUN)
        val state=SpaceCompassSunDailyPathUiState();state.select(p,two);state.select(p,repeat)
        assertEquals(2,state.selectedPoints.size);assertEquals(setOf(0,1),state.selectedPoints.map { it.key.daily!!.occurrence }.toSet())
        val normal=path(date,SpaceCompassCelestialBody.SUN,ZoneId.of("America/Los_Angeles"))
        state.refreshObservationDate(normal.date);assertEquals(2,state.selectedPoints.size)
        assertTrue(state.isSelected(p,two));assertTrue(state.isSelected(p,repeat))
    }

    @Test fun importedLogicalPinsDoNotDependOnAnInMemoryPath() {
        val source=SpaceCompassSunDailyPathUiState();val p=path(body=SpaceCompassCelestialBody.ISS)
        source.select(p,p.markers[2]);source.toggleAntipodes()
        val contents=SpaceCompassScenarioArchive("Pins",spaceCompassObserverPlan(45.0,9.0,0.0,p.samples.first().timeMs,zone.id)!!,
            SpaceCompassScenarioTimeSettings()).withCelestialObjects(SpaceCompassCelestialSelection(setOf(p.body),p.body),source.selectedPoints)
        val decoded=SpaceCompassScenarioArchive.decode(contents.encode())
        assertTrue(decoded.selectedOrbitPoints!!.all { it.path==null })
        val receiver=SpaceCompassSunDailyPathUiState();receiver.restoreImportedPoints(decoded.selectedOrbitPoints);receiver.restoreAntipodes(true)
        val next=path(date.plusDays(20),p.body);receiver.refreshObservationDate(next.date)
        assertTrue(receiver.isSelected(next,next.markers[2]));assertTrue(receiver.antipodesVisible)
        assertEquals(1,receiver.selectedPoints.single().key.daily!!.number)
    }
}

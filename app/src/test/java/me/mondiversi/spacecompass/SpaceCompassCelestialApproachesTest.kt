package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SpaceCompassCelestialApproachesTest {
    private fun position(az: Double,el: Double=20.0)=SpaceCompassSunPosition(az,el)
    @Test fun pairedGlyphsDeduplicateOnlyTheSameEventIncludingThePanoramaSeam() {
        val occupied = listOf(1000L to SpaceCompassSunScenePoint(2.0, 40.0))
        assertTrue(spaceCompassApproachMarkerIsDuplicate(1000, SpaceCompassSunScenePoint(4.0, 40.0), occupied, 18.0))
        assertFalse(spaceCompassApproachMarkerIsDuplicate(2000, SpaceCompassSunScenePoint(4.0, 40.0), occupied, 18.0))
        assertFalse(spaceCompassApproachMarkerIsDuplicate(1000, SpaceCompassSunScenePoint(40.0, 40.0), occupied, 18.0))
        assertTrue(spaceCompassApproachMarkerIsDuplicate(1000, SpaceCompassSunScenePoint(998.0, 40.0), occupied, 18.0, 1000.0))
    }
    @Test fun angularDistanceHandlesWrapZenithAndBadInput() {
        assertEquals(2.0,spaceCompassAngularSeparation(position(359.0,0.0),position(1.0,0.0)),1e-9)
        assertEquals(0.0,spaceCompassAngularSeparation(position(0.0,90.0),position(180.0,90.0)),1e-9)
        assertTrue(spaceCompassAngularSeparation(position(Double.NaN),position(0.0)).isNaN())
    }
    @Test fun crossingCurvesAtDifferentTimesNeverCreateApproach() {
        // Both traverse the same azimuth arc; the second is consistently ten degrees behind.
        val events=findSpaceCompassApproachMinima(0,60_000,1000,{ t ->
            position(t/1000.0,0.0) to position(t/1000.0+10,0.0) })
        assertTrue(events.isEmpty())
    }
    @Test fun simultaneousCrossingIsRefinedBetweenSamples() {
        val events=findSpaceCompassApproachMinima(0,60_000,5000,{ t ->
            position((t-27_300)/1000.0+100,0.0) to position(100.0,0.0) })
        assertEquals(1,events.size)
        assertTrue(kotlin.math.abs(events.single().first-27_300)<100)
        assertTrue(events.single().second<.1)
    }
    @Test fun thresholdIsAngularAndMissingExactDataNeverInventsAnEvent() {
        assertTrue(findSpaceCompassApproachMinima(0,60_000,1000,{ position(0.0,0.0) to position(1.51,0.0) }).isEmpty())
        assertEquals(1,findSpaceCompassApproachMinima(0,60_000,1000,{ position(0.0,0.0) to position(1.49,0.0) }).size)
        assertTrue(findSpaceCompassApproachMinima(0,60_000,1000,
            { position(0.0) to position(0.0) },exact={ null }).isEmpty())
    }
    @Test fun multipleClosePassesRemainSeparateAndCancellationIsHonored() {
        val events=findSpaceCompassApproachMinima(0,120_000,1000,{ t ->
            position(100.0+5*kotlin.math.cos(t/10_000.0),0.0) to position(100.0,0.0) })
        assertTrue(events.size>=3)
        try { findSpaceCompassApproachMinima(0,1000,100,{ null },checkCancelled={ throw IllegalStateException("cancel") }); fail() }
        catch (e: IllegalStateException) { assertEquals("cancel",e.message) }
    }
    @Test fun eventStoresCounterpartAndUsesExactSameTimeForBothPaths() {
        val zone=ZoneId.of("UTC"); val date=java.time.LocalDate.of(2026,10,16)
        fun p(t: Long,az: Double)=SpaceCompassSunPathPoint(t,position(az,0.0))
        val sun=SpaceCompassSunDailyPath(date,zone,listOf(p(0,90.0),p(30_000,100.0),p(60_000,110.0)),emptyList())
        val moon=sun.copy(body=SpaceCompassCelestialBody.MOON,samples=listOf(p(0,100.0),p(30_000,100.0),p(60_000,100.0)))
        val paths=spaceCompassApproachPaths(mapOf(sun.body to sun,moon.body to moon),{ body,t ->
            position(if(body==sun.body) 90+t/3000.0 else 100.0,0.0) })
        val a=paths.getValue(sun.body).markers.single(); val b=paths.getValue(moon.body).markers.single()
        assertEquals(a.timeMs,b.timeMs)
        assertEquals(SpaceCompassSunPathEvent.APPROACH,a.event)
        assertEquals(moon.body,a.approach!!.other)
        assertEquals(sun.body,b.approach!!.other)
        assertTrue(a.approach!!.separationDegrees<.1)
        val names=SpaceCompassSunPathPointNames(listOf(a),"rise","high","set","low","now",
            approach={ "approach ${it.approach!!.other}" }) { "hour $it" }
        assertEquals("approach MOON",names.name(a))
    }
    @Test fun pinsForDifferentCounterpartsDoNotCollapseIntoOne() {
        val a=SpaceCompassSunPathPoint(1_000,position(0.0),SpaceCompassSunPathEvent.APPROACH,
            approach=SpaceCompassApproach(SpaceCompassCelestialBody.MOON,.1))
        val b=a.copy(approach=SpaceCompassApproach(SpaceCompassCelestialBody.VENUS,.1))
        assertFalse(spaceCompassOrbitPointKeysMatch(spaceCompassOrbitPointKey(SpaceCompassCelestialBody.SUN,a,ZoneId.of("UTC")),
            spaceCompassOrbitPointKey(SpaceCompassCelestialBody.SUN,b,ZoneId.of("UTC"))))
    }
    @Test fun realSolarLunarApparentDirectionsCanProduceCurrentApproach() {
        val t=Instant.parse("2024-04-08T18:40:00Z").toEpochMilli()
        val positions=listOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON).associateWith {
            calculateSpaceCompassCelestialObservation(it,t,32.78,-96.8)!!.position }
        val event=spaceCompassCurrentApproaches(t,positions).single()
        assertEquals(t,event.timeMs)
        assertTrue(event.separationDegrees<.5)
    }
    @Test fun chronologicalCsvAndTextCarryCounterpartAndAngularDistance() {
        val t=Instant.parse("2026-10-16T12:30:00Z").toEpochMilli()
        val marker=SpaceCompassSunPathPoint(t,position(10.0),SpaceCompassSunPathEvent.APPROACH,
            approach=SpaceCompassApproach(SpaceCompassCelestialBody.MOON,.25))
        val path=SpaceCompassSunDailyPath(java.time.LocalDate.of(2026,10,16),ZoneId.of("UTC"),
            listOf(marker.copy(timeMs=t-3600_000),marker.copy(timeMs=t+3600_000)),listOf(marker))
        val snapshot=spaceCompassSingleOrbitExportSnapshot(path,null,emptyList(),t).copy(bodyNames=mapOf(SpaceCompassCelestialBody.MOON to "Moon"))
        val table=spaceCompassSingleOrbitExportTable(snapshot,path,null)
        val csv=renderSpaceCompassOrbitCsv(table,snapshot,t,"")
        val txt=renderSpaceCompassOrbitTxt(table,snapshot,t,"")
        assertTrue(csv.contains("Close approach · Moon · 0.3°"))
        assertTrue(txt.contains("Close approach · Moon · 0.3°"))
        assertTrue(formatSpaceCompassPanoramaPathPointLabel("12:30",marker,snapshot.formatting,
            snapshot.pointEventNames,t,snapshot.bodyNames).contains("Moon"))
    }
}

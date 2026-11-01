package me.mondiversi.spacecompass

import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassKeyEventsTest {
    private companion object {
        val rome = ZoneId.of("Europe/Rome")
        val north = SpaceCompassObservationPlace(45.0, 9.0, 100.0)
        val sunCalendar = SpaceCompassObservationCalendar(SpaceCompassCelestialBody.SUN,2026,rome)
        val solar by lazy { calculateSpaceCompassKeyEvents(sunCalendar,north)!! }
        val moonCalendar = SpaceCompassObservationCalendar(SpaceCompassCelestialBody.MOON,2026,rome)
        val lunar by lazy { calculateSpaceCompassKeyEvents(moonCalendar,north)!! }
    }

    @Test fun solarExtremesAreLocalChronologicalAndDoNotInventPolarPeriodsInItaly() {
        assertEquals(6,solar.size)
        assertTrue(solar.zipWithNext().all { (a,b) -> a.timeMs < b.timeMs })
        val longest=solar.single { it.kind==SpaceCompassKeyEventKind.LONGEST_DAY }
        val shortest=solar.single { it.kind==SpaceCompassKeyEventKind.SHORTEST_DAY }
        assertEquals(6,Instant.ofEpochMilli(longest.timeMs).atZone(rome).monthValue)
        assertEquals(12,Instant.ofEpochMilli(shortest.timeMs).atZone(rome).monthValue)
        assertTrue(longest.durationMs!!/3_600_000.0 in 15.0..16.5)
        assertTrue(shortest.durationMs!!/3_600_000.0 in 8.0..9.5)
        // Root brackets are 250 ms; at these latitudes that is below 0.001 degrees.
        solar.filter { it.durationMs==null }.forEach { assertEquals(-.833,it.elevation,.001) }
    }

    @Test fun polarPeriodsHaveActualHorizonEndpointsAndSpanNewYearWithoutArbitraryLongestDay() {
        val calendar=sunCalendar.copy(zone=ZoneId.of("Europe/Oslo"))
        val events=calculateSpaceCompassKeyEvents(calendar,SpaceCompassObservationPlace(69.6492,18.9553,0.0))!!
        val polar=events.filter { it.kind in setOf(SpaceCompassKeyEventKind.POLAR_DAY_START,
            SpaceCompassKeyEventKind.POLAR_DAY_END,SpaceCompassKeyEventKind.POLAR_NIGHT_START,SpaceCompassKeyEventKind.POLAR_NIGHT_END) }
        assertEquals(4,polar.size)
        assertTrue(polar.all { it.durationMs!! > 30*86_400_000L })
        assertTrue(polar.all { kotlin.math.abs(it.elevation+.833)<.0001 })
        assertEquals(1,Instant.ofEpochMilli(polar.single { it.kind==SpaceCompassKeyEventKind.POLAR_NIGHT_END }.timeMs).atZone(calendar.zone).monthValue)
        assertEquals(5,Instant.ofEpochMilli(polar.single { it.kind==SpaceCompassKeyEventKind.POLAR_DAY_START }.timeMs).atZone(calendar.zone).monthValue)
        assertFalse(events.any { it.kind==SpaceCompassKeyEventKind.LONGEST_DAY || it.kind==SpaceCompassKeyEventKind.SHORTEST_DAY })
    }

    @Test fun southernPolarSeasonsAreReversedAndExactPolesHaveNoDuplicateClockExtremes() {
        val south=calculateSpaceCompassKeyEvents(sunCalendar,SpaceCompassObservationPlace(-69.6492,18.9553,1000.0))!!
        assertEquals(11,Instant.ofEpochMilli(south.single { it.kind==SpaceCompassKeyEventKind.POLAR_DAY_START }.timeMs).atZone(rome).monthValue)
        val pole=calculateSpaceCompassKeyEvents(sunCalendar,SpaceCompassObservationPlace(90.0,0.0,0.0))!!
        assertEquals(4,pole.size)
    }

    @Test fun equatorialAndHighAltitudeEventsUseTheAcceptedObserver() {
        val place=SpaceCompassObservationPlace(0.0, -78.0, 4000.0,true,true)
        val events=calculateSpaceCompassKeyEvents(sunCalendar,place)!!
        assertTrue(events.none { it.kind.name.startsWith("POLAR_") })
        events.forEach { assertEquals(calculateSpaceCompassSunPosition(it.timeMs,place.latitude,place.longitude,
            place.altitude).elevationDegrees,it.elevation,0.0) }
        assertNotEquals(solar,events)
    }

    @Test fun lunarPhasesAndApsidesShareUniversalInstantsButUseEachLocationsElevation() {
        val other=calculateSpaceCompassKeyEvents(moonCalendar,SpaceCompassObservationPlace(-33.9,151.2,1500.0))!!
        assertTrue(lunar.size in 74..82)
        assertEquals(lunar.map { it.kind to it.timeMs },other.map { it.kind to it.timeMs })
        assertTrue(lunar.zip(other).any { (a,b) -> kotlin.math.abs(a.elevation-b.elevation)>20 })
        assertTrue(lunar.filter { it.kind==SpaceCompassKeyEventKind.PERIGEE }.all { it.distanceKm!! in 350_000.0..380_000.0 })
        assertTrue(lunar.filter { it.kind==SpaceCompassKeyEventKind.APOGEE }.all { it.distanceKm!! in 400_000.0..410_000.0 })
    }

    @Test fun planetaryEventsBelongToTheInspectedPlanetAndIncludeRealElongationsOrOppositions() {
        val mercury=calculateSpaceCompassKeyEvents(sunCalendar.copy(body=SpaceCompassCelestialBody.MERCURY),north)!!
        assertTrue(mercury.count { it.angle!=null }>=5)
        assertTrue(mercury.filter { it.angle!=null }.all { it.angle!! in 17.0..29.0 })
        assertFalse(mercury.any { it.kind==SpaceCompassKeyEventKind.OPPOSITION })
        val mars=calculateSpaceCompassKeyEvents(sunCalendar.copy(body=SpaceCompassCelestialBody.MARS,year=2025),north)!!
        val opposition=mars.single { it.kind==SpaceCompassKeyEventKind.OPPOSITION }
        val date=Instant.ofEpochMilli(opposition.timeMs).atZone(rome)
        assertEquals(1,date.monthValue);assertTrue(date.dayOfMonth in 14..18)
        assertEquals(2,mars.count { it.kind==SpaceCompassKeyEventKind.NEAREST_EARTH || it.kind==SpaceCompassKeyEventKind.FARTHEST_EARTH })
        assertTrue(mars.all { it.elevation.isFinite() })
    }

    @Test fun yearAndZoneBoundsHandleLeapYearsAndEveryReturnedInstant() {
        val calendar=moonCalendar.copy(year=2024,zone=ZoneId.of("Pacific/Kiritimati"))
        val bounds=spaceCompassKeyEventYearBounds(calendar)
        assertEquals(366*86_400_000L,bounds.last-bounds.first+1)
        val events=calculateSpaceCompassKeyEvents(calendar,north)!!
        assertTrue(events.all { Instant.ofEpochMilli(it.timeMs).atZone(calendar.zone).year==2024 })
        assertNotEquals(events.map { it.timeMs },lunar.map { it.timeMs })
    }

    @Test fun staleSatelliteOrShortRemoteDataCannotFabricateAnAnnualCalendar() {
        assertNull(calculateSpaceCompassKeyEvents(sunCalendar.copy(body=SpaceCompassCelestialBody.ISS),north))
        val body=SpaceCompassCelestialBody.SEDNA
        val time=spaceCompassKeyEventYearBounds(sunCalendar).first
        val ephemeris=SpaceCompassHorizonsEphemeris(body,listOf(SpaceCompassHorizonsSample(time,80.0,1.0,0.0),
            SpaceCompassHorizonsSample(time+86_400_000,80.1,1.0,0.0)))
        assertNull(calculateSpaceCompassKeyEvents(sunCalendar.copy(body=body),north,
            SpaceCompassCelestialRemoteData(ephemerides=mapOf(body to ephemeris))))
        assertTrue(calculateSpaceCompassKeyEvents(sunCalendar.copy(body=SpaceCompassCelestialBody.POLARIS),north)!!.isEmpty())
        assertTrue(calculateSpaceCompassKeyEvents(sunCalendar.copy(body=SpaceCompassCelestialBody.LV_426),north)!!.isEmpty())
    }

    @Test fun nearTangentSolarCrossingsAreFoundEvenWhenTheirDurationIsBelowTheSamplingInterval() {
        val centre=4_000_000L
        fun altitude(t:Long)=((t-centre)/1000.0).let { it*it/10_000.0-1.0 }
        val curve=spaceCompassEventCurve(0,8_000_000,3_600_000,{ altitude(it) },{})!!
        val crossings=spaceCompassEventCrossings(curve,0.0,::altitude,{})
        assertEquals(2,crossings.size)
        assertFalse(crossings[0].rising);assertTrue(crossings[1].rising)
        assertTrue(kotlin.math.abs(3_900_000L-crossings[0].timeMs)<=250L)
        assertTrue(kotlin.math.abs(4_100_000L-crossings[1].timeMs)<=250L)
    }

    @Test(expected=CancellationException::class) fun annualSearchHonoursCancellation() {
        calculateSpaceCompassKeyEvents(sunCalendar,north) { throw CancellationException() }
    }

    @Test fun choosingAnEventPreservesSpatialOverridesAndStopsAnAcceleratedClock() {
        val event=lunar.first()
        val state=SpaceCompassObserverState(null,elapsedClock={ 0L })
        val plan=SpaceCompassObserverPlan(-33.9,151.2,1500.0,event.timeMs-86_400_000L,"Australia/Sydney",
            simulatePosition=true,simulateAltitude=true,simulateZone=true,simulateDate=false,simulateTime=false)
        state.apply(plan)
        state.configureTime(SpaceCompassScenarioTimeSettings(enabled=true))
        state.configureTime(state.timeSettings.copy(rate=3600))
        assertTrue(state.observeAt(event.timeMs,rome))
        assertEquals(0,state.timeSettings.multiplier)
        assertEquals(plan.position,state.plan!!.position)
        assertEquals(plan.altitudeMeters,state.plan!!.altitudeMeters,0.0)
        assertEquals("Australia/Sydney",state.plan!!.zoneId)
        assertEquals(event.timeMs,state.plan!!.observationTimeMs(event.timeMs+86_400_000,rome))
    }
}

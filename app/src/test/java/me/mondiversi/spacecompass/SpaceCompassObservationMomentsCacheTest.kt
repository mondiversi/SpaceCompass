package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId

class SpaceCompassObservationMomentsCacheTest {
    private val calendar = SpaceCompassObservationCalendar(SpaceCompassCelestialBody.MOON, 2026, ZoneId.of("Europe/Rome"))
    private fun place(lat: Double = 45.000049, lon: Double = 9.000049, altitude: Double = 104.9,
        fixedPosition: Boolean = false, fixedAltitude: Boolean = false) =
        SpaceCompassObservationPlace(lat, lon, altitude, fixedPosition, fixedAltitude)
    private fun sample(request: SpaceCompassObservationRequest) = SpaceCompassObservationResult(request,
        listOf(SpaceCompassObservationMoment(SpaceCompassObservationMomentKind.MAXIMUM,
            SpaceCompassSunPathPoint(1_000, SpaceCompassSunPosition(180.0, 70.0), SpaceCompassSunPathEvent.CULMINATION))))

    @Test fun repeatedGpsRoundingBoundaryCrossingsKeepTheSameRequestAndCompletedResult() {
        val cache = SpaceCompassObservationMomentsCache()
        val first = cache.request(calendar, place())!!
        val result = sample(first); cache.put(result)
        repeat(100) { index ->
            val next = if (index % 2 == 0) place(45.000051,9.000051,105.1) else place()
            val request = cache.request(calendar,next)!!
            assertEquals(first, request)
            assertSame(result, cache.get(request))
            assertEquals(place(), request.place)
        }
    }

    @Test fun slowRealMovementIsComparedWithTheAcceptedAnchorRatherThanEachPreviousFix() {
        val cache = SpaceCompassObservationMomentsCache()
        val first = cache.request(calendar,place(0.0,0.0))!!
        assertEquals(first,cache.request(calendar,place(.0004,0.0)))
        assertEquals(first,cache.request(calendar,place(.0008,0.0)))
        val moved = cache.request(calendar,place(.0012,0.0))!!
        assertNotEquals(first,moved)
        assertEquals(moved,cache.request(calendar,place(.0014,0.0)))
    }

    @Test fun gpsHeightDriftUsesAnAnchorAndARealHeightChangeInvalidatesTheRequest() {
        val cache = SpaceCompassObservationMomentsCache()
        val first = cache.request(calendar,place(altitude=100.0))!!
        assertEquals(first,cache.request(calendar,place(altitude=149.0)))
        assertNotEquals(first,cache.request(calendar,place(altitude=151.0)))
    }

    @Test fun manualPositionChangesAreExactEvenForSmallOffsets() {
        val cache = SpaceCompassObservationMomentsCache()
        val first = cache.request(calendar,place(fixedPosition=true))!!
        assertNotEquals(first,cache.request(calendar,place(lat=45.000050,fixedPosition=true)))
    }

    @Test fun manualAltitudeChangesAndActivationChangesAreNeverTreatedAsGpsNoise() {
        val cache = SpaceCompassObservationMomentsCache()
        val automatic = cache.request(calendar,place())!!
        val fixed = cache.request(calendar,place(fixedAltitude=true))!!
        assertNotEquals(automatic,fixed)
        assertNotEquals(fixed,cache.request(calendar,place(altitude=104.91,fixedAltitude=true)))
        assertNotEquals(fixed,cache.request(calendar,place(fixedPosition=true,fixedAltitude=true)))
    }

    @Test fun geographicDistanceHandlesTheDateLineAndPolarLongitudes() {
        val cache = SpaceCompassObservationMomentsCache()
        val first = cache.request(calendar,place(0.0,179.9999))!!
        assertEquals(first,cache.request(calendar,place(0.0,-179.9999)))
        val pole = cache.request(calendar,place(89.99999,0.0))!!
        assertEquals(pole,cache.request(calendar,place(89.99999,180.0)))
        assertNotEquals(pole,cache.request(calendar,place(-89.99999,180.0)))
    }

    @Test fun invalidFixesCannotPoisonAValidAnchorOrExposeNonFiniteCoordinates() {
        val cache = SpaceCompassObservationMomentsCache()
        val first = cache.request(calendar,place())!!
        assertNull(cache.request(calendar,place(Double.NaN)))
        assertNull(cache.request(calendar,place(91.0)))
        assertNull(cache.request(calendar,place(lon=181.0)))
        assertNull(cache.request(calendar,place(altitude=Double.POSITIVE_INFINITY)))
        assertEquals(first,cache.request(calendar,place()))
    }

    @Test fun bodyYearAndZoneAreIndependentAndReturningToAnObjectReusesItsAnchor() {
        val cache = SpaceCompassObservationMomentsCache()
        val first = cache.request(calendar,place())!!
        val result = sample(first); cache.put(result)
        for (other in listOf(calendar.copy(body=SpaceCompassCelestialBody.SUN),calendar.copy(year=2027),
            calendar.copy(zone=ZoneId.of("Atlantic/Canary")))) {
            val request = cache.request(other,place())!!
            assertNotEquals(first,request); assertNull(cache.get(request))
        }
        assertSame(result,cache.get(cache.request(calendar.copy(),place(45.000051,9.000051,105.1))))
    }

    @Test fun changedEphemeridesInvalidateAnOtherwiseIdenticalAnnualRequest() {
        val cache = SpaceCompassObservationMomentsCache()
        val body = SpaceCompassCelestialBody.SEDNA
        val scope = calendar.copy(body=body)
        fun ephemeris(offset: Long) = SpaceCompassHorizonsEphemeris(body,listOf(
            SpaceCompassHorizonsSample(offset,80.0,0.0,0.0),SpaceCompassHorizonsSample(offset+1000,80.0,0.0,0.0)))
        val first = cache.request(scope,place(),ephemeris=ephemeris(0))!!
        cache.put(sample(first))
        val revised = cache.request(scope,place(),ephemeris=ephemeris(100))!!
        assertNotEquals(first,revised); assertNull(cache.get(revised)); assertNotNull(cache.get(first))
    }

    @Test fun unavailableResultsAreCompletedCacheEntriesAndCanReplaceOldSuccess() {
        val cache = SpaceCompassObservationMomentsCache()
        val request = cache.request(calendar,place())!!
        assertNull(cache.get(request))
        cache.put(sample(request)); assertNotNull(cache.get(request)!!.moments)
        cache.put(SpaceCompassObservationResult(request,null))
        assertNotNull(cache.get(request)); assertNull(cache.get(request)!!.moments)
    }

    @Test fun completedResultsUseBoundedLeastRecentlyUsedEviction() {
        val cache = SpaceCompassObservationMomentsCache(2)
        val a = cache.request(calendar.copy(year=2024),place())!!
        val b = cache.request(calendar.copy(year=2025),place())!!
        cache.put(sample(a)); cache.put(sample(b)); assertNotNull(cache.get(a))
        val c = cache.request(calendar.copy(year=2026),place())!!
        cache.put(sample(c))
        assertNotNull(cache.get(a)); assertNull(cache.get(b)); assertNotNull(cache.get(c))
    }
}

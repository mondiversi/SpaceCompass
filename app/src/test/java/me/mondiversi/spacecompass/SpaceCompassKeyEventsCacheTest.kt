package me.mondiversi.spacecompass

import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassKeyEventsCacheTest {
    private val calendar=SpaceCompassObservationCalendar(SpaceCompassCelestialBody.SUN,2026,ZoneId.of("Europe/Rome"))
    private val place=SpaceCompassObservationPlace(45.0,9.0,100.0)

    @Test fun liveTicksAndGpsNoiseReuseTheCompletedAnnualCalendar() {
        val cache=SpaceCompassKeyEventsCache()
        val request=cache.request(calendar,place)!!
        val result=SpaceCompassKeyEventsResult(request,emptyList());cache.put(result)
        repeat(100) {
            val next=cache.request(calendar,place.copy(latitude=45.00001,altitude=101.0))!!
            assertEquals(request,next);assertSame(result,cache.get(next))
        }
    }
    @Test fun objectYearZoneAndRealLocationChangesCannotExposeAnotherCalendarsDates() {
        val cache=SpaceCompassKeyEventsCache()
        val request=cache.request(calendar,place)!!;cache.put(SpaceCompassKeyEventsResult(request,emptyList()))
        for ((c,p) in listOf(calendar.copy(body=SpaceCompassCelestialBody.MOON) to place,
            calendar.copy(year=2027) to place,calendar.copy(zone=ZoneId.of("UTC")) to place,
            calendar to place.copy(latitude=46.0),calendar to place.copy(altitude=1000.0)))
            assertNull(cache.get(cache.request(c,p)))
    }
    @Test fun manualAltitudeAndPositionChangesStayExactAndUnavailableResultsAreCached() {
        val cache=SpaceCompassKeyEventsCache()
        val manual=place.copy(fixedPosition=true,fixedAltitude=true)
        val request=cache.request(calendar,manual)!!
        val unavailable=SpaceCompassKeyEventsResult(request,null);cache.put(unavailable)
        assertSame(unavailable,cache.get(request))
        assertNotEquals(request,cache.request(calendar,manual.copy(altitude=100.001)))
        assertNotEquals(request,cache.request(calendar,manual.copy(longitude=9.000001)))
    }
    @Test fun sessionCacheIsBoundedAndRejectsInvalidObservers() {
        val cache=SpaceCompassKeyEventsCache(2)
        val first=cache.request(calendar,place)!!;cache.put(SpaceCompassKeyEventsResult(first,emptyList()))
        for (year in 2027..2028) {
            val request=cache.request(calendar.copy(year=year),place)!!
            cache.put(SpaceCompassKeyEventsResult(request,emptyList()))
        }
        assertNull(cache.get(first))
        assertNull(cache.request(calendar,place.copy(latitude=Double.NaN)))
    }
}

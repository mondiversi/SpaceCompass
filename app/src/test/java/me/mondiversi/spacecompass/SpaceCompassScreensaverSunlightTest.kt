package me.mondiversi.spacecompass

import java.time.Instant
import java.util.TimeZone
import kotlin.math.*
import org.junit.Assert.*
import org.junit.Test

class SpaceCompassScreensaverSunlightTest {
    private fun ms(value: String)=Instant.parse(value).toEpochMilli()
    private fun normal(latitude: Double,longitude: Double): DoubleArray {
        val lat=Math.toRadians(latitude);val lon=Math.toRadians(longitude)
        return doubleArrayOf(cos(lat)*sin(lon),sin(lat),cos(lat)*cos(lon))
    }
    private fun lit(sun: SpaceCompassScreensaverSunlight,lat: Double,lon: Double): Double {
        val v=normal(lat,lon);return sun.x*v[0]+sun.y*v[1]+sun.z*v[2]
    }

    @Test fun earthFixedSolarDirectionMatchesIndependentNoaaGeometricSolarHeights() {
        for (date in listOf("2026-03-20T12:00:00Z","2026-06-21T00:00:00Z","2026-09-23T18:15:00Z",
            "2026-12-21T06:30:00Z","2026-10-28T07:00:00Z")) {
            val now=ms(date);val sun=spaceCompassScreensaverSunlight(now)
            assertEquals(1.0,sqrt(sun.x*sun.x+sun.y*sun.y+sun.z*sun.z),1e-12)
            for (lat in listOf(-85.0,-45.0,0.0,45.0,85.0)) for (lon in listOf(-170.0,-90.0,0.0,90.0,170.0)) {
                val actual=Math.toDegrees(asin(lit(sun,lat,lon).coerceIn(-1.0,1.0)))
                val expected=calculateSpaceCompassSunPosition(now,lat,lon).elevationDegrees
                assertEquals("$date at $lat,$lon",expected,actual,.03)
            }
        }
    }
    @Test fun solsticesIlluminateTheCorrectPolarHemisphere() {
        val june=spaceCompassScreensaverSunlight(ms("2026-06-21T12:00:00Z"))
        val december=spaceCompassScreensaverSunlight(ms("2026-12-21T12:00:00Z"))
        assertEquals(23.44,Math.toDegrees(asin(june.y)),.03)
        assertEquals(-23.44,Math.toDegrees(asin(december.y)),.03)
        assertTrue(lit(june,90.0,0.0)>0);assertTrue(lit(december,90.0,0.0)<0)
    }
    @Test fun noonAndMidnightMoveTheTerminatorWithUtcAndOppositePointsHaveOppositeLight() {
        val noon=spaceCompassScreensaverSunlight(ms("2026-03-20T12:00:00Z"))
        val midnight=spaceCompassScreensaverSunlight(ms("2026-03-21T00:00:00Z"))
        assertTrue(noon.z>.99);assertTrue(midnight.z<-.99)
        for (lat in listOf(-80.0,-30.0,0.0,40.0,85.0)) for (lon in listOf(-170.0,-40.0,80.0))
            assertEquals(-lit(noon,lat,lon),lit(noon,-lat,lon+180),1e-12)
    }
    @Test fun theSameInstantHasTheSameLightingInEveryPhoneTimeZone() {
        val original=TimeZone.getDefault();val moment=ms("2026-10-28T07:00:00Z")
        val sun=spaceCompassScreensaverSunlight(moment)
        try {
            for (zone in listOf("Pacific/Auckland","Europe/Rome","America/New_York","Asia/Kathmandu")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                assertEquals(sun,spaceCompassScreensaverSunlight(moment))
            }
        } finally { TimeZone.setDefault(original) }
    }
}

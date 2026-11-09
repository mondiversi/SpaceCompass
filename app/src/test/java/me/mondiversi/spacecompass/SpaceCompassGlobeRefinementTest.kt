package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import kotlin.math.*

class SpaceCompassGlobeRefinementTest {
    private val dates=listOf("2026-03-20T12:00:00Z","2026-06-21T12:00:00Z","2026-12-21T12:00:00Z")
    @Test fun seasonalEarthTransformAndItsInverseStayIdenticalForTextureAndOrbitPoints() {
        for(date in dates)for(lon in listOf(-3.0,0.0,2.0)) {
            val season=spaceCompassScreensaverSeason(Instant.parse(date).toEpochMilli())
            val pose=spaceCompassGlobeEffectivePose(SpaceCompassGlobePose(lon,.7),season,true)
            assertEquals(season.rollRadians,pose.roll,0.0);assertEquals(season.pitchRadians,pose.latitude,0.0)
            for(v in listOf(SpaceCompassGlobeVector(1.0,0.0,0.0),SpaceCompassGlobeVector(0.0,1.0,0.0),SpaceCompassGlobeVector(.3,-.5,.8))) {
                val roundTrip=spaceCompassGlobeInverseView(spaceCompassGlobeView(v,pose),pose)
                assertEquals(v.x,roundTrip.x,1e-12);assertEquals(v.y,roundTrip.y,1e-12);assertEquals(v.z,roundTrip.z,1e-12)
            }
        }
    }
    @Test fun axialDraggingCannotChangeAxisTiltAndFreeDraggingCan() {
        val p=SpaceCompassGlobePose(.3,.4,1.0,.2)
        val locked=spaceCompassGlobeDrag(p,40.0,100.0,300.0,500.0,true)
        assertEquals(p.latitude,locked.latitude,0.0);assertEquals(p.roll,locked.roll,0.0)
        assertNotEquals(p.longitude,locked.longitude,1e-9)
        assertNotEquals(p.latitude,spaceCompassGlobeDrag(p,40.0,100.0,300.0,500.0,false).latitude,1e-9)
    }
    @Test fun legacyStoredZoomStillDecodesWithoutChangingSavedMultiplier() {
        assertEquals(1.0,spaceCompassGlobeZoom(1.4,true),1e-12)
        assertEquals(1.5,spaceCompassGlobeZoom(1.5,false),1e-12)
    }
    @Test fun globeCentreUsesTheActualGapBetweenUpperAndLowerControls() {
        val frame=spaceCompassGlobeViewport(360.0,700.0,80.0,120.0)
        assertEquals(330.0,frame.top+frame.height/2,1e-12)
        val expanded=spaceCompassGlobeViewport(360.0,400.0,70.0,90.0)
        assertEquals(190.0,expanded.top+expanded.height/2,1e-12)
        val hidden=spaceCompassGlobeViewport(360.0,700.0,0.0,0.0)
        assertEquals(350.0,hidden.top+hidden.height/2,1e-12)
    }
    @Test fun satelliteGeocentricPositionAgreesWithItsIndependentTemePropagation() {
        val orbit=SpaceCompassIssOrbit.parse("1 25544U 98067A   26276.04623379  .00005750  00000+0  11349-3 0  9994\n"+
            "2 25544  51.6314 126.3061 0006899 216.4733 143.5786 15.48722558588472")
        val now=orbit.epochMs;val teme=orbit.teme(now)
        val jd=now/86400000.0+2440587.5;val t=(jd-2451545)/36525
        val angle=Math.toRadians(wrapSpaceCompassSunDegrees(280.46061837+360.98564736629*(jd-2451545)+.000387933*t*t-t*t*t/38710000))
        val expected=SpaceCompassGlobeVector(-sin(angle)*teme[0]+cos(angle)*teme[1],teme[2],cos(angle)*teme[0]+sin(angle)*teme[1])
        for((lat,lon) in listOf(0.0 to 0.0,45.0 to 9.0,-33.0 to 151.0)) {
            val observed=orbit.observe(now,lat,lon,150.0)
            val fixed=spaceCompassGlobeEarthFixed(observed.position,lat,lon,150.0,observed.distanceKm)!!
            assertEquals(expected.x,fixed.x,1e-7);assertEquals(expected.y,fixed.y,1e-7);assertEquals(expected.z,fixed.z,1e-7)
        }
    }
    @Test fun solarDirectionMatchesTheEarthLightingVectorInEveryHemisphere() {
        for(date in dates) {
            val now=Instant.parse(date).toEpochMilli();val sun=spaceCompassScreensaverSunlight(now)
            for((lat,lon) in listOf(0.0 to 0.0,45.0 to 9.0,-33.0 to 151.0)) {
                val observed=calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.SUN,now,lat,lon,120.0)!!
                val fixed=spaceCompassGlobeEarthFixed(observed.position,lat,lon,120.0,observed.distanceKm)!!.direction(1.0)!!
                val dot=(sun.x*fixed.x+sun.y*fixed.y+sun.z*fixed.z).coerceIn(-1.0,1.0)
                assertTrue(Math.toDegrees(acos(dot))<.04)
            }
        }
    }
    @Test fun solarAndLunarApparentDailyPathsIncludeTheEarthRotation() {
        val now=Instant.parse("2026-06-21T12:00:00Z").toEpochMilli()
        for(body in listOf(SpaceCompassCelestialBody.SUN,SpaceCompassCelestialBody.MOON)) {
            val zone=java.time.ZoneId.of("UTC");val date=java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val path=calculateSpaceCompassCelestialPath(body,date,zone,now,45.0,9.0,0.0,SpaceCompassCelestialRemoteData())!!
            val snapshot=SpaceCompassPanoramaSnapshot(now,45.0,9.0,0.0,SpaceCompassSunSkyPhase.AFTERNOON,null,
                listOf(SpaceCompassPanoramaObject(body,"Object",path)),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList())
            val vectors=buildSpaceCompassGlobeModel(snapshot).tracks.single().curve.map { it.first.direction(1.0)!! }
            val angle=vectors.zipWithNext().sumOf { (a,b) -> Math.toDegrees(acos((a.x*b.x+a.y*b.y+a.z*b.z).coerceIn(-1.0,1.0))) }
            assertTrue("$body unexpectedly traces $angle degrees",angle>300.0&&angle<380.0)
            assertTrue(buildSpaceCompassGlobeModel(snapshot).tracks.single().curve.all { abs(it.first.length-1.55)<1e-10 })
        }
    }

    @Test fun oneTimesContainsTheCompleteTrajectoryEnvelopeWithMarkerMarginInEitherOrientation() {
        for(frame in listOf(spaceCompassGlobeViewport(360.0,700.0,80.0,120.0),
            spaceCompassGlobeViewport(500.0,360.0,70.0,90.0)))for(extent in listOf(1.55,6.0,60.0)) {
            val radius=spaceCompassGlobeFitRadius(frame,extent,1.0)
            val half=min(frame.width,frame.height)/2
            assertEquals(half-24.0,radius*extent,1e-10)
            for(pose in listOf(SpaceCompassGlobePose(),SpaceCompassGlobePose(.7,.4,1.0,.3))) {
                val centreX=frame.left+frame.width/2;val centreY=frame.top+frame.height/2
                for(i in 0..72) {
                    val a=i*PI/36;val point=spaceCompassGlobeView(SpaceCompassGlobeVector(extent*cos(a),extent*sin(a),0.0),pose)
                    assertTrue(centreX+point.x*radius>=frame.left+24-1e-8)
                    assertTrue(centreX+point.x*radius<=frame.left+frame.width-24+1e-8)
                    assertTrue(centreY-point.y*radius>=frame.top+24-1e-8)
                    assertTrue(centreY-point.y*radius<=frame.top+frame.height-24+1e-8)
                }
            }
            assertEquals(radius*2,spaceCompassGlobeFitRadius(frame,extent,2.0),1e-10)
        }
    }

}

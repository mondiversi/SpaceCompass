package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import kotlin.math.*

class SpaceCompassGlobeGeometryTest {
    private val now=Instant.parse("2026-06-21T12:00:00Z").toEpochMilli()
    private fun snapshot(body: SpaceCompassCelestialBody=SpaceCompassCelestialBody.SUN,latitude: Double=45.0,longitude: Double=9.0): SpaceCompassPanoramaSnapshot {
        val date=Instant.ofEpochMilli(now).atZone(ZoneId.of("UTC")).toLocalDate()
        val path=calculateSpaceCompassCelestialPath(body,date,ZoneId.of("UTC"),now,latitude,longitude,0.0,SpaceCompassCelestialRemoteData())
        return SpaceCompassPanoramaSnapshot(now,latitude,longitude,0.0,SpaceCompassSunSkyPhase.AFTERNOON,null,
            listOf(SpaceCompassPanoramaObject(body,"Object",path)),SpaceCompassCelestialRemoteData(),"",emptyList(),emptyList())
    }
    @Test fun greenwichLocalAxesBecomeCorrectEarthFixedAxes() {
        val east=spaceCompassGlobeEarthFixed(SpaceCompassSunPosition(90.0,0.0),0.0,0.0,0.0,null)!!
        val north=spaceCompassGlobeEarthFixed(SpaceCompassSunPosition(0.0,0.0),0.0,0.0,0.0,null)!!
        val up=spaceCompassGlobeEarthFixed(SpaceCompassSunPosition(0.0,90.0),0.0,0.0,0.0,null)!!
        assertEquals(1.0,east.x,1e-12);assertEquals(1.0,north.y,1e-12);assertEquals(1.0,up.z,1e-12)
    }
    @Test fun knownRangeAddsTheObserverButUnknownRangeRemainsDirectionOnly() {
        val p=SpaceCompassSunPosition(0.0,90.0)
        assertEquals(7078.137,spaceCompassGlobeEarthFixed(p,0.0,0.0,0.0,700.0)!!.z,1e-8)
        assertEquals(1.0,spaceCompassGlobeEarthFixed(p,0.0,0.0,0.0,Double.NaN)!!.length,1e-12)
    }
    @Test fun observerIsAtTheGlobeCentreForEveryHemisphereAndLongitude() {
        for(lat in listOf(-90.0,-45.0,0.0,45.0,90.0))for(lon in listOf(-179.0,-90.0,0.0,90.0,179.0)) {
            val phi=Math.toRadians(lat);val l=Math.toRadians(lon)
            val v=spaceCompassGlobeView(SpaceCompassGlobeVector(cos(phi)*sin(l),sin(phi),cos(phi)*cos(l)),SpaceCompassGlobePose(l,phi))
            assertEquals(0.0,v.x,1e-12);assertEquals(0.0,v.y,1e-12);assertEquals(1.0,v.z,1e-12)
        }
    }
    @Test fun geocentricMoonDirectionAgreesAfterObserverParallaxIsUndone() {
        val directions=listOf(0.0 to 0.0,45.0 to 9.0,-35.0 to 149.0).map { (lat,lon) ->
            val o=calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.MOON,now,lat,lon,200.0)!!
            spaceCompassGlobeEarthFixed(o.position,lat,lon,200.0,o.distanceKm)!!.direction(1.0)!!
        }
        for(v in directions.drop(1)) {
            assertEquals(directions[0].x,v.x,1e-8);assertEquals(directions[0].y,v.y,1e-8);assertEquals(directions[0].z,v.z,1e-8)
        }
    }
    @Test fun distantObjectsAreDirectionsAndNeverDrawnAsPhysicalSatelliteOrbits() {
        val s=snapshot();val m=buildSpaceCompassGlobeModel(s)
        assertTrue(m.tracks.single().curve.isNotEmpty())
        m.tracks.single().curve.forEach { assertEquals(1.55,it.first.length,1e-10) }
    }
    @Test fun missingSatelliteDataNeverInventsATrackOrCurrentMarker() {
        val s=snapshot().copy(objects=listOf(SpaceCompassPanoramaObject(SpaceCompassCelestialBody.ISS,"ISS",null)))
        val t=buildSpaceCompassGlobeModel(s).tracks.single()
        assertTrue(t.curve.isEmpty());assertTrue(t.dots.isEmpty())
    }
    @Test fun unavailableLocationHasNoFabricatedGreenwichObserverOrTracks() {
        assertTrue(buildSpaceCompassGlobeModel(snapshot().copy(observerPositionKnown=false)).tracks.isEmpty())
    }
    @Test fun selectedPointsAndTheirAntipodesShareTheSameGeometryAndIdentity() {
        val s=snapshot();val path=s.objects.single().path!!;val point=path.markers.first()
        val key=spaceCompassOrbitPointKey(path.body,point,path.zone,path.date)
        val m=buildSpaceCompassGlobeModel(s.copy(selectedPoints=listOf(SpaceCompassSelectedOrbitPoint(key,point,path)),showAntipodes=true))
        val pair=m.tracks.single().dots.filter { it.selected }
        assertEquals(2,pair.size)
        val a=pair.first { !it.antipode }.vector;val b=pair.first { it.antipode }.vector
        assertEquals(-a.x,b.x,1e-12);assertEquals(-a.y,b.y,1e-12);assertEquals(-a.z,b.z,1e-12)
    }
    @Test fun privacyVariantsNeverExposeAnExactGlobeLocationMarker() {
        val s=snapshot().copy(globe=SpaceCompassGlobeCapture(SpaceCompassGlobePose(),"Directions"),globeLocationVisible=true)
        assertFalse(spaceCompassSnapshotForDisclosure(s,SpaceCompassPanoramaPosition.AREA).globeLocationVisible)
        assertFalse(spaceCompassSnapshotForDisclosure(s,SpaceCompassPanoramaPosition.HIDDEN).globeLocationVisible)
        assertTrue(spaceCompassSnapshotForDisclosure(s,SpaceCompassPanoramaPosition.COMPLETE).globeLocationVisible)
    }
}

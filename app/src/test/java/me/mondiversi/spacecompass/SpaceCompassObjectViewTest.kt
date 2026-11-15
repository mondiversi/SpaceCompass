package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*
import java.io.File
import java.time.Instant

class SpaceCompassObjectViewTest {
    private fun close(a: SpaceCompassGlobeVector,b: SpaceCompassGlobeVector,tolerance: Double=1e-8) {
        assertEquals(a.x,b.x,tolerance);assertEquals(a.y,b.y,tolerance);assertEquals(a.z,b.z,tolerance)
    }
    @Test fun onboardEyeSharesTheDrawnOrbitAndPutsTheForwardHorizonAtCentre() {
        for(radius in listOf(1.06,1.55,2.0)) {
            val position=SpaceCompassGlobeVector(0.0,0.0,radius)
            val c=spaceCompassObjectCamera(position,SpaceCompassGlobeVector(1.0,0.0,0.0))!!
            close(position,spaceCompassGlobeInverseView(c.eye!!,c.pose))
            val horizon=spaceCompassGlobeInverseView(c.target,c.pose)
            assertEquals(1.0,horizon.length,1e-10)
            assertEquals(1.0,horizon.x*position.x+horizon.y*position.y+horizon.z*position.z,1e-10)
            val p=c.project(horizon);assertEquals(0.0,p.x,1e-10);assertEquals(0.0,p.y,1e-10)
            val gaze=spaceCompassGlobeInverseView(SpaceCompassGlobeVector(0.0,0.0,-1.0),c.pose)
            assertTrue(gaze.x>0);assertTrue(gaze.z<0)
            assertNull(c.normal(0.0,.02));assertNotNull(c.normal(0.0,-.02))
        }
    }
    @Test fun earthRotationKeepsTheForwardEyeAndHorizonFixed() {
        val source=SpaceCompassGlobeVector(.7,-.3,1.4);val base=spaceCompassObjectCamera(source)!!
        for(yaw in listOf(-PI,-1.2,0.0,.6,PI))for(pitch in listOf(-PI/2,-.4,0.0,PI/2)) {
            val c=base.rotatingEarth(SpaceCompassObjectEarthRotation(yaw,pitch))
            close(base.eye!!,c.eye!!);close(base.target,c.target)
            close(base.normal(0.0,-.1)!!,c.normal(0.0,-.1)!!)
            assertNotNull(c.normal(0.0,0.0))
            assertEquals(base.focal,c.focal,1e-10);assertEquals(1.0,c.framing.z,0.0)
        }
    }
    @Test fun textureAndPointAxesAgreeAcrossFullTurnsAndPolarSingularities() {
        val sources=listOf(SpaceCompassGlobeVector(.3,.6,1.0),SpaceCompassGlobeVector(0.0,1.55,0.0),SpaceCompassGlobeVector(0.0,-1.55,0.0))
        val points=listOf(SpaceCompassGlobeVector(1.0,0.0,0.0),SpaceCompassGlobeVector(0.0,1.0,0.0),SpaceCompassGlobeVector(.3,-.5,.8))
        for(source in sources)for(yaw in listOf(-PI,-.7,0.0,1.2,PI))for(pitch in listOf(-PI/2,-.9,0.0,.5,PI/2)) {
            val base=spaceCompassObjectCamera(source)!!;val look=SpaceCompassObjectEarthRotation(yaw,pitch)
            val c=base.rotatingEarth(look)
            for(p in points)close(spaceCompassGlobeView(spaceCompassGlobeView(p,base.pose),SpaceCompassGlobePose(yaw,pitch)),spaceCompassGlobeView(p,c.pose),5e-8)
        }
    }
    @Test fun finiteProjectionAndRayIntersectionAgreeWhileLookingAcrossTheHorizon() {
        val base=spaceCompassObjectCamera(SpaceCompassGlobeVector(.3,.2,1.0))!!;var count=0
        for(yaw in listOf(-2.0,-1.4,-.4,0.0,.8,1.5,PI))for(pitch in listOf(-1.2,0.0,.7)) {
            val c=base.rotatingEarth(SpaceCompassObjectEarthRotation(yaw,pitch))
            for(lat in -75..75 step 5)for(lon in -180..180 step 5) {
                val a=Math.toRadians(lat.toDouble());val b=Math.toRadians(lon.toDouble())
                val v=SpaceCompassGlobeVector(cos(a)*sin(b),sin(a),cos(a)*cos(b));val p=c.project(v)
                if(p.z<.01)continue
                close(spaceCompassGlobeView(v,c.pose),c.normal(p.x,p.y)!!)
                count++
            }
        }
        assertTrue(count>100)
    }
    @Test fun rotatingTheEarthDoesNotTurnTheViewAwayFromTheForwardHorizon() {
        val base=spaceCompassObjectCamera(SpaceCompassGlobeVector(0.0,0.0,1.55))!!
        for(yaw in listOf(-PI,0.0,PI))for(pitch in listOf(-PI/2,0.0,PI/2)) {
            val c=base.rotatingEarth(SpaceCompassObjectEarthRotation(yaw,pitch))
            assertNull(c.normal(0.0,.02));assertNotNull(c.normal(0.0,-.02))
            close(base.eye!!,c.eye!!)
        }
    }
    @Test fun dragMovesTheViewWithTheFingerAndKeepsFineControlAtHighZoom() {
        val start=SpaceCompassObjectEarthRotation();val a=spaceCompassObjectEarthDrag(start,30.0,20.0,400.0,600.0,1.0)
        val b=spaceCompassObjectEarthDrag(start,30.0,20.0,400.0,600.0,8.0)
        assertTrue(abs(b.yaw)<abs(a.yaw));assertTrue(abs(b.pitch)<abs(a.pitch))
        val base=spaceCompassObjectCamera(SpaceCompassGlobeVector(0.0,0.0,1.55))!!
        val c=base.rotatingEarth(a)
        val surface=spaceCompassGlobeInverseView(base.normal(0.0,-.2)!!,base.pose)
        val before=base.project(surface);val p=c.project(surface)
        assertTrue(p.x<before.x);assertTrue(p.y>before.y)
        assertEquals(PI/2,SpaceCompassObjectEarthRotation(15.0,10.0).normalized().pitch,0.0)
        assertEquals(SpaceCompassObjectEarthRotation(),SpaceCompassObjectEarthRotation(Double.NaN,Double.NaN).normalized())
    }
    @Test fun onboardRadiusMatchesPhysicalSatellitesAndApparentDisplayShells() {
        val satellite=SpaceCompassCelestialBody.entries.first { it.isEarthSatellite }
        val fixed=SpaceCompassGlobeVector(0.0,0.0,6800.0)
        val satelliteVector=spaceCompassGlobeDisplayVector(satellite,fixed)!!
        val apparent=spaceCompassGlobeDisplayVector(SpaceCompassCelestialBody.SUN,fixed)!!
        assertEquals(6800.0/6378.137,satelliteVector.length,1e-10);assertEquals(1.55,apparent.length,1e-10)
        for(vector in listOf(satelliteVector,apparent)) {
            val c=spaceCompassObjectCamera(vector)!!
            close(vector,spaceCompassGlobeInverseView(c.eye!!,c.pose))
        }
        assertNull(spaceCompassObjectCamera(SpaceCompassGlobeVector(0.0,0.0,0.0)))
        assertNull(spaceCompassObjectCamera(SpaceCompassGlobeVector(0.0,0.0,.9)))
        assertNull(spaceCompassObjectCamera(SpaceCompassGlobeVector(Double.NaN,0.0,1.0)))
        assertEquals(8.0,SpaceCompassGlobePose(zoom=100.0).normalized().zoom,0.0)
    }
    @Test fun cameraFollowsTheDrawnOrbitWhileTheManualEarthRotationKeepsItsHorizon() {
        val start=Instant.parse("2026-11-14T12:00:00Z").toEpochMilli();val remote=SpaceCompassCelestialRemoteData()
        val eyes=(0..1).map { hour ->
            val now=start+hour*3600000
            val observation=calculateSpaceCompassCelestialObservation(SpaceCompassCelestialBody.SUN,now,45.0,9.0,120.0,remote)!!
            assertTrue(observation.distanceKm>1e6)
            val footprint=spaceCompassGlobeEarthFixed(observation.position,45.0,9.0,120.0,observation.distanceKm)!!.direction(1.55)!!
            val base=spaceCompassObjectCamera(SpaceCompassCelestialBody.SUN,now,45.0,9.0,120.0,remote)!!
            val c=base.rotatingEarth(SpaceCompassObjectEarthRotation(.3,-.2))
            close(base.eye!!,c.eye!!)
            val eye=spaceCompassGlobeInverseView(base.eye,base.pose);close(footprint,eye);eye
        }
        assertTrue(hypot(eyes[0].x-eyes[1].x,eyes[0].z-eyes[1].z)>.1)
    }
    @Test fun objectModeRemainsFramed() {
        assertTrue(SpaceCompassViewMode.OBJECT.globe);assertFalse(SpaceCompassViewMode.OBJECT.panoramic)
    }
    @Test fun interactiveRendererUsesTheActualViewerWithoutChangingTheScreensaver() {
        val file=listOf(File("src/main/assets/screensaver/mondiversi-space.html"),File("app/src/main/assets/screensaver/mondiversi-space.html")).first { it.isFile }
        val original=file.readText();val adapted=spaceCompassObjectRendererHtml(original)
        assertFalse(original.contains("uniform float objectView"));assertTrue(adapted.contains("uniform float objectView"))
        assertTrue(adapted.contains("normalize(objectEye-n)"));assertTrue(adapted.contains("1-facing"))
        assertTrue(adapted.contains("mapWidth=2048,mapHeight=1024"));assertTrue(adapted.contains("spaceCompassGlobeDetailSize"));assertTrue(adapted.contains("objectNormal"));assertTrue(adapted.contains("objectView<.5"))
    }
}

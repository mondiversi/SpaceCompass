package me.mondiversi.planetcompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.atan2

class PlanetCompassCelestialViewerTest {
    @Test fun mapDecodeSampleNeverStartsAtZeroAndCapsLargeMaps() {
        assertEquals(1, planetCompassCelestialTextureSampleSize(0))
        assertEquals(1, planetCompassCelestialTextureSampleSize(1024))
        assertEquals(1, planetCompassCelestialTextureSampleSize(2048))
        assertEquals(4, planetCompassCelestialTextureSampleSize(5926))
        for (width in listOf(1024, 2048, 5926, Int.MAX_VALUE)) {
            val sample = planetCompassCelestialTextureSampleSize(width)
            assertTrue(sample > 0)
            assertTrue(width / sample <= 2048)
            assertEquals(0, sample and (sample - 1))
        }
    }
    private val now = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    private fun orthonormal(g: PlanetCompassCelestialViewGeometry) {
        listOf(g.bodyX,g.bodyY,g.bodyZ,g.light).forEach { assertEquals(1.0,it.dot(it),1e-9) }
        assertEquals(0.0,g.bodyX.dot(g.bodyY),1e-9)
        assertEquals(0.0,g.bodyY.dot(g.bodyZ),1e-9)
        assertEquals(0.0,g.bodyZ.dot(g.bodyX),1e-9)
        assertEquals(1.0,g.bodyX.cross(g.bodyY).dot(g.bodyZ),1e-9)
    }
    @Test fun everyPhysicalBodyHasAWellConditionedCameraFrame() {
        PlanetCompassCelestialBody.entries.filter { it.hasPhysicalFace }.forEach { body ->
            for (latitude in listOf(-90.0,-33.0,41.9,90.0)) {
                val g=calculatePlanetCompassCelestialViewGeometry(body,now,latitude,12.5,50.0)!!
                orthonormal(g); assertTrue(g.distanceKm!! > 100_000)
                if (body != PlanetCompassCelestialBody.SUN) assertTrue(g.illuminatedFraction!! in 0.0..1.0)
            }
        }
    }
    @Test fun lunarNearSideUsesIauPrimeMeridianRatherThanArbitrarySpin() {
        val g=calculatePlanetCompassCelestialViewGeometry(PlanetCompassCelestialBody.MOON,now,41.9,12.5)!!
        val longitude=Math.toDegrees(atan2(g.bodyY.z,g.bodyX.z))
        assertTrue("Visible lunar longitude $longitude",abs(longitude)<12)
        assertTrue(g.bodyX.z > 0.85)
    }
    @Test fun phaseMatchesAstronomyEngineWithoutUsingThePhoneCompass() {
        for (day in 0..30) {
            val time=now+day*86_400_000L
            val g=calculatePlanetCompassCelestialViewGeometry(PlanetCompassCelestialBody.MOON,time,0.0,0.0)!!
            assertEquals(calculatePlanetCompassMoonPhase(time).illuminatedFraction,g.illuminatedFraction!!,0.02)
        }
    }
    @Test fun observerLocationChangesTheActualFaceOrientation() {
        val a=calculatePlanetCompassCelestialViewGeometry(PlanetCompassCelestialBody.MOON,now,41.9,12.5)!!
        val b=calculatePlanetCompassCelestialViewGeometry(PlanetCompassCelestialBody.MOON,now,-33.0,151.0)!!
        assertTrue(abs(a.bodyZ.x-b.bodyZ.x)+abs(a.bodyZ.y-b.bodyZ.y)>0.1)
    }
    @Test fun utcAndDaylightSavingCannotShiftTheRenderedFace() {
        val original=TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"))
            val a=calculatePlanetCompassCelestialViewGeometry(PlanetCompassCelestialBody.MARS,now,41.9,12.5)
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            assertEquals(a,calculatePlanetCompassCelestialViewGeometry(PlanetCompassCelestialBody.MARS,now,41.9,12.5))
        } finally { TimeZone.setDefault(original) }
    }
    @Test fun dragStopsAnimationAndTapResumesOnOriginalAxis() {
        val initial=PlanetCompassCelestialRotation()
        val dragged=initial.drag(50.0,30.0,200.0)
        assertFalse(dragged.automatic); assertEquals(initial.yaw,dragged.yaw,1e-9)
        assertEquals(dragged,dragged.advance(0.1))
        assertTrue(dragged.resume().automatic)
        assertEquals(initial.geometry(), dragged.resume().geometry())
        assertNotEquals(dragged.yaw,dragged.resume().advance(0.1).yaw)
    }
    @Test fun rotationRemainsOrthonormalAtEveryDragAngle() {
        for (yaw in 0..360 step 10) for(pitch in -85..85 step 10)
            orthonormal(PlanetCompassCelestialRotation(yaw.toDouble(),pitch.toDouble()).geometry())
    }
    @Test fun returningUsesAContinuousShortestArcBeforeResumingCanonicalSpin() {
        val initial=PlanetCompassCelestialRotation(yaw=147.0,pitch=50.0)
        val dragged=initial.drag(70.0,90.0,200.0)
        val returning=dragged.beginReturn()
        assertTrue(returning.returning); assertFalse(returning.automatic)
        assertEquals(dragged.geometry(),returning.geometry())
        var previous=abs(returning.manualOrientation.w)
        for(step in 0..20) {
            val q=returning.manualOrientation.returning(step/20.0)
            orthonormal(returning.copy(manualOrientation=q).geometry())
            assertTrue(abs(q.w)+1e-12>=previous); previous=abs(q.w)
        }
        assertEquals(initial.geometry(),returning.copy(manualOrientation=returning.manualOrientation.returning(1.0)).geometry())
        assertEquals(initial.geometry(),returning.resume().geometry())
        assertFalse(returning.drag(1.0,1.0,200.0).returning)
        assertEquals(initial,initial.beginReturn())
    }
    @Test fun antipodalQuaternionRepresentationsReturnAlongTheSamePhysicalArc() {
        val q=PlanetCompassViewQuaternion.axisAngle(PlanetCompassViewVector(0.0,1.0,0.0),270.0)
        val opposite=PlanetCompassViewQuaternion(-q.w,-q.x,-q.y,-q.z)
        for(progress in listOf(0.1,0.5,0.9,1.0))
            assertEquals(q.returning(progress),opposite.returning(progress))
    }
    @Test fun unrestrictedDragIgnoresInvalidInput() {
        val state=PlanetCompassCelestialRotation().drag(50000.0,50000.0,1.0)
        orthonormal(state.geometry())
        assertEquals(state,state.drag(Double.NaN,1.0,10.0))
        assertEquals(state,state.drag(1.0,1.0,0.0))
        assertEquals(state,state.drag(Double.MAX_VALUE,1.0,Double.MIN_VALUE))
    }
    @Test fun visibleSurfaceFollowsFingerInBothScreenAxes() {
        val initial = PlanetCompassCelestialRotation(yaw = 127.0)
        val g = initial.geometry()
        val front = PlanetCompassViewVector(g.bodyX.z, g.bodyY.z, g.bodyZ.z)
        assertTrue(initial.drag(20.0,0.0,200.0).geometry().toCamera(front).x > 0)
        assertTrue(initial.drag(-20.0,0.0,200.0).geometry().toCamera(front).x < 0)
        // Camera Y is upwards; screen Y is downwards.
        assertTrue(initial.drag(0.0,20.0,200.0).geometry().toCamera(front).y < 0)
        assertTrue(initial.drag(0.0,-20.0,200.0).geometry().toCamera(front).y > 0)
    }
    @Test fun verticalDragCanPassBothPolesAndCompleteAFullTurn() {
        val initial = PlanetCompassCelestialRotation()
        val half = initial.drag(0.0,200.0,200.0).geometry()
        assertTrue(initial.geometry().bodyZ.dot(half.bodyZ) < -0.8)
        val full = initial.drag(0.0,400.0,200.0).geometry()
        assertEquals(1.0,initial.geometry().bodyZ.dot(full.bodyZ),1e-9)
        assertEquals(1.0,initial.geometry().bodyX.dot(full.bodyX),1e-9)
    }
    @Test fun repeatedMixedDragsStayOrthonormalAndResumeCanonicalSpacecraftAxis() {
        val initial = PlanetCompassCelestialRotation(yaw = 214.0, pitch = 50.0)
        var state = initial
        repeat(10_000) { state = state.drag(3.0,-5.0,300.0) }
        orthonormal(state.geometry())
        assertEquals(initial.geometry(),state.resume().geometry())
        assertEquals(50.0,state.resume().pitch,0.0)
    }
    @Test fun simulationRotationHonoursRetrogradeAndDoesNotJumpAfterPause() {
        assertEquals(1.2,PlanetCompassCelestialRotation().advance(100.0).yaw,1e-9)
        assertEquals(358.8,PlanetCompassCelestialRotation().advance(0.1,true).yaw,1e-9)
        assertEquals(PlanetCompassCelestialRotation(),PlanetCompassCelestialRotation().advance(-1.0))
    }
    @Test fun spacecraftAndUnresolvedBodiesDoNotInventActualAttitude() {
        listOf(PlanetCompassCelestialBody.ISS,PlanetCompassCelestialBody.STARLINK_V3,PlanetCompassCelestialBody.VOYAGER_1,PlanetCompassCelestialBody.VOYAGER_2,PlanetCompassCelestialBody.SEDNA)
            .forEach { assertNull(calculatePlanetCompassCelestialViewGeometry(it,now,41.9,12.5)) }
    }
    @Test fun referenceDataCoversAllObjectsAndUsesNullForUnavailableQuantities() {
        PlanetCompassCelestialBody.entries.forEach { body ->
            val f=planetCompassCelestialFacts(body)
            listOf(f.diameterKm,f.massKg,f.gravity,f.density,f.revolutionDays,f.minimumParentKm,f.maximumParentKm)
                .filterNotNull().forEach { assertTrue(it.isFinite() && it>0) }
            if(f.minimumParentKm!=null) assertTrue(f.maximumParentKm!!>f.minimumParentKm)
            if(body.isSpacecraft) { assertNull(f.rotationHours); assertNull(f.gravity); assertNull(f.revolutionDays) }
        }
        assertNull(planetCompassCelestialFacts(PlanetCompassCelestialBody.SEDNA).massKg)
        assertNull(planetCompassCelestialFacts(PlanetCompassCelestialBody.SUN).parent)
        assertTrue(planetCompassCelestialFacts(PlanetCompassCelestialBody.MOON).parentIsEarth)
        assertTrue(planetCompassCelestialFacts(PlanetCompassCelestialBody.VENUS).rotationHours!!<0)
    }
    @Test fun spacecraftMeshesAreNonEmptyAndFinite() {
        for(body in listOf(PlanetCompassCelestialBody.ISS,PlanetCompassCelestialBody.STARLINK_V3,PlanetCompassCelestialBody.VOYAGER_1,PlanetCompassCelestialBody.VOYAGER_2)) {
            val mesh=planetCompassCelestialCraftMesh(body); assertTrue(mesh.size>100)
            mesh.forEach { f -> listOf(f.a,f.b,f.c).forEach { p ->
                assertTrue(p.x.isFinite() && p.y.isFinite() && p.z.isFinite())
            } }
        }
    }
}

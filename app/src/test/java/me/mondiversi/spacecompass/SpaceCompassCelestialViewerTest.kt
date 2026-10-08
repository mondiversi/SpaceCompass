package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.atan2

class SpaceCompassCelestialViewerTest {
    @Test fun mapDecodeSampleNeverStartsAtZeroAndCapsLargeMaps() {
        assertEquals(1, spaceCompassCelestialTextureSampleSize(0))
        assertEquals(1, spaceCompassCelestialTextureSampleSize(1024))
        assertEquals(1, spaceCompassCelestialTextureSampleSize(2048))
        assertEquals(4, spaceCompassCelestialTextureSampleSize(5926))
        for (width in listOf(1024, 2048, 5926, Int.MAX_VALUE)) {
            val sample = spaceCompassCelestialTextureSampleSize(width)
            assertTrue(sample > 0)
            assertTrue(width / sample <= 2048)
            assertEquals(0, sample and (sample - 1))
        }
    }
    private val now = Instant.parse("2026-10-04T12:00:00Z").toEpochMilli()
    private fun orthonormal(g: SpaceCompassCelestialViewGeometry) {
        listOf(g.bodyX,g.bodyY,g.bodyZ,g.light).forEach { assertEquals(1.0,it.dot(it),1e-9) }
        assertEquals(0.0,g.bodyX.dot(g.bodyY),1e-9)
        assertEquals(0.0,g.bodyY.dot(g.bodyZ),1e-9)
        assertEquals(0.0,g.bodyZ.dot(g.bodyX),1e-9)
        assertEquals(1.0,g.bodyX.cross(g.bodyY).dot(g.bodyZ),1e-9)
    }
    @Test fun everyPhysicalBodyHasAWellConditionedCameraFrame() {
        SpaceCompassCelestialBody.entries.filter { it.hasPhysicalFace }.forEach { body ->
            for (latitude in listOf(-90.0,-33.0,41.9,90.0)) {
                val titan = SpaceCompassHorizonsEphemeris(SpaceCompassCelestialBody.TITAN,
                    listOf(now - 3_600_000L, now + 3_600_000L).map { SpaceCompassHorizonsSample(it, 9.0, 1.0, 1.0) })
                val remote = SpaceCompassCelestialRemoteData(ephemerides = mapOf(SpaceCompassCelestialBody.TITAN to titan))
                val g=calculateSpaceCompassCelestialViewGeometry(body,now,latitude,12.5,50.0,remote)!!
                orthonormal(g); assertTrue(g.distanceKm!! > 100_000)
                if (body != SpaceCompassCelestialBody.SUN) assertTrue(g.illuminatedFraction!! in 0.0..1.0)
            }
        }
    }
    @Test fun lunarNearSideUsesIauPrimeMeridianRatherThanArbitrarySpin() {
        val g=calculateSpaceCompassCelestialViewGeometry(SpaceCompassCelestialBody.MOON,now,41.9,12.5)!!
        val longitude=Math.toDegrees(atan2(g.bodyY.z,g.bodyX.z))
        assertTrue("Visible lunar longitude $longitude",abs(longitude)<12)
        assertTrue(g.bodyX.z > 0.85)
    }
    @Test fun phaseMatchesAstronomyEngineWithoutUsingThePhoneCompass() {
        for (day in 0..30) {
            val time=now+day*86_400_000L
            val g=calculateSpaceCompassCelestialViewGeometry(SpaceCompassCelestialBody.MOON,time,0.0,0.0)!!
            assertEquals(calculateSpaceCompassMoonPhase(time).illuminatedFraction,g.illuminatedFraction!!,0.02)
        }
    }
    @Test fun observerLocationChangesTheActualFaceOrientation() {
        val a=calculateSpaceCompassCelestialViewGeometry(SpaceCompassCelestialBody.MOON,now,41.9,12.5)!!
        val b=calculateSpaceCompassCelestialViewGeometry(SpaceCompassCelestialBody.MOON,now,-33.0,151.0)!!
        assertTrue(abs(a.bodyZ.x-b.bodyZ.x)+abs(a.bodyZ.y-b.bodyZ.y)>0.1)
    }
    @Test fun utcAndDaylightSavingCannotShiftTheRenderedFace() {
        val original=TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"))
            val a=calculateSpaceCompassCelestialViewGeometry(SpaceCompassCelestialBody.MARS,now,41.9,12.5)
            TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
            assertEquals(a,calculateSpaceCompassCelestialViewGeometry(SpaceCompassCelestialBody.MARS,now,41.9,12.5))
        } finally { TimeZone.setDefault(original) }
    }
    @Test fun dragStopsAnimationAndTapResumesOnOriginalAxis() {
        val initial=SpaceCompassCelestialRotation()
        val dragged=initial.drag(50.0,30.0,200.0)
        assertFalse(dragged.automatic); assertEquals(initial.yaw,dragged.yaw,1e-9)
        assertEquals(dragged,dragged.advance(0.1))
        assertTrue(dragged.resume().automatic)
        assertEquals(initial.geometry(), dragged.resume().geometry())
        assertNotEquals(dragged.yaw,dragged.resume().advance(0.1).yaw)
    }
    @Test fun rotationRemainsOrthonormalAtEveryDragAngle() {
        for (yaw in 0..360 step 10) for(pitch in -85..85 step 10)
            orthonormal(SpaceCompassCelestialRotation(yaw.toDouble(),pitch.toDouble()).geometry())
    }
    @Test fun returningUsesAContinuousShortestArcBeforeResumingCanonicalSpin() {
        val initial=SpaceCompassCelestialRotation(yaw=147.0,pitch=50.0)
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
        val q=SpaceCompassViewQuaternion.axisAngle(SpaceCompassViewVector(0.0,1.0,0.0),270.0)
        val opposite=SpaceCompassViewQuaternion(-q.w,-q.x,-q.y,-q.z)
        for(progress in listOf(0.1,0.5,0.9,1.0))
            assertEquals(q.returning(progress),opposite.returning(progress))
    }
    @Test fun unrestrictedDragIgnoresInvalidInput() {
        val state=SpaceCompassCelestialRotation().drag(50000.0,50000.0,1.0)
        orthonormal(state.geometry())
        assertEquals(state,state.drag(Double.NaN,1.0,10.0))
        assertEquals(state,state.drag(1.0,1.0,0.0))
        assertEquals(state,state.drag(Double.MAX_VALUE,1.0,Double.MIN_VALUE))
    }
    @Test fun visibleSurfaceFollowsFingerInBothScreenAxes() {
        val initial = SpaceCompassCelestialRotation(yaw = 127.0)
        val g = initial.geometry()
        val front = SpaceCompassViewVector(g.bodyX.z, g.bodyY.z, g.bodyZ.z)
        assertTrue(initial.drag(20.0,0.0,200.0).geometry().toCamera(front).x > 0)
        assertTrue(initial.drag(-20.0,0.0,200.0).geometry().toCamera(front).x < 0)
        // Camera Y is upwards; screen Y is downwards.
        assertTrue(initial.drag(0.0,20.0,200.0).geometry().toCamera(front).y < 0)
        assertTrue(initial.drag(0.0,-20.0,200.0).geometry().toCamera(front).y > 0)
    }
    @Test fun verticalDragCanPassBothPolesAndCompleteAFullTurn() {
        val initial = SpaceCompassCelestialRotation()
        val half = initial.drag(0.0,200.0,200.0).geometry()
        assertTrue(initial.geometry().bodyZ.dot(half.bodyZ) < -0.8)
        val full = initial.drag(0.0,400.0,200.0).geometry()
        assertEquals(1.0,initial.geometry().bodyZ.dot(full.bodyZ),1e-9)
        assertEquals(1.0,initial.geometry().bodyX.dot(full.bodyX),1e-9)
    }
    @Test fun repeatedMixedDragsStayOrthonormalAndResumeCanonicalSpacecraftAxis() {
        val initial = SpaceCompassCelestialRotation(yaw = 214.0, pitch = 50.0)
        var state = initial
        repeat(10_000) { state = state.drag(3.0,-5.0,300.0) }
        orthonormal(state.geometry())
        assertEquals(initial.geometry(),state.resume().geometry())
        assertEquals(50.0,state.resume().pitch,0.0)
    }
    @Test fun simulationRotationHonoursRetrogradeAndDoesNotJumpAfterPause() {
        assertEquals(1.2,SpaceCompassCelestialRotation().advance(100.0).yaw,1e-9)
        assertEquals(358.8,SpaceCompassCelestialRotation().advance(0.1,true).yaw,1e-9)
        assertEquals(SpaceCompassCelestialRotation(),SpaceCompassCelestialRotation().advance(-1.0))
    }
    @Test fun spacecraftAndUnresolvedBodiesDoNotInventActualAttitude() {
        listOf(SpaceCompassCelestialBody.ISS,SpaceCompassCelestialBody.STARLINK_V3,SpaceCompassCelestialBody.VOYAGER_1,SpaceCompassCelestialBody.VOYAGER_2,SpaceCompassCelestialBody.SEDNA)
            .forEach { assertNull(calculateSpaceCompassCelestialViewGeometry(it,now,41.9,12.5)) }
    }
    @Test fun referenceDataCoversAllObjectsAndUsesNullForUnavailableQuantities() {
        SpaceCompassCelestialBody.entries.forEach { body ->
            val f=spaceCompassCelestialFacts(body)
            listOf(f.diameterKm,f.massKg,f.gravity,f.density,f.revolutionDays,f.minimumParentKm,f.maximumParentKm)
                .filterNotNull().forEach { assertTrue(it.isFinite() && it>0) }
            if(f.minimumParentKm!=null) assertTrue(f.maximumParentKm!!>f.minimumParentKm)
            if(body.isSpacecraft) { assertNull(f.rotationHours); assertNull(f.gravity); assertNull(f.revolutionDays) }
        }
        assertNull(spaceCompassCelestialFacts(SpaceCompassCelestialBody.SEDNA).massKg)
        assertNull(spaceCompassCelestialFacts(SpaceCompassCelestialBody.SUN).parent)
        assertTrue(spaceCompassCelestialFacts(SpaceCompassCelestialBody.MOON).parentIsEarth)
        assertTrue(spaceCompassCelestialFacts(SpaceCompassCelestialBody.VENUS).rotationHours!!<0)
    }
    @Test fun spacecraftMeshesAreNonEmptyAndFinite() {
        for(body in listOf(SpaceCompassCelestialBody.ISS,SpaceCompassCelestialBody.STARLINK_V3,SpaceCompassCelestialBody.VOYAGER_1,SpaceCompassCelestialBody.VOYAGER_2)) {
            val mesh=spaceCompassCelestialCraftMesh(body); assertTrue(mesh.size>100)
            mesh.forEach { f -> listOf(f.a,f.b,f.c).forEach { p ->
                assertTrue(p.x.isFinite() && p.y.isFinite() && p.z.isFinite())
            } }
        }
    }
}

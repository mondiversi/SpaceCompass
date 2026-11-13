package me.mondiversi.spacecompass

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class SpaceCompassGlobeCoordinateAnchorsTest {
    @Test fun latitudesReachBothSidesOfTheTrueVisibleLimb() {
        val anchors=spaceCompassGlobeCoordinateAnchors(SpaceCompassGlobePose())
        for(degrees in -75..75 step 15) {
            val pair=anchors.filter { it.latitude&&it.degrees==degrees }.sortedBy { it.side }
            assertEquals(2,pair.size)
            pair.forEach { a ->
                assertEquals(sin(Math.toRadians(degrees.toDouble())),a.point.y,1e-10)
                assertEquals(0.0,a.point.z,1e-10);assertEquals(1.0,hypot(a.rim.x,a.rim.y),1e-10)
                assertEquals(a.side.toDouble(),sign(a.point.x),0.0)
            }
        }
    }
    @Test fun longitudeAnchorsStayOnTheEquatorAndIgnoreBackFaceCrossings() {
        val anchors=spaceCompassGlobeCoordinateAnchors(SpaceCompassGlobePose()).filterNot { it.latitude }
        assertEquals((-90..90 step 15).toSet(),anchors.map { it.degrees }.toSet())
        anchors.forEach { assertEquals(0.0,it.point.y,1e-10);assertTrue(it.point.z>=-1e-9) }
        assertEquals(0.0,anchors.single { it.degrees==0 }.point.x,1e-10)
    }
    @Test fun anchorsRemainGeographicallyTrueAfterTiltRotationAndRoll() {
        for(pose in listOf(SpaceCompassGlobePose(.7,.3,1.0,-.35),SpaceCompassGlobePose(-2.0,-.4,1.7,.6))) {
            spaceCompassGlobeCoordinateAnchors(pose).forEach { a ->
                val v=spaceCompassGlobeInverseView(a.point,pose)
                if(a.latitude) assertEquals(sin(Math.toRadians(a.degrees.toDouble())),v.y,1e-10)
                else {
                    assertEquals(0.0,v.y,1e-10)
                    val difference=Math.toDegrees(atan2(v.x,v.z))-a.degrees
                    assertEquals(0.0,atan2(sin(Math.toRadians(difference)),cos(Math.toRadians(difference))),1e-9)
                }
                assertTrue(a.point.z>=-1e-9)
            }
        }
    }
    @Test fun polarViewsUseLeadersFromVisibleParallelsRatherThanInventingLimbIntersections() {
        val pose=SpaceCompassGlobePose(0.0,PI/2)
        val anchors=spaceCompassGlobeCoordinateAnchors(pose)
        val north=anchors.filter { it.latitude&&it.degrees==60 }
        assertEquals(2,north.size)
        north.forEach { a ->
            assertTrue(a.point.z>.8);assertEquals(1.0,hypot(a.rim.x,a.rim.y),1e-10)
            assertEquals(60.0,Math.toDegrees(asin(spaceCompassGlobeInverseView(a.point,pose).y)),1e-7)
        }
        assertFalse(anchors.any { it.latitude&&it.degrees==-60 })
    }
    @Test fun visiblePolesUseTheirActualProjectedPositions() {
        for(pose in listOf(SpaceCompassGlobePose(),SpaceCompassGlobePose(.7,.3,1.0,-.35),
            SpaceCompassGlobePose(-2.0,-.4,1.7,.6),SpaceCompassGlobePose(0.0,PI/2))) {
            val poles=spaceCompassGlobeCoordinateAnchors(pose).filter { it.latitude&&abs(it.degrees)==90 }
            for(degrees in listOf(-90,90)) {
                val expected=spaceCompassGlobeView(SpaceCompassGlobeVector(0.0,degrees/90.0,0.0),pose)
                val actual=poles.singleOrNull { it.degrees==degrees }
                if(expected.z>=-1e-9) {
                    assertNotNull(actual);assertEquals(expected,actual!!.point);assertEquals(expected,actual.rim)
                    assertEquals(0,actual.side)
                } else assertNull(actual)
            }
        }
    }
    @Test fun everyFrontFifteenDegreeMeridianIsAnchoredToItsTrueEquatorialCrossing() {
        for(pose in listOf(SpaceCompassGlobePose(),SpaceCompassGlobePose(.41,.28,1.0,-.3))) {
            val anchors=spaceCompassGlobeCoordinateAnchors(pose).filterNot { it.latitude }
            for(degrees in -180 until 180 step 15) {
                val angle=Math.toRadians(degrees.toDouble())
                val expected=spaceCompassGlobeView(SpaceCompassGlobeVector(sin(angle),0.0,cos(angle)),pose)
                assertEquals(expected.z>=-1e-9,anchors.any { it.degrees==degrees })
            }
        }
    }

}
